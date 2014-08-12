package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Dataset, Quad}
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.wikiparser.impl.json.JsonWikiParser

/**
 * User: hadyelsahar
 * Date: 11/19/13
 * Time: 12:43 PM
 *
 * JsonParseExtractor as explained in the design : https://f.cloud.github.com/assets/607468/363286/1f8da62c-a1ff-11e2-99c3-bb5136accc07.png
 *
 * send page to JsonParser, if jsonparser returns none do nothing
 * if it's parsed correctly send the JsonNode to the next level extractors
 *
 * @param extractors a Sequence of CompositeJsonNodeExtractor
 *
 * */
 class JsonParseExtractor(extractors: CompositeJsonNodeExtractor)extends Extractor[WikiPage]{

  override val datasets: Set[Dataset] = extractors.datasets

  override def extract(input: WikiPage, subjectUri: String, context: PageContext): Seq[Quad] = {

    val parser = new JsonWikiParser()
    val node = parser(input)
    node match {
      case Some(n) =>  extractors.extract(n, subjectUri, context)
      case None =>  Seq.empty
    }

  }

}
