package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.destinations.{Quad, Dataset}
import collection.mutable.ListBuffer
import org.dbpedia.extraction.util.Language

class DummyExtractor(extractionContext : { def language : Language })
  extends Extractor
{
  private val dataset = new Dataset("base")
  
  override val datasets = Set(dataset)
  
  def extract(page: PageNode, subjectUri: String, context: PageContext) : Seq[Quad] =
  {
    val quads = new ListBuffer[Quad]


    val skosSubject = "http://www.w3.org/TR/skos-reference/skos.html#subject"
    val catPrefix = AugmentExtractorConstants.categoryPrefix

    quads.append(new Quad(extractionContext.language, dataset, "http://s", skosSubject, catPrefix + "Something_with_London", "", null))
    quads.append(new Quad(extractionContext.language, dataset, "http://s", skosSubject, catPrefix + "Something_with_Germany", "", null))
    quads.append(new Quad(extractionContext.language, dataset, "http://s", skosSubject, catPrefix + "Something_with_Mali", "", null))

    return quads.toList
  }
}
