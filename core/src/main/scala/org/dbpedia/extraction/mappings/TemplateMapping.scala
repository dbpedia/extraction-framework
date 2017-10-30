package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{AnnotationKey, PropertyNode, TemplateNode}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls


@SoftwareAgentAnnotation(classOf[TemplateMapping], AnnotationType.Extractor)
class   TemplateMapping(
  val mapToClass : OntologyClass,
  val correspondingClass : OntologyClass, // must be public val for converting to rml
  val correspondingProperty : OntologyProperty, // must be public for converting to rml
  val mappings : List[PropertyMapping], // must be public val for statistics
  context: {
    def ontology : Ontology
    def language : Language 
  }  
) 
extends Extractor[TemplateNode]
{
    override val datasets: Set[Dataset] = mappings.flatMap(_.datasets).toSet ++ Set(DBpediaDatasets.OntologyTypes, DBpediaDatasets.OntologyTypesTransitive, DBpediaDatasets.OntologyPropertiesObjects)

    private val classOwlThing = context.ontology.classes("owl:Thing")
    private val propertyRdfType = context.ontology.properties("rdf:type")
    private val dboPerson = context.ontology.getOntologyClass("dbo:Person").get

    override def extract(node: TemplateNode, subjectUri: String): Seq[Quad] =
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
                graph ++= mappings.flatMap(_.extract(node, subjectUri))
            }
            case Some(pageClass) => //This page already has a root template.
            {
                // Depending on the following conditions we create a new "blank node" or append the data to the main resource.
                // Example case for creating new resources are the pages: enwiki:Volkswagen_Golf , enwiki:List_of_Playboy_Playmates_of_2012
                // Example case we could append to existing class are where we have to different mapped templates that one is a subclass of the other

                // Condition #1
                //  Check if the root template has been mapped to the corresponding Class of this template
                //  If the mapping already defines a corresponding class & propery then we should create a new resource
                val condition1_createCorrespondingProperty = correspondingClass != null &&
                  correspondingProperty != null && pageClass.relatedClasses.contains(correspondingClass)

                // Condition #2
                // If we have more than one of the same template it means that we want to create multiple resources. See for example
                // the pages: enwiki:Volkswagen_Golf , enwiki:List_of_Playboy_Playmates_of_2012
                val pageTemplateSet = pageNode.getAnnotation(TemplateMapping.TEMPLATELIST_ANNOTATION).getOrElse(Seq.empty)
                val condition2_template_exists = pageTemplateSet.contains(node.title.decoded)
                if (!condition2_template_exists)
                  node.setAnnotation(TemplateMapping.TEMPLATELIST_ANNOTATION, pageTemplateSet ++ Seq(node.title.decoded))

                // Condition #3
                // The current mapping is a subclass or a superclass of previous class or owl:Thing
                // FIXME: I think the third condition is too harsh for some examples, especially for subclasses of Person. For example: a person can be an Athlete and a Soldier in his life
                // Chile: I added the exception for dbo:Person
                val condition3_subclass =
                  mapToClass.isSubclassOf(dboPerson) && pageClass.isSubclassOf(dboPerson) ||
                  mapToClass.relatedClasses.contains(pageClass) ||
                  pageClass.relatedClasses.contains(mapToClass) ||
                  mapToClass.equals(classOwlThing) ||
                  pageClass.equals(classOwlThing)

                // If all above conditions are met then use the main resource, otherwise create a new one
                val instanceUri =
                  if ( (!condition1_createCorrespondingProperty) && (!condition2_template_exists) && condition3_subclass ) subjectUri
                  else generateUri(subjectUri, node)

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
                    graph += new Quad(context.language, DBpediaDatasets.OntologyPropertiesObjects, instanceUri, correspondingProperty, subjectUri, node.sourceIri)
                }

                //Extract properties
                graph ++= mappings.flatMap(_.extract(node, instanceUri))
            }
        }
        
        graph
    }

    private def createMissingTypes(graph: mutable.Buffer[Quad], uri : String, node : TemplateNode): Unit =
    {
        val pageClass = node.root.getAnnotation(TemplateMapping.CLASS_ANNOTATION).getOrElse(throw new IllegalArgumentException("missing class Annotation"))

        // Compute missing types, i.e. the set difference between the page classes and this TemplateMapping relatedClasses
        val diffSet = mapToClass.relatedClasses.filterNot(c => pageClass.relatedClasses.contains(c))

        // Set annotations
        node.setAnnotation(TemplateMapping.CLASS_ANNOTATION, mapToClass)
        node.setAnnotation(TemplateMapping.INSTANCE_URI_ANNOTATION, uri)

        // Set new annotation (if new map is a subclass)
        if (mapToClass.relatedClasses.contains(pageClass))
          node.root.setAnnotation(TemplateMapping.CLASS_ANNOTATION, mapToClass)

        // Create missing type statements
        // Here we do not split the transitive and the direct types because different types may come from different mappings
        // Splitting the types of the main resource is done at the MappingExtractor.extract()
        for (cls <- diffSet)
          graph += new Quad(context.language, DBpediaDatasets.OntologyTypes, uri, propertyRdfType, cls.uri, node.sourceIri+"&mappedTemplate="+node.title.encoded)

    }

    private def createInstance(graph: mutable.Buffer[Quad], uri : String, node : TemplateNode): Unit =
    {
        val classes = mapToClass.relatedClasses

        //Set annotations
        node.setAnnotation(TemplateMapping.CLASS_ANNOTATION, mapToClass)
        node.setAnnotation(TemplateMapping.INSTANCE_URI_ANNOTATION, uri)

        if(node.root.getAnnotation(TemplateMapping.CLASS_ANNOTATION).isEmpty)
        {
            node.root.setAnnotation(TemplateMapping.CLASS_ANNOTATION, mapToClass)
        }
        
        //Create type statements
        for (cls <- classes) {
          // Here we split the transitive types from the direct type assignment
          val typeDataset = if (cls.equals(mapToClass)) DBpediaDatasets.OntologyTypes else DBpediaDatasets.OntologyTypesTransitive
          graph += new Quad(context.language, typeDataset, uri, propertyRdfType, cls.uri, node.sourceIri+"&mappedTemplate="+node.title.encoded)
        }
    }

    /**
     * Generates a new URI from a template node
     *
     * @param subjectUri The base string of the generated URI
     * @param templateNode The template for which the URI is to be generated
     * @return The generated URI
     */
    private def generateUri(subjectUri : String, templateNode : TemplateNode) : String =
    {
        val properties = templateNode.children

        //Cannot generate URIs for empty templates
        if(properties.isEmpty)
        {
            return templateNode.generateUri(subjectUri, templateNode.title.decoded)
        }

        //Try to find a property which contains 'name'
        var nameProperty : PropertyNode = null
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

        templateNode.generateUri(subjectUri, nameProperty)
    }
}

private object TemplateMapping
{
    val CLASS_ANNOTATION = new AnnotationKey[OntologyClass]
    val TEMPLATELIST_ANNOTATION = new AnnotationKey[Seq[String]]
    val INSTANCE_URI_ANNOTATION = new AnnotationKey[String]
}
