package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.destinations.{Quad, Dataset, Graph}
import collection.mutable.ListBuffer
import org.dbpedia.extraction.util.Language

class DummyExtractor(extractionContext : { val language : Language })
  extends Extractor
{
  def extract(page: PageNode, subjectUri: String, context: PageContext) : Graph =
  {
    val dataSet = new Dataset("base")
    val quads = new ListBuffer[Quad]


    val skosSubject = "http://www.w3.org/TR/skos-reference/skos.html#subject"
    val catPrefix = AugmentExtractorConstants.categoryPrefix

    quads.append(new Quad(extractionContext.language, dataSet, "http://s", skosSubject, catPrefix + "Something_with_London", "", null))
    quads.append(new Quad(extractionContext.language, dataSet, "http://s", skosSubject, catPrefix + "Something_with_Germany", "", null))
    quads.append(new Quad(extractionContext.language, dataSet, "http://s", skosSubject, catPrefix + "Something_with_Mali", "", null))

    return new Graph(quads.toList)
  }
}
