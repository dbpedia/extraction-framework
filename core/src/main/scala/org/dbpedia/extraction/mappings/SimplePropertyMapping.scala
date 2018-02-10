package org.dbpedia.extraction.mappings

import org.apache.log4j.Level
import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.{ExtractionLogger, ExtractionRecorder, RecordEntry}
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, ExtractorRecord}
import org.dbpedia.extraction.ontology.datatypes._
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.dbpedia.extraction.ontology._
import org.dbpedia.extraction.wikiparser.{Node, PropertyNode, TemplateNode}
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
  }
)
extends PropertyMapping
{

  private val logger = ExtractionLogger.getLogger(getClass, context.language)

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

                case _ if unit != null => require(unit == ontologyProperty.range, "Unit must be compatible to the range of the ontology property: " + templateProperty + " in " + templateName + ": " + unit + " vs. " + ontologyProperty.range)
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
      val baseBuilder = new QuadBuilder(Some(subjectUri), Some(ontologyProperty), None, None, language, None, None, None)

        for(propertyNode <- node.property(templateProperty) if propertyNode.children.nonEmpty)
        {
          val parseResults = try {
            parser.parsePropertyNode(propertyNode, !ontologyProperty.isFunctional, transform, valueTransformer)
          } catch {
            case e: Throwable =>
              logger.warn(new RecordEntry[Node](node, node.root.title.language, "Failed to parse '" + propertyNode.key + "' from template '" + node.title.decoded + "' in page '" + node.root.title.decoded, e))
              List()
          }


          for( parseResult <- selector(parseResults) )
          {
            baseBuilder.setSourceUri(propertyNode.sourceIri)
            baseBuilder.setExtractor(ExtractorRecord(
              this.softwareAgentAnnotation,
              parseResult.provenance.toList,
              Some(parseResults.size),
              Some(templateProperty),
              Some(node.title),
              propertyNode.containedTemplateNames()
            ))
            val nr = propertyNode.getNodeRecord
            baseBuilder.setNodeRecord(nr)

            val g = parseResult match {
                case ParseResult(_: Double, _, u, _) if u.nonEmpty =>
                  writeUnitValue(node, parseResult.asInstanceOf[ParseResult[Double]], baseBuilder)
                case pr: ParseResult[_] =>
                  writeValue(pr, baseBuilder)
            }

            graph ++= g
          }
        }
        
        graph
    }

    private def writeUnitValue(node : TemplateNode, pr: ParseResult[Double], baseBuilder: QuadBuilder): Seq[Quad] =
    {
        //Write generic property
        val stdValue = pr.unit match{
          case Some(u) if u.isInstanceOf[InconvertibleUnitDatatype] => {
            val qb = baseBuilder.clone
            qb.setPredicate(ontologyProperty)
            qb.setValue(pr.value.toString)
            qb.setDataset(DBpediaDatasets.OntologyPropertiesLiterals)
            qb.setDatatype(u)
            return Seq(qb.getQuad)
          }
          case Some(u) if u.isInstanceOf[UnitDatatype] =>
            u.asInstanceOf[UnitDatatype].toStandardUnit(pr.value)
          case None => pr.value  //should not happen
        }
        
        val graph = new ArrayBuffer[Quad]
        val qb = baseBuilder.clone
        qb.setPredicate(ontologyProperty)
        qb.setValue(stdValue.toString)
        qb.setDataset(DBpediaDatasets.OntologyPropertiesLiterals)
        qb.setDatatype(new Datatype("xsd:double"))
        graph += qb.getQuad
        
        // Write specific properties
        for(templateClass <- node.getAnnotation(TemplateMapping.CLASS_ANNOTATION);
            currentClass <- templateClass.relatedClasses)
        {
          for(specificPropertyUnit <- context.ontology.specializations.get((currentClass, ontologyProperty)))
          {
            val outputValue = specificPropertyUnit.fromStandardUnit(stdValue)
            val propertyUri = DBpediaNamespace.ONTOLOGY.append(currentClass.name+'/'+ontologyProperty.name)
            val qbi = baseBuilder.clone
            qbi.setPredicate(propertyUri)
            qbi.setValue(outputValue.toString)
            qbi.setDataset(DBpediaDatasets.SpecificProperties)
            qbi.setDatatype(specificPropertyUnit)
            graph += qbi.getQuad
          }
        }

        graph
    }

    private def writeValue(pr : ParseResult[_], baseBuilder: QuadBuilder): Seq[Quad] =
    {
        val datatype = ontologyProperty.range match {
          case datatype1: Datatype => datatype1
          case _ => null
        }
        val qb = baseBuilder.clone
        qb.setPredicate(ontologyProperty)
        qb.setValue(pr.value.toString)
        qb.setDatatype(datatype)
        qb.setLanguage(pr.lang.getOrElse(language))
        qb.setDataset(if (datatype == null) DBpediaDatasets.OntologyPropertiesObjects else DBpediaDatasets.OntologyPropertiesLiterals)
        Seq(qb.getQuad)
    }

  /**
    * Deprecated way of passing provenance information as fragment of the sourceUri
    * @param pr
    * @param propertyNode
    * @return
    */
  @Deprecated
    private def oldSourceUriExtension(pr: ParseResult[_], propertyNode: PropertyNode): String = {

      //get the property wikitext and plainText size
      val propertyNodeWikiLength = propertyNode.toWikiText.substring(propertyNode.toWikiText.indexOf('=')+1).trim.length // exclude '| propKey ='
      val propertyNodeTextLength = propertyNode.propertyNodeValueToPlainText.trim.length
      //TODO -> get this into a prov object
      val resultString = pr.value.toString
      val isDBpediaResource = resultString.startsWith(languageResourceNamespace)

      // get the actual value length
      val resultLength = {
        val length = resultString.length
        // if it is a dbpedia resource, do not count http://xx.dbpedia.org/resource/
        if (isDBpediaResource) length - languageResourceNamespace.length else length
      }

      val iriHasFragment = {
        if (isDBpediaResource) {
          val linkTitle = resultString.replace(languageResourceNamespace, "")

          ExtractorUtils.collectInternalLinksFromNode(propertyNode) // find the link and check if it has a fragment
            .exists(p => p.destination.encoded.equals(linkTitle) && p.destination.fragment != null)
        } else false
      }

      //we add this in the triple context
      "&wikiTextSize=" + propertyNodeWikiLength +
      "&plainTextSize=" + propertyNodeTextLength +
      "&valueSize=" + resultLength +
      (if (iriHasFragment) "&objectHasFragment=" else "")

    }
}
