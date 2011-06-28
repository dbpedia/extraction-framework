package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.{OntologyClass, OntologyProperty}
import org.dbpedia.extraction.wikiparser.{Node, PropertyNode, TemplateNode}
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad}

class TemplateMapping( mapToClass : OntologyClass,
                       correspondingClass : OntologyClass,
                       correspondingProperty : OntologyProperty,
                       mappings : List[PropertyMapping],
                       extractionContext : ExtractionContext ) extends ClassMapping
{
    private val (propertyMappings, constantMappings) = splitMappings

    override def extract(node : Node, subjectUri : String, pageContext : PageContext) : Graph =
    {
        val graph = node match
        {
            case templateNode : TemplateNode => extractTemplate(templateNode, subjectUri, pageContext)
            case _ => new Graph()
        }

        //do these only once
        constantMappings.map(mapping => mapping.extract(node.asInstanceOf[TemplateNode], subjectUri, null))
                                .foldRight(graph)(_ merge _)
    }

    def extractTemplate(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        val pageNode = node.root

        pageNode.annotation(TemplateMapping.CLASS_ANNOTATION) match
        {
            case None => //So far, no template has been mapped on this page
            {
                //Add ontology instance
                val typeGraph = createInstance(subjectUri, node)

                //Extract properties
                propertyMappings.map(mapping => mapping.extract(node, subjectUri, pageContext))
                                .foldLeft(typeGraph)(_ merge _)
            }
            case Some(pageClasses) => //This page already has a root template.
            {
                //Create a new instance URI
                val instanceUri = generateUri(subjectUri, node, pageContext)

                //Add ontology instance
                var graph = createInstance(instanceUri, node)

                //Check if the root template has been mapped to the corresponding Class of this template
                if (correspondingClass != null && correspondingProperty != null)
                {
                    var found = false;
                    for(pageClass <- pageClasses.asInstanceOf[List[OntologyClass]])
                    {
                        if(correspondingClass.name == pageClass.name)
                        {
                            found = true
                        }
                    }

                    if(found)
                    {
                        //Connect new instance to the instance created from the root template
                        val quad = new Quad(extractionContext.language, DBpediaDatasets.OntologyProperties, instanceUri, correspondingProperty, subjectUri, node.sourceUri)
                        graph = graph.merge(new Graph(quad))
                    }
                }

                //Extract properties
                propertyMappings.map(mapping => mapping.extract(node, instanceUri, pageContext))
                               .foldLeft(graph)(_ merge _)
            }
        }
    }

    private def createInstance(uri : String, node : Node) : Graph =
    {
        //Collect all classes
        def collectClasses(clazz : OntologyClass) : List[OntologyClass] =
        {
            clazz :: clazz.subClassOf.toList.flatMap(collectClasses)
        }

        val classes = collectClasses(mapToClass)

        //Set annotations
        node.setAnnotation(TemplateMapping.CLASS_ANNOTATION, classes);
        node.setAnnotation(TemplateMapping.INSTANCE_URI_ANNOTATION, uri);

        if(node.root.annotation(TemplateMapping.CLASS_ANNOTATION).isEmpty)
        {
            node.root.setAnnotation(TemplateMapping.CLASS_ANNOTATION, classes);
        }

        //Create type statements
        val quads = for(clazz <- classes) yield new Quad(extractionContext.language, DBpediaDatasets.OntologyTypes, uri,
                                                         extractionContext.ontology.getProperty("rdf:type").get,
                                                         clazz.uri, node.sourceUri )

        new Graph(quads)
    }

    /**
     * Generates a new URI from a template node
     *
     * @param subjectUri The base string of the generated URI
     * @param templateNode The template for which the URI is to be generated
     * @param pageContext The current page context
     *
     * @return The generated URI
     */
    private def generateUri(subjectUri : String, templateNode : TemplateNode, pageContext : PageContext) : String =
    {
        val properties = templateNode.children

        //Cannot generate URIs for empty templates
        if(properties.isEmpty)
        {
            return pageContext.generateUri(subjectUri, templateNode.title.decoded)
        }

        //Try to find a property which contains 'name'
        var nameProperty : PropertyNode = null;
        for(property <- properties if nameProperty == null)
        {
            if(property.key.toLowerCase.contains("name"))
            {
                nameProperty = property
            }
        }

        //If no name property has been found -> Use the first property of the template
        if(nameProperty == null)
        {
            nameProperty = properties.head
        }

        pageContext.generateUri(subjectUri, nameProperty)
    }

    private def splitMappings : (List[PropertyMapping],List[PropertyMapping]) =
    {
        var propertyMappings : List[PropertyMapping] = List()
        var constantPropertyMappings : List[PropertyMapping] = List()
        for(mapping <- mappings)
        {
            if(mapping.isInstanceOf[ConstantMapping])
            {
                constantPropertyMappings ::= mapping
            }
            else
            {
                propertyMappings ::= mapping
            }
        }
        (propertyMappings, constantPropertyMappings)
    }
}

private object TemplateMapping
{
    val CLASS_ANNOTATION = "TemplateMapping.class";
    val INSTANCE_URI_ANNOTATION = "TemplateMapping.uri";
}
