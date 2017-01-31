package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.ExtractorAnnotation
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.nif.WikipediaNifExtractor
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{Config, Language}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces

import scala.language.reflectiveCalls

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

@ExtractorAnnotation("nif extractor")
class NifExtractor(
     context : {
       def ontology : Ontology
       def language : Language
       def configFile : Config
     }
   )
  extends AbstractExtractor(context)
{
  //API parameters to geht HTML of first section
  override val apiParametersFormat = "uselang="+language + context.configFile.nifParameters.nifQuery

  override val xmlPath = context.configFile.nifParameters.nifTags.split(",").map(_.trim)

  protected val isTestRun = context.configFile.nifParameters.isTestRun
  protected val writeLinkAnchors = context.configFile.nifParameters.writeLinkAnchor
  protected val writeStrings = context.configFile.nifParameters.writeAnchor
  protected val shortAbstractLength = context.configFile.abstractParameters.shortAbstractMinLength

  protected val dbpediaVersion = context.configFile.dbPediaVersion

  override val datasets = Set(DBpediaDatasets.NifContext,DBpediaDatasets.NifPageStructure,DBpediaDatasets.NifTextLinks,DBpediaDatasets.LongAbstracts, DBpediaDatasets.ShortAbstracts, DBpediaDatasets.RawTables)

  private val templateString = Namespaces.names(context.language).get(Namespace.Template.code) match {
    case Some(x) => x
    case None => "Template"
  }

  override def extract(pageNode : WikiPage, subjectUri : String): Seq[Quad] =
  {
    //Only extract abstracts for pages from the Main namespace
    if(pageNode.title.namespace != Namespace.Main) return Seq.empty

    //Don't extract abstracts from redirect and disambiguation pages
    if(pageNode.isRedirect || pageNode.isDisambiguation) return Seq.empty

    //Retrieve page text
    val html = retrievePage(pageNode.title, pageNode.id, pageNode.isRetry) match{
      case Some(t) => postProcess(pageNode.title, t)
      case None => return Seq.empty
    }

    val nifContextIri = subjectUri + "?dbpv=" + dbpediaVersion + "&nif=context"
    new WikipediaNifExtractor(context, nifContextIri).extractNif(pageNode.sourceIri, subjectUri, html)(err => pageNode.addExtractionRecord("", err, pushToStd = true))
  }

}
