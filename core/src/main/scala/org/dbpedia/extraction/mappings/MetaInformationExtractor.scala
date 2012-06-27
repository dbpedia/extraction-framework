package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
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

  override val datasets = Set(DBpediaDatasets.Revisions)

  //override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Main) return Seq.empty

    val pageURL = "http://" + context.language.wikiCode + ".wikipedia.org/wiki/" + node.root.title.encoded
    val modificationDatePredicate = "http://purl.org/dc/terms/modified"
    val editLinkPredicate = "http://dbpedia.org/meta/editlink"
    val revisionPredicate = "http://dbpedia.org/meta/revision"

    //new Graph(quads)
    //println("NODECHILDREN = " + node.children.find(x => "timestamp"))
    //    node.children.foreach(child => println("CHILD = " + child.))
    //    try{
    //println("NODECHILDREN = " + node.asInstanceOf[LivePageNode].timestamp);



    val quadModificationDate = new Quad(context.language, DBpediaDatasets.Revisions, pageURL, modificationDatePredicate,
      node.asInstanceOf[LivePageNode].nodeTimestamp, node.sourceUri,context.ontology.datatypes.get("xsd:dateTime").get )

    val editLink = "http://" + context.language.wikiCode + ".wikipedia.org/w/index.php?title=" + node.title.encoded +
      "&action=edit";
    val quadEditlink = new Quad(context.language, DBpediaDatasets.Revisions, pageURL, editLinkPredicate,
      editLink, node.sourceUri, null )

    val revisionLink = "http://" + context.language.wikiCode + ".wikipedia.org/w/index.php?title=" + node.title.encoded +
      "&oldid=" + node.revision;

    //private val foafPrimaryTopicProperty = context.ontology.getProperty("foaf:primaryTopic").getOrElse(throw new Exception("Property 'foaf:primaryTopic' not found"))

    val quadRevisionlink = new Quad(context.language, DBpediaDatasets.Revisions, pageURL, revisionPredicate,
      revisionLink, node.sourceUri, null )


    Seq(quadModificationDate, quadEditlink, quadRevisionlink);


  }
}