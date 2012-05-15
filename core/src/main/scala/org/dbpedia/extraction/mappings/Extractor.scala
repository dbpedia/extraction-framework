package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.wikiparser._

/**
 * The base class of all extractors.
 * Concrete extractors override the extract() method.
 * Each implementing class must be thread-safe.
 * 
 * TODO: improve class hierarchy and object tree. It doesn't smell right that the apply method
 * is only called on the root of a tree of extractors, and extract is called on all others.
 */
trait Extractor extends Mapping[PageNode] with (PageNode => Seq[Quad])
{
    /**
     * Processes a wiki page and returns the extracted data.
     *
     * @param page The source page
     * @return A graph holding the extracted data
     */
    final def apply(page : PageNode) : Seq[Quad] =
    {
      //Generate the page URI
      val uri = page.title.language.resourceUri.append(page.title.decodedWithNamespace)
      
      //Extract
      extract(page, uri, new PageContext())
    }
}
