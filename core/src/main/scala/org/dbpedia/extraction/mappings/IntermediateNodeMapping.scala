package org.dbpedia.extraction.mappings

import java.util.logging.Logger
import org.dbpedia.extraction.wikiparser.{NodeUtil, TemplateNode}
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}
import org.dbpedia.extraction.util.Language

class IntermediateNodeMapping(nodeClass : OntologyClass,
                              correspondingProperty : OntologyProperty,
                              val mappings : List[PropertyMapping], // must be public val for statistics
                              context : {
                                  def ontology : Ontology
                                  def language : Language } ) extends PropertyMapping
{
    private val logger = Logger.getLogger(classOf[IntermediateNodeMapping].getName)

    private val splitRegex = """<br\s*\/?>"""

    override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        var graph = new Graph()

        val affectedTemplatePropertyNodes = mappings.flatMap(_ match
            {
                case spm : SimplePropertyMapping => node.property(spm.templateProperty)
                case _ => None
            }).toSet //e.g. Set(leader_name, leader_title)

        val valueNodes = affectedTemplatePropertyNodes.map(NodeUtil.splitPropertyNode(_, splitRegex))

        //more than one template proerty is affected (e.g. leader_name, leader_title)
        if(affectedTemplatePropertyNodes.size > 1)
        {
            //require their values to be all singles
            if(valueNodes.forall(_.size == 1))
            {
                graph = graph.merge(createInstance(node, subjectUri, pageContext))
            }
            else
            {
                //TODO muliple properties having multiple values
                /**
                 * fictive example:
                 * leader_name = Bill_Gates<br>Steve_Jobs
                 * leader_title = Microsoft dictator<br>Apple evangelist
                 */
                logger.fine("IntermediateNodeMapping for muliple properties having multiple values not implemented!")
            }
        }
        //one template property is affected (e.g. engine)
        else if(affectedTemplatePropertyNodes.size == 1)
        {
            //allow multiple values in this property
            for( valueNodesForOneProperty <- valueNodes ;
                 value <- valueNodesForOneProperty )
            {
                graph = graph.merge(createInstance(value.parent.asInstanceOf[TemplateNode], subjectUri, pageContext))
            }
        }

        graph
    }

    private def createInstance(node : TemplateNode, originalSubjectUri : String, pageContext : PageContext) : Graph =
    {
        val instanceUri = pageContext.generateUri(originalSubjectUri, node)

        def writeTypes(clazz : OntologyClass, graph : Graph) : Graph =
        {
            var thisGraph = graph

            val quad = new Quad(context.language, DBpediaDatasets.OntologyTypes, instanceUri, context.ontology.properties("rdf:type"), clazz.uri, node.sourceUri)
            thisGraph = graph.merge(new Graph(quad))

            for(baseClass <- clazz.subClassOf)
            {
                thisGraph = writeTypes(baseClass, thisGraph)
            }

            for(eqClass <- clazz.equivalentClasses)
            {
                thisGraph = writeTypes(eqClass, thisGraph)
            }

            thisGraph
        }

        // extract quads
        var graph = mappings.map(propertyMapping => propertyMapping.extract(node, instanceUri, pageContext)).reduceLeft(_ merge _)

        // write types
        if(!graph.isEmpty)
        {
            graph = writeTypes(nodeClass, graph)

            val quad2 = new Quad(context.language, DBpediaDatasets.OntologyProperties, originalSubjectUri, correspondingProperty, instanceUri, node.sourceUri);
            graph = graph.merge(new Graph(quad2))
        }

        graph
    }
}
