package org.dbpedia.extraction.scripts

import java.io.{Writer, File}

import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.ontology.{OntologyClass, OntologyProperty, Ontology}
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.scripts.QuadMapper.QuadMapperFormatter
import org.dbpedia.extraction.sources.{XMLSource}

import scala.Console._
import scala.collection._
import scala.collection.convert.decorateAsScala._
import java.util.concurrent.ConcurrentHashMap

import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.util._

import scala.collection.mutable.{HashMap, ArrayBuffer, ListBuffer}

/**
  * Created by Chile on 9/1/2016.
  * Calculates types for untyped resources following Heiko Paulheims SD-Type algorithm
  * (http://www.heikopaulheim.com/docs/iswc2013.pdf)
  * input files:
  * - instance-types
  * - instance-types-transitive
  * - disambiguations-unredirected
  * - mappingbased-objects-uncleaned
  *
  */
object SdTypeCreation {

  object PredicateDirection extends Enumeration {
    val In, Out = Value
  }
  var resourceCount = 0
  val type_count: concurrent.Map[String, ListBuffer[String]] = new ConcurrentHashMap[String, ListBuffer[String]]().asScala
  val disambiguations: concurrent.Map[String, Int] = new ConcurrentHashMap[String, Int]().asScala
  val typeStatistics = new ConcurrentHashMap[String, Float]().asScala
  val predStatisticsIn = new ConcurrentHashMap[String, Int]().asScala
  val predStatisticsOut = new ConcurrentHashMap[String, Int]().asScala
  val stat_resource_predicate_tf_in: concurrent.Map[String, concurrent.Map[String, Int]] = new ConcurrentHashMap[String, concurrent.Map[String, Int]]().asScala   //Object -> Predicate -> count
  val stat_resource_predicate_tf_out: concurrent.Map[String, concurrent.Map[String, Int]] = new ConcurrentHashMap[String, concurrent.Map[String, Int]]().asScala  //Subject -> Predicate -> count
  val stat_type_predicate_perc_in: concurrent.Map[String, concurrent.Map[String, (Float, Float)]] = new ConcurrentHashMap[String, concurrent.Map[String, (Float, Float)]]().asScala //Predicate(in) -> Type -> count
  val stat_type_predicate_perc_out: concurrent.Map[String, concurrent.Map[String, (Float, Float)]] = new ConcurrentHashMap[String, concurrent.Map[String, (Float, Float)]]().asScala//Predicate(out) -> Type -> count
  val stat_predicate_weight_apriori: concurrent.Map[PredicateDirection.Value, concurrent.Map[String, Float]] = new ConcurrentHashMap[PredicateDirection.Value, concurrent.Map[String, Float]]().asScala                           //Predicate(out) -> Weight
  stat_predicate_weight_apriori.put(PredicateDirection.In, new ConcurrentHashMap[String, Float]().asScala)
  stat_predicate_weight_apriori.put(PredicateDirection.Out, new ConcurrentHashMap[String, Float]().asScala)
  var propertyMap = Map[String, OntologyProperty]()




  def getProperty(uri: String, ontology: Ontology) : Option[OntologyProperty] = {
    if (propertyMap.contains(uri)) {
      propertyMap.get(uri)
    } else {
      val predicateOpt = ontology.properties.find(x => x._2.uri == uri)
      val predicate: OntologyProperty =
        if (predicateOpt != null && predicateOpt != None) { predicateOpt.get._2 }
        else null
      propertyMap += (uri -> predicate)
      Option(predicate)
    }
  }

  private def mapBaseClassesToDistanceFromThing(currentClass: OntologyClass, scoreMap: ConcurrentHashMap[OntologyClass, Float], currentDistance: Int): Int ={
    var ret = currentDistance
    for(clas <- currentClass.baseClasses if !clas.isExternalClass && clas.baseClasses.nonEmpty)
      {
        scoreMap.put(clas, currentDistance)
        ret = Math.max(mapBaseClassesToDistanceFromThing(clas, scoreMap, currentDistance+1), ret)
      }
    ret
  }

  var boosterScoreMaps = Map[OntologyClass, Map[OntologyClass, Float]]()
  val thingScore = 0.5f                                                           //TODO make this configurable

  private def createScoreMap(targetClass: OntologyClass): Map[OntologyClass, Float] = {
    boosterScoreMaps.get(targetClass) match{
      case Some(m) => m
      case None =>
      {
        val scoreMap = new ConcurrentHashMap[OntologyClass, Float]()
        scoreMap.put(targetClass, 0f)
        val ontologyRootDistance = mapBaseClassesToDistanceFromThing(targetClass, scoreMap, 1)
        val step = (1f - thingScore) / ontologyRootDistance
        val retMap = scoreMap.asScala.map(x => x._1 -> (thingScore + (((x._2 - ontologyRootDistance) * (-1)) * step))) //calculate booster scores for the whole hierarchy
        retMap += (OntologyClass.owlThing -> thingScore)
        boosterScoreMaps += (targetClass -> retMap)
        retMap
      }
    }
  }

  def calculateDomainRangeBooster(targetClass: String, predicate: String, inout: PredicateDirection.Value, ontology: Ontology): Float = {
    val target = ontology.classes.find(x => x._2.uri == targetClass.trim) match {
      case Some(x ) => x._2
      case None => return 0f
    }
    val clas = getProperty(predicate, ontology) match{
      case Some(property) => if(inout == PredicateDirection.In) property.range else property.domain
      case None => return 0f
    }
    clas.isInstanceOf[OntologyClass] match{
      case true => {
        val scoreMapTarget = createScoreMap(target)
        val scoreMapClass = createScoreMap(clas.asInstanceOf[OntologyClass])     //add owl:Thing
        scoreMapTarget.get(clas.asInstanceOf[OntologyClass]) match{
          case Some(booster) => booster
          case None => scoreMapClass.get(target) match {
            case Some(booster) => booster/2                       //Target subclass of Clas -> is taxed twice as hard
            case None => 0f
          }
        }
      }
      case false => 0f
    }
  }

  private def createDestination(finder: DateFinder[File], formats: scala.collection.Map[String, Formatter], datasets: Dataset*) : Destination = {
    val destination = new ArrayBuffer[Destination]()
    for ((suffix, format) <- formats) {
      val datasetDestinations = new HashMap[String, Destination]()
      for (dataset <- datasets) {
        val file = finder.byName(dataset.name.replace('_', '-') + suffix)
        datasetDestinations(dataset.name) = new WriterDestination(writer(file), format)
      }
      destination += new DatasetDestination(datasetDestinations)
    }
    new CompositeDestination(destination.toSeq: _*)
  }

  private def writer(file: File): () => Writer = {
    () => IOUtils.writer(file)
  }

  def main(args: Array[String]): Unit = {

    require(args != null && args.length == 1, "One arguments required, extraction config file")
/*    require(args != null && args.length >= 5,
      "need at least seven args: " +
        /*0*/ "base dir, " +
        /*1*/ "sdTypes output file name, " +
        /*2*/ "sdInvalid output file name, " +
        /*3*/ "file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
        /*4*/ "sdType threshold as float (e.g. 0.4f)" +
        /*5*/ "dbpedia ontology file path" +
        /*6*/ "fitting domain/range multiplicator for further weighting exactly fitting domains and ranges of properties as float (e.g. 3.3f)" +
        /*7*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')")*/

    val config = ConfigUtils.loadConfig(args(0), "UTF-8")

    val baseDir = ConfigUtils.getValue(config, "base-dir", required=true)(new File(_))
    require(baseDir.isDirectory() && baseDir.canRead() && baseDir.canWrite(), "Please specify a valid local base extraction directory!")

    val suffix = ConfigUtils.getValue(config, "suffix", required=true)(x => x)
    require(suffix.startsWith("."), "Please specify a valid file extension starting with a '.'!")
    require("\\.[a-zA-Z0-9]{2,3}\\.(gz|bz2)".r.replaceFirstIn(suffix, "") == "", "provide a valid serialization extension starting with a dot (e.g. .ttl.bz2)")

    //require(!args(1).contains("."), "Please specify a valid sdTypes file name without extensions (suffixes)!")
    val dataset = ConfigUtils.getValue(config, "output", required=true)(x => x)
    val sdTypes = new File(dataset + suffix)
    sdTypes.createNewFile()

/*    require(!args(2).contains("."), "Please specify a valid sdInvalid file name without extensions (suffixes)!")
    val sdInvalid = new File(baseDir + args(2).trim + suffix)
    sdInvalid.createNewFile()*/

    val sdScoreThreshold =  ConfigUtils.getValue(config, "threshold", required=true)(x => x.toFloat)
    require(sdScoreThreshold >= 0.01f && sdScoreThreshold <= 0.99f, "Please specify a valid sdTypes score in the range of [0.01, 0.99].")

    val owlThingPenalty =  ConfigUtils.getValue(config, "owl-thing-penalty", required=true)(x => x.toFloat)
    require(owlThingPenalty >= 0.01f && owlThingPenalty <= 0.99f, "Please specify a valid owlThingPenalty score in the range of [0.01, 0.99].")

    val langConfString = ConfigUtils.getString(config, "languages", true)
    val language = ConfigUtils.parseLanguages(baseDir, langConfString.split(","))(0) //TODO

    val finder = new DateFinder(baseDir, language)
    finder.byName("instance-types" + suffix, auto = true)   //work around to set date of finder

    val ontology = {
      val ontologySource = ConfigUtils.getValue(config, "ontology", false)(new File(_))
      new OntologyReader().read( XMLSource.fromFile(ontologySource, Language.Mappings))
    }

    val formatter = new QuadMapperFormatter()
    val destination = new WriterDestination(writer(finder.byName(sdTypes.name)), formatter)

   def getTypeScores(resource: String): List[(String, Float, Int, Float)] ={
      val ret = new ConcurrentHashMap[String, ListBuffer[(String, Float, Float, Int)]]().asScala
      stat_resource_predicate_tf_in.get(resource) match{
        case Some(map) => for(pred <- map){
          val allResWithPred = stat_type_predicate_perc_in.get(pred._1).map(x => x.values.map(y => y._1).sum)
          allResWithPred match {
            case Some (allRes) =>
              for(typ <- stat_type_predicate_perc_in.get(pred._1).get) {
                val booster = calculateDomainRangeBooster(typ._1, pred._1, PredicateDirection.In, ontology)
                ret.get(typ._1) match {
                  case Some(m) => {
                    val zw = (typ._2._1 / allRes) * getAprioriDistribution(pred._1, PredicateDirection.In)
                    m += ((pred._1, zw*booster*pred._2, zw*booster, pred._2))
                  }
                  case None =>
                  {
                    val zw = new ListBuffer[(String, Float, Float, Int)]()
                    zw += ((pred._1, (typ._2._1 / allRes) * getAprioriDistribution(pred._1, PredicateDirection.In) * booster * pred._2,
                      (typ._2._1 / allRes) * getAprioriDistribution(pred._1, PredicateDirection.In) * booster, pred._2))
                    ret.put(typ._1, zw)
                  }
                }
              }
            case None =>
          }
        }
        case None =>
      }
      stat_resource_predicate_tf_out.get(resource) match{
        case Some(map) => for(pred <- map){
          val allResWithPred = stat_type_predicate_perc_out.get(pred._1).map(x => x.values.map(y => y._1).sum)
          allResWithPred match {
            case Some(allRes) =>
              for (typ <- stat_type_predicate_perc_out.get(pred._1).get) {
                val booster = calculateDomainRangeBooster(typ._1, pred._1, PredicateDirection.Out, ontology)
                ret.get(typ._1) match {
                  case Some(m) => {
                    val zw = (typ._2._1 / allRes) * getAprioriDistribution(pred._1, PredicateDirection.Out)
                    m += ((pred._1, zw*booster*pred._2, zw*booster, pred._2))
                  }
                  case None =>
                  {
                    val zw = new ListBuffer[(String, Float, Float, Int)]()
                    zw += ((pred._1, (typ._2._1 / allRes) * getAprioriDistribution(pred._1, PredicateDirection.Out) * booster * pred._2,
                      (typ._2._1 / allRes) * getAprioriDistribution(pred._1, PredicateDirection.Out) * booster, pred._2))
                    ret.put(typ._1, zw)
                  }
                }
              }
            case None =>
          }
        }
        case None =>
      }
      val normFactor = getNormalizationFactor(resource)
      ret.map(x => (x._1, x._2.map(_._2).sum * normFactor, x._2.map(_._4).sum, x._2.map(_._3).sum * normFactor)).toList.sortBy[Float](_._2).reverse
    }

    def getTypePropability(typ: String): Float = {
      if(resourceCount <= 0) throw new Exception("no resources found!")

      typeStatistics.get(typ) match {
        case Some(x)=> x
        case None => typeStatistics.put(typ, type_count(typ).length.toFloat)
      }
      typeStatistics.get(typ).get / resourceCount.toFloat
    }

    def getResourcePredicateCount(res: String, pred: String, inout: PredicateDirection.Value): Int = {
      val map = inout match{
        case PredicateDirection.In => stat_resource_predicate_tf_in
        case PredicateDirection.Out => stat_resource_predicate_tf_out
      }
      map.get(res) match{
        case Some(m) => m.get(pred) match{
          case Some(v) => return v
          case None =>
        }
        case None =>
      }
      0
    }

    def saveAprioriDistributions(typ: (String, List[String]), pred: String, inout: PredicateDirection.Value): Unit ={
      val map = inout match{
        case PredicateDirection.In => predStatisticsIn
        case PredicateDirection.Out => predStatisticsOut
      }
      val writeMap = inout match{
        case PredicateDirection.In => stat_type_predicate_perc_in
        case PredicateDirection.Out => stat_type_predicate_perc_out
      }
      map.get(pred) match {
        case Some(resWithPred) => {                                                                                               //resources with predicate pred
        val count = (for (res <- typ._2) yield getResourcePredicateCount(res, pred, inout)).sum                                 //count resources of type t with predicate pred
        val percentage = count.toFloat / resWithPred.toFloat
        val massFactor = 1f - (type_count.get(typ._1).get.length / resourceCount.toFloat)
          writeMap.get(pred) match {
            case Some(p) => p.get(typ._1) match {
              case Some(c) => throw new Exception("this should be unique!")
              case None => p.put(typ._1, (count.toFloat, if(count == 0) 0f else Math.pow(getTypePropability(typ._1) - percentage , 2).toFloat * massFactor))              // saving percentage and wp
            }
            case None => {
              val zw = new ConcurrentHashMap[String, (Float, Float)]().asScala
              val wp = Math.pow(percentage - getTypePropability(typ._1), 2).toFloat * massFactor
              zw.put(typ._1, (count.toFloat, if(count == 0) 0f else wp))                          // saving percentage and wp
              writeMap.put(pred, zw)
            }
          }
        }
        case None =>
      }
    }

    def getAprioriDistribution(predicate: String, inout: PredicateDirection.Value): Float ={
      val map = inout match{
        case PredicateDirection.In => stat_type_predicate_perc_in
        case PredicateDirection.Out => stat_type_predicate_perc_out
      }
      stat_predicate_weight_apriori.get(inout).get.get(predicate) match {
        case Some(v) => v
        case None => {
          val wp = map.get(predicate) match {
            case Some(x) => x.map(_._2._2).sum                                 //make sum over all types pointed out by a predicate
            case None => 0f
          }
          stat_predicate_weight_apriori.get(inout).get.put(predicate, wp)
          wp
        }
      }
    }

    def getNormalizationFactor(resource: String): Float ={
      val ret1 = stat_resource_predicate_tf_in.get(resource) match{
        case Some(map) => (for( pred <- map.keys) yield getAprioriDistribution(pred, PredicateDirection.In)).sum
        case None => 0f
      }
      val ret2 = stat_resource_predicate_tf_out.get(resource) match{
        case Some(map) => (for( pred <- map.keys) yield getAprioriDistribution(pred, PredicateDirection.Out)).sum
        case None => 0f
      }
      1f / (ret1 + ret2)
    }

    val typesWorker = SimpleWorkers(1.5, 1.0) { language: Language =>
        QuadReader.readQuads(finder, "instance-types" + suffix, auto = true) { quad =>
        if(quad.value.trim.startsWith("http://dbpedia.org/ontology/")) // TODO check if this is relevant
          type_count.get(quad.value) match {
            case Some(list) => type_count.put(quad.value, list += quad.subject)
            case None => {
              val buff = new ListBuffer[String]()
              buff += quad.subject
              type_count.put(quad.value, buff)
            }
          }
      }
    }

    val workerDisamb = SimpleWorkers(1.5, 1.0) { language: Language =>
      QuadReader.readQuads(finder, "disambiguations-unredirected" + suffix, auto = true) { quad =>
        disambiguations.put(quad.subject, 1)
      }
    }

    val objectPropWorker = SimpleWorkers(1.5, 1.0) { language: Language =>
      QuadReader.readQuads(finder, "mappingbased-objects-uncleaned" + suffix, auto = true) { quad =>
        stat_resource_predicate_tf_in.get(quad.value) match {
          case Some(m) => m.get(quad.predicate) match {
            case Some(c) =>
              m.put(quad.predicate, c + 1)
            case None => m.put(quad.predicate, 1)
          }
          case None => {
            val zw = new ConcurrentHashMap[String, Int]().asScala
            zw.put(quad.predicate, 1)
            stat_resource_predicate_tf_in.put(quad.value, zw)
          }
        }
        predStatisticsIn.get(quad.predicate) match{
          case Some(c) => predStatisticsIn.put(quad.predicate, c +1)
          case None => predStatisticsIn.put(quad.predicate, 1)
        }
        stat_resource_predicate_tf_out.get(quad.subject) match {
          case Some(m) => m.get(quad.predicate) match {
            case Some(c) =>
              m.put(quad.predicate, c + 1)
            case None =>
              m.put(quad.predicate, 1)
          }
          case None => {
            val zw = new ConcurrentHashMap[String, Int]().asScala
            zw.put(quad.predicate, 1)
            stat_resource_predicate_tf_out.put(quad.subject, zw)
          }
        }
        predStatisticsOut.get(quad.predicate) match{
          case Some(c) => predStatisticsOut.put(quad.predicate, c +1)
          case None => predStatisticsOut.put(quad.predicate, 1)
        }
      }
    }

    val literalWorker = SimpleWorkers(1.5, 1.0) { language: Language =>
      QuadReader.readQuads(finder, "mappingbased-literals" + suffix, auto = true) { quad =>
        stat_resource_predicate_tf_out.get(quad.subject) match {
          case Some(m) => m.get(quad.predicate) match {
            case Some(c) =>
              m.put(quad.predicate, c + 1)
            case None =>
              m.put(quad.predicate, 1)
          }
          case None => {
            val zw = new ConcurrentHashMap[String, Int]().asScala
            zw.put(quad.predicate, 1)
            stat_resource_predicate_tf_out.put(quad.subject, zw)
          }
        }
        predStatisticsOut.get(quad.predicate) match {
          case Some(c) => predStatisticsOut.put(quad.predicate, c + 1)
          case None => predStatisticsOut.put(quad.predicate, 1)
        }
      }
    }

    val probabilityWorker = SimpleWorkers(1.5, 1.0) { pred: String =>
        for (typ <- type_count) //all types
        {
          saveAprioriDistributions((typ._1, typ._2.toList), pred, PredicateDirection.In)
          saveAprioriDistributions((typ._1, typ._2.toList), pred, PredicateDirection.Out)
        }
    }

    val quadMappingWorker = SimpleWorkers(1.5, 1.0) { resource: String =>
        val newType = getTypeScores(resource)
        if (newType.nonEmpty && newType.head._2 >= sdScoreThreshold) {
          //newType.head._3 > 2 && //TODO
          //compare to score threshold
          destination.write(List(new Quad(
            language = language.wikiCode,
            dataset = dataset.trim,
            subject = resource,
            predicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
            value = newType.head._1,
            context = resource + "#typeCalculatedBy=sdTypeAlgorithm&sdTypeScore=" + (if(newType.head._2 > 1f) 1f else newType.head._2) + "&sdTypeBasedOn=" + newType.head._3,
            datatype = null)
          ))
        }
    }

    val mappingWorker = SimpleWorkers(1.5, 1.0) { language: Language =>

      err.println("Starting to write " + dataset.trim + suffix)
      destination.open()
      val allResources = (stat_resource_predicate_tf_in.keySet.toList ::: stat_resource_predicate_tf_out.keySet.toList).filter(x => x.startsWith("http://de.dbpedia.org/resource/")).distinct
      Workers.work[String](quadMappingWorker, allResources, "New type statements written")
      destination.close()
    }

    Workers.workInParallel[Language](Array(typesWorker, objectPropWorker, workerDisamb,literalWorker), Seq(language))

    predStatisticsOut.remove("http://dbpedia.org/ontology/wikiPageOutDegree")
    predStatisticsOut.remove("http://dbpedia.org/ontology/wikiPageID")
    predStatisticsOut.remove("http://dbpedia.org/ontology/individualisedGnd")
    predStatisticsOut.remove("http://dbpedia.org/ontology/viafId")
    predStatisticsOut.remove("http://dbpedia.org/ontology/wikiPageRevisionID")
    predStatisticsOut.remove("http://www.w3.org/2002/07/owl#sameAs")
    predStatisticsOut.remove("http://dbpedia.org/ontology/lccn")
    predStatisticsIn.remove("http://www.w3.org/2002/07/owl#sameAs")
    resourceCount = (stat_resource_predicate_tf_in.keys.toList ::: stat_resource_predicate_tf_out.keys.toList).distinct.length

    val allPreds = (predStatisticsIn.keys.toList ::: predStatisticsOut.keys.toList).distinct

    Workers.work[String](probabilityWorker, allPreds, "Type statistics calculation")
    Workers.work[Language](mappingWorker, Seq(language))
  }
}