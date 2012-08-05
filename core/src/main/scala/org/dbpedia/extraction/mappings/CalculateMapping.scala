package org.dbpedia.extraction.mappings

import scala.collection.mutable.ArrayBuffer
import org.dbpedia.extraction.ontology.datatypes._
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad,QuadBuilder}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.{OntologyClass, Ontology, DBpediaNamespace, OntologyProperty}

class CalculateMapping (
  val templateProperty1 : String,
  val templateProperty2 : String,
  unit1 : Datatype,
  unit2 : Datatype,
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

  private val staticType = QuadBuilder(context.language, DBpediaDatasets.OntologyProperties, ontologyProperty, ontologyProperty.range.asInstanceOf[Datatype]) _
  private val genericType = QuadBuilder(context.language, DBpediaDatasets.OntologyProperties, ontologyProperty, new Datatype("xsd:double")) _
  private val dynamicType = QuadBuilder.dynamicType(context.language, DBpediaDatasets.OntologyProperties, ontologyProperty) _
  private val specificType = QuadBuilder.dynamicPredicate(context.language, DBpediaDatasets.SpecificProperties) _
  
  override val datasets = Set(DBpediaDatasets.OntologyProperties, DBpediaDatasets.SpecificProperties)

  def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
      for( property1 <- node.property(templateProperty1);
           property2 <- node.property(templateProperty2);
           parseResult1 <- parser1.parse(property1);
           parseResult2 <- parser2.parse(property2) )
      {
          val quad = (parseResult1, parseResult2) match
          {
              //UnitValueParser
              case((value1 : Double, unit1 : UnitDatatype), (value2 : Double, unit2 : UnitDatatype)) =>
              {
                  val value = operation match
                  {
                      case "add" => unit1.toStandardUnit(value1) + unit2.toStandardUnit(value2)
                  }
                  return writeUnitValue(value, unit1, subjectUri, node.sourceUri)
              }
              //DoubleParser
              case (value1 : Double, value2 : Double) =>
              {
                  val value = operation match
                  {
                      case "add" => (value1 + value2).toString
                  }
                  staticType(subjectUri, value, node.sourceUri)
              }
              //IntegerParser
              case (value1 : Int, value2 : Int) =>
              {
                  val value = operation match
                  {
                      case "add" => (value1 + value2).toString
                  }
                  staticType(subjectUri, value, node.sourceUri)
              }
          }

          return Seq(quad)
      }
      
      Seq.empty
  }

  //TODO duplicated from SimplePropertyMapping
  private def writeUnitValue(value : Double, unit : UnitDatatype, subjectUri : String, sourceUri : String) : Seq[Quad] =
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
