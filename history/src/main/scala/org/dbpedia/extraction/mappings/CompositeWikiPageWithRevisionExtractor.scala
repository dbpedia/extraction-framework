package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.{WikiPage, WikiPageWithRevisions}

class CompositeWikiPageWithRevisionExtractor(extractors: Extractor[WikiPageWithRevisions]*)
extends CompositeExtractor[WikiPageWithRevisions](extractors: _*)
with WikiPageWithRevisionsExtractor


