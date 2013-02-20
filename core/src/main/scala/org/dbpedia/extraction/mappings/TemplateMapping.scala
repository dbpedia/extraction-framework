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

                //Extract properties
                graph ++= mappings.flatMap(_.extract(node, subjectUri, pageContext))
            }
            case Some(pageClasses) => //This page already has a root template.
            {
                //Check if the root template has been mapped to the corresponding Class of this template
                val createCorrespondingProperty = correspondingClass != null && correspondingProperty != null && pageClasses.contains(correspondingClass)

                //Create a new instance URI. If the mappings has no corresponding property and the current mapping is a subclass or superclass
                //of all previous mappings then do not create a new instance
                val instanceUri =
                  if ( (!createCorrespondingProperty) && isSubOrSuperClass(mapToClass, pageClasses) ) subjectUri
                  else generateUri(subjectUri, node, pageContext)

                //Add ontology instance
                createInstance(graph, instanceUri, node)


                if (createCorrespondingProperty)
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

    private def createInstance(graph: Buffer[Quad], uri : String, node : Node): Unit =
    {
        val classes = mapToClass.relatedClasses

        //Set annotations
        node.setAnnotation(TemplateMapping.CLASS_ANNOTATION, classes);
        node.setAnnotation(TemplateMapping.INSTANCE_URI_ANNOTATION, uri);

        if(node.root.getAnnotation(TemplateMapping.CLASS_ANNOTATION).isEmpty)
        {
            node.root.setAnnotation(TemplateMapping.CLASS_ANNOTATION, classes);
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

  /**
   * Checks if current class is a subclass or supoerclass of a list
   *
   * @param cl The class we have to check for sub/super-classsing
   * @param clSeq the list of classes we have to check against
   *
   * @return True if a subclass The generated URI
   */
    private def isSubOrSuperClass(cl : OntologyClass, clSeq : Seq[OntologyClass]) : Boolean =
    {

        for (i <- clSeq)
        {
            //if a class is contained in the others related classes (hierarchy) it is a subclass or superclass
            //it must be true for all items so we return false if not true for one item
            if ( ! (i.relatedClasses.contains(cl) || cl.relatedClasses.contains(i) ))
                return false
        }
        true
    }
}

private object TemplateMapping
{
    val CLASS_ANNOTATION = new AnnotationKey[Seq[OntologyClass]]
    
    val INSTANCE_URI_ANNOTATION = new AnnotationKey[String]
}
