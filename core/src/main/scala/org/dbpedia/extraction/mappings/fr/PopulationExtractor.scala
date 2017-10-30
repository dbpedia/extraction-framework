package org.dbpedia.extraction.mappings.fr

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
 * Extracts links to the official homepage of an instance.
 */
@SoftwareAgentAnnotation(classOf[PopulationExtractor], AnnotationType.Extractor)
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

    override def extract(page: PageNode, subjectUri: String): Seq[Quad] =
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
			    
			        return Seq (new Quad(context.language, DBpediaDatasets.FrenchPopulation, newUri, populationProperty, m.group(1), node.sourceIri))
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
