package org.dbpedia.extraction.mappings

import be.ugent.mmlab.rml.model.RMLMapping
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Dataset, Quad}
import org.dbpedia.extraction.mappings.rml.processing.RMLProcessorRunner
import org.dbpedia.extraction.mappings.rml.util.RMLMappingWrapper
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Node, PageNode, TemplateNode}

import scala.language.reflectiveCalls

/**
  *
  * Extractor that extracts a page node using the RML Processor
  *
  */
class RMLExtractor(
  context : {
    def redirects: Redirects
    def language: Language
    def rmlMap : Map[String, RMLMapping]
    def ontology: Ontology
  }
) extends PageNodeExtractor{



  //val rmlMappingWrapper = new RMLMappingWrapper(context.rmlMappings)
  val rmlProcessorRunner = new RMLProcessorRunner(context.rmlMap)

  /**
    * @param input       The source node
    * @param subjectUri The subject URI of the generated triples
    * @param context    The page context which holds the state of the extraction.
    * @return A graph holding the extracted data
    */
  override def extract(input: PageNode, subjectUri: String, context: PageContext): Seq[Quad] = {
    val result = extractNode(input, subjectUri, context)
    result
  }

  override val datasets: Set[Dataset] = Set(DBpediaDatasets.OntologyPropertiesObjects) ++ Set(DBpediaDatasets.OntologyPropertiesLiterals) ++ Set(DBpediaDatasets.OntologyPropertiesGeo)

  /**
    * Extracts a data from a node.
    * Recursively traverses it children if the node itself does not contain any useful data.
    */
  private def extractNode(node : Node, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    //Try to extract data from the node itself
    val graph = node match
    {
      case templateNode : TemplateNode =>
      {
        /*
        rmlMappingWrapper.getTriplesMap(templateNode.title.decoded) match {
          case Some(triplesMap) =>
            rmlProcessorRunner.process(templateNode, triplesMap, subjectUri, context)
          case None => Seq.empty
        }
        */
        val templateTitle = "Mapping_en:" + templateNode.title.encoded.toString
        if (context.rmlMap.contains(templateTitle)) {
          val start = System.nanoTime()
          val result = rmlProcessorRunner.process(templateNode, templateTitle, subjectUri, context)
          val delta = System.nanoTime() - start
          RMLProcessorRunner.ProcessorRunnerCounter += delta
          result
        } else {
          Seq.empty
        }



      }
      case _ => Seq.empty
    }

    //Check the result and return it if non-empty.
    //Otherwise continue with extracting the children of the current node.
    if(graph.isEmpty)
    {
      node.children.flatMap(child => extractNode(child, subjectUri, pageContext))
    }
    else
    {
      graph
    }
  }

}
