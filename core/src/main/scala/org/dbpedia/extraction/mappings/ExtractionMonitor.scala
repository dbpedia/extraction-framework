package org.dbpedia.extraction.mappings

import java.io.File

import org.apache.jena.rdf.model._
import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.util._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import org.dbpedia.extraction.util.RichFile.wrapFile

import scala.util.Try

class ExtractionMonitor[T] {
  private var stats : mutable.HashMap[ExtractionRecorder[T], mutable.HashMap[String, Int]] = mutable.HashMap()
  private var errors : mutable.HashMap[ExtractionRecorder[T], ListBuffer[Throwable]] = mutable.HashMap()

  private var currentVersion = Config.universalConfig.dbPediaVersion
  private var compareVersions = false
  private var oldDumpDir : File = null
  private var dumpDir : File = null
  private val tripleProperty = "http://rdfs.org/ns/void#triples"
  private var expectedChanges = Array(-1.0, 8.0)
  private var oldVersion = ""
  private val ignorableExceptionsFile: JsonConfig = new JsonConfig(this.getClass.getClassLoader.getResource("ignorableExceptions.json"))
  private val ignorableExceptions : mutable.HashMap[ExtractionRecorder[T], List[String]] = mutable.HashMap()
  private var summarizeExceptions : Boolean = false

  this.loadConf()

  /**
    * Loads Config Values
    * @param configPath path to config
    */
  def loadConf(configPath : String = null): Unit ={
    val config = if(configPath == null) Config.universalConfig
    else ConfigUtils.loadConfig(configPath)
    compareVersions = Try[Boolean]{ config.getProperty("compareDatasetIDs").toBoolean }.getOrElse(false)
    if (compareVersions){
      require(config.getProperty("old-base-dir") != null, "Old build directory needs to be defined under 'old-base-dir' for the dataID comparison!")
      oldDumpDir = new File(config.getProperty("old-base-dir"))
      dumpDir = Config.universalConfig.dumpDir
      val changes = config.getProperty("expectedChanges")
      if(changes != null) {
        if(changes.split(",").length == 2) {
          expectedChanges = Array(changes.split(",")(0).toFloat, changes.split(",")(1).toFloat)
        }
      }
      summarizeExceptions = Try[Boolean]{ config.getProperty("summarizeExceptions").toBoolean }.getOrElse(false)
    }
  }

  /**
    * Initializes the Extraction Monitor for a specific ExtractionRecorder
    * @param er ExtractionRecorder
    */
  def init(er : ExtractionRecorder[T]): Unit ={
    val new_map = mutable.HashMap[String, Int]()

    new_map.put("ERROR", 0)
    new_map.put("CRASHED", 0)
    new_map.put("SUCCESSFUL", 0)

    stats.put(er, new_map)
    errors.put(er, ListBuffer())

    // Load ignorable Exceptions
    var exceptions = ListBuffer[String]()
    er.datasets.foreach(dataset => ignorableExceptionsFile.get(dataset.canonicalUri).foreach(jsonNode => {
      val it = jsonNode.elements()
      while(it.hasNext){
        exceptions += it.next().asText()
      }
    }))
    ignorableExceptions.put(er, exceptions.toList)
  }

  def init(er_list: List[ExtractionRecorder[T]]): Unit ={
    er_list.foreach(init)
  }

  /**
    * reports the success of an page extraction for the Recorder
    * @param er ExtractionRecorder
    */
  def reportSuccess(er: ExtractionRecorder[T]): Unit ={
    stats(er).put("SUCCESSFUL", stats(er).get("SUCCESSFUL").getOrElse(0) + 1)
  }

  /**
    * Marks the ExtractionRecorder as crashed
    * @param er ExtractionRecorder
    * @param ex Exception
    */
  def reportCrash(er : ExtractionRecorder[T], ex : Throwable): Unit ={
    stats(er).put("CRASHED", 1)
    errors(er) += ex
  }

  /**
    * Adds an Exception to the statistics for the Recorder
    * @param er ExtractionRecorder
    * @param ex Exception
    */
  def reportError(er : ExtractionRecorder[T], ex : Throwable): Unit = {
    var ignorable = false
    if(ignorableExceptions(er).contains(ex.getClass.getName.split("\\.").last)) ignorable = true
    if(!ignorable) {
      errors(er) += ex
      stats(er).put("ERROR", stats(er).get("ERROR").getOrElse(0) + 1)
    }
  }

  /**
    * Summary of data for the ExtractionRecorder
    * @param er ExtractionRecorder
    * @param datasets List of Datasets that will be compared by DatasetID
    * @return Summary-Report
    */
  def summarize(er : ExtractionRecorder[T], datasets : ListBuffer[Dataset] = ListBuffer()): mutable.HashMap[String, Object] ={
    // Get the monitor stats for this ER
    val crashed = if(stats(er).getOrElse("CRASHED", 0) == 1) "yes" else "no"
    val error = stats(er).getOrElse("ERROR", 0)
    val success = stats(er).getOrElse("SUCCESSFUL", 0)
    var dataIDResults = ""

    // DatasetID Comparison
    if(compareVersions) {
      // Find the DatasetID Files
      val language = er.language
      val oldDate = new Finder[File](oldDumpDir, language, "wiki").dates().last
      val newDate = new Finder[File](dumpDir, language, "wiki").dates().last
      oldVersion = oldDate.substring(0,4) + "-" + oldDate.substring(4,6)
      val oldPath = oldDumpDir + "/" + language.wikiCode + "wiki/"+ oldDate + "/" +
        oldVersion + "_dataid_" + language.wikiCode + ".ttl"
      val newPath = dumpDir + "/" + language.wikiCode + "wiki/" + newDate + "/" +
        currentVersion + "_dataid_" + language.wikiCode + ".ttl"

      // Compare & Get the results
      dataIDResults = "DATAIDRESULTS: \n"
      compareTripleCount(oldPath, newPath).foreach(r => {
        // Datasets for ER given => only compare these
        if(datasets.nonEmpty){
          datasets.foreach(dataset => if(dataset.canonicalUri == r._1) dataIDResults += r._1 + " : " + r._2 + "\n")
        }
        // Datasets for ER not given => Compare all datasets
        else dataIDResults += String.format("%-30s", r._2 + " : ") + r._1 + "\n"
      })
    }
    val summary = mutable.HashMap[String, Object]()
    summary.put("EXCEPTIONCOUNT", error.asInstanceOf[Object])
    summary.put("SUCCESSFUL", success.asInstanceOf[Object])
    summary.put("CRASHED", crashed.asInstanceOf[Object])
    summary.put("DATAID", dataIDResults.asInstanceOf[Object])
    summary.put("EXCEPTIONS",
      if(summarizeExceptions) errors.getOrElse(er, ListBuffer[Throwable]()).asInstanceOf[Object]
      else ListBuffer[Throwable]())
    summary
  }

  /**
    * Reads two RDF files and compares the triple-count-values.
    * @param oldPath path to first RDF file
    * @param newPath path to second RDF file
    * @return HashMap: Subject -> Comparison Result
    */
  def compareTripleCount(oldPath : String, newPath : String): mutable.HashMap[String, String] ={
    // Load Graphs
    val oldModel = ModelFactory.createDefaultModel()
    val newModel = ModelFactory.createDefaultModel()
    oldModel.read(oldPath)
    newModel.read(newPath)
    val oldValues = getPropertyValues(oldModel, oldModel.getProperty(tripleProperty))
    val newValues = getPropertyValues(newModel, newModel.getProperty(tripleProperty))

    // Compare Graphs
    val diff = mutable.HashMap[String, Float]()
    val result = mutable.HashMap[String, String]()
    oldValues.keySet.foreach(key => {
      if(newValues.keySet.contains(key)) // Get Difference between Files
        diff += (key -> newValues(key).asLiteral().getFloat / oldValues(key).asLiteral().getFloat)
      else result += (key -> "File missing in newer Build") // File missing in new DataID
    })
    newValues.keySet.foreach(key => {
      if(!diff.keySet.contains(key)) result += (key -> "File added in newer Build")// new File added to DataID
    })

    // Summarize Comparison
    diff.keySet.foreach(key => {
      val v = (math floor (diff(key) - 1) * 10000000)/100000
      if(v != -1.0 && v < expectedChanges(0)) result += (key -> ("Suspicious: " + v + "%"))
      else if(v != 1 && v > expectedChanges(1)) result += (key -> ("Suspicious: +" + v + "%"))
      else if(v == 1.0) result += (key -> "No change in triple-count")
      else result += (key -> ((if (v >= 1) " +" else "") + v + "%"))
    })
    result
  }

  /**
    * Queries over the graph and returns the property values
    * @param model graph
    * @param property property
    * @return HashMap: Subject -> Value
    */
  private def getPropertyValues(model: Model, property : Property) : mutable.HashMap[String, RDFNode] = {
    var map = mutable.HashMap[String, RDFNode]()
    val it = model.listResourcesWithProperty(property)
    while(it.hasNext) {
      val res = it.next()
      map += res.getURI.split("\\?")(0).split("\\&")(0) -> res.getProperty(property).getObject
    }
    map
  }
}
