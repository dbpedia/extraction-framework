package org.dbpedia.extraction.config.mappings.wikidata

import java.net.URL

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
  def createConfig(fileUrl:URL): WikidataExtractorConfig = {
    fileUrl match {
      case selection if selection.getFile.endsWith(".json") => {
        new JsonConfig(selection)
      }
    }
  }
}

class JsonConfig(fileUrl:URL) extends WikidataExtractorConfig {
  val configMap = readConfiguration(fileUrl)

  private final def readConfiguration(fileUrl: URL):  mutable.Map[String, mutable.Map[String, String]] = {
    val configToMap = mutable.Map.empty[String, mutable.Map[String, String]]

    val stream = Option(fileUrl.openStream()) match
    {
      case Some(x) => new BufferedSource(x)
      case None => throw new NotImplementedError("This shouldn't have happened! If this exception occurred, please contact the Github Issue section of the extraction framework with the complete exception stack.")//scala.io.Source.fromFile(fileUrl)
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