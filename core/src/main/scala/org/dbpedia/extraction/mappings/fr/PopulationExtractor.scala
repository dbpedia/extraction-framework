package org.dbpedia.extraction.mappings.fr

import java.net.{URI,URISyntaxException}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.config.mappings.HomepageExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, UriUtils}
import org.dbpedia.extraction.mappings._

/**
 * Extracts links to the official homepage of an instance.
 */
class PopulationExtractor(
  context : {
    def ontology : Ontology
    def language : Language
    def redirects : Redirects
  }
)
extends Extractor
{
  private val populationProperty = context.ontology.properties("populationTotal")

  private val populationRegex = """pop=(\d+)""".r

  override val datasets = Set(DBpediaDatasets.FrenchPopulation)

  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Template || page.isRedirect || !page.title.decoded.contains("évolution population") || page.title.decoded.contains("Discussion")) return Seq.empty
   
    for (node <- page.children) {
	if (node.toWikiText.contains("|pop=")) {
		populationRegex.findAllIn(node.toWikiText).matchData foreach {
			m =>
			val city = page.title.decoded.split("/")(1).replace(" ", "_")
			if (!isAllDigits(city)) {
				val newUri = "http://fr.dbpedia.org/resource/" + city
				return Seq (new Quad(context.language, DBpediaDatasets.FrenchPopulation, newUri, populationProperty, m.group(1), node.sourceUri, context.ontology.datatypes("xsd:nonNegativeInteger")))
			}
		}
	}
    }

    Seq.empty
  }

  private def isAllDigits(x: String) = x.matches("^\\d*$")
}
