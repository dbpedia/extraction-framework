package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.{Node, PropertyNode, TemplateNode}
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}
import org.dbpedia.extraction.util.Language
import scala.collection.mutable.{Buffer,ArrayBuffer}
import org.dbpedia.extraction.wikiparser.AnnotationKey

class TemplateMapping( 
  mapToClass : OntologyClass,
  correspondingClass : OntologyClass,
  correspondingProperty : OntologyProperty,
  val mappings : List[PropertyMapping], // must be public val for statistics
  context: {
    def ontology : Ontology
    def language : Language 
  }  
) 
extends Mapping[TemplateNode]
{
    override val datasets = mappings.flatMap(_.datasets).toSet ++ Set(DBpediaDatasets.OntologyTypes,DBpediaDatasets.OntologyProperties)

    override def extract(node: TemplateNode, subjectUri: String, pageContext: PageContext): Seq[Quad] =
    {
        val pageNode = node.root
        val graph = new ArrayBuffer[Quad]

        pageNode.getAnnotation(TemplateMapping.CLASS_ANNOTATION) match
        {
            case None => //So far, no template has been mapped on this page
            {
                //Add ontology instance
                createInstance(graph, subjectUri, node)

                //Save existing template (this is the first one)
                node.setAnnotation(TemplateMapping.TEMPLATELIST_ANNOTATION, Seq(node.title.decoded))

                //Extract properties
                graph ++= mappings.flatMap(_.extract(node, subjectUri, pageContext))
            }
            case Some(pageClass) => //This page already has a root template.
            {
                // Depending on the following conditions we create a new "blank node" or append the data to the main resource.
                // Example case for creating new resources are the pages: enwiki:Volkswagen_Golf , enwiki:List_of_Playboy_Playmates_of_2012
                // Example case we could append to existing class are where we have to different mapped templates that one is a subclass of the other

                // Condition #1
                //  Check if the root template has been mapped to the corresponding Class of this template
                //  If the mapping already defines a corresponding class & propery then we should create a new resource
                val condition1_createCorrespondingProperty = correspondingClass != null && correspondingProperty != null && pageClass.relatedClasses.contains(correspondingClass)

                // Condition #2
                // If we have more than one of the same template it means that we want to create multiple resources. See for example
                // the pages: enwiki:Volkswagen_Golf , enwiki:List_of_Playboy_Playmates_of_2012
                val pageTemplateSet = pageNode.getAnnotation(TemplateMapping.TEMPLATELIST_ANNOTATION).getOrElse(Seq.empty)
                val condition2_template_exists = pageTemplateSet.contains(node.title.decoded)
                if (!condition2_template_exists)
                  node.setAnnotation(TemplateMapping.TEMPLATELIST_ANNOTATION, pageTemplateSet ++ Seq(node.title.decoded))

                // Condition #3
                // The current mapping is a subclass or a superclass of previous class
                val condition3_subclass = mapToClass.relatedClasses.contains(pageClass) || pageClass.relatedClasses.contains(mapToClass)

                // If all above conditions are met then use the main resource, otherwise create a new one
                val instanceUri =
                  if ( (!condition1_createCorrespondingProperty) && (!condition2_template_exists) && condition3_subclass ) subjectUri
                  else generateUri(subjectUri, node, pageContext)

                //Add ontology instance
                if (instanceUri == subjectUri) {
                  createMissingTypes(graph, instanceUri, node)
                }
                else {
                  createInstance(graph, instanceUri, node)
                }

                if (condition1_createCorrespondingProperty)
                {
                    //Connect new instance to the instance created from the root template
                    graph += new Quad(context.language, DBpediaDatasets.OntologyProperties, instanceUri, correspondingProperty, subjectUri, node.sourceUri)
                }

                //Extract properties
                graph ++= mappings.flatMap(_.extract(node, instanceUri, pageContext))
            }
        }
        
        graph
    }

    private def createMissingTypes(graph: Buffer[Quad], uri : String, node : Node): Unit =
    {
        val pageClass = node.root.getAnnotation(TemplateMapping.CLASS_ANNOTATION).getOrElse(throw new IllegalArgumentException("missing class Annotation"))

        // Compute missing types, i.e. the set difference between the page classes and this TemplateMapping relatedClasses
        val diffSet = mapToClass.relatedClasses.filterNot(c => pageClass.relatedClasses.contains(c))

        // Set annotations
        node.setAnnotation(TemplateMapping.CLASS_ANNOTATION, mapToClass);
        node.setAnnotation(TemplateMapping.INSTANCE_URI_ANNOTATION, uri);

        // Set new annotation (if new map is a subclass)
        if (mapToClass.relatedClasses.contains(pageClass))
          node.root.setAnnotation(TemplateMapping.CLASS_ANNOTATION, mapToClass)

        // Create missing type statements
        for (cls <- diffSet)
          graph += new Quad(context.language, DBpediaDatasets.OntologyTypes, uri, context.ontology.properties("rdf:type"), cls.uri, node.sourceUri)

    }

    private def createInstance(graph: Buffer[Quad], uri : String, node : Node): Unit =
    {
        val classes = mapToClass.relatedClasses

        //Set annotations
        node.setAnnotation(TemplateMapping.CLASS_ANNOTATION, mapToClass);
        node.setAnnotation(TemplateMapping.INSTANCE_URI_ANNOTATION, uri);

        if(node.root.getAnnotation(TemplateMapping.CLASS_ANNOTATION).isEmpty)
        {
            node.root.setAnnotation(TemplateMapping.CLASS_ANNOTATION, mapToClass);
        }
        
        //Create type statements
        for (cls <- classes)
          graph += new Quad(context.language, DBpediaDatasets.OntologyTypes, uri, context.ontology.properties("rdf:type"), cls.uri, node.sourceUri)
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
}

private object TemplateMapping
{
    val CLASS_ANNOTATION = new AnnotationKey[OntologyClass]
    val TEMPLATELIST_ANNOTATION = new AnnotationKey[Seq[String]]
    val INSTANCE_URI_ANNOTATION = new AnnotationKey[String]
}
