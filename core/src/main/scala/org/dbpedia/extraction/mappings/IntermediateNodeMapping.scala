package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.{ExtractionRecorder, RecordCause, RecordEntry}
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.wikiparser.{NodeUtil, PageNode, TemplateNode, WikiPage}
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}

import scala.collection.mutable.ArrayBuffer
import org.dbpedia.extraction.config.dataparser.DataParserConfig
import org.dbpedia.extraction.util.Language

import scala.collection.mutable
import scala.language.reflectiveCalls
import scala.reflect.ClassTag

@SoftwareAgentAnnotation(classOf[IntermediateNodeMapping], AnnotationType.Extractor)
class IntermediateNodeMapping (
  val nodeClass : OntologyClass, // public for rml mappings
  val correspondingProperty : OntologyProperty, //public for rml mappings
  val mappings : List[PropertyMapping], // must be public val for statistics
  context : {
    def ontology : Ontology
    def language : Language
    def recorder[T: ClassTag] : ExtractionRecorder[T]
  }
)
extends PropertyMapping
{
  private val splitRegex = if (DataParserConfig.splitPropertyNodeRegexInfobox.contains(context.language.wikiCode))
                             DataParserConfig.splitPropertyNodeRegexInfobox(context.language.wikiCode)
                           else DataParserConfig.splitPropertyNodeRegexInfobox("en")

  override val datasets: Set[Dataset] = mappings.flatMap(_.datasets).toSet ++
    Set(DBpediaDatasets.OntologyTypes, DBpediaDatasets.OntologyTypesTransitive, DBpediaDatasets.OntologyPropertiesObjects)

  override def extract(node : TemplateNode, subjectUri : String) : Seq[Quad] =
  {
    val graph = new ArrayBuffer[Quad]()

    val affectedTemplatePropertyNodes = mappings.flatMap(_ match {
      case spm : SimplePropertyMapping => node.property(spm.templateProperty)
      case dim : DateIntervalMapping => node.property(dim.templateProperty)
      case _ => None
    }).toSet //e.g. Set(leader_name, leader_title)

    val valueNodes = affectedTemplatePropertyNodes.map(NodeUtil.splitPropertyNode(_, splitRegex))

    //more than one template property is affected (e.g. leader_name, leader_title)
    if(affectedTemplatePropertyNodes.size > 1)
    {
      if(valueNodes.forall(_.size <= 1))
        context.recorder[PageNode].record(new RecordEntry[PageNode](node.root, RecordCause.Internal, context.language, "IntermediateNodeMapping for multiple properties have multiple values in: " + subjectUri))

      createInstance(graph, node, subjectUri)
    }
    //one template property is affected (e.g. engine)
    else if(affectedTemplatePropertyNodes.size == 1)
    {
      //allow multiple values in this property
      for(valueNodesForOneProperty <- valueNodes; value <- valueNodesForOneProperty)
        createInstance(graph, value.parent.asInstanceOf[TemplateNode], subjectUri)
    }

    graph
  }

  private def createInstance(graph: mutable.Buffer[Quad], node : TemplateNode, originalSubjectUri : String): Unit =
  {
    val instanceUri = node.generateUri(originalSubjectUri, nodeClass.name)
    
    // extract quads
    val values = mappings.flatMap(_.extract(node, instanceUri))

    // only generate triples if we actually extracted some values
    if(values.nonEmpty)
    {
      graph += new Quad(context.language, DBpediaDatasets.OntologyPropertiesObjects, originalSubjectUri, correspondingProperty, instanceUri, node.sourceIri)
      
      for (cls <- nodeClass.relatedClasses) {
        // Here we split the transitive types from the direct type assignment
        val typeDataset = if (cls.equals(nodeClass)) DBpediaDatasets.OntologyTypes else DBpediaDatasets.OntologyTypesTransitive
        graph += new Quad(context.language, typeDataset, instanceUri, context.ontology.properties("rdf:type"), cls.uri, node.sourceIri)
      }
      
      graph ++= values
    }
  }
}
