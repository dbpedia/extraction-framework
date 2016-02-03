package org.dbpedia.extraction.config.mappings.wikidata

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper, ObjectReader}
import org.dbpedia.extraction.ontology.{OntologyClass, OntologyProperty}
import org.wikidata.wdtk.datamodel.interfaces.Value

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.BufferedSource
import scala.language.postfixOps

/**
 * Created by ali on 12/20/14.
 * Abstract factory design pattern is used to get wikidata mappings from config file.
 * Currently json configuration is supported
 */

trait WikidataExtractorConfig {
  def getCommand(property: String, value:Value,equivClassSet:Set[OntologyClass],equivPropertySet:Set[OntologyProperty], receiver: WikidataCommandReceiver): WikidataTransformationCommands
  def getValue(property: String) : mutable.Map[String, String]
  def keys(): List[String]
}

object WikidataExtractorConfigFactory {
  def createConfig(conf: String): WikidataExtractorConfig = {
    val selection = conf.toLowerCase()
    selection match {
      case selection if selection.endsWith(".json") => {
        new JsonConfig(selection)
      }
    }
  }
}

class JsonConfig(filePath:String) extends WikidataExtractorConfig {
  val configMap = readConfiguration(filePath)

  private final def readConfiguration(filePath:String):  mutable.Map[String, mutable.Map[String, String]] = {
    val configToMap = mutable.Map.empty[String, mutable.Map[String, String]]

    val stream = Option(getClass.getResourceAsStream(filePath)) match
    {
      case Some(x) => new BufferedSource(x)
      case None => scala.io.Source.fromFile(filePath)
    }
    val source = stream mkString

    val factory = new JsonFactory()
    val objectMapper = new ObjectMapper(factory)
    val objectReader: ObjectReader = objectMapper.reader()
    val json = objectReader.readTree(source)
//    println (json.findValue("P625"))
    for (k <- json.fieldNames()) {
      val jsonNode = json.get(k)
      val jsonNodeType = jsonNode.getNodeType

      jsonNodeType match {
        case JsonNodeType.ARRAY => configToMap += (k -> getArrayMap(jsonNode))
//        case JsonNodeType.STRING => configToMap += (k -> getStringMap(jsonNode))
        case JsonNodeType.OBJECT => configToMap += (k ->getObjectMap(jsonNode))
        case _=>
      }
    }
//    println(configToMap)
    configToMap

  }

  private final def getObjectMap(jsonNode:JsonNode):mutable.Map[String,String] = {
    val map = mutable.Map.empty[String, String]
    for (key <- jsonNode.fieldNames()) {
      map += (key -> jsonNode.get(key).asText())
    }
    map
  }

  private final def getStringMap(jsonNode:JsonNode): mutable.Map[String, String] = {
    val map = mutable.Map.empty[String, String]
    map += (jsonNode.asText() -> null)
    map
  }

  private final def getArrayMap(jsonNode:JsonNode): mutable.Map[String, String] = {
    val map = mutable.Map.empty[String, String]
    jsonNode.foreach {
      eachPair => {
        for (key <- eachPair.fieldNames()) {
          map += (key -> eachPair.get(key).asText())
        }
      }
    }
    map
  }

  private def getMap(property: String): mutable.Map[String, String] = configMap.get(property) match {
    case Some(map) => map
    case None => mutable.Map.empty
  }

  def getCommand(property: String, value: Value, equivClassSet: Set[OntologyClass], equivPropertySet: Set[OntologyProperty], receiver: WikidataCommandReceiver): WikidataTransformationCommands = {
    var command = new WikidataTransformationCommands {
      override def execute(): Unit = print("")
    }
    if (getMap(property).size >= 1) {
      receiver.setParameters(property, value, equivClassSet, equivPropertySet, getMap(property))
      val oneToManyCommand = new WikidataOneToManyCommand(receiver)
      command = oneToManyCommand
    }
    else {
      receiver.setParameters(property, value, equivClassSet, equivPropertySet, getMap(property))
      val oneToOneCommand = new WikidataOneToOneCommand(receiver)
      command = oneToOneCommand
    }
    command
  }

  override def getValue(property: String): mutable.Map[String, String] = getMap(property)

  override def keys(): List[String] = configMap.keySet.toList
}