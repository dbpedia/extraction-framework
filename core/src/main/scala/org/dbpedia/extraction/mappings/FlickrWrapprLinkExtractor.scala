package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.extraction.destinations.QuadBuilder

/**
 * Extracts links to flickr wrappr. Scala version of 
 * http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/file/default/related_apps/flickrwrappr/generateLinks.php
 */
class FlickrWrapprLinkExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends Extractor
{
  private val language = context.language
  
  require (language == Language.English, getClass.getSimpleName+" can only be used for language "+Language.English.wikiCode)

  private val hasPhotoCollectionProperty = language.propertyUri.append("hasPhotoCollection")
  
  private val flickrWrapprUrlPrefix = "http://www4.wiwiss.fu-berlin.de/flickrwrappr/photos/"
  
  override val datasets = Set(DBpediaDatasets.FlickrWrapprLinks)
  
  private val quad = QuadBuilder.stringPredicate(language, DBpediaDatasets.FlickrWrapprLinks, hasPhotoCollectionProperty, null) _
  
  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] =
  {
    if (page.title.namespace != Namespace.Main || page.isRedirect || page.isDisambiguation) return Seq.empty
    
    // Note: subjectUri is probably identical to the URI we use here, but we want to be sure. 
    Seq(quad(language.resourceUri.append(page.title.decodedWithNamespace), flickrWrapprUrlPrefix+page.title.encoded, page.sourceUri))
  }
}