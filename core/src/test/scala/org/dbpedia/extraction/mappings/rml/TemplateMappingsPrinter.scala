package org.dbpedia.extraction.mappings.rml

import org.dbpedia.extraction.destinations.Dataset
import org.dbpedia.extraction.mappings.{DateIntervalMapping, GeoCoordinatesMapping, _}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.TemplateNode

object TemplateMappingsPrinter {

  def printTemplateMappings(templateMappings : Map[String, Extractor[TemplateNode]]): Unit = {
    println("Iterating over template mappings...")
    for((k,v: TemplateMapping) <- templateMappings) {
      println("TemplateMapping: " + k)
      println("\tOntology class: " + v.mapToClass.name + " ("+ v.mapToClass.uri +")")
      println("\tLabels: ")
      print("\t\t")
      var i = 0
      for((k : Language, v2: String) <- v.mapToClass.labels) {
        if(i < v.mapToClass.labels.size - 1) print(v2 + ", ") else println(v2)
        i = i + 1
      }
      println("\tProperty Mappings: ")
      for(propertyMapping: PropertyMapping <- v.mappings) {
        printPropertyMapping(propertyMapping, 0)
      }
    }
  }

  private def printPropertyMapping(propertyMapping: PropertyMapping, indent : Int) : Boolean = {
    try {
      propertyMapping.getClass.getSimpleName match {
        case "SimplePropertyMapping" => {
          val simplePropertyMapping = propertyMapping.asInstanceOf[SimplePropertyMapping]
          printIndent(indent)
          println("SimplePropertyMapping: ")
          printIndent(indent)
          println("\tTemplate property: " + simplePropertyMapping.templateProperty)
          printIndent(indent)
          println("\tOntology property: " + simplePropertyMapping.ontologyProperty.name + "\t(" + simplePropertyMapping.ontologyProperty.uri + ")")
          printIndent(indent)
          false
        }
        case "IntermediateNodeMapping" => {
          val intermediateNodeMapping = propertyMapping.asInstanceOf[IntermediateNodeMapping]
          printIndent(indent)
          println("IntermediateNodeMapping: ")
          for(propertyMapping <- intermediateNodeMapping.mappings) {
            printPropertyMapping(propertyMapping, indent + 1)
          }
          true
        }
        case "DateIntervalMapping" => {
          val dateIntervalMapping = propertyMapping.asInstanceOf[DateIntervalMapping]
          printIndent(indent)
          println("DateIntervalMapping: ")
          printIndent(indent)
          println("\tTemplate Property: " + dateIntervalMapping.templateProperty)
          true
        }
        case "GeoCoordinatesMapping" => {
          val geoCoordinatesMapping = propertyMapping.asInstanceOf[GeoCoordinatesMapping]
          printIndent(indent)
          println("GeoCorodinatesMapping: ")
          printIndent(indent)
          println("\tCoordinates: " + geoCoordinatesMapping.coordinates)
          true
        }
      }

    } catch {
      case e: scala.MatchError => throw new IllegalArgumentException("Couldn't load Property Mapping: " + propertyMapping.getClass.getSimpleName)
    }

  }

  private def printIndent(indent: Int) {
    for(i <- 0 to indent) {
      print("\t\t")
    }
  }

}
