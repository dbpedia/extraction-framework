package org.dbpedia.extraction.mappings

import scala.language.reflectiveCalls
import org.dbpedia.extraction.destinations.{QuadBuilder, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
  * Extracts page abstract html.
  *
  * Based on AbstractExtractor, major difference is the parameter
  * apiParametersFormat = "action=parse&prop=text&section=0&format=xml&page=%s"
  *
  * Should a new AbstractExtractor be developed, setting this api parameters
  * should result in the same functionality.
  */

class AbstractLinkExtractor(
     context : {
       def ontology : Ontology
       def language : Language
     }
   )
  extends AbstractExtractor(context)
{
  //API parameters to geht HTML of first section
  override val apiParametersFormat = "uselang="+language + protectedParams.get("apiNifParametersFormat").get

  override val xmlPath = protectedParams.get("apiNifXmlPath").get.split(",").map(_.trim)

  override val datasets = Set(DBpediaDatasets.LinkedAbstracts)

  protected lazy val linkedAbstracts = QuadBuilder(context.language, DBpediaDatasets.LinkedAbstracts, context.ontology.properties(protectedParams.get("nifProperty").get), null) _

  override def extract(pageNode : PageNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    //Only extract abstracts for pages from the Main namespace
    if(pageNode.title.namespace != Namespace.Main) return Seq.empty

    //Don't extract abstracts from redirect and disambiguation pages
    if(pageNode.isRedirect || pageNode.isDisambiguation) return Seq.empty

    //Retrieve page text
    var text = super.retrievePage(pageNode.title /*, abstractWikiText*/)

    text = super.postProcess(pageNode.title, text)

    if (text.trim.isEmpty)
      return Seq.empty

    //Create statements
    val quadLong = linkedAbstracts(subjectUri, text, pageNode.sourceUri)

    Seq(quadLong)
  }

}
