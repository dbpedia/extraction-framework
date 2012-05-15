package org.dbpedia.extraction.mappings

import java.util.logging.Logger
import org.dbpedia.extraction.dataparser.DateTimeParser
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.config.mappings.DateIntervalMappingConfig._
import org.dbpedia.extraction.wikiparser.{PropertyNode, NodeUtil, TemplateNode}
import java.lang.IllegalStateException

class DateIntervalMapping( val templateProperty : String, //TODO CreateMappingStats requires this to be public. Is there a better way?
                           startDateOntologyProperty : OntologyProperty,
                           endDateOntologyProperty : OntologyProperty,
                           context : {
                               def redirects : Redirects  // redirects required by DateTimeParser
                               def language : Language } ) extends PropertyMapping
{
    private val logger = Logger.getLogger(classOf[DateIntervalMapping].getName)

    private val startDateParser = new DateTimeParser(context, startDateOntologyProperty.range.asInstanceOf[Datatype])
    private val endDateParser = new DateTimeParser(context, endDateOntologyProperty.range.asInstanceOf[Datatype])

    private val presentString = presentMap.getOrElse(context.language.wikiCode, presentMap("en"))

    // TODO: the parser should resolve HTML entities
    private val intervalSplitRegex = "(—|–|-|&mdash;|&ndash;)"
    
    override def extract(node : TemplateNode, subjectUri: String, pageContext : PageContext) : Seq[Quad] =
    {
        for(propertyNode <- node.property(templateProperty))
        {
            //Split the node. Note that even if some of these hyphens are looking similar, they represent different Unicode numbers.
            val splitNodes = splitPropertyNodes(propertyNode)

            //Can only map exactly two values onto an interval
            if(splitNodes.size > 2 || splitNodes.size  <= 0)
            {
                return Seq.empty
            }

            //Parse start; return if no start year has been found
            val startDate = startDateParser.parse(splitNodes(0)).getOrElse(return Seq.empty)

            //Parse end
            val endDateOpt = splitNodes match
            {
                //if there were two elements found
                case List(start, end) => end.retrieveText match
                {
                    //if until "present" is specified in words, don't write end triple
                    case Some(text : String) if text.trim == presentString => None

                    //normal case of specified end date
                    case _ => endDateParser.parse(end)
                }

                //make start and end the same if there is no end specified
                case List(start) => Some(startDate)

                case _ => throw new IllegalStateException("size of split nodes must be 0 < l < 3; is " + splitNodes.size)
            }

            //Write start date quad
            val quad1 = new Quad(context.language, DBpediaDatasets.OntologyProperties, subjectUri, startDateOntologyProperty, startDate.toString, propertyNode.sourceUri)

            //Writing the end date is optional if "until present" is specified
            for(endDate <- endDateOpt)
            {
                //Validate interval
                if(startDate > endDate)
                {
                    logger.fine("startDate > endDate")
                    return Seq(quad1)
                }

                //Write end year quad
                val quad2 = new Quad(context.language, DBpediaDatasets.OntologyProperties, subjectUri, endDateOntologyProperty, endDate.toString, propertyNode.sourceUri)

                return Seq(quad1, quad2)
            }

            return Seq(quad1)
        }
        
        Seq.empty
    }

    private def splitPropertyNodes(propertyNode : PropertyNode) : List[PropertyNode] =
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
}
