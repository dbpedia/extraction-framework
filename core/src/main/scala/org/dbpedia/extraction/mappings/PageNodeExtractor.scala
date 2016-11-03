package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

/**
 * Extractors are mappings that extract data from a PageNode.
 * Necessary to get some type safety in CompositeExtractor: 
 * Class[_ <: Extractor] can be checked at runtime, but Class[_ <: Mapping[PageNode]] can not.
 */
trait PageNodeExtractor extends Extractor[PageNode] {

  /**
    * A map for failed pages, which could be used for a better way to record extraction fails than just a simple console output.
    * @return the failed pages (id, title) for every Language
    */
  def listFailedPages: Map[Language, scala.collection.mutable.Map[(Long, WikiTitle), Throwable]] = Map.empty[Language, scala.collection.mutable.Map[(Long, WikiTitle), Throwable]]

}