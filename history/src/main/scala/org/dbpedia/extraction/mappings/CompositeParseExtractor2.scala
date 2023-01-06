package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.wikiparser.{WikiPage, WikiPageWithRevisions}

import scala.collection.mutable.ArrayBuffer

/**
 * TODO: generic type may not be optimal.
 */
class CompositeParseExtractor2(extractors: Extractor[_]*)
extends WikiPageWithRevisionsExtractor
{

  override val datasets: Set[Dataset] = extractors.flatMap(_.datasets).toSet

  //define different types of Extractors
  private val wikiPageExtractors = new ArrayBuffer[Extractor[WikiPageWithRevisions]]()
  //private val pageNodeExtractors = new ArrayBuffer[PageNodeExtractor]()
  //private val jsonNodeExtractors = new ArrayBuffer[JsonNodeExtractor]()
  private val finalExtractors = new ArrayBuffer[Extractor[WikiPageWithRevisions]]()


  //if extractor is not either PageNodeExtractor or JsonNodeExtractor so it accepts WikiPage as input
  extractors foreach { extractor =>
    extractor match {

    //  case _: PageNodeExtractor => pageNodeExtractors += extractor.asInstanceOf[PageNodeExtractor] //select all extractors which take PageNode to wrap them in WikiParseExtractor
     // case _: JsonNodeExtractor => jsonNodeExtractors += extractor.asInstanceOf[JsonNodeExtractor]
      case _: WikiPageWithRevisionsExtractor => wikiPageExtractors += extractor.asInstanceOf[WikiPageWithRevisionsExtractor] //select all extractors which take Wikipage to wrap them in a CompositeExtractor
      case _ =>
    }
    println(extractor.toString)
  }

  println(wikiPageExtractors.toString())
  if (wikiPageExtractors.nonEmpty)
    finalExtractors += new CompositeWikiPageWithRevisionExtractor(new CompositeExtractor2[WikiPageWithRevisions](wikiPageExtractors :_*))

  //create and load WikiParseExtractor here
 // if (pageNodeExtractors.nonEmpty)
   // finalExtractors += new WikiParseExtractor(new CompositePageNodeExtractor2(pageNodeExtractors :_*))

  //create and load JsonParseExtractor here
  //if (jsonNodeExtractors.nonEmpty)
    //finalExtractors += new JsonParseExtractor(new CompositeJsonNodeExtractor(jsonNodeExtractors :_*))

  private val immutableExtractors = finalExtractors.toList

  override def extract(input: WikiPageWithRevisions, subjectUri: String): Seq[Quad] = {

    if (finalExtractors.isEmpty)
      Seq.empty
    else
      new CompositeExtractor2[WikiPageWithRevisions](immutableExtractors :_*).extract(input, subjectUri)
  }
}

/**
 * Creates new extractors.
 */
object CompositeParseExtractor2
{
  /**
   * Creates a new CompositeExtractor loaded with same type of Extractors[T]
   *
   * TODO: using reflection here loses compile-time type safety.
   *
   * @param classes List of extractor classes to be instantiated
   * @param context Any type of object that implements the required parameter methods for the extractors
   */
  def load(classes: Seq[Class[_ <: Extractor[_]]], context: AnyRef): WikiPageWithRevisionsExtractor =
  {
    println("IN CompositeParseExtractor2")
    val extractors = classes.map(_.getConstructor(classOf[AnyRef]).newInstance(context))
    new CompositeParseExtractor2(extractors: _*)
  }
}




