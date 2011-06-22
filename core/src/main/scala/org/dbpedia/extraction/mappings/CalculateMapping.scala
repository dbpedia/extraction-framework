package org.dbpedia.extraction.mappings

import _root_.org.dbpedia.extraction.ontology.{OntologyNamespaces, OntologyProperty}
import org.dbpedia.extraction.ontology.datatypes._
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.wikiparser.{TemplateNode}
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}

class CalculateMapping( templateProperty1 : String,
                        templateProperty2 : String,
                        unit1 : Datatype,
                        unit2 : Datatype,
                        operation : String,
                        ontologyProperty : OntologyProperty,
                        extractionContext : ExtractionContext ) extends PropertyMapping
{
    require(operation == "add", "Operation '" + operation + "' is not supported. Supported operations: 'add'")

    //TODO use different parsers for unit1 and unit2
    val parser : DataParser = ontologyProperty.range match
    {
        case dt : UnitDatatype => new UnitValueParser(extractionContext, unit1)
        case dt : DimensionDatatype => new UnitValueParser(extractionContext, unit1)
        case dt : Datatype => dt.name match
        {
            case "xsd:integer" => new IntegerParser(extractionContext)
            case "xsd:positiveInteger"    => new IntegerParser(extractionContext, validRange = (i => i > 0))
            case "xsd:nonNegativeInteger" => new IntegerParser(extractionContext, validRange = (i => i >=0))
            case "xsd:nonPositiveInteger" => new IntegerParser(extractionContext, validRange = (i => i <=0))
            case "xsd:negativeInteger"    => new IntegerParser(extractionContext, validRange = (i => i < 0))
            case "xsd:double" => new DoubleParser(extractionContext)
            case "xsd:float" => new DoubleParser(extractionContext)
            case name => throw new IllegalArgumentException("Datatype " + name + " is not supported by CalculateMapping")
        }
        case dt => throw new IllegalArgumentException("Datatype " + dt + " is not supported by CalculateMapping")
    }
    
    def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        for( property1 <- node.property(templateProperty1);
             property2 <- node.property(templateProperty2);
             parseResult1 <- parser.parse(property1);
             parseResult2 <- parser.parse(property2) )
        {
            val quad = (parseResult1, parseResult2) match
            {
                //UnitValueParser
                case((value1 : Double, unit1 : UnitDatatype), (value2 : Double, unit2 : UnitDatatype)) =>
                {
                    //TODO convert both units to their std unit
                    assert(unit1.name == unit2.name, "Incompatible units")

                    return writeUnitValue(value1 + value2, unit1, subjectUri, node.sourceUri)
                }
                //DoubleParser
                case (value1 : Double, value2 : Double) =>
                {
                    new Quad(extractionContext.language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, (value1 + value2).toString, node.sourceUri, ontologyProperty.range.asInstanceOf[Datatype])
                }
                //IntegerParser
                case (value1 : Int, value2 : Int) =>
                {
                    new Quad(extractionContext.language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, (value1 + value2).toString, node.sourceUri, ontologyProperty.range.asInstanceOf[Datatype])
                }
            }

            return new Graph(quad)
        }
        
        new Graph()
    }

    //TODO duplicated from SimplePropertyMapping
    private def writeUnitValue(value : Double, unit : UnitDatatype, subjectUri : String, sourceUri : String) : Graph =
    {
        //TODO better handling of inconvertible units
        if(unit.isInstanceOf[InconvertibleUnitDatatype])
        {
            val quad = new Quad(extractionContext.language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, value.toString, sourceUri, unit)
            return new Graph(quad)
        }

        //Write generic property
        val stdValue = unit.toStandardUnit(value)
        val quad = new Quad(extractionContext.language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, stdValue.toString, sourceUri, new Datatype("xsd:double"))
        var graph = new Graph(quad)

        //Write specific properties
        var currentClass = ontologyProperty.domain
        while(currentClass != null)
        {
            for(specificPropertyUnit <- extractionContext.ontology.specializations.get((currentClass, ontologyProperty)))
            {
                 val outputValue = specificPropertyUnit.fromStandardUnit(stdValue)
                 val propertyUri = OntologyNamespaces.DBPEDIA_SPECIFICPROPERTY_NAMESPACE + currentClass.name + "/" + ontologyProperty.name
                 val quad = new Quad(extractionContext.language, DBpediaDatasets.SpecificProperties, subjectUri,
                                     propertyUri, outputValue.toString, sourceUri, specificPropertyUnit)
                 graph = graph.merge(new Graph(quad))
            }

            currentClass = currentClass.subClassOf
        }

        graph
    }
}
