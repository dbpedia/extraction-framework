package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.sources.WikiPage

/**
 * Parses WikiText source and builds an Abstract Syntax Tree.
 * Create new instances of this trait by using the companion object.
 */
trait WikiParser extends (WikiPage => PageNode)
{
    /**
     * Parses WikiText source and returns its Abstract Syntax Tree.
     *
     * @param page The page
     * @return The PageNode which represents the root of the AST
     * @throws WikiParserException if an error occured during parsing
     */
    def apply(page : WikiPage) : PageNode
}
