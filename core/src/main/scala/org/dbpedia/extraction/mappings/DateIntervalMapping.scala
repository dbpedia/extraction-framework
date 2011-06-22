package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.OntologyProperty
import java.util.logging.{Logger}
import org.dbpedia.extraction.dataparser.DateTimeParser
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.wikiparser.{NodeUtil, TemplateNode}
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}

class DateIntervalMapping( templateProperty : String,
                           startDateOntologyProperty : OntologyProperty,
                           endDateOntologyProperty : OntologyProperty,
                           extractionContext : ExtractionContext ) extends PropertyMapping
{
    private val logger = Logger.getLogger(classOf[DateIntervalMapping].getName)

    private val startDateParser = new DateTimeParser(extractionContext, startDateOntologyProperty.range.asInstanceOf[Datatype])
    private val endDateParser = new DateTimeParser(extractionContext, endDateOntologyProperty.range.asInstanceOf[Datatype])
    
    override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        for(propertyNode <- node.property(templateProperty))
        {
            //Split the node. Note that even if some of these hyphens are looking similar, they represent different Unicode numbers.
            val splitNodes = NodeUtil.splitPropertyNode(propertyNode, "(—|–|-|&mdash;|&ndash;)")

            //Parse
            val startDateOpt = if(splitNodes.size >= 1) startDateParser.parse(splitNodes(0)) else None
            val endDateOpt = if (splitNodes.size >= 2) endDateParser.parse(splitNodes(1)) else None

            //Return if no start year has been found
            if(startDateOpt.isEmpty) return new Graph()

            val startDate = startDateOpt.get
            
            //Write start date quad
            val quad1 = new Quad(extractionContext.language, DBpediaDatasets.OntologyProperties, subjectUri, startDateOntologyProperty, startDate.toString, propertyNode.sourceUri)

            //Writing the end date is optional
            for(endDate <- endDateOpt)
            {
                //Validate interval
                if(startDate > endDate)
                {
                    logger.fine("startDate > endDate")
                    return new Graph(quad1 :: Nil)
                }

                //Write end year quad
                val quad2 = new Quad(extractionContext.language, DBpediaDatasets.OntologyProperties, subjectUri, endDateOntologyProperty, endDate.toString, propertyNode.sourceUri)

                return new Graph(quad1 :: quad2 :: Nil)
            }

            return new Graph(quad1 :: Nil)
        }
        
        return new Graph()
    }
}
