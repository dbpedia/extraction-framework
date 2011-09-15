package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}

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
  override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
  {
    if(node.title.namespace != WikiTitle.Namespace.Main) return new Graph()

        val pageURL = "http://" + context.language.wikiCode + ".wikipedia.org/wiki/" + node.root.title.encoded
        val editlinkPredicate = "http://purl.org/dc/terms/modified"


        //new Graph(quads)
    //println("NODECHILDREN = " + node.children.find(x => "timestamp"))
//    node.children.foreach(child => println("CHILD = " + child.))
    println("NODECHILDREN = " + node.asInstanceOf[LivePageNode].timestamp);
    val quadEditlink = new Quad(context.language, DBpediaDatasets.Revisions, pageURL, editlinkPredicate,
      node.asInstanceOf[LivePageNode].timestamp, node.sourceUri,context.ontology.getDatatype("xsd:dateTime").get )


    /*new Graph(new Quad(context.language, DBpediaDatasets.Revisions, "", "",
            node.revision.toString, node.sourceUri, context.ontology.getDatatype("xsd:integer").get ))*/
    new Graph(quadEditlink);
  }
}