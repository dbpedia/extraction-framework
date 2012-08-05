package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.dataparser.DateTimeParser
import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad,QuadBuilder}
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.util.{Language, Date}
import scala.collection.mutable.ArrayBuffer
import scala.collection.Map

/**
 * TODO: change the syntax on the mappings wiki to allow an arbitrary number of template properties.
 */
class CombineDateMapping (
  ontologyProperty : OntologyProperty,
  val templateProperties: Map[String, Datatype], // CreateMappingStats requires these to be public
  context : {
    def redirects : Redirects  // redirects required by DateTimeParser
    def language : Language
  }
)
extends PropertyMapping
{
  require(Set("xsd:date", "xsd:gDay", "xsd:gMonth", "xsd:gYear", "xsd:gMonthDay", "xsd:gYearMonth").contains(ontologyProperty.range.name),
      "ontologyProperty must be one of: xsd:date, xsd:gDay, xsd:gMonth, xsd:gYear, xsd:gMonthDay, xsd:gYearMonth")

  private val datatype = ontologyProperty.range.asInstanceOf[Datatype]
  
  private val quad = QuadBuilder(context.language, DBpediaDatasets.OntologyProperties, ontologyProperty, datatype) _
  
  private def parserOption(unit: Datatype) = Option(unit).map(new DateTimeParser(context, _))

  override val datasets = Set(DBpediaDatasets.OntologyProperties)

  override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    var dates = ArrayBuffer[Date]()
    
    for ( 
      (templateProperty, unit) <- templateProperties;
      parser <- parserOption(unit);
      property <- node.property(templateProperty);
      parseResult <- parser.parse(property)
    )
      dates += parseResult
    
    try {
      val mergedDate = Date.merge(dates, datatype)
      Seq(quad(subjectUri, mergedDate.toString, node.sourceUri))
    } catch {
      case ex : Exception => Seq.empty // TODO: logging
    }
    
  }
}