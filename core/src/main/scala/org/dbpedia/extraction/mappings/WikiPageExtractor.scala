package org.dbpedia.extraction.mappings

/**
 * Extractors are mappings that extract data from a WikiPage.
 * Necessary to get some type safety in CompositeExtractor: 
 * Class[_ <: Extractor] can be checked at runtime, but Class[_ <: Mapping[PageNode]] can not.
 */
trait WikiPageExtractor extends PageNodeExtractor{

}
