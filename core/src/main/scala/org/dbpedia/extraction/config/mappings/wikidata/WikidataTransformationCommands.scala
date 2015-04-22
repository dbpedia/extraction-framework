package org.dbpedia.extraction.config.mappings.wikidata

import org.dbpedia.extraction.ontology.{OntologyProperty, OntologyClass}
import org.dbpedia.extraction.util.{Language, WikidataUtil}
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue
import org.wikidata.wdtk.datamodel.interfaces.Value
import scala.collection.mutable

/**
 * Created by ali on 12/20/14.
 * Command pattern is used to set mappings parameters (property,value) and
 * execute mapping commands.
 */

trait WikidataTransformationCommands {
  def execute()
}

class WikidataOneToOneCommand(receiver: WikidataCommandReceiver) extends WikidataTransformationCommands {
  def execute(): Unit = {
    receiver.oneToOne()
  }
}

class WikidataOneToManyCommand(receiver: WikidataCommandReceiver) extends WikidataTransformationCommands {
  def execute(): Unit = {
    receiver.oneToMany()
  }
}

class WikidataCommandReceiver() {

  var MapResult = mutable.Map.empty[String, String]
  private var property: String = ""
  private var value: Value = _
  private var map = mutable.Map.empty[String, String]
  private var equivClassSet = Set[OntologyClass]()
  private var equivPropertySet = Set[OntologyProperty]()


  def setParameters(property: String, value: Value, equivClassSet: Set[OntologyClass], equivPropSet: Set[OntologyProperty], map: mutable.Map[String, String]): Unit = {
    this.property = property
    this.value = value
    this.equivClassSet = equivClassSet
    this.equivPropertySet = equivPropSet
    this.map = map

  }


  def getMap(): mutable.Map[String, String] = {
    MapResult
  }

  def oneToOne() {
    getDBpediaProperties(property, value)
  }

  def oneToMany(): Unit = {
    oldMapToNewMap()
  }

  private def oldMapToNewMap(): Unit = {

    map.foreach {
      keyVal => {
        if (keyVal._2 != null) {
          if (keyVal._2.contains("$1")) {
              val v = substitute(keyVal._2, WikidataUtil.replacePunctuation(value.toString).replace(" ", "_").trim)
              MapResult += (keyVal._1 -> v)
          } else if (keyVal._2.contains("$2")) {
              Language.get("commons") match {
                case Some(dbpedia_lang) => {
                  val wikiTitle = WikiTitle.parse(WikidataUtil.replacePunctuation(value.toString), dbpedia_lang)
                  val v = substitute(keyVal._2, wikiTitle.encoded.toString)
                  MapResult += (keyVal._1 -> v)
                }
                case _=>
              }
            } else {
            keyVal._2 match {
              case "$getLatitude" => MapResult += (keyVal._1 -> getLatitude(value).toString)
              case "$getLongitude" => MapResult += (keyVal._1 -> getLongitude(value).toString)
              case "$getGeoRss" => MapResult += (keyVal._1 -> getGeoRss(value))
              case "$getDBpediaClass" => if (!equivClassSet.isEmpty) MapResult ++= getDBpediaClass(keyVal._1)
              case _ => MapResult += (keyVal._1 -> keyVal._2)
            }

          }
        } else {
          if (value.toString!="") MapResult += (keyVal._1 -> WikidataUtil.replacePunctuation(WikidataUtil.replaceItemId(value.toString)))
          else MapResult += (keyVal._1 -> "")
        }
      }
    }
  }

  def getDBpediaClass(key: String): mutable.Map[String, String] = {
    var classMap = mutable.Map.empty[String, String]
    equivClassSet.foreach {
      mappedClass => classMap += (key -> mappedClass.toString)
    }
    classMap
  }

  def getDBpediaProperties(key: String, value: Value): Unit = {
    if (!equivPropertySet.isEmpty) {
      equivPropertySet.foreach {
        mappedProperty => {
          val propKey = mappedProperty.toString.replace("http://dbpedia.org/ontology/", "")
            MapResult += (propKey -> WikidataUtil.getValue(value))
        }
      }
    }
  }

  def getLatitude(value: Value) = value match {
    case v: GlobeCoordinatesValue => {
      v.getLatitude
    }
    case _ => ""
  }

  def getLongitude(value: Value) = value match {
    case v: GlobeCoordinatesValue => {
      v.getLongitude
    }
    case _ => ""
  }

  def getGeoRss(value: Value) = value match {
    case v: GlobeCoordinatesValue => {
      v.getLatitude + " " + v.getLongitude
    }
    case _ => ""
  }

  def substitute(newValue: String, value: String): String = {
    if (newValue.contains("$2")) newValue.replace("$2", value)
    else newValue.replace("$1", value)
  }
}
