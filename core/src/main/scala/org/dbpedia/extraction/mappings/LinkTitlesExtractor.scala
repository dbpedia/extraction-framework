package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.dataparser.StringParser
import org.dbpedia.extraction.ontology.datatypes.Datatype
import scala.collection.mutable.ArrayBuffer

class LinkTitlesExtractor(
    context : {
        def ontology : Ontology
        def language : Language
    	def redirects : Redirects
    }
)
extends PageNodeExtractor {
     override val datasets = Set(DBpediaDatasets.LinkTitles)
    
    override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] =
    {
        if(page.title.namespace != Namespace.Main) return Seq.empty
        val quads = new ArrayBuffer[Quad]()
        extractTitle(page, subjectUri, pageContext, quads)
    }
    
    private def extractTitle(node : Node, subjectUri : String, pageContext : PageContext, quads : ArrayBuffer[Quad]) : Seq[Quad] =
    {
        val graph = node match
        {
            case templateNode : TemplateNode =>
            {
                if(templateNode.title.decoded.contains("Infobox")) {
                    for(propertyNode <- templateNode.children) {
                        for(property <- propertyNode.children) {
                            property match
                            {
                                case internalLinkNode : InternalLinkNode => {
                                    if (!internalLinkNode.destination.decoded.toLowerCase.equals(StringParser.parse(internalLinkNode.children(0)).mkString.toLowerCase)) {
                                        quads += new Quad(context.language, DBpediaDatasets.LinkTitles, context.language.resourceUri.append(internalLinkNode.destination.decoded).replace(" ", "_"), context.ontology.properties("linkTitle"), StringParser.parse(internalLinkNode.children(0)).mkString, internalLinkNode.sourceUri, new Datatype("rdf:langString"))
                                    }
                                }
                                case _ => quads
                            }
                        }
                    }
                }
                quads
            }
            case _ => quads
        }

        if(graph.isEmpty)
        {
            node.children.flatMap(child => extractTitle(child, subjectUri, pageContext, quads))
        }
        quads
    }
}
