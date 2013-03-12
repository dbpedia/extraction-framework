package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.StringUtils._
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import java.net.URI

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 9/13/11
 * Time: 9:03 PM
 * Extracts page's meta-information e.g. editlink, revisonlink, ....
 */

class MetaInformationExtractor( context : {
  def ontology : Ontology
  def language : Language } ) extends Extractor
{
  val modificationDatePredicate = context.ontology.properties("dct:modified")
  val editLinkPredicate = "http://dbpedia.org/meta/editlink"
  val revisionPredicate = "http://dbpedia.org/meta/revision"

  override val datasets = Set(DBpediaDatasets.RevisionMeta)

  //override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  override def extract(page : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main) return Seq.empty

    val editLink     = context.language.baseUri + "/w/index.php?title=" + page.title.encodedWithNamespace + "&action=edit";
    val revisionLink = context.language.baseUri + "/w/index.php?title=" + page.title.encodedWithNamespace + "&oldid=" + page.revision;

    val quadModificationDate = new Quad(context.language, DBpediaDatasets.RevisionMeta, page.title.pageIri, modificationDatePredicate,
      formatTimestamp(page.timestamp), page.sourceUri,context.ontology.datatypes.get("xsd:dateTime").get )

    val quadExtractionDate = new Quad(context.language, DBpediaDatasets.RevisionMeta, subjectUri, modificationDatePredicate,
      formatCurrentTimestamp, page.sourceUri,context.ontology.datatypes.get("xsd:dateTime").get )

    val quadEditlink = new Quad(context.language, DBpediaDatasets.RevisionMeta, page.title.pageIri, editLinkPredicate,
      editLink, page.sourceUri, null )

    val quadRevisionlink = new Quad(context.language, DBpediaDatasets.RevisionMeta, page.title.pageIri, revisionPredicate,
      revisionLink, page.sourceUri, null )


    Seq(quadModificationDate, quadExtractionDate, quadEditlink, quadRevisionlink);


  }
}