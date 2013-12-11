package org.dbpedia.extraction.mappings
import org.dbpedia.extraction.wikiparser.JsonNode

class CompositeJsonNodeExtractor(extractors: Extractor[JsonNode]*)
extends CompositeExtractor[JsonNode](extractors: _*)
with JsonNodeExtractor

/**
 * Creates new extractors.
 */
object CompositeJsonNodeExtractor
{
  /**
   * Creates a new extractor.
   *
   * TODO: using reflection here loses compile-time type safety.
   *
   * @param extractors List of extractor classes to be instantiated
   * @param context Any type of object that implements the required parameter methods for the extractors
   */
  def load(classes: Seq[Class[_ <: JsonNodeExtractor]], context: AnyRef): JsonNodeExtractor =
  {
    val extractors = classes.map(_.getConstructor(classOf[AnyRef]).newInstance(context))
    new CompositePageNodeExtractor(extractors: _*)
  }
}
