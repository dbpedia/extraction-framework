package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Dataset,Quad}
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.mappings.Extractor
import org.dbpedia.extraction.sources.{WikiPage}
/**
 * TODO: generic type may not be optimal.
 */
class CompositeExtractor[N](mappings: Extractor[N]*) extends Extractor[N]
{
  override val datasets: Set[Dataset] = mappings.flatMap(_.datasets).toSet

  override def extract(input: N, subjectUri: String, context: PageContext): Seq[Quad] = {
    mappings.flatMap(_.extract(input, subjectUri, context))
}
}


/**
 * Creates new extractors.
 */
object CompositeExtractor
{
    /**
     *
     * @param classes List of Extractors to be initialized Might be of Different Types
     * @param context Any type of object that implements the required parameter methods for the extractors
     * @return new CompositeExtractor that is loaded with Extractors and ParseExtractors
     */
    def loadToParsers(classes : Seq[Class [_ <:Extractor[_]]], context : AnyRef) : CompositeExtractor[WikiPage] = {

      val extractors = classes.map(_.getConstructor(classOf[AnyRef]).newInstance(context))

      //define different types of Extractors
      var wikiPageExtractors =Seq[Extractor[WikiPage]]()
      var pageNodeExtractors =Seq[PageNodeExtractor]()
      var jsonNodeExtractors =Seq[JsonNodeExtractor]()
      //to do: add json extractors

      //if extractor is not either PageNodeExtractor or JsonNodeExtractor so it accepts WikiPage as input
      extractors foreach { extractor =>
        extractor match {

          case _ :PageNodeExtractor =>  pageNodeExtractors  = pageNodeExtractors :+ extractor.asInstanceOf[PageNodeExtractor]           //select all extractors which take PageNode to wrap them in WikiParseExtractor
          case _ :JsonNodeExtractor =>  jsonNodeExtractors  = jsonNodeExtractors :+ extractor.asInstanceOf[JsonNodeExtractor]
          case _ => wikiPageExtractors  = wikiPageExtractors :+ extractor.asInstanceOf[Extractor[WikiPage]]           //select all extractors which take Wikipage to wrap them in a CompositeExtractor
          case _ =>
        }
      }

      val wikipageCompositeExtractor = new CompositeExtractor[WikiPage](wikiPageExtractors :_*)

      //create and load WikiParseExtractor here
      val compositePageNodeExtractors = new CompositePageNodeExtractor(pageNodeExtractors :_*)
      val wikiParseExtractor = new WikiParseExtractor(compositePageNodeExtractors)

      //create and load JsonParseExtractor here
      val compositeJsonNodeExtractors = new CompositeJsonNodeExtractor(jsonNodeExtractors :_*)
      val jsonParseExtractor = new JsonParseExtractor(compositeJsonNodeExtractors)

      //collect ParseExtractors and CompositeExtractor
      val allExtractors = Seq[Extractor[WikiPage]](wikiParseExtractor,jsonParseExtractor) ++ wikipageCompositeExtractor

      new CompositeExtractor[WikiPage](allExtractors :_*)
    }

    /**
     * Creates a new CompositeExtractor loaded with same type of Extractors[T]
     *
     * TODO: using reflection here loses compile-time type safety.
     *
     * @param classes List of extractor classes to be instantiated
     * @param context Any type of object that implements the required parameter methods for the extractors
     */
    def load(classes: Seq[Class[_ <: PageNodeExtractor]], context: AnyRef): PageNodeExtractor =
    {
      val extractors = classes.map(_.getConstructor(classOf[AnyRef]).newInstance(context))
      new CompositePageNodeExtractor(extractors: _*)
    }
}
