package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.{OntologyClass, Ontology, OntologyNamespaces, OntologyProperty}
import org.dbpedia.extraction.ontology.datatypes._
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.wikiparser.{TemplateNode}
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad, IriRef, TypedLiteral}

class CalculateMapping( val templateProperty1 : String,
                        val templateProperty2 : String,
                        unit1 : Datatype,
                        unit2 : Datatype,
                        operation : String,
                        ontologyProperty : OntologyProperty,
                        context : {
                            def ontology : Ontology
                            def redirects : Redirects  // redirects required by UnitValueParser
                            def language : Language } ) extends PropertyMapping
{
    require(operation == "add", "Operation '" + operation + "' is not supported. Supported operations: 'add'")

    val parser1 : DataParser = ontologyProperty.range match
    {
        case dt : UnitDatatype => new UnitValueParser(context, unit1)
        case dt : DimensionDatatype => new UnitValueParser(context, unit1)
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

    val parser2 : DataParser = ontologyProperty.range match
    {
        case dt : UnitDatatype => new UnitValueParser(context, unit2)
        case dt : DimensionDatatype => new UnitValueParser(context, unit2)
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
    
    def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Graph =
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
                    new Quad(DBpediaDatasets.OntologyProperties, new IriRef(subjectUri), new IriRef(ontologyProperty), new TypedLiteral((value1 + value2).toString, ontologyProperty.range.asInstanceOf[Datatype]), new IriRef(node.sourceUri))
                }
                //IntegerParser
                case (value1 : Int, value2 : Int) =>
                {
                    new Quad(DBpediaDatasets.OntologyProperties, new IriRef(subjectUri), new IriRef(ontologyProperty), new TypedLiteral((value1 + value2).toString, ontologyProperty.range.asInstanceOf[Datatype]), new IriRef(node.sourceUri))
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
            val quad = new Quad(DBpediaDatasets.OntologyProperties, new IriRef(subjectUri), new IriRef(ontologyProperty), new TypedLiteral(value.toString, unit), new IriRef(sourceUri))
            return new Graph(quad)
        }

        //Write generic property
        val stdValue = unit.toStandardUnit(value)
        val quad = new Quad(DBpediaDatasets.OntologyProperties, new IriRef(subjectUri), new IriRef(ontologyProperty), new TypedLiteral(stdValue.toString, new Datatype("xsd:double")), new IriRef(sourceUri))
        var graph = new Graph(quad)

        //Write specific properties
        var currentClass = ontologyProperty.domain
        while(currentClass != null)
        {
            for(specificPropertyUnit <- context.ontology.specializations.get((currentClass, ontologyProperty)))
            {
                 val outputValue = specificPropertyUnit.fromStandardUnit(stdValue)
                 val propertyUri = OntologyNamespaces.DBPEDIA_SPECIFICPROPERTY_NAMESPACE + currentClass.name + "/" + ontologyProperty.name
                 val quad = new Quad(DBpediaDatasets.SpecificProperties, new IriRef(subjectUri),
                                     new IriRef(propertyUri), new TypedLiteral(outputValue.toString, specificPropertyUnit), new IriRef(sourceUri))
                 graph = graph.merge(new Graph(quad))
            }

            //TODO there can be multiple parent classes
            currentClass = currentClass.subClassOf.apply(0)
        }

        graph
    }
}
