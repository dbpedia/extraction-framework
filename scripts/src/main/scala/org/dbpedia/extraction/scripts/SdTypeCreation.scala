package org.dbpedia.extraction.scripts

import java.io.File

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.transform.Quad

import scala.Console._
import scala.collection._
import scala.collection.convert.decorateAsScala._
import java.util.concurrent.ConcurrentHashMap

import org.dbpedia.extraction.config.{Config, ConfigUtils}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.util._

import scala.collection.mutable.ListBuffer

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

  case class PredicateTypeScore(typ: String, predicate: String, predCount: Int, rawScore: Float, boostedScore: Float, normalizedScore: Float)

  private var resourceCount = 0
  private val type_count: concurrent.Map[String, ListBuffer[String]] = new ConcurrentHashMap[String, ListBuffer[String]]().asScala
  private val disambiguations: concurrent.Map[String, Int] = new ConcurrentHashMap[String, Int]().asScala
  private val typeStatistics = new ConcurrentHashMap[String, Float]().asScala
  private val predStatisticsIn = new ConcurrentHashMap[String, Int]().asScala
  private val predStatisticsOut = new ConcurrentHashMap[String, Int]().asScala
  private val stat_resource_predicate_tf_in: concurrent.Map[String, concurrent.Map[String, Int]] = new ConcurrentHashMap[String, concurrent.Map[String, Int]]().asScala   //Object -> Predicate -> count
  private val stat_resource_predicate_tf_out: concurrent.Map[String, concurrent.Map[String, Int]] = new ConcurrentHashMap[String, concurrent.Map[String, Int]]().asScala  //Subject -> Predicate -> count
  private val stat_type_predicate_perc_in: concurrent.Map[String, concurrent.Map[String, (Float, Float)]] = new ConcurrentHashMap[String, concurrent.Map[String, (Float, Float)]]().asScala //Predicate(in) -> Type -> count
  private val stat_type_predicate_perc_out: concurrent.Map[String, concurrent.Map[String, (Float, Float)]] = new ConcurrentHashMap[String, concurrent.Map[String, (Float, Float)]]().asScala//Predicate(out) -> Type -> count
  private val stat_predicate_weight_apriori: concurrent.Map[PredicateDirection.Value, concurrent.Map[String, Float]] = new ConcurrentHashMap[PredicateDirection.Value, concurrent.Map[String, Float]]().asScala                           //Predicate(out) -> Weight
  stat_predicate_weight_apriori.put(PredicateDirection.In, new ConcurrentHashMap[String, Float]().asScala)
  stat_predicate_weight_apriori.put(PredicateDirection.Out, new ConcurrentHashMap[String, Float]().asScala)
  private var propertyMap = new ConcurrentHashMap[String, OntologyProperty]().asScala
  private val contextMap = new ConcurrentHashMap[String, String]().asScala
  private val typeMap = new ConcurrentHashMap[String, String]().asScala

  private var suffix:String = _

  val dataset = DBpediaDatasets.SDInstanceTypes

  private var sdScoreThreshold:Float = 0f
  private var owlThingPenalty: Float = 0f

  private var inPropertiesExceptions: Seq[String] = _
  private var outPropertiesExceptions: Seq[String] = _

  private var classExceptions: Seq[String] = _

  private var returnAllValid: Boolean = false
  private var returnOnlyUntyped: Boolean = false
  private var compareToRealType: Boolean = false

  private var  finder: DateFinder[File] = _
  private var destination : Destination = _

  private var ontology: Ontology = _

  def clearAllMaps: Unit ={
    resourceCount = 0
    type_count.clear()
    disambiguations.clear()
    typeStatistics.clear()
    predStatisticsIn.clear()
    predStatisticsOut.clear()
    stat_resource_predicate_tf_in.clear()
    stat_resource_predicate_tf_out.clear()
    stat_type_predicate_perc_in.clear()
    stat_type_predicate_perc_out.clear()
    stat_predicate_weight_apriori.clear()
    stat_predicate_weight_apriori.put(PredicateDirection.In, new ConcurrentHashMap[String, Float]().asScala)
    stat_predicate_weight_apriori.put(PredicateDirection.Out, new ConcurrentHashMap[String, Float]().asScala)
    propertyMap.clear()
    contextMap.clear()
    typeMap.clear()
  }

  def getProperty(uri: String, ontology: Ontology) : Option[OntologyProperty] = {
    if (propertyMap.contains(uri)) {
      propertyMap.get(uri)
    } else {
      val predicateOpt = ontology.properties.find(x => x._2.uri == uri)
      val predicate: OntologyProperty =
        if (predicateOpt != null && predicateOpt.isDefined) { predicateOpt.get._2 }
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

  private def createScoreMap(targetClass: OntologyClass): Map[OntologyClass, Float] = {
    boosterScoreMaps.get(targetClass) match{
      case Some(m) => m
      case None =>
      {
        val scoreMap = new ConcurrentHashMap[OntologyClass, Float]()
        scoreMap.put(targetClass, 0f)
        val ontologyRootDistance = mapBaseClassesToDistanceFromThing(targetClass, scoreMap, 1)
        val step = (1f - owlThingPenalty) / ontologyRootDistance
        val retMap = scoreMap.asScala.map(x => x._1 -> (owlThingPenalty + (((x._2 - ontologyRootDistance) * (-1)) * step))) //calculate booster scores for the whole hierarchy
        retMap += (OntologyClass.owlThing -> owlThingPenalty)
        boosterScoreMaps += (targetClass -> retMap)
        retMap
      }
    }
  }

  def calculateDomainRangePenalty(targetClass: String, predicate: String, inout: PredicateDirection.Value, ontology: Ontology): Float = {
    val target = ontology.classes.find(x => x._2.uri == targetClass.trim) match {
      case Some(x ) => x._2
      case None => return 0f
    }
    val clas = getProperty(predicate, ontology) match{
      case Some(property) => if(inout == PredicateDirection.In) property.range.isInstanceOf[OntologyClass] match{
        case true => property.range.asInstanceOf[OntologyClass]
        case false => null.asInstanceOf[OntologyClass]
      } else property.domain
      case None => return 0f
    }
    typeDiffPenalty(target, clas)
  }

  def typeDiffPenalty(targetClass: String, actualClass: String, ontology: Ontology): Float = {
    val target = ontology.classes.find(x => x._2.uri == targetClass.trim) match {
      case Some(x ) => x._2
      case None => return 0f
    }
    val clas = ontology.classes.find(x => x._2.uri == actualClass.trim) match {
      case Some(x ) => x._2
      case None => return 0f
    }
    typeDiffPenalty(target, clas)
  }

  private def typeDiffPenalty(target: OntologyClass, clas: OntologyClass) = {
    val scoreMapTarget = createScoreMap(target)
    val scoreMapClass = createScoreMap(clas) //add owl:Thing
    scoreMapTarget.get(clas) match {
      case Some(booster) => booster
      case None => scoreMapClass.get(target) match {
        case Some(booster) => booster / 2f                //Target subclass of Clas -> is taxed twice as hard
        case None => 0f
      }
    }
  }

  private def calculateOneDirectionalScore(resource: String, results: concurrent.Map[String, ListBuffer[PredicateTypeScore]], inout: PredicateDirection.Value): Unit = {
    val precentageMap = inout match{
      case PredicateDirection.In => stat_type_predicate_perc_in
      case PredicateDirection.Out => stat_type_predicate_perc_out
    }
    (if(inout == PredicateDirection.In) stat_resource_predicate_tf_in else stat_resource_predicate_tf_out).get(resource) match {
      case Some(predicateMap) =>
        for (pred <- predicateMap) {
          val allResWithPred = precentageMap.get(pred._1).map(x => x.values.map(y => y._1).sum)
          allResWithPred match {
            case Some(allRes) =>
              for (typ <- precentageMap(pred._1)) {
                results.get(typ._1) match {
                  case Some(m) =>  m += claculateScores(resource, typ._1, typ._2._1, pred._1, pred._2, allRes, inout)
                  case None => {
                    val zw = new ListBuffer[PredicateTypeScore]()
                    zw += claculateScores(resource, typ._1, typ._2._1, pred._1, pred._2, allRes, inout)
                    results.put(typ._1, zw)
                  }
                }
              }
            case None =>
          }
      }
      case None =>
    }
  }

  def claculateScores(resource: String, typ: String, typeCount: Float, pred: String, predCount: Float, allResWithPred: Float, inout: PredicateDirection.Value): PredicateTypeScore ={
    val domainRangePanelty = calculateDomainRangePenalty(typ, pred, inout, ontology)
    val predReocurenceBooster = 2.5f - (1f/predCount*2)   // 1:0.5,2:1.5,3:1.833,4:2,5:2.1....
    val normFactor = getNormalizationFactor(resource)
    val zw = (typeCount / allResWithPred) * getAprioriDistribution(pred, inout)
    PredicateTypeScore(typ, pred, predCount.toInt, zw, zw*predReocurenceBooster*domainRangePanelty, zw*predReocurenceBooster*domainRangePanelty*normFactor)
  }

  private def getTypeScores(resource: String): List[(String, PredicateTypeScore)] = {
    val ret = new ConcurrentHashMap[String, ListBuffer[PredicateTypeScore]]().asScala
    calculateOneDirectionalScore(resource, ret, PredicateDirection.In)
    calculateOneDirectionalScore(resource, ret, PredicateDirection.Out)
    ret.map(x => x._1 ->
      PredicateTypeScore(x._1,
        "allPreds",
        x._2.map(_.predCount).sum,
        x._2.map(_.rawScore).sum,
        x._2.map(_.boostedScore).sum,
        x._2.map(_.normalizedScore).sum))
    .toList.sortBy[Float](_._2.normalizedScore).reverse
  }

  def getTypePropability(typ: String): Float = {
    if(resourceCount <= 0) throw new Exception("no resources found!")

    typeStatistics.get(typ) match {
      case Some(x)=> x
      case None => typeStatistics.put(typ, type_count(typ).length.toFloat)
    }
    typeStatistics(typ) / resourceCount.toFloat
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

  def saveAprioriDistributions(typ: String, pred: String, inout: PredicateDirection.Value): Unit ={
    val map = inout match{
      case PredicateDirection.In => predStatisticsIn
      case PredicateDirection.Out => predStatisticsOut
    }
    val writeMap = inout match{
      case PredicateDirection.In => stat_type_predicate_perc_in
      case PredicateDirection.Out => stat_type_predicate_perc_out
    }
    map.get(pred) match {
      case Some(resWithPred) => {
        //resources with predicate pred
        val count = (for (res <- type_count(typ)) yield getResourcePredicateCount(res, pred, inout)).sum         //count resources of type t with predicate pred
        val percentage = count.toFloat / resWithPred.toFloat
        val massFactor = 1f - (type_count(typ).length / resourceCount.toFloat)
        writeMap.get(pred) match {
          case Some(p) => {
            p.get(typ) match {
              case Some(c) => {
                p.put(typ, (c._1 + count.toFloat, c._2))
              }
              case None => p.put(typ, (count.toFloat, if(count == 0) 0f else Math.pow(getTypePropability(typ) - percentage , 2).toFloat * massFactor))              // saving percentage and wp
            }
            //addSuperTypeCounts(typ, count, p)
          }
          case None => {
            val zw : concurrent.Map[String, (Float, Float)] = new ConcurrentHashMap[String, (Float, Float)]().asScala
            val wp = Math.pow(percentage - getTypePropability(typ), 2).toFloat * massFactor
            zw.put(typ, (count.toFloat, if(count == 0) 0f else wp))                          // saving percentage and wp
            //addSuperTypeCounts(typ, count, zw)
            writeMap.put(pred, zw)
          }
        }
      }
      case None =>
    }
  }

  def addSuperTypeCounts(typ: String, count: Int, scoremap: concurrent.Map[String, (Float, Float)]) ={
    val ontoType = ontology.getOntologyClass(typ) match{
      case Some(c) => c
      case None => OntologyClass.owlThing
    }
    val scores = createScoreMap(ontoType)
    ontoType.superClasses.values.flatten.foreach(x => { if(!x.isExternalClass) {
      scoremap.get(x.uri) match {
        case Some(k) => scoremap.put(x.uri, (k._1 + (scores(x) * count), k._2))
        case None => scoremap.put(x.uri, (scores(x) * count, 0f))
      }
    }
    })
  }

  def getAprioriDistribution(predicate: String, inout: PredicateDirection.Value): Float ={
    val map = inout match{
      case PredicateDirection.In => stat_type_predicate_perc_in
      case PredicateDirection.Out => stat_type_predicate_perc_out
    }
    stat_predicate_weight_apriori(inout).get(predicate) match {
      case Some(v) => v
      case None => {
        val wp = map.get(predicate) match {
          case Some(x) => x.map(_._2._2).sum                                 //make sum over all types pointed out by a predicate
          case None => 0f
        }
        stat_predicate_weight_apriori(inout).put(predicate, wp)
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
    Math.max(1f / (0.1f + ret1 + ret2), 1f)   //this will ensure that the booster <= 10f and >= 1f
  }

  def typesWorker = SimpleWorkers(1.5, 1.0) { language: Language =>
    new QuadMapper().readQuads(finder, DBpediaDatasets.OntologyTypes.filenameEncoded + suffix, auto = true) { quad =>
      if(quad.value.trim.startsWith("http://dbpedia.org/ontology/")) { // TODO check if this is relevant
        //if (compareToRealType) //store to have the original type later
        typeMap.put(quad.subject, quad.value)
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
  }

  def workerDisamb = SimpleWorkers(1.5, 1.0) { language: Language =>
    finder.byName(DBpediaDatasets.DisambiguationLinks.filenameEncoded + suffix, auto = true) match{
      case Some(x) if x.exists() => new QuadMapper().readQuads(language, x) { quad =>
        disambiguations.put(quad.subject, 1)
      }
      case _ => Console.err.println("No disambiguation dataset found for language: " + language.wikiCode)
    }
  }

  def objectPropWorker = SimpleWorkers(1.5, 1.0) { language: Language =>
    new QuadMapper().readQuads(finder, DBpediaDatasets.OntologyPropertiesObjects.filenameEncoded + suffix, auto = true) { quad =>
      contextMap.get(quad.subject) match{
        case Some(l) =>
        case None => contextMap.put(quad.subject, quad.context)
      }
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
      predStatisticsIn.get(quad.predicate) match {
        case Some(c) => predStatisticsIn.put(quad.predicate, c + 1)
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
      predStatisticsOut.get(quad.predicate) match {
        case Some(c) => predStatisticsOut.put(quad.predicate, c + 1)
        case None => predStatisticsOut.put(quad.predicate, 1)
      }
      contextMap.get(quad.subject) match {
        case Some(l) =>
        case None => contextMap.put(quad.subject, quad.context)
      }
    }
  }

  def literalWorker = SimpleWorkers(1.5, 1.0) { language: Language =>
    new QuadMapper().readQuads(finder, DBpediaDatasets.OntologyPropertiesLiterals.filenameEncoded + suffix, auto = true) { quad =>
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

  def probabilityWorker = SimpleWorkers(1.5, 1.0) { pred: String =>
    for (typ <- type_count) //all types
    {
      saveAprioriDistributions(typ._1, pred, PredicateDirection.In)
      saveAprioriDistributions(typ._1, pred, PredicateDirection.Out)
    }
  }

  def resultCalculator = SimpleWorkers(1.5, 5.0) { tuple: (List[String], Language) =>
    val zw = new ListBuffer[Quad]()
    for(resource <- tuple._1) {
      val newType = getTypeScores(resource)
      val current = newType.head._2
      var read = true
      while (read && !classExceptions.contains(current.typ) && current.normalizedScore >= sdScoreThreshold) {
        //compare to score threshold
        zw += new Quad(
          language = tuple._2,
          dataset = dataset,
          subject = resource,
          predicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
          value = current.typ,
          context = (contextMap.get(resource) match{
              case Some (c) => if(c.contains('#')) c.substring(0, c.indexOf('#')) else c
              case None => resource + "?nowikientry=linktarget"}) +
            "#typeCalculatedBy=sdTypeAlgorithm&sdTypeScore=" + (if (current.normalizedScore > 1f) 1f else current.normalizedScore) +
            "&sdTypeBasedOn=" + current.predCount,
          datatype = null)

        if(compareToRealType)
          typeMap.get(resource) match{
            case Some(x) => zw += new Quad(
              language = tuple._2,
              dataset = dataset,
              subject = resource,
              predicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
              value = x,
              context = (contextMap.get(resource) match{
                case Some (c) => if(c.contains('#')) c.substring(0, c.indexOf('#')) else c
                case None => resource + "?nowikientry=linktarget"}) +
                "#typeCalculatedBy=sdTypeDiffPenalty=" + typeDiffPenalty(current.typ, x, ontology),
              datatype = null)
            case None =>
          }

        read = returnAllValid
      }
    }
    destination.write(zw)
  }

  def main(args: Array[String]): Unit = {

    require(args != null && args.length == 1, "One arguments required, extraction config file")

    val config = new Config(args(0))

    val baseDir = config.dumpDir
    require(baseDir.isDirectory && baseDir.canRead && baseDir.canWrite, "Please specify a valid local base extraction directory - invalid path: " + baseDir)

    suffix = config.inputSuffix match{
      case Some(x) if x.startsWith(".") => x
      case _ => throw new IllegalArgumentException("Please specify a valid file extension starting with a '.'!")
    }
    require("\\.[a-zA-Z0-9]{2,3}\\.(gz|bz2)".r.replaceFirstIn(suffix, "") == "", "provide a valid serialization extension starting with a dot (e.g. .ttl.bz2)")

    sdScoreThreshold =  ConfigUtils.getString(config, "threshold", required=true).toFloat
    require(sdScoreThreshold >= 0.01f && sdScoreThreshold <= 0.99f, "Please specify a valid sdTypes score in the range of [0.01, 0.99].")

    owlThingPenalty =  ConfigUtils.getString(config, "owl-thing-penalty", required=true).toFloat
    require(owlThingPenalty >= 0.01f && owlThingPenalty <= 0.99f, "Please specify a valid owlThingPenalty score in the range of [0.01, 0.99].")

    inPropertiesExceptions = ConfigUtils.getValues(config, "in-properties-exceptions",",")(x => x)
    outPropertiesExceptions = ConfigUtils.getValues(config, "out-properties-exceptions",",")(x => x)

    classExceptions = ConfigUtils.getValues(config, "class-exceptions",",")(x => x)

    returnAllValid = ConfigUtils.getString(config, "return-all-valid-types").toBoolean
    returnOnlyUntyped = ConfigUtils.getString(config, "return-only-untyped").toBoolean
    compareToRealType = ConfigUtils.getString(config, "compare-to-original-type").toBoolean

    ontology = {
      val ontologySource = ConfigUtils.getValue(config, "ontology")(new File(_))
      new OntologyReader().read( XMLSource.fromFile(ontologySource, Language.Mappings))
    }

    val policies = parsePolicies(config, "uri-policy")
    val formats = parseFormats(config, "format", policies).map( x=>
      x._1 -> (if(x._2.isInstanceOf[TerseFormatter]) new QuadMapperFormatter(x._2.asInstanceOf[TerseFormatter]) else x._2)).toMap

    for(language <- config.languages) {
      clearAllMaps
      finder = new DateFinder(baseDir, language)
      finder.byName("instance-types" + suffix, auto = true) //work around to set date of finder
      destination = DestinationUtils.createDatasetDestination(finder, Array(dataset.getLanguageVersion(language, config.dbPediaVersion).get), formats)

      //read all input files and process the content
      Workers.workInParallel[Language](Array(typesWorker, objectPropWorker, workerDisamb, literalWorker), List(language))

      //delete properties exempted by the user
      outPropertiesExceptions.map(x => predStatisticsOut.remove(x))
      inPropertiesExceptions.map(x => predStatisticsIn.remove(x))

      //count unique resources
      resourceCount = (stat_resource_predicate_tf_in.keys.toList ::: stat_resource_predicate_tf_out.keys.toList).distinct.length
      //get all predicates
      val allPreds = (predStatisticsIn.keys.toList ::: predStatisticsOut.keys.toList).distinct

      //run the intermediate statistical calculations
      Workers.work[String](probabilityWorker, allPreds, language.wikiCode + ": Type statistics calculation")

      //do the type calculations and write to files(s)
      val baseuri = if (language == Language.English) "http://dbpedia.org/resource/" else language.dbpediaUri
      val allResources = returnOnlyUntyped match {
        case true => (stat_resource_predicate_tf_in.keySet.toList ::: stat_resource_predicate_tf_out.keySet.toList)
          .filter(x => x.startsWith(baseuri))
          .diff[String](typeMap.keys.toSeq)
          .diff[String](disambiguations.keys.toSeq)
          .distinct
        case false => (stat_resource_predicate_tf_in.keySet.toList ::: stat_resource_predicate_tf_out.keySet.toList)
          .filter(x => x.startsWith(baseuri))
          .distinct
      }
      //resultMap = new ConcurrentHashMap[String, List[Quad]]((allResources.size / 0.75f + 1).toInt).asScala //setting initial size for performance reasons

      //write results to file
      err.println(language.wikiCode + ": Starting to calculate new types for " + allResources.size + " instances.")
      destination.open()
      Workers.work[(List[String], Language)](resultCalculator, allResources.grouped(100).map(x => (x, language)).toList, language.wikiCode + ": New type statements calculation")
      destination.close()
    }
  }
}