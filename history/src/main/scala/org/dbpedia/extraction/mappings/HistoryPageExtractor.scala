package org.dbpedia.extraction.mappings

import java.net.URL
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty}
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls



class HistoryPageExtractor(
  context : {
    def ontology: Ontology
    def language: Language
  }
)
  extends WikiPageWithRevisionsExtractor {

  //PageNodeExtractor? WikiPageExtractor ?
  private val subjectOntProperty = context.ontology.properties("dc:subject")
  private val typeOntProperty = context.ontology.properties("rdf:type")
  private val featureOntClass = context.ontology.classes("prov:Revision")
  //val sameAsProperty: OntologyProperty = context.ontology.properties("prov:sameAs")

  override val datasets = Set(DBpediaDatasets.History) //# ToADD

// WikiPage ? PageNode?

  override def extract(node:WikiPageWithRevisions , subjectUri: String): Seq[Quad] = {


      println("xxxxxxxxxxxxxx HISTORY EXTRACTOR xxxxxxxxxxxxxxxxx")
    val quads = new ArrayBuffer[Quad]()

    //node.pageNode.children.foreach(n => {
      println(">>>>>>>>>>>>>>>>")
      println(node.toString)
    println("NB REVISIONS :"+node.revisions)
      //if(n.isInstanceOf[TextNode]){ println( "FOUND TEXT")}
      println(">>>>>>>>>>>>>>>>")
    //})
    quads += new Quad(context.language, DBpediaDatasets.History,  node.title.pageIri, typeOntProperty, featureOntClass.uri,  node.sourceIri)
    quads += new Quad(context.language, DBpediaDatasets.History, node.title.pageIri, subjectOntProperty, subjectUri, node.sourceIri)

    quads
  }

  /*def collectRevisions(node: Node): List[RevisionNode] = {
    node match {
      case revisionNode: RevisionNode if node.destination.namespace == Namespace.Category => List(linkNode)
      case _ => node.children.flatMap(collectRevisions)
    }
  }
*/
}
