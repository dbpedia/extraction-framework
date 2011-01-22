package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.datatypes._
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser.{NodeUtil, TemplateNode}
import org.dbpedia.extraction.ontology.{OntologyDatatypeProperty, OntologyNamespaces, OntologyClass, OntologyProperty}

class SimplePropertyMapping( val templateProperty : String, //TODO IntermediaNodeMapping requires this to be public. Is there a better way?
                             ontologyProperty : OntologyProperty,
                             unit : Datatype,
                             extractionContext : ExtractionContext ) extends PropertyMapping
{
    validate()

    /**
     * Validates the mapping
     */
    private def validate()
    {
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
    }
    
    private val parser : DataParser = ontologyProperty.range match
    {
        //TODO
        case c : OntologyClass if ontologyProperty.name != "foaf:homepage" => new ObjectParser()
        case c : OntologyClass => new LinkParser()
        case dt : UnitDatatype => new UnitValueParser(extractionContext, if(unit != null) unit else dt)
        case dt : DimensionDatatype => new UnitValueParser(extractionContext, if(unit != null) unit else dt)
        case dt : EnumerationDatatype => new EnumerationParser(dt)
        case dt : Datatype => dt.name match
        {
            case "xsd:integer" => new IntegerParser(extractionContext)
            case "xsd:positiveInteger"    => new IntegerParser(extractionContext, validRange = (i => i > 0))
            case "xsd:nonNegativeInteger" => new IntegerParser(extractionContext, validRange = (i => i >=0))
            case "xsd:nonPositiveInteger" => new IntegerParser(extractionContext, validRange = (i => i <=0))
            case "xsd:negativeInteger"    => new IntegerParser(extractionContext, validRange = (i => i < 0))            
            case "xsd:double" => new DoubleParser(extractionContext)
            case "xsd:float" => new DoubleParser(extractionContext)
            case "xsd:string" => StringParser
            case "xsd:anyURI" => new LinkParser(false)
            case "xsd:date" => new DateTimeParser(extractionContext, dt)
            case "xsd:gYear" => new DateTimeParser(extractionContext, dt)
            case "xsd:gYearMonth" => new DateTimeParser(extractionContext, dt)
            case "xsd:gMonthDay" => new DateTimeParser(extractionContext, dt)
            case "xsd:boolean" => BooleanParser
            case name => throw new IllegalArgumentException("Not implemented range " + name + " of property " + ontologyProperty)
        }
        case dt => throw new IllegalArgumentException("Property " + ontologyProperty + " does have invalid range " + dt)
    }
    
    override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        var graph = new Graph()

        for(propertyNode <- node.property(templateProperty) if propertyNode.children.size > 0)
        {
            for( parseResult <- parser.parsePropertyNode(propertyNode, !ontologyProperty.isFunctional) )
            {
                val g = parseResult match
                {
                    case (value : Double, unit : UnitDatatype) => writeUnitValue(node, value, unit, subjectUri, propertyNode.sourceUri)
                    case value => writeValue(value, subjectUri, propertyNode.sourceUri)
                }
                graph = graph.merge(g)
            }
        }
        
        graph
    }

    private def writeUnitValue(node : TemplateNode, value : Double, unit : UnitDatatype, subjectUri : String, sourceUri : String) : Graph =
    {
        //TODO better handling of inconvertible units
        if(unit.isInstanceOf[InconvertibleUnitDatatype])
        {
            val quad = new Quad(extractionContext, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, value.toString, sourceUri, unit)
            return new Graph(quad)
        }

        //Write generic property
        val stdValue = unit.toStandardUnit(value)
        val quad = new Quad(extractionContext, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, stdValue.toString, sourceUri, new Datatype("xsd:double"))
        var graph = new Graph(quad)

        //Write specific properties
        for(classAnnotation <- node.annotation(TemplateMapping.CLASS_ANNOTATION);
            currentClass <- classAnnotation.asInstanceOf[List[OntologyClass]])
        {
            for(specificPropertyUnit <- extractionContext.ontology.specializations.get((currentClass, ontologyProperty)))
            {
                 val outputValue = specificPropertyUnit.fromStandardUnit(stdValue)
                 val propertyUri = OntologyNamespaces.DBPEDIA_SPECIFICPROPERTY_NAMESPACE + currentClass.name + "/" + ontologyProperty.name
                 val quad = new Quad(extractionContext, DBpediaDatasets.SpecificProperties, subjectUri,
                                     propertyUri, outputValue.toString, sourceUri, specificPropertyUnit)
                 graph = graph.merge(new Graph(quad))
            }
        }

        graph
    }

    private def writeValue(value : Any, subjectUri : String, sourceUri : String) : Graph =
    {
        val datatype = if(ontologyProperty.range.isInstanceOf[Datatype]) ontologyProperty.range.asInstanceOf[Datatype] else null

        val quad = new Quad( extractionContext, DBpediaDatasets.OntologyProperties, subjectUri,
                             ontologyProperty, value.toString, sourceUri, datatype )

        new Graph(quad)
    }
}
