package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.{Config, ExtractionRecorder, RecordEntry}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.nif.WikipediaNifExtractor
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{Language, MediaWikiConnector}
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls
import scala.reflect.ClassTag

/**
  * Extracts page html.
  *
  * Based on AbstractExtractor, major difference is the parameter
  * apiParametersFormat = "action=parse&prop=text&section=0&format=xml&page=%s"
  *
  * This class produces all nif related datasets for the abstract as well as the short-, long-abstracts datasets.
  * Where the long abstracts is the nif:isString attribute of the nif instance representing the abstract section of a wikipage.
  *
  * We are going to to use this method for generating the abstracts from release 2016-10 onwards.
  * It will be expanded to cover the whole wikipage in the future.
  */

@SoftwareAgentAnnotation(classOf[NifExtractor], AnnotationType.Extractor)
class NifExtractor(
     context : {
       def ontology : Ontology
       def language : Language
       def configFile : Config
       def recorder[T: ClassTag] : ExtractionRecorder[T]
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

  protected val dbpediaVersion: String = context.configFile.dbPediaVersion

  override val datasets = Set(DBpediaDatasets.NifContext,DBpediaDatasets.NifPageStructure,DBpediaDatasets.NifTextLinks,DBpediaDatasets.LongAbstracts, DBpediaDatasets.ShortAbstracts, DBpediaDatasets.RawTables, DBpediaDatasets.Equations)

  private val mwConnector = new MediaWikiConnector(context.configFile.mediawikiConnection, context.configFile.nifParameters.nifTags.split(","))

  override def extract(pageNode : WikiPage, subjectUri : String): Seq[Quad] =
  {
    //Only extract abstracts for pages from the Main namespace
    if(pageNode.title.namespace != Namespace.Main) return Seq.empty

    //Don't extract abstracts from redirect and disambiguation pages
    if(pageNode.isRedirect || pageNode.isDisambiguation) return Seq.empty

    //Retrieve page text
    val html = mwConnector.retrievePage(pageNode.title, apiParametersFormat) match{
      case Some(t) => NifExtractor.postProcessExtractedHtml(pageNode.title, t)
      case None => return Seq.empty
    }

    new WikipediaNifExtractor(context, pageNode).extractNif(html)(err => context.recorder[PageNode].record(err.asInstanceOf[RecordEntry[PageNode]]))
  }

}

object NifExtractor{
  //TODO check if this function is still relevant
  //copied from AbstractExtractor
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
