package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.ExtractionRecorder
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, ExtractorRecord}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.wikiparser.{Node, TemplateNode}
import org.dbpedia.extraction.dataparser.DateTimeParser
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty}
import org.dbpedia.extraction.util.{Date, Language}

import scala.collection.mutable.ArrayBuffer
import scala.collection.Map
import scala.language.reflectiveCalls
import scala.reflect.ClassTag

/**
 * TODO: change the syntax on the mappings wiki to allow an arbitrary number of template properties.
 */
@SoftwareAgentAnnotation(classOf[CombineDateMapping], AnnotationType.Extractor)
class CombineDateMapping (
  ontologyProperty : OntologyProperty,
  val templateProperties: Map[String, Datatype], // CreateMappingStats requires these to be public
  context : {
    def redirects : Redirects  // redirects required by DateTimeParser
    def ontology: Ontology
    def language : Language
  }
)
extends PropertyMapping
{
  require(Set("xsd:date", "xsd:gDay", "xsd:gMonth", "xsd:gYear", "xsd:gMonthDay", "xsd:gYearMonth").contains(ontologyProperty.range.name),
      "ontologyProperty must be one of: xsd:date, xsd:gDay, xsd:gMonth, xsd:gYear, xsd:gMonthDay, xsd:gYearMonth")

  private val datatype = ontologyProperty.range.asInstanceOf[Datatype]
  
  private val qb = QuadBuilder(context.language, DBpediaDatasets.OntologyPropertiesLiterals, ontologyProperty, datatype)
  
  private def parserOption(unit: Datatype) = Option(unit).map(new DateTimeParser(context, _))

  override val datasets = Set(DBpediaDatasets.OntologyPropertiesLiterals)

  override def extract(node : TemplateNode, subjectUri : String): Seq[Quad] =
  {
    var dates = ArrayBuffer[Date]()
    
    val parseResults = for (
      (templateProperty, unit) <- templateProperties;
      parser <- parserOption(unit);
      property <- node.property(templateProperty);
      parseResult <- parser.parseWithProvenance(property)
    ) yield {
      dates += parseResult.value
      parseResult
    }

    
    try {
      val mergedDate = Date.merge(dates, datatype)

      //set metadata
      qb.setNodeRecord(node.getNodeRecord)
      qb.setExtractor(ExtractorRecord(
        this.softwareAgentAnnotation,
        parseResults.flatMap(p => p.provenance).toSeq,
        Some(parseResults.size),
        Some(templateProperties.keys.reduce((t1, t2) => t1 + "," + t2)),
        Some(node.title),
        node.containedTemplateNames()
      ))
      //set values
      qb.setSubject(subjectUri)
      qb.setValue(mergedDate.toString)
      qb.setSourceUri(node.sourceIri)
      Seq(qb.getQuad)
    } catch {
      case ex : Exception => Seq.empty // TODO: logging
    }
    
  }
}