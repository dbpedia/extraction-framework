package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.dataparser.DateTimeParser
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.util.{Language, Date}

/**
 * TODO: replace templateProperty1: String, unit1: Datatype etc. by Map[String,Datatype].
 * Makes the code in this class much simpler.
 * 
 * Later we should also change the syntax on the mappings wiki to allow an arbitrary number
 * of template properties.
 *  
 * I just looked through the mappings wiki. CombineDateMapping is used in five template mappings.
 * I did not find a case where a template property occurred multiple times with different data types.
 * If such a case should occur, we would have to allow multiple occurrences of the same
 * template property with a type like Seq[(String,Datatype)] instead of Map[String,Datatype].
 * JC 2012-05-24
 */
class CombineDateMapping (
  ontologyProperty : OntologyProperty,
  val templateProperty1 : String,  // CreateMappingStats requires these to be public
  unit1 : Datatype,
  val templateProperty2 : String,
  unit2 : Datatype,
  val templateProperty3 : String,
  unit3 : Datatype,
  context : {
    def redirects : Redirects  // redirects required by DateTimeParser
    def language : Language
  }
)
extends PropertyMapping
{
  require(Set("xsd:date", "xsd:gDay", "xsd:gMonth", "xsd:gYear", "xsd:gMonthDay", "xsd:gYearMonth").contains(ontologyProperty.range.name),
      "ontologyProperty must be one of: xsd:date, xsd:gDay, xsd:gMonth, xsd:gYear, xsd:gMonthDay, xsd:gYearMonth")

  val datatype = ontologyProperty.range.asInstanceOf[Datatype]
  
  private def parser(unit: Datatype) = Option(unit).map(new DateTimeParser(context, _))

  private val parser1 = parser(unit1)
  private val parser2 = parser(unit2)
  private val parser3 = parser(unit3)
  
  override val datasets = Set(DBpediaDatasets.OntologyProperties)

  override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    var dates = List[Date]()
    
    for ( 
      parser <- parser1;
      property1 <- node.property(templateProperty1);
      parseResult1 <- parser.parse(property1)
    ) {
      dates ::= parseResult1
    }
    
    for (
      parser <- parser2;
      property2 <- node.property(templateProperty2);
      parseResult2 <- parser.parse(property2) 
    ) {
      dates ::= parseResult2
    }
    
    for(
      parser <- parser3;
      property3 <- node.property(templateProperty3);
      parseResult3 <- parser.parse(property3) 
    ) {
      dates ::= parseResult3
    }

    try {
      val mergedDate = Date.merge(dates, datatype)
      Seq(new Quad(context.language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, mergedDate.toString, node.sourceUri, datatype))
    } catch {
      case ex : Exception => Seq.empty // TODO: logging
    }
    
  }
}