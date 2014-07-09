package org.dbpedia.extraction.mappings.fr

import java.net.{URI,URISyntaxException}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.config.mappings.HomepageExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, UriUtils}
import org.dbpedia.extraction.mappings._
import scala.language.reflectiveCalls

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
extends PageNodeExtractor {
    private val populationProperty = context.ontology.properties("populationTotal")
    private val populationRegex = """pop=(\d+)""".r
    override val datasets = Set(DBpediaDatasets.FrenchPopulation)

    override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] =
    {
	if (context.language.wikiCode == "fr") {
            if(page.title.namespace != Namespace.Template || page.isRedirect || !page.title.decoded.contains("évolution population") || page.title.decoded.contains("Discussion") || page.title.decoded.contains("Modèles")) return Seq.empty
  	        for (node <- page.children) {
	            if (node.toWikiText.contains("|pop=")) {
		        populationRegex.findAllIn(node.toWikiText).matchData foreach {
			    m =>
			    val city = page.title.decoded.split("/")(1).replace(" ", "_")
			
			    if (!isAllDigits(city)) {
		    	        val newUri = context.language.resourceUri.append(city)
			    
			        return Seq (new Quad(context.language, DBpediaDatasets.FrenchPopulation, newUri, populationProperty, m.group(1), node.sourceUri))
			    }
		        }
	            }
                }

                Seq.empty
        }
	else {
	    throw new Exception("The PopulationExtractor is only made for the french Wikipedia dump")
	}
    }
    
    private def isAllDigits(x: String) = x.matches("^\\d*$")
}
