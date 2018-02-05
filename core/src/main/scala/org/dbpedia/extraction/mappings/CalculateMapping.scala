package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, ExtractorRecord}
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.ontology.datatypes.{Datatype, _}
import org.dbpedia.extraction.ontology.{DBpediaNamespace, Ontology, OntologyProperty}
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.TemplateNode

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

@SoftwareAgentAnnotation(classOf[CalculateMapping], AnnotationType.Extractor)
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

  def parser(unit: Datatype): DataParser[_] = ontologyProperty.range match
  {
      case dt : UnitDatatype => new UnitValueParser(context, unit)
      case dt : DimensionDatatype => new UnitValueParser(context, unit)
      case dt : Datatype => dt.name match
      {
          case "xsd:integer" => new IntegerParser(context)
          case "xsd:positiveInteger"    => new IntegerParser(context, validRange = i => i > 0)
          case "xsd:nonNegativeInteger" => new IntegerParser(context, validRange = i => i >= 0)
          case "xsd:nonPositiveInteger" => new IntegerParser(context, validRange = i => i <= 0)
          case "xsd:negativeInteger"    => new IntegerParser(context, validRange = i => i < 0)
          case "xsd:double" => new DoubleParser(context)
          case "xsd:float" => new DoubleParser(context)
          case name => throw new IllegalArgumentException("Datatype " + name + " is not supported by CalculateMapping")
      }
      case dt => throw new IllegalArgumentException("Datatype " + dt + " is not supported by CalculateMapping")
  }
  
  private val parser1 = parser(unit1)
  private val parser2 = parser(unit2)

  private val staticType = QuadBuilder(context.language, DBpediaDatasets.OntologyPropertiesLiterals, ontologyProperty, ontologyProperty.range.asInstanceOf[Datatype])
  private val genericType = QuadBuilder(context.language, DBpediaDatasets.OntologyPropertiesLiterals, ontologyProperty, new Datatype("xsd:double"))
  private val dynamicType = QuadBuilder.dynamicType(context.language, DBpediaDatasets.OntologyPropertiesLiterals, ontologyProperty)
  private val specificType = QuadBuilder.dynamicPredicate(context.language, DBpediaDatasets.SpecificProperties)
  
  override val datasets = Set(DBpediaDatasets.OntologyPropertiesLiterals, DBpediaDatasets.SpecificProperties)

  def setStaticValues(node : TemplateNode, subjectUri : String, pr1: ParseResult[_], pr2: ParseResult[_]): Unit ={
    //add metadata to each QuadBuilder
    val er = ExtractorRecord(
      this.softwareAgentAnnotation,
      Seq(pr1.provenance,pr2.provenance).flatten , //add two parser results
      Some(2),
      Some(templateProperty1 + "," + templateProperty2),
      Some(node.title)
    )
    staticType.setExtractor(er)
    genericType.setExtractor(er)
    dynamicType.setExtractor(er)
    specificType.setExtractor(er)
    val nr = node.getNodeRecord
    staticType.setNodeRecord(nr)
    genericType.setNodeRecord(nr)
    dynamicType.setNodeRecord(nr)
    specificType.setNodeRecord(nr)

    //set subject and context
    staticType.setSubject(subjectUri)
    genericType.setSubject(subjectUri)
    dynamicType.setSubject(subjectUri)
    specificType.setSubject(subjectUri)
    val si =node.sourceIri
    staticType.setSourceUri(si)
    genericType.setSourceUri(si)
    dynamicType.setSourceUri(si)
    specificType.setSourceUri(si)
  }

  def extract(node : TemplateNode, subjectUri : String) : Seq[Quad] =
  {
    for( property1 <- node.property(templateProperty1);
         property2 <- node.property(templateProperty2);
         parseResult1 <- parser1.parseWithProvenance(property1);
         parseResult2 <- parser2.parseWithProvenance(property2) )
    {

      // set static values and metadata in QuadBuilders
      setStaticValues(node, subjectUri, parseResult1, parseResult2)

      val quad = (parseResult1, parseResult2) match
      {
          //UnitValueParser
        case (ParseResult(val1: Double, _, u1, _),ParseResult(val2: Double, _, u2, _))  if u1.nonEmpty && u2.nonEmpty =>
            val value1 = u1.get match{
              case u : UnitDatatype => u.toStandardUnit(val1)
              case d : Datatype => val1
            }
            val value2 = u2.get match{
              case u : UnitDatatype => u.toStandardUnit(val2)
              case d : Datatype => val2
            }
            val value = operation match
            {
                case "add" => value1 + value2
            }
            return writeUnitValue(value, u1.get)
          //DoubleParser
        case (ParseResult(val1: Double, _, _, _),ParseResult(val2: Double, _, _, _)) =>
          val value = operation match
          {
              case "add" => (val1 + val2).toString
          }
          staticType.setValue(value)
          staticType.getQuad
          //IntegerParser
        case (ParseResult(val1: Int, _, _, _),ParseResult(val2: Int, _, _, _)) =>
          val value = operation match
          {
              case "add" => (val1 + val2).toString
          }
          staticType.setValue(value)
          staticType.getQuad
      }

      return Seq(quad)
    }

    Seq.empty
  }

  //TODO duplicated from SimplePropertyMapping
  private def writeUnitValue(value : Double, unit : Datatype) : Seq[Quad] =
  {
    //TODO better handling of inconvertible units
    if(unit.isInstanceOf[InconvertibleUnitDatatype])
    {
      dynamicType.setValue(value.toString)
      dynamicType.setDatatype(unit)
      return Seq(dynamicType.getQuad)
    }

    var graph = new ArrayBuffer[Quad]
    
    //Write generic property
    genericType.setValue(value.toString)
    graph += genericType.getQuad

    // Write specific properties
    // FIXME: copy-and-paste in SimplePropertyMapping
    for(cls <- ontologyProperty.domain.relatedClasses)
    {
      for(specificPropertyUnit <- context.ontology.specializations.get((cls, ontologyProperty)))
      {
         val outputValue = specificPropertyUnit.fromStandardUnit(value)
         val propertyUri = DBpediaNamespace.ONTOLOGY.append(cls.name+'/'+ontologyProperty.name)
        specificType.setPredicate(propertyUri)
        specificType.setValue(outputValue.toString)
        specificType.setDatatype(specificPropertyUnit)
         graph += specificType.getQuad
      }
    }

    graph
  }
}
