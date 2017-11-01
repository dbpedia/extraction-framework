package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.{ExtractionRecorder, RecordCause, RecordEntry}
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, DBpediaMetadata, ExtractorRecord, ProvenanceRecord}
import org.dbpedia.extraction.ontology.datatypes._
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.dbpedia.extraction.ontology._
import org.dbpedia.extraction.wikiparser.{Node, TemplateNode}
import org.dbpedia.extraction.ontology.{DBpediaNamespace, OntologyClass, OntologyDatatypeProperty, OntologyProperty}

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls
import scala.reflect.ClassTag

@SoftwareAgentAnnotation(classOf[SimplePropertyMapping], AnnotationType.Extractor)
class SimplePropertyMapping (
  val templateName: String,
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
    def recorder[T: ClassTag] : ExtractionRecorder[T]
  }
)
extends PropertyMapping
{
    private val recorder = context.recorder[Node]

    val selector: List[ParseResult[_]] => List[ParseResult[_]] =
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
                case (dt1 : UnitDatatype, dt2 : UnitDatatype) =>
                  require(dt1.dimension == dt2.dimension, "Unit must conform to the dimension of the range of the ontology property")

                case (dt1 : UnitDatatype, dt2 : DimensionDatatype) =>
                  require(dt1.dimension == dt2, "Unit must conform to the dimension of the range of the ontology property")

                case (dt1 : DimensionDatatype, dt2 : UnitDatatype) =>
                  require(dt1 == dt2.dimension, "The dimension of unit must match the range of the ontology property")

                case (dt1 : DimensionDatatype, dt2 : DimensionDatatype) =>
                  require(dt1 == dt2,"Unit must match the range of the ontology property")

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
    
    private val parser : DataParser[_] = ontologyProperty.range match
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
              new DateTimeParser(context, dt, strict = false, tryMinorTypes = true)
            case "xsd:gYear" =>
              checkMultiplicationFactor("xsd:gYear")
              new DateTimeParser(context, dt)
            case "xsd:gYearMonth" =>
              checkMultiplicationFactor("xsd:gYearMonth")
              new DateTimeParser(context, dt, strict = false, tryMinorTypes = true)
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
                //recorder.record(new RecordEntry[_](node, RecordCause.Warning, node.root.title.language, "Failed to parse '" + propertyNode.key + "' from template '" + node.title.decoded + "' in page '" + node.root.title.decoded, e))
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
                val g = parseResult match {
                    case ParseResult(_: Double, _, u, _) if u.nonEmpty =>
                      writeUnitValue(node, parseResult.asInstanceOf[ParseResult[Double]], subjectUri, propertyNode.sourceIri+resultLengthPercentageTxt)
                    case pr: ParseResult[_] =>
                      writeValue(pr, subjectUri, propertyNode.sourceIri+resultLengthPercentageTxt)
                }

              val annotation = SoftwareAgentAnnotation.getAnnotationIri(this.getClass)

              g.foreach(q => q.setProvenanceRecord(new DBpediaMetadata(
                propertyNode.getNodeRecord,
                if(q.dataset != null) Seq(RdfNamespace.fullUri(DBpediaNamespace.DATASET, q.dataset)) else Seq(),
                Some(ExtractorRecord(
                  annotation.toString,
                  parseResult.provenance.getOrElse(throw new IllegalArgumentException("No ParserRecord found.")),
                  Some(parseResults.size),
                  Some(templateName),
                  Some(templateProperty),
                  Some("Mapping " + language.wikiCode + ":" + templateName),
                  Node.collectTemplates(propertyNode, Set.empty).map(x => x.title.decoded)
                )),
                None,
                Seq.empty
              )))

              graph ++= g
            }
        }
        
        graph
    }

    private def writeUnitValue(node : TemplateNode, pr: ParseResult[Double], subjectUri : String, sourceUri : String): Seq[Quad] =
    {
        //Write generic property
        val stdValue = pr.unit match{
          case Some(u) if u.isInstanceOf[InconvertibleUnitDatatype] => {
            return Seq(new Quad(language, DBpediaDatasets.OntologyPropertiesLiterals, subjectUri, ontologyProperty, pr.value.toString, sourceUri, unit))
          }
          case Some(u) if u.isInstanceOf[UnitDatatype] =>
            u.asInstanceOf[UnitDatatype].toStandardUnit(pr.value)
          case None => pr.value  //should not happen
        }
        
        val graph = new ArrayBuffer[Quad]

        graph += new Quad(language, DBpediaDatasets.OntologyPropertiesLiterals, subjectUri, ontologyProperty, stdValue.toString, sourceUri, new Datatype("xsd:double"))
        
        // Write specific properties
        for(templateClass <- node.getAnnotation(TemplateMapping.CLASS_ANNOTATION);
            currentClass <- templateClass.relatedClasses)
        {
            for(specificPropertyUnit <- context.ontology.specializations.get((currentClass, ontologyProperty)))
            {
                 val outputValue = specificPropertyUnit.fromStandardUnit(stdValue)
                 val propertyUri = DBpediaNamespace.ONTOLOGY.append(currentClass.name+'/'+ontologyProperty.name)
                 val quad = new Quad(language, DBpediaDatasets.SpecificProperties, subjectUri, propertyUri, outputValue.toString, sourceUri, specificPropertyUnit)
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

  /**
    * provides a summary about this extractor used for provenance
    *
    * @return
    */
}
