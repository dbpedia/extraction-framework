package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.datatypes._
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology._
import java.lang.IllegalArgumentException
import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.ontology.{OntologyDatatypeProperty,OntologyClass,OntologyProperty,DBpediaNamespace}
import scala.collection.mutable.ArrayBuffer

class SimplePropertyMapping (
  val templateProperty : String, // IntermediateNodeMapping and CreateMappingStats requires this to be public
  ontologyProperty : OntologyProperty,
  unit : Datatype,
  private var language : Language,
  factor : Double,
  context : {
    def ontology : Ontology
    def redirects : Redirects  // redirects required by DateTimeParser and UnitValueParser
    def language : Language
  }
)
extends PropertyMapping
{
    if(language == null) language = context.language

    ontologyProperty match
    {
        case datatypeProperty : OntologyDatatypeProperty =>
        {
            //Check if unit is compatible to the range of the ontology property
            (unit, datatypeProperty.range) match
            {
                case (dt1 : UnitDatatype, dt2 : UnitDatatype) => require(dt1.dimension == dt2.dimension,
                    "Unit must conform to the dimension of the range of the ontology property")

                case (dt1 : UnitDatatype, dt2 : DimensionDatatype) => require(dt1.dimension == dt2,
                    "Unit must conform to the dimension of the range of the ontology property")

                case (dt1 : DimensionDatatype, dt2 : UnitDatatype) => require(dt1 == dt2.dimension,
                    "The dimension of unit must match the range of the ontology property")

                case (dt1 : DimensionDatatype, dt2 : DimensionDatatype) => require(dt1 == dt2,
                    "Unit must match the range of the ontology property")

                case _ if unit != null => require(unit == ontologyProperty.range, "Unit must be compatible to the range of the ontology property")
                case _ =>
            }
        }
        case _ =>
    }

    if(language != context.language)
    {
      require(ontologyProperty.isInstanceOf[OntologyDatatypeProperty],
        "Language can only be specified for datatype properties")

      // TODO: don't use string constant, use RdfNamespace
      require(ontologyProperty.range.uri == "http://www.w3.org/2001/XMLSchema#string",
        "Language can only be specified for string datatype properties")
    }
    
    private val parser : DataParser = ontologyProperty.range match
    {
        //TODO
        case c : OntologyClass =>
          if (ontologyProperty.name == "foaf:homepage") {
            checkMultiplicationFactor("foaf:homepage")
            new LinkParser()
          } 
          else {
            new ObjectParser(context)
          }
        case dt : UnitDatatype      => new UnitValueParser(context, if(unit != null) unit else dt, multiplicationFactor = factor)
        case dt : DimensionDatatype => new UnitValueParser(context, if(unit != null) unit else dt, multiplicationFactor = factor)
        case dt : EnumerationDatatype =>
        {
            checkMultiplicationFactor("EnumerationDatatype")
            new EnumerationParser(dt)
        }
        case dt : Datatype => dt.name match
        {
            case "xsd:integer" => new IntegerParser(context, multiplicationFactor = factor)
            case "xsd:positiveInteger"    => new IntegerParser(context, multiplicationFactor = factor, validRange = (i => i > 0))
            case "xsd:nonNegativeInteger" => new IntegerParser(context, multiplicationFactor = factor, validRange = (i => i >=0))
            case "xsd:nonPositiveInteger" => new IntegerParser(context, multiplicationFactor = factor, validRange = (i => i <=0))
            case "xsd:negativeInteger"    => new IntegerParser(context, multiplicationFactor = factor, validRange = (i => i < 0))
            case "xsd:double" => new DoubleParser(context, multiplicationFactor = factor)
            case "xsd:float" => new DoubleParser(context, multiplicationFactor = factor)
            case "xsd:string" =>
            {
                checkMultiplicationFactor("xsd:string")
                StringParser
            }
            case "xsd:anyURI" =>
            {
                checkMultiplicationFactor("xsd:anyURI")
                new LinkParser(false)
            }
            case "xsd:date" =>
            {
                checkMultiplicationFactor("xsd:date")
                new DateTimeParser(context, dt)
            }
            case "xsd:gYear" =>
            {
                checkMultiplicationFactor("xsd:gYear")
                new DateTimeParser(context, dt)
            }
            case "xsd:gYearMonth" =>
            {
                checkMultiplicationFactor("xsd:gYearMonth")
                new DateTimeParser(context, dt)
            }
            case "xsd:gMonthDay" =>
            {
                checkMultiplicationFactor("xsd:gMonthDay")
                new DateTimeParser(context, dt)
            }
            case "xsd:boolean" =>
            {
                checkMultiplicationFactor("xsd:boolean")
                BooleanParser
            }
            case name => throw new IllegalArgumentException("Not implemented range " + name + " of property " + ontologyProperty)
        }
        case other => throw new IllegalArgumentException("Property " + ontologyProperty + " does have invalid range " + other)
    }

    private def checkMultiplicationFactor(datatypeName : String)
    {
        if(factor != 1)
        {
            throw new IllegalArgumentException("multiplication factor cannot be specified for " + datatypeName)
        }
    }
    
    override val datasets = Set(DBpediaDatasets.OntologyProperties,DBpediaDatasets.SpecificProperties)

    override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
    {
        val graph = new ArrayBuffer[Quad]

        for(propertyNode <- node.property(templateProperty) if propertyNode.children.size > 0)
        {
            for( parseResult <- parser.parsePropertyNode(propertyNode, !ontologyProperty.isFunctional) )
            {
                val g = parseResult match
                {
                    case (value : Double, unit : UnitDatatype) => writeUnitValue(node, value, unit, subjectUri, propertyNode.sourceUri)
                    case value => writeValue(value, subjectUri, propertyNode.sourceUri)
                }
                graph ++= g
            }
        }
        
        graph
    }

    private def writeUnitValue(node : TemplateNode, value : Double, unit : UnitDatatype, subjectUri : String, sourceUri : String): Seq[Quad] =
    {
        //TODO better handling of inconvertible units
        if(unit.isInstanceOf[InconvertibleUnitDatatype])
        {
            val quad = new Quad(language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, value.toString, sourceUri, unit)
            return Seq(quad)
        }

        //Write generic property
        val stdValue = unit.toStandardUnit(value)
        
        val graph = new ArrayBuffer[Quad]

        graph += new Quad(language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, stdValue.toString, sourceUri, new Datatype("xsd:double"))
        
        // Write specific properties
        // FIXME: copy-and-paste in CalculateMapping
        for(classes <- node.getAnnotation(TemplateMapping.CLASS_ANNOTATION);
            currentClass <- classes)
        {
            for(specificPropertyUnit <- context.ontology.specializations.get((currentClass, ontologyProperty)))
            {
                 val outputValue = specificPropertyUnit.fromStandardUnit(stdValue)
                 val propertyUri = DBpediaNamespace.ONTOLOGY.append(currentClass.name+'/'+ontologyProperty.name)
                 val quad = new Quad(language, DBpediaDatasets.SpecificProperties, subjectUri,
                                     propertyUri, outputValue.toString, sourceUri, specificPropertyUnit)
                 graph += quad
            }
        }

        graph
    }

    private def writeValue(value : Any, subjectUri : String, sourceUri : String): Seq[Quad] =
    {
        val datatype = if(ontologyProperty.range.isInstanceOf[Datatype]) ontologyProperty.range.asInstanceOf[Datatype] else null

        Seq(new Quad(language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, value.toString, sourceUri, datatype))
    }
}
