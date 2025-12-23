package org.dbpedia.extraction.mappings

import java.util.logging.Logger

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.datatypes._
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.dbpedia.extraction.ontology._
import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.ontology.{OntologyDatatypeProperty,OntologyClass,OntologyProperty,DBpediaNamespace}
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

class SimplePropertyMapping (
  val templateProperty : String, // IntermediateNodeMapping and CreateMappingStats requires this to be public
  val ontologyProperty : OntologyProperty,
  val select : String,  // rml mappings require this to be public (e.g. ModelMapper)
  val prefix : String,  // rml mappings require this to be public (e.g. ModelMapper)
  val suffix : String,  // rml mappings require this to be public (e.g. ModelMapper)
  val transform : String, // rml mappings require this to be public (e.g. ModelMapper)
  val unit : Datatype,  // rml mappings require this to be public (e.g. ModelMapper)
  var language : Language,  // rml mappings require this to be public (e.g. ModelMapper)
  val factor : Double,  // rml mappings require this to be public (e.g. ModelMapper)
  context : {
    def ontology : Ontology
    def redirects : Redirects  // redirects required by DateTimeParser and UnitValueParser
    def language : Language
  }
)
extends PropertyMapping
{
    val selector: List[_] => List[_] =
        select match {
            case "first" => _.take(1)
            case "last" => _.reverse.take(1)
            case null => identity
            case _ => throw new IllegalArgumentException("Only 'first' or 'last' are allowed in property 'select'")
        }

  /**
   * Transforms a text value appending/prepending a suffix/prefix.
   * Note that the value will be trimmed iff the function needs to apply suffix/prefix.
   * Otherwise the value will be left untouched.
   *
   * The suffix/prefix will never be trimmed.
   *
   * @param value The text value to transform
   * @return  Transformed text (after applying prefix/suffix)
   */
    private def valueTransformer(value : String) = {

        val p = prefix match {
            case _ : String => prefix
            case _ => ""
        }

        val s = suffix match {
            case _ : String => suffix
            case _ => ""
        }

        p + value.trim + s
    }

    if(language == null) language = context.language
    val languageResourceNamespace: String = language.resourceUri.namespace

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
      require(ontologyProperty.range.uri == RdfNamespace.fullUri(RdfNamespace.RDF, "langString"),
        "Language can only be specified for rdf:langString datatype properties")
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
        case dt : UnitDatatype      =>
          new UnitValueParser(context, if(unit != null) unit else dt, multiplicationFactor = factor)
        case dt : DimensionDatatype =>
          new UnitValueParser(context, if(unit != null) unit else dt, multiplicationFactor = factor)
        case dt : EnumerationDatatype =>
          checkMultiplicationFactor("EnumerationDatatype")
          new EnumerationParser(dt)
        case dt : Datatype => dt.name match
        {
            case "xsd:integer" => new IntegerParser(context, multiplicationFactor = factor)
            case "xsd:positiveInteger"    => new IntegerParser(context, multiplicationFactor = factor, validRange = i => i > 0)
            case "xsd:nonNegativeInteger" => new IntegerParser(context, multiplicationFactor = factor, validRange = i => i >= 0)
            case "xsd:nonPositiveInteger" => new IntegerParser(context, multiplicationFactor = factor, validRange = i => i <= 0)
            case "xsd:negativeInteger"    => new IntegerParser(context, multiplicationFactor = factor, validRange = i => i < 0)
            case "xsd:double" => new DoubleParser(context, multiplicationFactor = factor)
            case "xsd:float" => new DoubleParser(context, multiplicationFactor = factor)
            case "xsd:string" => // strings with no language tags
              checkMultiplicationFactor("xsd:string")
              StringParser
            case "rdf:langString" => // strings with language tags
              checkMultiplicationFactor("rdf:langString")
              StringParser
            case "xsd:anyURI" =>
              checkMultiplicationFactor("xsd:anyURI")
              new LinkParser(false)
            case "xsd:date" =>
              checkMultiplicationFactor("xsd:date")
              new DateTimeParser(context, dt)
            case "xsd:gYear" =>
              checkMultiplicationFactor("xsd:gYear")
              new DateTimeParser(context, dt)
            case "xsd:gYearMonth" =>
              checkMultiplicationFactor("xsd:gYearMonth")
              new DateTimeParser(context, dt)
            case "xsd:gMonthDay" =>
              checkMultiplicationFactor("xsd:gMonthDay")
              new DateTimeParser(context, dt)
            case "xsd:boolean" =>
              checkMultiplicationFactor("xsd:boolean")
              BooleanParser
            case name =>
              throw new IllegalArgumentException("Not implemented range " + name + " of property " + ontologyProperty)
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

    override val datasets = Set(DBpediaDatasets.OntologyPropertiesObjects, DBpediaDatasets.OntologyPropertiesLiterals, DBpediaDatasets.SpecificProperties)

    override def extract(node : TemplateNode, subjectUri : String): Seq[Quad] =
    {
        val graph = new ArrayBuffer[Quad]

        for(propertyNode <- node.property(templateProperty) if propertyNode.children.nonEmpty)
        {
            val parseResults = try {
              parser.parsePropertyNode(propertyNode, !ontologyProperty.isFunctional, transform, valueTransformer)
            } catch {
              case e: Throwable =>
                Logger
                  .getLogger(this.getClass.getName)
                  .warning("Failed to parse '" + propertyNode.key + "' from template '" + node.title.decoded
                    + "' in page '" + node.root.title.decoded + " reason: " + e.toString)
                List()
            }

            //get the property wikitext and plainText size
            val propertyNodeWikiLength = propertyNode.toWikiText.substring(propertyNode.toWikiText.indexOf('=')+1).trim.length // exclude '| propKey ='
            val propertyNodeTextLength = propertyNode.propertyNodeValueToPlainText.trim.length

            for( parseResult <- selector(parseResults) )
            {
                val resultString = parseResult.toString
                val isDBpediaResource = resultString.startsWith(languageResourceNamespace)

                // get the actual value length
                val resultLength = {
                  val length = resultString.length
                  // if it is a dbpedia resource, do not count http://xx.dbpedia.org/resource/
                  if (isDBpediaResource) length - languageResourceNamespace.length else length
                }

                val isHashIri = {
                  if (isDBpediaResource) {
                    val linkTitle = resultString.replace(languageResourceNamespace, "")

                    ExtractorUtils.collectInternalLinksFromNode(propertyNode) // find the link and check if it has a fragment
                      .exists(p => p.destination.encoded.equals(linkTitle) && p.destination.fragment != null)


                  } else false


                }

                //we add this in the triple context
                val resultLengthPercentageTxt =
                    "&split=" + parseResults.size +
                    "&wikiTextSize=" + propertyNodeWikiLength +
                    "&plainTextSize=" + propertyNodeTextLength +
                    "&valueSize=" + resultLength +
                    (if (isHashIri) "&objectHasFragment=" else "")
                val g = parseResult match
                {
                    case pr: ParseResult[Double] if pr.unit.nonEmpty => writeUnitValue(node, pr, subjectUri, propertyNode.sourceIri+resultLengthPercentageTxt)
                    case pr: ParseResult[_] => writeValue(pr, subjectUri, propertyNode.sourceIri+resultLengthPercentageTxt)
                }

                graph ++= g
            }
        }

        graph
    }

    private def writeUnitValue(node : TemplateNode, pr: ParseResult[Double], subjectUri : String, sourceUri : String): Seq[Quad] =
    {

        // fix for https://github.com/dbpedia/extraction-framework/issues/630
        //Write generic property
        val stdValue = pr.unit match {

          case Some(currentUnit) if currentUnit.isInstanceOf[InconvertibleUnitDatatype] => {

            val quad = new Quad(language, DBpediaDatasets.OntologyPropertiesLiterals, subjectUri, ontologyProperty, pr.value.toString, sourceUri, currentUnit)
            return Seq(quad)
          }

          case Some(u) if u.isInstanceOf[UnitDatatype] =>
            u.asInstanceOf[UnitDatatype].toStandardUnit(pr.value)

          case None => pr.value  //should not happen
        }
        // end of fix

        val graph = new ArrayBuffer[Quad]

        graph += new Quad(language, DBpediaDatasets.OntologyPropertiesLiterals, subjectUri, ontologyProperty, stdValue.toString, sourceUri, new Datatype("xsd:double"))

        // Write specific properties
        // FIXME: copy-and-paste in CalculateMapping
        for(templateClass <- node.getAnnotation(TemplateMapping.CLASS_ANNOTATION);
            currentClass <- templateClass.relatedClasses)
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

    private def writeValue(pr : ParseResult[_], subjectUri : String, sourceUri : String): Seq[Quad] =
    {
        val datatype = ontologyProperty.range match {
          case datatype1: Datatype => datatype1
          case _ => null
        }
        val mapDataset = if (datatype == null) DBpediaDatasets.OntologyPropertiesObjects else DBpediaDatasets.OntologyPropertiesLiterals

        Seq(new Quad(pr.lang.getOrElse(language), mapDataset, subjectUri, ontologyProperty, pr.value.toString, sourceUri, datatype))
    }
}
