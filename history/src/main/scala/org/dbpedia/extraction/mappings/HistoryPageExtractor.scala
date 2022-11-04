package org.dbpedia.extraction.mappings

import java.net.URL

import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.mappings.wikitemplate._
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._
import org.openrdf.model.impl.ValueFactoryImpl
import org.openrdf.model.{Literal, Statement, URI}

import scala.collection.mutable.{HashMap, ListBuffer, Stack}
import scala.util.control.Breaks._
import scala.util.matching.Regex
import scala.xml.{NodeSeq, XML, Node => XMLNode}

//some of my utilities
import org.dbpedia.extraction.mappings.Matcher._
import org.dbpedia.extraction.mappings.WiktionaryPageExtractor._
import org.dbpedia.extraction.mappings.wikitemplate.MyNodeList._
import org.dbpedia.extraction.mappings.wikitemplate.MyStack._
import org.dbpedia.extraction.mappings.wikitemplate.TimeMeasurement._
import org.dbpedia.extraction.mappings.wikitemplate.VarBinder._

import scala.language.reflectiveCalls

/**
 * parses (wiktionary) wiki pages
 * is meant to be configurable for multiple languages
 *
 * is even meant to be usable for non-wiktionary wikis -> arbitrary wikis, but where all pages follow a common schema
 * but in contrast to infobox-focused extraction, we *aim* to be more flexible:
 * dbpedia core is hardcoded extraction. here we try to use a meta-language describing the information to be extracted
 * this is done via xml containing wikisyntax snippets (called templates) containing placeholders (called variables), which are then bound
 *
 * we also extended this approach to match the wiktionary schema
 * a page can contain information about multiple entities (sequential blocks)
 *
 * @author Jonas Brekle <jonas.brekle@gmail.com>
 * @author Sebastian Hellmann <hellmann@informatik.uni-leipzig.de>
 */

class HistoryPageExtractor(
  context : {
   def redirects : Redirects
   def language : Language
  }
)
  extends PageNodeExtractor
{

  # dc: <http://purl.org/dc/element/1.1/>
  # prov: <http://www.w3.org/ns/prov#>
  # swp: <http://www.w3.org/2004/03/trix/swp-2/>
  val sameAsProperty: OntologyProperty = context.ontology.properties("owl:sameAs")

  override val datasets = Set(DBpediaDatasets.ExternalLinks) # ToADD

  override def extract(node : PageNode, subjectUri : String) : Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(node.title))
      return Seq.empty

    var quads = new ArrayBuffer[Quad]()
    for(link <- collectExternalLinks(node);
        uri <- UriUtils.cleanLink(link.destination))
    {
      quads += new Quad(context.language, DBpediaDatasets.ExternalLinks, subjectUri, wikiPageExternalLinkProperty, uri, link.sourceIri, null)
    }

    quads
  }

  private def collectExternalLinks(node : Node) : List[ExternalLinkNode] =
  {
    node match
    {
      case linkNode : ExternalLinkNode => List(linkNode)
      case _ => node.children.flatMap(collectExternalLinks)
    }
  }
}

