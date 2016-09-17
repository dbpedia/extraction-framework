package org.dbpedia.extraction.scripts

import java.io.File
import java.util.concurrent.atomic.AtomicLong

import scala.collection._
import scala.collection.convert.decorateAsScala._
import java.util.concurrent.ConcurrentHashMap

import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util._

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

  def main(args: Array[String]): Unit = {

    require(args != null && args.length >= 5,
      "need at least seven args: " +
        /*0*/ "base dir, " +
        /*1*/ "sdTypes output file name, " +
        /*2*/ "sdInvalid output file name, " +
        /*3*/ "file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
        /*4*/ "sdType threshold as float (e.g. 0.4f)" +
        /*5*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')")

    val baseDir = new File(args(0))
    require(baseDir.isDirectory() && baseDir.canRead() && baseDir.canWrite(), "Please specify a valid local base extraction directory!")

    require("\\.[a-zA-Z0-9]{2,3}\\.(gz|bz2)".r.replaceFirstIn(args(3).trim, "") == "", "provide a valid serialization extension starting with a dot (e.g. .ttl.bz2)")

    require(!args(1).contains("."), "Please specify a valid sdTypes file name without extensions (suffixes)!")
    val sdTypes = new File(baseDir + args(1).trim + args(3).trim)
    sdTypes.createNewFile()

    require(!args(2).contains("."), "Please specify a valid sdInvalid file name without extensions (suffixes)!")
    val sdInvalid = new File(baseDir + args(2).trim + args(3).trim)
    sdInvalid.createNewFile()

    val sdScoreThreshold =  args(4).toFloat
    require(sdScoreThreshold >= 0.01f && sdScoreThreshold <= 0.99f, "Please specify a valid sdTypes score in the range of [0.01, 0.99].")

    val language = ConfigUtils.parseLanguages(baseDir, args(5).split(","))(0) //TODO

    var resourceCount = 0
    val type_count: concurrent.Map[String, List[String]] = new ConcurrentHashMap[String, List[String]]().asScala
    val disambiguations: concurrent.Map[String, Int] = new ConcurrentHashMap[String, Int]().asScala
    val typeStatistics = new ConcurrentHashMap[String, Float]().asScala
    //var stat_subjects: concurrent.Map[String, Int] = new ConcurrentHashMap[String, Int]().asScala
    //var stat_objects: concurrent.Map[String, Int] = new ConcurrentHashMap[String, Int]().asScala
    //var stat_property: concurrent.Map[String, Int] = new ConcurrentHashMap[String, Int]().asScala
    val stat_resource_predicate_tf_in: concurrent.Map[String, concurrent.Map[String, Int]] = new ConcurrentHashMap[String, concurrent.Map[String, Int]]().asScala   //Object -> Predicate -> count
    val stat_resource_predicate_tf_out: concurrent.Map[String, concurrent.Map[String, Int]] = new ConcurrentHashMap[String, concurrent.Map[String, Int]]().asScala  //Subject -> Predicate -> count
    val stat_type_predicate_perc_in: concurrent.Map[String, concurrent.Map[String, (Float, Float)]] = new ConcurrentHashMap[String, concurrent.Map[String, (Float, Float)]]().asScala //Predicate(in) -> Type -> count
    val stat_type_predicate_perc_out: concurrent.Map[String, concurrent.Map[String, (Float, Float)]] = new ConcurrentHashMap[String, concurrent.Map[String, (Float, Float)]]().asScala//Predicate(out) -> Type -> count
    val stat_predicate_weight_apriori_in: concurrent.Map[String, Float] = new ConcurrentHashMap[String, Float]().asScala                          //Predicate(in) -> Weight
    val stat_predicate_weight_apriori_out: concurrent.Map[String, Float] = new ConcurrentHashMap[String, Float]().asScala                         //Predicate(out) -> Weight



    //(in, out) : Type -> Predicate -> (tf,percentage,weight)
    def getTypeScores(quad: Quad): Map[String, Float] ={
      val in =  new ConcurrentHashMap[String, Map[String, (Float, Float, Float)]]().asScala
      val out =  new ConcurrentHashMap[String, Map[String, (Float, Float, Float)]]().asScala
      stat_resource_predicate_tf_in.get(quad.value) match {
        case Some(m) => for (pred <- m) {
          for (percM <- stat_type_predicate_perc_in.get(pred._1)) {
            for (typ <- percM) {
              in.get(typ._1) match {
                case Some(map) => map.updated(pred._1, (pred._2, typ._2, stat_predicate_weight_apriori_in.get(pred._1).get))
                case None => {
                  val zw = new ConcurrentHashMap[String, (Float, Float, Float)]().asScala
                  zw.put(pred._1, (pred._2.toFloat, typ._2._2, stat_predicate_weight_apriori_in.get(pred._1).get))
                  in.put(typ._1, zw)
                }
              }
            }
          }
        }
        case None => throw new Exception("resource " + quad.value + " does not exist!")
      }
      stat_resource_predicate_tf_out.get(quad.subject) match {
        case Some(m) => for (pred <- m) {
          for (percM <- stat_type_predicate_perc_out.get(pred._1)) {
            for (typ <- percM) {
              out.get(typ._1) match {
                case Some(map) => map.updated(pred._1, (pred._2, typ._2, stat_predicate_weight_apriori_out.get(pred._1).get))
                case None => {
                  val zw = new ConcurrentHashMap[String, (Float, Float, Float)]().asScala
                  zw.put(pred._1, (pred._2.toFloat, typ._2._2, stat_predicate_weight_apriori_out.get(pred._1).get))
                  out.put(typ._1, zw)
                }
              }
            }
          }
        }
        case None => throw new Exception("resource " + quad.subject + " does not exist!")
      }
      var ret = Map[String, Float]()
      for(typ <- in.keys.toList) {
        ret += (typ -> innerCalc(typ, in))
      }
      for(typ <- out.keys.toList) {
        ret += (typ -> innerCalc(typ, out))
      }
      ret.toMap
    }

    def innerCalc(typ: String, map: Map[String, Map[String, (Float, Float, Float)]]): Float ={
      var dividend = 0f
      var divisor = 0f
      for (pred <- map.get(typ).get.toList) {
        dividend += pred._2._1 * pred._2._2 * pred._2._3
        divisor += pred._2._1 * pred._2._3
      }
      dividend/divisor
    }

    def getTypePropability(typ: String): Float = {
      if(resourceCount <= 0) throw new Exception("no resources found!")

      typeStatistics.get(typ) match {
        case Some(x)=>
        case None => typeStatistics.put(typ, type_count.map(x => if(x._2.contains(typ)) 1 else 0).sum.toFloat)
      }
      typeStatistics.get(typ).get / resourceCount.toFloat
    }

/*    var lineCount = 0
    val finder = new DateFinder(baseDir, language)
    IOUtils.readLines(new RichFile(finder.byName("instance-types" + args(3).trim, true))) { line =>
      line match {
        case null => // ignore last value
        case Quad(quad) => {
          lineCount += 1
          if (lineCount % 100000 == 0) System.out.println(lineCount)
        }
        case str => if (str.nonEmpty && !str.startsWith("#")) throw new IllegalArgumentException("line did not match quad or triple syntax: " + line)
      }
    }*/

    val worker1 = SimpleWorkers(1.5, 1.0) { language: Language =>
      val finder = new DateFinder(baseDir, language)
      val typePattern = "instance-types(-transitive)?" + args(3).trim
      QuadReader.readQuadsOfMultipleFiles(finder, typePattern, auto = true) { quad =>
        if(quad.value.trim.startsWith("http://dbpedia.org/ontology/"))
        type_count.get(quad.subject) match {
          case Some(typeList) => type_count.put(quad.subject, typeList ::: List(quad.value))
          case None => type_count.put(quad.subject, List(quad.value))
        }
      }
    }

    val workerDisamb = SimpleWorkers(1.5, 1.0) { language: Language =>
      val finder = new DateFinder(baseDir, language)
      QuadReader.readQuads(finder, "disambiguations-unredirected" + args(3).trim, auto = true) { quad =>
        disambiguations.put(quad.subject, 1)
      }
    }

    //count all predicates in special entry!
    val predAll = new ConcurrentHashMap[String, Int]().asScala


    val worker2 = SimpleWorkers(1.5, 1.0) { language: Language =>
      val finder = new DateFinder(baseDir, language)
      QuadReader.readQuads(finder, "mappingbased-objects-uncleaned" + args(3).trim, auto = true) { quad =>
        //if (quad.hasObjectPredicate) {  no need since we use the objects dataset
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

        predAll.get(quad.predicate) match {
          case Some(c) => predAll.put(quad.predicate, c + 1)
          case None => predAll.put(quad.predicate, 1)
        }
        /*          stat_subjects.get(quad.subject) match {
            case Some(count) => stat_subjects.put(quad.subject, count + 1)
            case None => stat_subjects.put(quad.subject, 1)
          }
          stat_objects.get(quad.subject) match {
            case Some(count) => stat_objects.put(quad.value, count + 1)
            case None => stat_objects.put(quad.value, 1)
          }
          stat_property.get(quad.predicate) match {
            case Some(count) => stat_property.put(quad.predicate, count + 1)
            case None => stat_property.put(quad.predicate, 1)
          }*/
        //}
      }
      resourceCount = (stat_resource_predicate_tf_in.keys.toList ::: stat_resource_predicate_tf_out.keys.toList).distinct.length
    }

    val worker3 = SimpleWorkers(1.5, 1.0) { language: Language =>
      for (uri <- type_count.keys) {
        for (typ <- type_count.get(uri).get) {
          stat_resource_predicate_tf_in.get(uri) match{
            case Some(predMap) =>
              for (pred <- predMap) {
                val percentage = pred._2.toFloat
              stat_type_predicate_perc_in.get(pred._1) match {
                case Some(p) => p.get(typ) match {
                  case Some(c) => p.put(typ, (c._1+percentage, 0f))
                  case None => p.put(typ, (percentage, 0f))
                }
                case None => {
                  val zw = new ConcurrentHashMap[String, (Float, Float)]().asScala
                  zw.put(typ, (percentage, 0f))
                  stat_type_predicate_perc_in.put(pred._1, zw)
                }
              }
            }
            case None =>
          }
          stat_resource_predicate_tf_out.get(uri) match {
            case Some(predMap) =>
              for (pred <- predMap) {
                val percentage = pred._2.toFloat
                stat_type_predicate_perc_out.get (pred._1) match {
                    case Some (p) => p.get (typ) match {
                    case Some (c) => p.put(typ, (c._1+percentage, 0f))
                    case None => p.put (typ, (percentage, 0f))
                  }
                  case None => {
                    val zw = new ConcurrentHashMap[String, (Float, Float)]().asScala
                    zw.put (typ, (percentage, 0f))
                    stat_type_predicate_perc_out.put (pred._1, zw)
                  }
                }
              }
            case None =>
          }
        }
      }
      for (typeMap <- stat_type_predicate_perc_in) {
        for(typ <- typeMap._2.keys){
          predAll.get(typeMap._1) match{
            case Some(c) => typeMap._2.put(typ, (typeMap._2.get(typ).get._1, typeMap._2.get(typ).get._1 / c.toFloat))
            case None => System.out.println("pred not found, why?")
          }
          stat_predicate_weight_apriori_in.get(typeMap._1) match {
            case Some(w) => stat_predicate_weight_apriori_in.put(typeMap._1, w + Math.pow(typeMap._2.get(typ).get._2 - getTypePropability(typ), 2).toFloat)
            case None => stat_predicate_weight_apriori_in.put(typeMap._1, Math.pow(typeMap._2.get(typ).get._2 - getTypePropability(typ), 2).toFloat)
          }
        }
      }
      for (typeMap <- stat_type_predicate_perc_out) {
        for(typ <- typeMap._2.keys){
          predAll.get(typeMap._1) match{
            case Some(c) => typeMap._2.put(typ, (typeMap._2.get(typ).get._1, typeMap._2.get(typ).get._1 / c.toFloat))
            case None => System.out.println("pred not found, why?")
          }
          stat_predicate_weight_apriori_out.get(typeMap._1) match {
            case Some(w) => stat_predicate_weight_apriori_out.put(typeMap._1, w + Math.pow(typeMap._2.get(typ).get._2 - getTypePropability(typ), 2).toFloat)
            case None => stat_predicate_weight_apriori_out.put(typeMap._1, Math.pow(typeMap._2.get(typ).get._2 - getTypePropability(typ), 2).toFloat)
          }
        }
      }
    }

    val worker4 = SimpleWorkers(1.5, 1.0) { language: Language =>
      val finder = new DateFinder(baseDir, language)
      //run second and final pass
      var lastResource: String = ""
      QuadMapper.mapQuads(finder, "mappingbased-objects-uncleaned" + args(3).trim, args(1).trim + args(3).trim, auto = true, required = true) { quad =>
        //check if resource has no type and has not already been processed
        if (lastResource.trim != quad.subject.trim){// && disambiguations.get(quad.subject).isEmpty && type_count.get(quad.subject).isEmpty) {
          lastResource = quad.subject
          var zw = ("zw", 0f)
          for (newType <- getTypeScores(quad) if newType._2 >= sdScoreThreshold) //compare to score threshold
            if(zw._2 < newType._2)
                zw = newType
          List(new Quad(
              language = quad.language,
              dataset = args(1).trim ,
              subject = quad.subject,
              predicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
              value = zw._1,
              context = quad.context + "&typeCalculatedBy=sdTypeAlgorithm&sdTypeScore=" + zw._2,
              datatype = null),
            new Quad(
              language = quad.language,
              dataset = null ,
              subject = quad.subject,
              predicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
              value = type_count.getOrElse(quad.subject.trim, List("http://fakeuri.org/notype")).last,
              context = quad.context,
              datatype = null
            )
          )
        }
        else List()
      }
    }

    worker1.start()
    worker1.process(language)
    worker2.start()
    worker2.process(language)
    workerDisamb.start()
    workerDisamb.process(language)
    worker1.stop()
    worker2.stop()
    workerDisamb.stop()
    worker3.start()
    worker3.process(language)
    worker3.stop()
    worker4.start()
    worker4.process(language)
    worker4.stop()
  }
}