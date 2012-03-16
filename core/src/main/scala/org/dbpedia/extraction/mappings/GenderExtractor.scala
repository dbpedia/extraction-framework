package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.mappings.GenderExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import util.matching.Regex
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad, Graph}
import org.dbpedia.extraction.ontology.datatypes.Datatype

/**
 * Extracts the grammatical gender of people using a heuristic.
 */
class GenderExtractor( context : {
                             def mappings : Mappings
                             def ontology : Ontology
                             def language : Language
                             def redirects : Redirects } ) extends MappingExtractor(context)
{
    private val language = context.language.wikiCode

    require(GenderExtractorConfig.supportedLanguages.contains(language),"Gender Extractor supports the following languages: " + GenderExtractorConfig.supportedLanguages.mkString(", ")+"; not "+language)

    private val pronounMap: Map[String, String] = GenderExtractorConfig.pronounsMap.getOrElse(language, GenderExtractorConfig.pronounsMap("en"))

    private val genderProperty = "http://xmlns.com/foaf/0.1/gender"

    private val typeProperty = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
    private val personUri = "http://dbpedia.org/ontology/Person"

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        // apply mappings
        val mappingGraph = super.extract(node, subjectUri, pageContext)

        // if this page is mapped onto Person
        if (mappingGraph.quads.exists((q: Quad) => q.predicate == typeProperty && q.value == personUri))
        {
            // get the page text
            val wikiText: String = node.toWikiText()

            // count gender pronouns
            var genderCounts: Map[String, Int] = Map()
            for ((pronoun, gender) <- pronounMap)
            {
                val regex = new Regex("\\W" + pronoun + "\\W")
                val count = regex.findAllIn(wikiText).size
                val oldCount = genderCounts.getOrElse(gender, 0)
                genderCounts = genderCounts.updated(gender, oldCount + count)
            }

            // get maximum gender
            var maxGender = ""
            var maxCount = 0
            var secondCount = 0.0
            for ((gender, count) <- genderCounts)
            {
                if (count > maxCount)
                {
                    secondCount = maxCount.toDouble
                    maxCount = count
                    maxGender = gender
                }
            }

            // output triple for maximum gender
            if (maxGender != "" && maxCount > GenderExtractorConfig.minCount && maxCount/secondCount > GenderExtractorConfig.minDifference)
            {
                return new Graph(new Quad(context.language, DBpediaDatasets.Genders, subjectUri, genderProperty, maxGender, node.sourceUri, new Datatype("xsd:string")) :: Nil)
            }
        }

        new Graph()
    }

}
