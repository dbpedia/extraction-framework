package org.dbpedia.extraction.mappings

import java.util.logging.Logger
import org.dbpedia.extraction.wikiparser.{NodeUtil, TemplateNode}
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}
import org.dbpedia.extraction.util.Language
import scala.collection.mutable.{Buffer,ArrayBuffer}

class IntermediateNodeMapping (
  nodeClass : OntologyClass,
  correspondingProperty : OntologyProperty,
  val mappings : List[PropertyMapping], // must be public val for statistics
  context : {
    def ontology : Ontology
    def language : Language 
  }
)
extends PropertyMapping
{
  private val logger = Logger.getLogger(classOf[IntermediateNodeMapping].getName)

  private val splitRegex = """<br\s*\/?>"""

  override val datasets = mappings.flatMap(_.datasets).toSet ++ Set(DBpediaDatasets.OntologyTypes,DBpediaDatasets.OntologyProperties)
    

  override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    var graph = new ArrayBuffer[Quad]()

    val affectedTemplatePropertyNodes = mappings.flatMap(_ match {
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
        createInstance(graph, node, subjectUri, pageContext)
      }
      else
      {
        //TODO muliple properties having multiple values
        /**
         * fictive example:
         * leader_name = Bill_Gates<br>Steve_Jobs
         * leader_title = Microsoft dictator<br>Apple evangelist
         */
        logger.warning("IntermediateNodeMapping for muliple properties having multiple values not implemented!")
      }
    }
    //one template property is affected (e.g. engine)
    else if(affectedTemplatePropertyNodes.size == 1)
    {
      //allow multiple values in this property
      for(valueNodesForOneProperty <- valueNodes; value <- valueNodesForOneProperty)
      {
        createInstance(graph, value.parent.asInstanceOf[TemplateNode], subjectUri, pageContext)
      }
    }

    graph
  }

  private def createInstance(graph: Buffer[Quad], node : TemplateNode, originalSubjectUri : String, pageContext : PageContext): Unit =
  {
    val instanceUri = pageContext.generateUri(originalSubjectUri, node)
    
    // extract quads
    val values = mappings.flatMap(_.extract(node, instanceUri, pageContext))

    // only generate triples if we actually extracted some values
    if(! values.isEmpty)
    {
      graph += new Quad(context.language, DBpediaDatasets.OntologyProperties, originalSubjectUri, correspondingProperty, instanceUri, node.sourceUri);
      
      for (cls <- nodeClass.relatedClasses)
        graph += new Quad(context.language, DBpediaDatasets.OntologyTypes, instanceUri, context.ontology.properties("rdf:type"), cls.uri, node.sourceUri)
      
      graph ++= values
    }
  }
}
