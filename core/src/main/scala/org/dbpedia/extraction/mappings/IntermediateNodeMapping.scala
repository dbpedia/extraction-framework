package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.{OntologyClass, OntologyProperty}
import java.util.logging.{Logger}
import org.dbpedia.extraction.wikiparser.{NodeUtil, TemplateNode}
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}

class IntermediateNodeMapping(nodeClass : OntologyClass,
                              correspondingProperty : OntologyProperty,
                              mappings : List[PropertyMapping],
                              extractionContext : ExtractionContext) extends PropertyMapping
{
    private val logger = Logger.getLogger(classOf[IntermediateNodeMapping].getName)
    
    override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        var graph = new Graph()

        val affectedTemplateProperties =
        {
            for(propertyMapping <- mappings; if propertyMapping.isInstanceOf[SimplePropertyMapping])
                yield propertyMapping.asInstanceOf[SimplePropertyMapping].templateProperty
        }.toSet
            
        if (affectedTemplateProperties.size == 1)
        {
            for(affectedTemplateProperty <- affectedTemplateProperties;
                propertyNode <- node.property(affectedTemplateProperty) )
            {
                val newPropertyNodes = NodeUtil.splitPropertyNode(propertyNode, """<br\s*\/?>""")
                if (newPropertyNodes.size > 1)
                {
                    for(newPropertyNode <- newPropertyNodes)
                    {
                        val instanceUri = pageContext.generateUri(subjectUri, newPropertyNode)

                        graph = graph.merge(createInstance(newPropertyNode.parent.asInstanceOf[TemplateNode], instanceUri, subjectUri, pageContext))
                    }
                }
                else
                {
                    val instanceUri = pageContext.generateUri(subjectUri, propertyNode)

                    graph = graph.merge(createInstance(node, instanceUri, subjectUri, pageContext))
                }
            }
        }
        else
        {
            //TODO
            logger.fine("IntermediaNodeMapping for more than one affected template property not implemented!")
        }

        return graph
    }
    
    private def createInstance(node : TemplateNode, instanceUri : String, originalSubjectUri : String, pageContext : PageContext) : Graph =
    {
        // extract quads
        var graph = mappings.map(propertyMapping => propertyMapping.extract(node, instanceUri, pageContext)).reduceLeft(_ merge _)

        // write types
        if(!graph.isEmpty)
        {
            var currentClass = nodeClass
            while(currentClass != null)
            {
                val quad = new Quad(extractionContext.language, DBpediaDatasets.OntologyTypes, instanceUri, extractionContext.ontology.getProperty("rdf:type").get, currentClass.uri, node.sourceUri)
                graph = graph.merge(new Graph(quad))
                
                currentClass = currentClass.subClassOf
            }

            val quad2 = new Quad(extractionContext.language, DBpediaDatasets.OntologyProperties, originalSubjectUri, correspondingProperty, instanceUri, node.sourceUri);
            graph = graph.merge(new Graph(quad2))
        }
        
        return graph
    }
}
