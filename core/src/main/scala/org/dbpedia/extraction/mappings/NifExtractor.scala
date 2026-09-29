package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.ExtractorAnnotation
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.nif.{WikipediaNifExtractorRest, WikipediaNifExtractor}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{Language, MediawikiConnectorConfigured, MediaWikiConnectorRest}
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
  * Extracts page html.
  *
  * Based on PlainAbstractExtractor, major difference is the parameter
  * apiParametersFormat = "action=parse&prop=text&section=0&format=xml&page=%s"
  *
  * This class produces all nif related datasets for the abstract as well as the short-, long-abstracts datasets.
  * Where the long abstracts is the nif:isString attribute of the nif instance representing the abstract section of a wikipage.
  *
  * We are going to to use this method for generating the abstracts from release 2016-10 onwards.
  * It will be expanded to cover the whole wikipage in the future.
  */

@ExtractorAnnotation("nif extractor")
class NifExtractor(
     context : {
       def ontology : Ontology
       def language : Language
       def configFile : Config
     }
   )
  extends WikiPageExtractor
{
  //API parameters to geht HTML of first section
  val apiParametersFormat: String = context.configFile.nifParameters.nifQuery

  protected val isTestRun: Boolean = context.configFile.nifParameters.isTestRun
  protected val writeLinkAnchors: Boolean = context.configFile.nifParameters.writeLinkAnchor
  protected val writeStrings: Boolean = context.configFile.nifParameters.writeAnchor
  protected val shortAbstractLength: Int = context.configFile.abstractParameters.shortAbstractMinLength
  protected val abstractsOnly : Boolean =  context.configFile.nifParameters.abstractsOnly
  protected val dbpediaVersion: String = context.configFile.dbPediaVersion

  override val datasets = Set(DBpediaDatasets.NifContext,DBpediaDatasets.NifPageStructure,DBpediaDatasets.NifTextLinks,DBpediaDatasets.LongAbstracts, DBpediaDatasets.ShortAbstracts, DBpediaDatasets.RawTables, DBpediaDatasets.Equations)


  override def extract(pageNode : WikiPage, subjectUri : String): Seq[Quad] =
  {
    //Only extract abstracts for pages from the Main namespace
    if(pageNode.title.namespace != Namespace.Main) return Seq.empty

    //Don't extract abstracts from redirect and disambiguation pages
    if(pageNode.isRedirect || pageNode.isDisambiguation) return Seq.empty

    var html = ""
    val mwcType = context.configFile.mediawikiConnection.apiType

    if (mwcType == "rest") {
      val mwConnector = new MediaWikiConnectorRest(context.configFile.mediawikiConnection, context.configFile.nifParameters.nifTags.split(","))
      html = mwConnector.retrievePage(pageNode.title, apiParametersFormat, pageNode.isRetry) match {
        case Some(t) => NifExtractor.postProcessExtractedHtml(pageNode.title, t)
        case None => return Seq.empty
      }
      new WikipediaNifExtractorRest(context, pageNode).extractNif(html)(err => pageNode.addExtractionRecord(err))
    } else {
      val mwConnector = new MediawikiConnectorConfigured(context.configFile.mediawikiConnection, context.configFile.nifParameters.nifTags.split(","))
      html = mwConnector.retrievePage(pageNode.title, apiParametersFormat, pageNode.isRetry) match {
        case Some(t) => NifExtractor.postProcessExtractedHtml(pageNode.title, t)
        case None => return Seq.empty
      }
      new WikipediaNifExtractor(context, pageNode).extractNif(html)(err => pageNode.addExtractionRecord(err))
    }
  }

}

object NifExtractor{
  //TODO check if this function is still relevant
  //copied from PlainAbstractExtractor
  def postProcessExtractedHtml(pageTitle: WikiTitle, text: String): String =
  {
    val startsWithLowercase =
      if (text.isEmpty) {
        false
      } else {
        val firstLetter = text.substring(0,1)
        firstLetter != firstLetter.toUpperCase(pageTitle.language.locale)
      }

    //HACK
    if (startsWithLowercase)
    {
      val decodedTitle = pageTitle.decoded.replaceFirst(" \\(.+\\)$", "")

      if (! text.toLowerCase.contains(decodedTitle.toLowerCase))
      {
        // happens mainly for Japanese names (abstract starts with template)
        return decodedTitle + " " + text
      }
    }

    text
  }
}
