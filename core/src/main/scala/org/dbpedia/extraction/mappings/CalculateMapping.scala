package org.dbpedia.extraction.mappings

import scala.collection.mutable.ArrayBuffer
import org.dbpedia.extraction.ontology.datatypes._
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.{OntologyClass, Ontology, DBpediaNamespace, OntologyProperty}

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
                    new Quad(context.language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, value, node.sourceUri, ontologyProperty.range.asInstanceOf[Datatype])
                }
                //IntegerParser
                case (value1 : Int, value2 : Int) =>
                {
                    val value = operation match
                    {
                        case "add" => (value1 + value2).toString
                    }
                    new Quad(context.language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, value, node.sourceUri, ontologyProperty.range.asInstanceOf[Datatype])
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
            val quad = new Quad(context.language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, value.toString, sourceUri, unit)
            return Seq(quad)
        }

        var graph = new ArrayBuffer[Quad]
        
        //Write generic property
        graph += new Quad(context.language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, value.toString, sourceUri, new Datatype("xsd:double"))

        // Write specific properties
        // FIXME: copy-and-paste in SimplePropertyMapping
        for(cls <- ontologyProperty.domain.relatedClasses)
        {
            for(specificPropertyUnit <- context.ontology.specializations.get((cls, ontologyProperty)))
            {
                 val outputValue = specificPropertyUnit.fromStandardUnit(value)
                 val propertyUri = DBpediaNamespace.ONTOLOGY.append(cls.name+'/'+ontologyProperty.name)
                 graph += new Quad(context.language, DBpediaDatasets.SpecificProperties, subjectUri,
                                     propertyUri, outputValue.toString, sourceUri, specificPropertyUnit)
            }
        }

        graph
    }
}
