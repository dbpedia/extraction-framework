package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Dataset
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.sources.WikiPage
import scala.collection.mutable.ArrayBuffer

/**
 * TODO: generic type may not be optimal.
 */
class CompositeParseExtractor(extractors: Extractor[_]*)
extends WikiPageExtractor
{
  override val datasets: Set[Dataset] = extractors.flatMap(_.datasets).toSet

  override def extract(input: WikiPage, subjectUri: String, context: PageContext): Seq[Quad] = {

    //val extractors = classes.map(_.getConstructor(classOf[AnyRef]).newInstance(context))

    //define different types of Extractors
    val wikiPageExtractors = new ArrayBuffer[Extractor[WikiPage]]()
    val pageNodeExtractors = new ArrayBuffer[PageNodeExtractor]()
    val jsonNodeExtractors = new ArrayBuffer[JsonNodeExtractor]()
    val finalExtractors    = new ArrayBuffer[Extractor[WikiPage]]()
    //to do: add json extractors

    val quads = new ArrayBuffer[Quad]()

    //if extractor is not either PageNodeExtractor or JsonNodeExtractor so it accepts WikiPage as input
    extractors foreach { extractor =>
      extractor match {

        case _ :PageNodeExtractor =>  pageNodeExtractors  += extractor.asInstanceOf[PageNodeExtractor]           //select all extractors which take PageNode to wrap them in WikiParseExtractor
        case _ :JsonNodeExtractor =>  jsonNodeExtractors  += extractor.asInstanceOf[JsonNodeExtractor]
        case _ :WikiPageExtractor =>  wikiPageExtractors  += extractor.asInstanceOf[Extractor[WikiPage]]           //select all extractors which take Wikipage to wrap them in a CompositeExtractor
        case _ =>
      }
    }

    if (!wikiPageExtractors.isEmpty)
      finalExtractors += new CompositeWikiPageExtractor(wikiPageExtractors :_*)

    //create and load WikiParseExtractor here
    if (!pageNodeExtractors.isEmpty)
      finalExtractors += new WikiParseExtractor(new CompositePageNodeExtractor(pageNodeExtractors :_*))

    //create and load JsonParseExtractor here
    if (!jsonNodeExtractors.isEmpty)
      finalExtractors += new JsonParseExtractor(new CompositeJsonNodeExtractor(jsonNodeExtractors :_*))

    if (finalExtractors.isEmpty)
      Seq.empty
    else
      new CompositeExtractor[WikiPage](finalExtractors :_*).extract(input, subjectUri, context)
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
  def load(classes: Seq[Class[_ <: Extractor[_]]], context: AnyRef): WikiPageExtractor =
  {
    val extractors = classes.map(_.getConstructor(classOf[AnyRef]).newInstance(context))
    new CompositeParseExtractor(extractors: _*)
  }
}




