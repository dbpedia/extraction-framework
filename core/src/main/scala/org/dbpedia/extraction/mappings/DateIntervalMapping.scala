package org.dbpedia.extraction.mappings

import org.apache.log4j.Logger
import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.{ExtractionLogger, ExtractionRecorder}
import org.dbpedia.extraction.config.dataparser.DataParserConfig
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, ExtractorRecord}
import org.dbpedia.extraction.dataparser.{DateRangeParser, DateTimeParser, StringParser}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty}
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.config.mappings.DateIntervalMappingConfig._
import org.dbpedia.extraction.wikiparser.{Node, NodeUtil, PropertyNode, TemplateNode}

import scala.language.reflectiveCalls
import scala.reflect.ClassTag

@SoftwareAgentAnnotation(classOf[DateIntervalMapping], AnnotationType.Extractor)
class DateIntervalMapping ( 
  val templateProperty : String, //TODO CreateMappingStats requires this to be public. Is there a better way?
  val startDateOntologyProperty : OntologyProperty, //TODO: rml mappings need this to be public (e.g. ModelMapper)
  val endDateOntologyProperty : OntologyProperty,   //TODO: same as above
  context : {
    def redirects : Redirects  // redirects required by DateTimeParser
    def ontology: Ontology
    def language : Language
  }
) 
extends PropertyMapping
{
  private val logger = ExtractionLogger.getLogger(getClass, context.language)

  private val dateIntervalParser = new DateRangeParser(context, rangeType(startDateOntologyProperty))
  private val startDateParser = new DateTimeParser(context, rangeType(startDateOntologyProperty))
  private val endDateParser = new DateTimeParser(context, rangeType(endDateOntologyProperty))

  private val splitPropertyNodeRegex = splitPropertyNodeMap.getOrElse(context.language.wikiCode, splitPropertyNodeMap("en"))
  private val presentStrings : Set[String] = presentMap.getOrElse(context.language.wikiCode, presentMap("en"))
  private val sinceString = sinceMap.getOrElse(context.language.wikiCode, sinceMap("en"))
  private val onwardString = onwardMap.getOrElse(context.language.wikiCode, onwardMap("en"))
  private val splitString = splitMap.getOrElse(context.language.wikiCode, splitMap("en"))

  // TODO: the parser should resolve HTML entities
  private val intervalSplitRegex = "(?iu)(" + DataParserConfig.dashVariationsRegex + "|&mdash;|&ndash;" + ( if (splitString.isEmpty) "" else "|" + splitString ) + ")"
  
  override val datasets = Set(DBpediaDatasets.OntologyPropertiesLiterals)

  override def extract(node : TemplateNode, subjectUri: String) : Seq[Quad] =
  {
    // replicate standard mechanism implemented by dataparsers
    for(propertyNode <- node.property(templateProperty))
    {
      // for now just return the first interval
      // waiting to decide what to do when there are several
      // see discussion at https://github.com/dbpedia/extraction-framework/pull/254
      // return NodeUtil.splitPropertyNode(propertyNode, splitPropertyNodeRegex).flatMap( node => extractInterval(node, subjectUri).toList )
      val splits =  NodeUtil.splitPropertyNode(propertyNode, splitPropertyNodeRegex)

      // set ER and NR
      qb.setNodeRecord(propertyNode.getNodeRecord)
      qb.setExtractor(ExtractorRecord(
        this.softwareAgentAnnotation,
        Seq(),
        Some(splits.size),
        Some(propertyNode.key),
        Some(node.title),
        propertyNode.containedTemplateNames()
      ))

      return splits.map( node => extractInterval(node, subjectUri) )
              .dropWhile(e => e.isEmpty).headOption.getOrElse(return Seq.empty)
    }
    
    Seq.empty
  }

  private val qb = QuadBuilder.dynamicPredicate(context.language, DBpediaDatasets.OntologyPropertiesLiterals)
  
  def extractInterval(propertyNode : PropertyNode, subjectUri: String) : Seq[Quad] =
  {
    //Split the node. Note that even if some of these hyphens are looking similar, they represent different Unicode numbers.
    val splitNodes = splitIntervalNode(propertyNode)

    //Can only map exactly two values onto an interval
    if(splitNodes.size > 2 || splitNodes.size  <= 0)
    {
      return Seq.empty
    }

    //fill base QuadBuilder
    qb.setSubject(subjectUri)
    qb.setSourceUri(propertyNode.sourceIri)

    if(splitNodes.size == 1){
      dateIntervalParser.parseRange(propertyNode) match{
        case Some(i) =>
          val qb1 = qb.clone
          qb1.setPredicate(startDateOntologyProperty)
          qb1.setValue(i.value._1.toString)
          qb1.setDatatype(i.value._1.datatype)

          val qb2 = qb.clone
          qb2.setPredicate(endDateOntologyProperty)
          qb2.setValue(i.value._2.toString)
          qb2.setDatatype(i.value._2.datatype)

          return Seq(qb1.getQuad, qb2.getQuad)
        case None =>
      }
    }

    //Parse start; return if no start year has been found
    val startDate = startDateParser.parseWithProvenance(splitNodes.head).getOrElse(return Seq.empty)

    //Parse end
    val endDateOpt = splitNodes match {
      //if there were two elements found
      case List(start, end) => end.retrieveText match
      {
        //if until "present" is specified through one of the special words, don't write end triple
        case Some(text : String) if presentStrings.contains(text.trim.toLowerCase) => None

        //normal case of specified end date
        case _ => endDateParser.parseWithProvenance(end)
      }

      //if there was only one element found
      case List(start) => StringParser.parseWithProvenance(start) match
      {
        //if in a "since xxx" construct, don't write end triple
        case Some(pr) if (pr.value.trim.toLowerCase.startsWith(sinceString)
                                  || pr.value.trim.toLowerCase.endsWith(onwardString)) => None

        //make start and end the same if there is no end specified
        case _ => Some(startDate)
      }

      case _ => throw new IllegalStateException("size of split nodes must be 0 < l < 3; is " + splitNodes.size)
    }

    //Write start date quad
    val qb1 = qb.clone
    qb1.setPredicate(startDateOntologyProperty)
    qb1.setValue(startDate.value.toString)
    qb1.setDatatype(startDate.value.datatype)

    //Writing the end date is optional if "until present" is specified
    for(endDate <- endDateOpt)
    {
      //Validate interval
      if(startDate.value > endDate.value)
      {
        logger.trace("startDate > endDate")
        return Seq(qb1.getQuad)
      }

      //Write end year quad
      val qb2 = qb.clone
      qb2.setPredicate(endDateOntologyProperty)
      qb2.setValue(endDate.value.toString)
      qb2.setDatatype(endDate.value.datatype)

      return Seq(qb1.getQuad, qb2.getQuad)
    }

    Seq(qb1.getQuad)
  }

  private def splitIntervalNode(propertyNode : PropertyNode) : List[PropertyNode] =
  {
    //Split the node. Note that even if some of these hyphens are looking similar, they represent different Unicode numbers.
    val splitNodes = NodeUtil.splitPropertyNode(propertyNode, intervalSplitRegex)

    //Did we split a date? e.g. 2009-10-13
    if(splitNodes.size > 2)
    {
      NodeUtil.splitPropertyNode(propertyNode, "\\s" + intervalSplitRegex + "\\s")
    }
    else
    {
      splitNodes
    }
  }
  
  private def rangeType(property: OntologyProperty) : Datatype =
  {
    if (! property.range.isInstanceOf[Datatype])
      throw new IllegalArgumentException("property "+property+" has range "+property.range+", which is not a datatype")
    property.range.asInstanceOf[Datatype]    
  }

}
