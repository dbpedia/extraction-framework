package org.dbpedia.extraction.mappings

import org.apache.jena.rdf.model._
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.util.{Config, ConfigUtils, Language}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object ExtractionMonitor {
  // Config => Datei/Pfad Auswahl
  // DataID Comparison after Everything is done

  def main(args: Array[String]): Unit = {
    val monitor = new ExtractionMonitor()
    if(args.size > 0) {
      monitor.loadConf(args(0))
      if(monitor.compareVersions) monitor.compareList.foreach(version => {
        monitor.compare(version, monitor.currentVersion, monitor.languages.toList, "http://rdfs.org/ns/void#triples", true)
        // Output Comparison
      })
    }

  }


}

class ExtractionMonitor[T] {
  private var stats : mutable.HashMap[ExtractionRecorder[T], mutable.HashMap[String, Int]] = mutable.HashMap()
  private var errors : mutable.HashMap[ExtractionRecorder[T], ListBuffer[Throwable]] = mutable.HashMap()

  private var currentVersion = Config.universalConfig.dbPediaVersion
  private var compareVersions = false
  private var compareList = Array[String]()
  private var languages = Array[String]()

  def loadConf(configPath : String): Unit ={
    val config = ConfigUtils.loadConfig(configPath)
    compareVersions = config.getProperty("compareVersions").asInstanceOf[Boolean]
    if (compareVersions){
      require(config.getProperty("compareTo") != null, "Versions to compare need to be defined in the config under 'compareTo'!")
      require(config.getProperty("languages") != null, "languages to compare need to be defined in the config under 'languages'!")
      compareList = config.getProperty("compareTo").split(",")
      languages = config.getProperty("languages").split(",")
    }

  }

  def reportCrash(er : ExtractionRecorder[T], ex : Throwable): Unit ={
    stats(er)("CRASHED") = 1
    stats(er)("ERROR") += 1
    errors(er) += ex
  }

  def reportError(er : ExtractionRecorder[T], ex : Throwable): Unit = {
    stats(er)("ERROR") += 1
    errors(er) += ex
  }

  def summarize(er : ExtractionRecorder[T]): String ={
    // TODO
    ""
  }

  def init(er : ExtractionRecorder[T]): Unit ={
    val new_map = mutable.HashMap[String, Int]()

    new_map.put("ERROR", 0)
    new_map.put("CRASHED", 0)

    stats.put(er, new_map)
  }

  def init(er_list: List[ExtractionRecorder[T]]): Unit ={
    er_list.foreach(init)
  }

  // TODO clean this
  def compare(oldBuild : String, newBuild : String, languages : List[String], property : String, numberCompare : Boolean) : CompareResult = {
    var list = ListBuffer[mutable.HashMap[String, RDFNode]]()
    val sizeDiff = list(1).keySet.size - list(0).keySet.size
    val diff_missing = ListBuffer[String]()
    val diff_new = ListBuffer[String]()
    var numberdiff : Option[mutable.HashMap[String, Float]] = None

    // get Property Values
    languages.foreach(langCode => List(oldBuild,newBuild).foreach(build => {
      val model = ModelFactory.createDefaultModel()
      model.read("/home/termi/dbpedia/data/" + langCode +"wiki/" + build + "/" + currentVersion + "_dataid_" + langCode + ".ttl")
      list += getPropertyValues(model, model.getProperty(property))
    }))



    // Get Missing and New Subjects
    list(0).keySet.foreach(key => {
      if(!list(1).contains(key)) diff_missing += key
    })
    list(1).keySet.foreach(key => {
      if(!list(0).contains(key)) diff_new += key
    })

    // Compare Number Values
    if(numberCompare) {
      var map = mutable.HashMap[String, Float]()
      list(0).keySet.foreach(key => {
        val o = list(0).get(key) match {
          case Some(node) => node.asLiteral().getLong
          case None => 0
        }
        val n = list(1).get(key) match {
          case Some(node) => node.asLiteral().getLong
          case None => 0
        }
        val of : Float = o
        val on : Float = n
        val increase : Float = if(n >= o) (n/o) - 1 else - ((o/n) - 1)
        map += key -> increase
      })
      numberdiff = Some(map)
    }

    new CompareResult(sizeDiff, diff_missing, diff_new, numberdiff)
  }

  def getPropertyValues(model: Model, property : Property) : mutable.HashMap[String, RDFNode] = {
    var map = mutable.HashMap[String, RDFNode]()
    val it = model.listResourcesWithProperty(property)
    while(it.hasNext) {
      val res = it.next()
      map += res.getURI -> res.getProperty(property).getObject
    }
    map
  }

  class CompareResult(sizeDiff : Long, diff_missing : ListBuffer[String], diff_new : ListBuffer[String], numberDiff : Option[mutable.HashMap[String, Float]] = None) {
    def getSizeDiff() {sizeDiff}
    def getNumberDiff() {numberDiff}
  }
}
