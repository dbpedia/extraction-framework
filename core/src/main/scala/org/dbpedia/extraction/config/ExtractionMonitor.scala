package org.dbpedia.extraction.config

import java.io.File
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

import org.apache.jena.rdf.model._
import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.util.JsonConfig

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.sys.process._
import scala.util.Try

class ExtractionMonitor {
  private val stats : mutable.HashMap[ExtractionRecorder[_], mutable.HashMap[String, Int]] = mutable.HashMap()
  private val errors : mutable.HashMap[ExtractionRecorder[_], ListBuffer[Throwable]] = mutable.HashMap()

  private var compareVersions = false
  private var old_version_URL : String = _
  private val tripleProperty = "http://rdfs.org/ns/void#triples"
  private var expectedChanges = Array(-1.0, 8.0)
  private val ignorableExceptionsFile: JsonConfig = new JsonConfig(this.getClass.getClassLoader.getResource("ignorableExceptions.json"))
  private val ignorableExceptions : mutable.HashMap[ExtractionRecorder[_], List[String]] = mutable.HashMap()
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
      require(config.getProperty("old-base-dir-url") != null, "Old build directory needs to be defined under 'old-base-dir-url' for the dataID comparison!")
      old_version_URL = config.getProperty("old-base-dir-url")
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
  def init(er : ExtractionRecorder[_]): Unit ={
    val new_map = mutable.HashMap[String, Int]()

    new_map.put("ERROR", 0)
    new_map.put("CRASHED", 0)
    new_map.put("SUCCESSFUL", 0)

    stats.put(er, new_map)
    errors.put(er, ListBuffer())

    // Load ignorable Exceptions
    var exceptions = ListBuffer[String]()
    er.getDatasets.foreach(dataset => ignorableExceptionsFile.get(dataset.canonicalUri).foreach(jsonNode => {
      val it = jsonNode.elements()
      while(it.hasNext){
        exceptions += it.next().asText()
      }
    }))
    ignorableExceptions.put(er, exceptions.toList)
  }

  def init(er_list: List[ExtractionRecorder[_]]): Unit ={
    er_list.foreach(init)
  }

  /**
    * Marks the ExtractionRecorder as crashed
    * @param er ExtractionRecorder
    * @param ex Exception
    */
  def reportCrash(er : ExtractionRecorder[_], ex : Throwable): Unit ={
    stats(er).put("CRASHED", 1)
    errors(er) += ex
  }

  /**
    * Adds an Exception to the statistics for the Recorder
    * @param er ExtractionRecorder
    * @param ex Exception
    */
  def reportError(er : ExtractionRecorder[_], ex : Throwable): Unit = {
    var ignorable = false
    if(ignorableExceptions(er).contains(ex.getClass.getName.split("\\.").last)) ignorable = true
    if(!ignorable) {
      errors(er) += ex
      stats(er).put("ERROR", stats(er).getOrElse("ERROR", 0) + 1)
    }
  }

  /**
    * Summary of data for the ExtractionRecorder
    * @param er ExtractionRecorder
    * @param datasets List of Datasets that will be compared by DatasetID
    * @return Summary-Report
    */
  def summarize(er : ExtractionRecorder[_], datasets : ListBuffer[Dataset] = ListBuffer()): mutable.HashMap[String, Object] ={
    // Get the monitor stats for this ER
    val crashed = if(stats(er).getOrElse("CRASHED", 0) == 1) "yes" else "no"
    val error = stats(er).getOrElse("ERROR", 0)
    var dataIDResults = ""

    // DatasetID Comparison
    if(compareVersions) {
      // Find the DatasetID Files
      val language = er.language
      val oldDate = old_version_URL.split("/")(3)
      val url = old_version_URL + language.wikiCode + "/"+ oldDate + "_dataid_" + language.wikiCode + ".ttl"

      // Compare & Get the results
      //compareTripleCount(new URL(url), er, datasets)
    }
    val exceptions = mutable.HashMap[String, Int]()
    errors(er).foreach(ex => {
      exceptions.put(ex.getClass.getName, exceptions.getOrElse(ex.getClass.getName, 0) + 1)
    })
    val summary = mutable.HashMap[String, Object]()
    summary.put("EXCEPTIONCOUNT", error.asInstanceOf[Object])

    val s : AtomicLong = new AtomicLong(0)
    val map = er.getSuccessfulPageCount
    map.keySet.foreach(key => s.set(s.get() + map(key).get()))
    summary.put("SUCCESSFUL", s)
    summary.put("CRASHED", crashed.asInstanceOf[Object])
    summary.put("DATAID", dataIDResults.asInstanceOf[Object])
    summary.put("EXCEPTIONS",
      if(summarizeExceptions) exceptions.asInstanceOf[Object]
      else mutable.HashMap[String, Int]())
    summary
  }

  /**
    * Reads two RDF files and compares the triple-count-values.
    */
  def compareTripleCount(dataIDfile : URL, extractionRecorder: ExtractionRecorder[_], datasets : ListBuffer[Dataset]): String ={

    var resultString = ""

    // Load Graph
    val oldModel = ModelFactory.createDefaultModel()

    oldModel.read(dataIDfile.openStream(), "")
    val oldValues = getPropertyValues(oldModel, oldModel.getProperty(tripleProperty))

    // Compare Values
    oldValues.keySet.foreach(fileName => {
      if(datasets.nonEmpty) {
        datasets.foreach(dataset =>
          if(dataset.canonicalUri == fileName) {
            val oldValue : Long = oldValues(fileName).asLiteral().getLong
            val newValue : Long = extractionRecorder.successfulTriples(dataset)
            val change : Float =
              if(newValue > oldValue) (newValue.toFloat / oldValue.toFloat) * 10000000 / 100000
              else (-1 * (1- (newValue.toFloat / oldValue.toFloat))) * 10000000 / 100000
            if(change < expectedChanges(0) || change > expectedChanges(1)) resultString += "! "
            resultString += dataset.canonicalUri + ": " + change + "%"
          })
      }
      // Datasets for ER not given => Compare all datasets
      else {
        resultString += fileName + ": " + oldValues(fileName).asLiteral().getLong
      }
    })
    resultString
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
