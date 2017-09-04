package org.dbpedia.extraction.mappings

import java.io.File

import org.apache.jena.rdf.model._
import org.dbpedia.extraction.util.{Config, ConfigUtils, Finder, Language}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util._

object ExtractionMonitor {
  // Config => Datei/Pfad Auswahl
  // DataID Comparison after Everything is done

  def main(args: Array[String]): Unit = {
    val monitor = new ExtractionMonitor()
    if(args.size > 0) {
      monitor.loadConf(args(0))
    }
  }
}

class ExtractionMonitor[T] {
  private var stats : mutable.HashMap[ExtractionRecorder[T], mutable.HashMap[String, Int]] = mutable.HashMap()
  private var errors : mutable.HashMap[ExtractionRecorder[T], ListBuffer[Throwable]] = mutable.HashMap()

  private var currentVersion = Config.universalConfig.dbPediaVersion
  private var compareVersions = false
  private var oldDumpDir : File = null
  private var newDumpDir : File = null
  private var languages = Array[String]()
  private var dumpDir : File = null

  private val tripleProperty = "http://rdfs.org/ns/void#triples"
  private val normalTriplecountchangeInterval = Array(0.99, 1.08)
  private var compareDataID = false
  private var olddatecode = ""
  private var newdatecode = ""

  this.loadConf()

  def loadConf(configPath : String = null): Unit ={
    val config = if(configPath == null) Config.universalConfig
    else ConfigUtils.loadConfig(configPath)
    compareVersions = config.getProperty("compareVersions").toBoolean
    if (compareVersions){
      require(config.getProperty("oldDumpDir") != null, "Versions to compare need to be defined in the config under 'compareTo'!")
      require(config.getProperty("newDumpDir") != null, "Versions to compare need to be defined in the config under 'compareTo'!")
      require(config.getProperty("dataIDLangs") != null, "languages to compare need to be defined in the config under 'languages'!")
      oldDumpDir = new File(config.getProperty("oldDumpDir"))
      newDumpDir = new File(config.getProperty("newDumpDir"))
      languages = config.getProperty("dataIDLangs").split(",")
      dumpDir = Config.universalConfig.dumpDir
    }

  }

  def reportCrash(er : ExtractionRecorder[T], ex : Throwable): Unit ={
    stats(er).put("CRASHED", 1)
    errors(er) += ex
  }

  def reportError(er : ExtractionRecorder[T], ex : Throwable): Unit = {
    stats(er).put("ERROR", stats(er).get("ERROR").getOrElse(0) + 1)
    errors(er) += ex
  }

  def reportPositive(er: ExtractionRecorder[T]): Unit ={
    stats(er).put("POSITIVE", stats(er).get("POSITIVE").getOrElse(0) + 1)
  }

  def summarize(er : ExtractionRecorder[T]): String ={
    val crashed = if(stats(er).get("CRASHED").getOrElse(0) == 1) "yes" else "no"
    var dataIDResults = ""
    if(compareVersions) {
      dataIDResults = "DATAIDRESULTS: "
      languages.foreach(langCode => {
        val oldfinder = new Finder[File](oldDumpDir, Language(langCode), "wiki")
        val olddate = oldfinder.dates().last
        val newfinder = new Finder[File](newDumpDir, Language(langCode), "wiki")
        val newdate = newfinder.dates().last
        olddatecode = olddate.substring(0,4) + "-" + olddate.substring(4,6)
        newdatecode = newdate.substring(0,4) + "-" + newdate.substring(4,6)
        val oldPath = oldDumpDir + "/" + langCode + "wiki/"+ olddate + "/" +
          olddatecode + "_dataid_" + langCode + ".ttl"
        val newPath = newDumpDir + "/" + langCode + "wiki/" + newdate + "/" +
          newdatecode + "_dataid_" + langCode + ".ttl"
        dataIDResults += "\n" + langCode + ":\n"
        compareTripleCount(oldPath, newPath).foreach(r => dataIDResults += r._2 + ": " + r._1 + "\n")
      })
    }
    "\n<------ EXTRACTION MONITOR STATISTICS ----->\n" +
    "ERRORS:\t\t" + stats(er).get("ERROR").getOrElse(0) + "\n" +
    "SUCCESSFUL:\t" + stats(er).get("POSITIVE").getOrElse(0) + "\n" +
    "CRASHED:\t" + crashed + "\n" + dataIDResults +
    "\n<------------------------------------------>"
  }

  def init(er : ExtractionRecorder[T]): Unit ={
    val new_map = mutable.HashMap[String, Int]()

    new_map.put("ERROR", 0)
    new_map.put("CRASHED", 0)
    new_map.put("POSITIVE", 0)

    stats.put(er, new_map)
  }

  def init(er_list: List[ExtractionRecorder[T]]): Unit ={
    er_list.foreach(init)
  }

  def compareTripleCount(oldPath : String, newPath : String): mutable.HashMap[String, String] ={
    val oldModel = ModelFactory.createDefaultModel()
    val newModel = ModelFactory.createDefaultModel()

    oldModel.read(oldPath)
    newModel.read(newPath)

    val oldValues = getPropertyValues(oldModel, oldModel.getProperty(tripleProperty))
    val newValues = getPropertyValues(newModel, newModel.getProperty(tripleProperty))

    val diff = mutable.HashMap[String, Float]()
    val result = mutable.HashMap[String, String]()

    oldValues.keySet.foreach(key => {
      if(newValues.keySet.contains(key)) // Get Difference between Files
        diff += (key -> newValues(key).asLiteral().getFloat / oldValues(key).asLiteral().getFloat)
      else result += (key -> "File missing in newer Build!") // File missing in new DataID
    })
    newValues.keySet.foreach(key => {
      if(!diff.keySet.contains(key)) result += (key -> "File added in newer Build!")// new File added to DataID
    })
    diff.keySet.foreach(key => {
      val v = diff(key)
      if(v != -1.0 && v < normalTriplecountchangeInterval(0)) result += (key -> ("suspicious decrease by " + (math floor (v - 1) * 10000000)/100000 + "%"))
      else if(v != 1 && v > normalTriplecountchangeInterval(1)) result += (key -> ("suspicious increase by " + (math floor (v - 1) * 10000000)/100000 + "%"))
      else if(v == 1.0) result += (key -> "No Changes!")
      else if(v == null) {}
      else result += (key -> ("Triple count changed by " + (math floor (v - 1) * 100) + "%"))
    })
    result
  }


  def getPropertyValues(model: Model, property : Property) : mutable.HashMap[String, RDFNode] = {
    var map = mutable.HashMap[String, RDFNode]()
    val it = model.listResourcesWithProperty(property)
    while(it.hasNext) {
      val res = it.next()
      map += res.getURI.split("\\?")(0).split("\\&")(0) -> res.getProperty(property).getObject
    }
    map
  }
}
