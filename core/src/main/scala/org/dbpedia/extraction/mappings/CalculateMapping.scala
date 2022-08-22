package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.ontology.datatypes.{Datatype, _}
import org.dbpedia.extraction.ontology.{DBpediaNamespace, Ontology, OntologyProperty}
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.TemplateNode

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

class CalculateMapping (
  val templateProperty1 : String,
  val templateProperty2 : String,
  val unit1 : Datatype, // rml mappings require this to be public (e.g. ModelMapper)
  val unit2 : Datatype, // rml mappings require this to be public (e.g. ModelMapper)
  operation : String,
  ontologyProperty : OntologyProperty,
  context : {
    def ontology : Ontology
    def redirects : Redirects  // redirects required by UnitValueParser
    def language : Language
  } 
)
extends PropertyMapping
{
  require(operation == "add", "Operation '" + operation + "' is not supported. Supported operations: 'add'")

  def parser(unit: Datatype): DataParser = ontologyProperty.range match
  {
      case dt : UnitDatatype => new UnitValueParser(context, unit)
      case dt : DimensionDatatype => new UnitValueParser(context, unit)
      case dt : Datatype => dt.name match
      {
          case "xsd:integer" => new IntegerParser(context)
          case "xsd:positiveInteger"    => new IntegerParser(context, validRange = (i => i > 0))
          case "xsd:nonNegativeInteger" => new IntegerParser(context, validRange = (i => i >=0))
          case "xsd:nonPositiveInteger" => new IntegerParser(context, validRange = (i => i <=0))
          case "xsd:negativeInteger"    => new IntegerParser(context, validRange = (i => i < 0))
          case "xsd:double" => new DoubleParser(context)
          case "xsd:float" => new DoubleParser(context)
          case name => throw new IllegalArgumentException("Datatype " + name + " is not supported by CalculateMapping")
      }
      case dt => throw new IllegalArgumentException("Datatype " + dt + " is not supported by CalculateMapping")
  }
  
  private val parser1 = parser(unit1)
  private val parser2 = parser(unit2)

  private val staticType = QuadBuilder(context.language, DBpediaDatasets.OntologyPropertiesLiterals, ontologyProperty, ontologyProperty.range.asInstanceOf[Datatype]) _
  private val genericType = QuadBuilder(context.language, DBpediaDatasets.OntologyPropertiesLiterals, ontologyProperty, new Datatype("xsd:double")) _
  private val dynamicType = QuadBuilder.dynamicType(context.language, DBpediaDatasets.OntologyPropertiesLiterals, ontologyProperty) _
  private val specificType = QuadBuilder.dynamicPredicate(context.language, DBpediaDatasets.SpecificProperties) _
  
  override val datasets = Set(DBpediaDatasets.OntologyPropertiesLiterals, DBpediaDatasets.SpecificProperties)

  def extract(node : TemplateNode, subjectUri : String) : Seq[Quad] =
  {
      for( property1 <- node.property(templateProperty1);
           property2 <- node.property(templateProperty2);
           parseResult1 <- parser1.parse(property1);
           parseResult2 <- parser2.parse(property2) )
      {
          val quad = (parseResult1, parseResult2) match
          {
              //UnitValueParser
              case(val1: ParseResult[Double], val2: ParseResult[Double]) if val1.unit.nonEmpty && val2.unit.nonEmpty =>
              {
                val value1 = val1.unit.get match{
                  case u : UnitDatatype => u.toStandardUnit(val1.value)
                  case d : Datatype => val1.value
                }
                val value2 = val2.unit.get match{
                  case u : UnitDatatype => u.toStandardUnit(val2.value)
                  case d : Datatype => val2.value
                }
                  val value = operation match
                  {
                      case "add" => value1 + value2
                  }
                  return writeUnitValue(value, val1.unit.get, subjectUri, node.sourceIri)
              }
              //DoubleParser
              case (val1: ParseResult[Double], val2: ParseResult[Double]) =>
              {
                  val value = operation match
                  {
                      case "add" => (val1.value + val2.value).toString
                  }
                  staticType(subjectUri, value, node.sourceIri)
              }
              //IntegerParser
              case (val1: ParseResult[Double], val2: ParseResult[Double]) =>
              {
                  val value = operation match
                  {
                      case "add" => (val1.value + val2.value).toString
                  }
                  staticType(subjectUri, value, node.sourceIri)
              }
          }

          return Seq(quad)
      }
      
      Seq.empty
  }

  //TODO duplicated from SimplePropertyMapping
  private def writeUnitValue(value : Double, unit : Datatype, subjectUri : String, sourceUri : String) : Seq[Quad] =
  {
    //TODO better handling of inconvertible units
    if(unit.isInstanceOf[InconvertibleUnitDatatype])
    {
        return Seq(dynamicType(subjectUri, value.toString, sourceUri, unit))
    }

    var graph = new ArrayBuffer[Quad]
    
    //Write generic property
    graph += genericType(subjectUri, value.toString, sourceUri)

    // Write specific properties
    // FIXME: copy-and-paste in SimplePropertyMapping
    for(cls <- ontologyProperty.domain.relatedClasses)
    {
      for(specificPropertyUnit <- context.ontology.specializations.get((cls, ontologyProperty)))
      {
         val outputValue = specificPropertyUnit.fromStandardUnit(value)
         val propertyUri = DBpediaNamespace.ONTOLOGY.append(cls.name+'/'+ontologyProperty.name)
         graph += specificType(subjectUri, propertyUri, outputValue.toString, sourceUri, specificPropertyUnit)
      }
    }

    graph
  }
}
