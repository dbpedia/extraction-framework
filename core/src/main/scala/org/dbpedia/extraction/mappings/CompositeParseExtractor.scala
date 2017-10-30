package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.wikiparser.WikiPage

import scala.collection.mutable.ArrayBuffer

/**
 * TODO: generic type may not be optimal.
 */
@SoftwareAgentAnnotation(classOf[CompositeParseExtractor], AnnotationType.Extractor)
class CompositeParseExtractor(context : { def redirects : Redirects }, extractors: Extractor[_]*)
extends WikiPageExtractor
{
  override val datasets: Set[Dataset] = extractors.flatMap(_.datasets).toSet

  //define different types of Extractors
  private val wikiPageExtractors = new ArrayBuffer[Extractor[WikiPage]]()
  private val pageNodeExtractors = new ArrayBuffer[PageNodeExtractor]()
  private val jsonNodeExtractors = new ArrayBuffer[JsonNodeExtractor]()
  private val finalExtractors  = new ArrayBuffer[Extractor[WikiPage]]()

  //if extractor is not either PageNodeExtractor or JsonNodeExtractor so it accepts WikiPage as input
  extractors foreach {
    case extractor@(_: PageNodeExtractor) => pageNodeExtractors += extractor.asInstanceOf[PageNodeExtractor] //select all extractors which take PageNode to wrap them in WikiParseExtractor
    case extractor@(_: JsonNodeExtractor) => jsonNodeExtractors += extractor.asInstanceOf[JsonNodeExtractor]
    case extractor@(_: WikiPageExtractor) => wikiPageExtractors += extractor.asInstanceOf[WikiPageExtractor] //select all extractors which take Wikipage to wrap them in a CompositeExtractor
    case _ =>
  }

  if (wikiPageExtractors.nonEmpty)
    finalExtractors += new CompositeWikiPageExtractor(new CompositeExtractor[WikiPage](wikiPageExtractors :_*))

  //create and load WikiParseExtractor here
  if (pageNodeExtractors.nonEmpty)
    finalExtractors += new WikiParseExtractor(new CompositePageNodeExtractor(pageNodeExtractors :_*), context)

  //create and load JsonParseExtractor here
  if (jsonNodeExtractors.nonEmpty)
    finalExtractors += new JsonParseExtractor(new CompositeJsonNodeExtractor(jsonNodeExtractors :_*))

  private val immutableExtractors = finalExtractors.toList

  override def extract(input: WikiPage, subjectUri: String): Seq[Quad] = {

    if (finalExtractors.isEmpty)
      Seq.empty
    else
      new CompositeExtractor[WikiPage](immutableExtractors :_*).extract(input, subjectUri)
  }
}

/**
 * Creates new extractors.
 */
object CompositeParseExtractor
{
  /**
   * Creates a new CompositeExtractor loaded with same type of Extractors[T]
   *
   * TODO: using reflection here loses compile-time type safety.
   *
   * @param classes List of extractor classes to be instantiated
   * @param context Any type of object that implements the required parameter methods for the extractors
   */
  def load(classes: Seq[Class[_ <: Extractor[_]]], context : { def redirects : Redirects }): WikiPageExtractor =
  {
    val extractors = classes.map(_.getConstructor(classOf[AnyRef]).newInstance(context))
    new CompositeParseExtractor(context, extractors: _*)
  }
}




