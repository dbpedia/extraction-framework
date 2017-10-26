package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.wikiparser.impl.WikiParserWrapper

/**
 * Parses WikiText source and builds an Abstract Syntax Tree.
 * Create new instances of this trait by using the companion object.
 */
trait WikiParser extends ((WikiPage, Redirects) => Option[PageNode])
{
    /**
     * Parses WikiText source and returns its Abstract Syntax Tree.
     *
     * @param page The page
      * @param templateRedirects - if available, the template redirects (usually taken from an extraction context)
     * @return The PageNode which represents the root of the AST
     * @throws WikiParserException if an error occured during parsing
     */
    def apply(page : WikiPage, templateRedirects: Redirects = new Redirects(Map())) : Option[PageNode]
}

/**
 * Creates new WikiParser instances.
 */
object WikiParser
{
  /**
   * Creates a new WikiParser instance.
   */
  def getInstance(name : String = null) : WikiParser =  {
    new WikiParserWrapper(name)
  }
}
