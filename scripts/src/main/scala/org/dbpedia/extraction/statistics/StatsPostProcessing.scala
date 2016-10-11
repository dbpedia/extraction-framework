package org.dbpedia.extraction.statistics

import java.io.{FileOutputStream, File}
import java.util.logging.{Level, Logger}

import org.apache.jena.atlas.json.{JsonNumber, JsonObject, JSON}
import scala.collection.JavaConverters._
import scala.Console._

/**
  * Created by Chile on 3/9/2016.
  */
object StatsPostProcessing {
  def main(args: Array[String]): Unit = {

    val logger = Logger.getLogger(getClass.getName)

    require(args != null && args.length == 9,
      "need at least three args: " +
        /*0*/ "base directory - all files have to be found in this directory" +
        /*1*/ "localized instance count file" +
        /*2*/ "canonicalized instance count file" +
        /*3*/ "mappingbased properties and statements" +
        /*4*/ "raw infobox properties and statements" +
        /*5*/ "type statistics file" +
        /*6*/ "general statistics filename (saved in basedir/statistics)" +
        /*7*/ "property statistics filename (saved in basedir)" +
        /*8*/ "type statistics filename (saved in basedir)"
    )

    logger.log(Level.INFO, "starting postprocessing")

    val baseDir = new File(args(0) + "/statistics")
    require(baseDir.isDirectory, "basedir is not a directory")

    val localizedInstances = new File(baseDir + "/" + args(1))
    require(localizedInstances.isFile && localizedInstances.canRead, "localizedInstances is not readable")
    err.println("postprocessing statistics: reading " + localizedInstances)
    val lib = scala.io.Source.fromFile(localizedInstances)
    val lim = JSON.parse(lib.mkString.replaceAll("\\n", "").replaceAll("\\t", ""))
    lib.close()

    val canonicalizedInstances = new File(baseDir + "/" + args(2))
    require(canonicalizedInstances.isFile && canonicalizedInstances.canRead, "canonicalizedInstances is not readable")
    err.println("postprocessing statistics: reading " + canonicalizedInstances)
    val cib = scala.io.Source.fromFile(canonicalizedInstances)
    val cim = JSON.parse(cib.mkString.replaceAll("\\n", "").replaceAll("\\t", ""))
    cib.close()

    val mappingbasedProperties = new File(baseDir + "/" + args(3))
    require(mappingbasedProperties.isFile && mappingbasedProperties.canRead, "mappingbasedProperties is not readable")
    err.println("postprocessing statistics: reading " + mappingbasedProperties)
    val mpb = scala.io.Source.fromFile(mappingbasedProperties)
    val mpm = JSON.parse(mpb.mkString.replaceAll("\\n", "").replaceAll("\\t", ""))
    mpb.close()

    val rawInfoboxProperties = new File(baseDir + "/" + args(4))
    require(rawInfoboxProperties.isFile && rawInfoboxProperties.canRead, "rawInfoboxProperties is not readable")
    err.println("postprocessing statistics: reading " + rawInfoboxProperties)
    val rpb = scala.io.Source.fromFile(rawInfoboxProperties)
    val rpm = JSON.parse(rpb.mkString.replaceAll("\\n", "").replaceAll("\\t", ""))
    rpb.close()

    val typeStatistics = new File(baseDir + "/" + args(5))
    require(typeStatistics.isFile && typeStatistics.canRead, "typeStatistics is not readable")
    err.println("postprocessing statistics: reading " + typeStatistics)
    val tsb = scala.io.Source.fromFile(typeStatistics)
    val tsm = JSON.parse(tsb.mkString.replaceAll("\\n", "").replaceAll("\\t", ""))
    tsb.close()

    val generalOut = new File(baseDir + "/" + args(6))
    generalOut.createNewFile()
    require(generalOut.isFile && generalOut.canWrite, "output file is not writable")

    val propsOut = new File(baseDir + "/" + args(7))
    propsOut.createNewFile()
    require(propsOut.isFile && propsOut.canWrite, "output file is not writable")

    val typesOut = new File(baseDir + "/" + args(8))
    typesOut.createNewFile()
    require(typesOut.isFile && typesOut.canWrite, "output file is not writable")

    val countsMap = new JsonObject()
    val propsMap = new JsonObject()
    val typesMap = new JsonObject()
    for(lang <- lim.keys().asScala)
    {
      val map = countsMap.get(lang) match {
        case x : JsonObject => x
        case _ => {
          countsMap.put(lang, new JsonObject())
          propsMap.put(lang, new JsonObject())
          typesMap.put(lang, new JsonObject())
          countsMap.get(lang).getAsObject
        }
      }
      map.put("Localiced Instances", lim.get(lang).getAsObject.get("subjects").getAsObject.get("count").getAsNumber.value().intValue())
    }
    for(lang <- cim.keys().asScala)
    {
      val map = countsMap.get(lang).getAsObject
      map.put("Canonicalized Instances", cim.get(lang).getAsObject.get("subjects").getAsObject.get("count").getAsNumber.value().intValue())
    }
    for(lang <- mpm.keys().asScala)
    {
      val map = countsMap.get(lang).getAsObject
      val properties = mpm.get(lang).getAsObject.get("properties").getAsObject
      map.put("Mappingbased Properties", properties.size() -1)
      map.put("Mappingbased Statements", mpm.get(lang).getAsObject.get("statements").getAsObject.get("count").getAsNumber.value().intValue())

      val langProps = propsMap.get(lang).getAsObject
      for(prop: String <- properties.keys().asScala)
      {
        if(prop.trim != "www.w3.org/1999/02/22-rdf-syntax-ns#type") {
          val value = langProps.get(prop.trim) match {
            case x : JsonNumber => x.value().intValue()
            case _ => 0
          }
          langProps.put(prop.trim, value + properties.get(prop).getAsNumber.value().intValue())
        }
      }
    }
    for(lang <- rpm.keys().asScala)
    {
      val map = countsMap.get(lang).getAsObject
      map.put("Raw Infobox Properties", rpm.get(lang).getAsObject.get("properties").getAsObject.size() -1)
      map.put("Raw Infobox Statements", rpm.get(lang).getAsObject.get("statements").getAsObject.get("count").getAsNumber.value().intValue())
    }
    for(lang <- tsm.keys().asScala)
    {
      val map = countsMap.get(lang).getAsObject
      val objects = tsm.get(lang).getAsObject.get("objects").getAsObject
      map.put("Type Statements", tsm.get(lang).getAsObject.get("statements").getAsObject.get("count").getAsNumber.value().intValue())

      val langObjects = typesMap.get(lang).getAsObject
      for(prop: String <- objects.keys().asScala)
      {
        val value = langObjects.get(prop.trim) match {
          case x : JsonNumber => x.value().intValue()
          case _ => 0
        }
        langObjects.put(prop.trim, value + objects.get(prop).getAsNumber.value().intValue())
      }
    }

    //now sum over all props and types by adding a new lang called 'all'
    sumAll(propsMap)
    sumAll(typesMap)
    sumAll(countsMap)

    def sumAll(inputMap: JsonObject) : Unit = {
      val allProps = new JsonObject()
      for (lang <- inputMap.keys.asScala) {
        val map = inputMap.get(lang) match {
          case x : JsonObject => x
          case _ => {
            logger.log(Level.WARNING, "no entry found do language: " + lang)
            new JsonObject()
          }
        }
        for (prop <- map.keys.asScala) {
          val value = allProps.get(prop) match {
            case x : JsonNumber => x.value().intValue()
            case _ => 0
          }
          allProps.put(prop, value + map.get(prop).getAsNumber.value().intValue())
        }
      }
      inputMap.put("all", allProps)
    }

    //write to files
    JSON.write(new FileOutputStream(generalOut), countsMap)
    JSON.write(new FileOutputStream(propsOut), propsMap)
    JSON.write(new FileOutputStream(typesOut), typesMap)
    logger.log(Level.INFO, "finished postprocessing")
  }
}
