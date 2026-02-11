package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.mappings.GenderExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import util.matching.Regex
import org.dbpedia.extraction.ontology.datatypes.Datatype
import scala.language.reflectiveCalls

/**
 * Extracts the grammatical gender of people using a heuristic.
 */
class GenderExtractor(
  context : {
    def mappings : Mappings
    def ontology : Ontology
    def language : Language
    def redirects : Redirects
  }
)
extends MappingExtractor(context)
{
  private val language = context.language.wikiCode

  private val pronounMap: Map[String, String] =
    GenderExtractorConfig.pronounsMap(language)

  // ✅ Use ontology instead of hardcoded URIs
  private val genderProperty =
    context.ontology.properties("foaf:gender").uri

  private val typeProperty =
    context.ontology.properties("rdf:type").uri

  private val personUri =
    context.ontology.classes("Person").uri

  override val datasets = Set(DBpediaDatasets.Genders)

  override def extract(node : PageNode, subjectUri : String) : Seq[Quad] =
  {
    // apply mappings
    // FIXME: To find out if it's a person, we extract all mapped properties a second time and throw them away.
    // Find a better solution. For example: Make sure that this extractor runs after the 
    // MappingExtractor. In the MappingExtractor, set the page type as an attriute.
    // Even better: in the first extraction pass, extract all types. Use them in the second pass.
    val mappingGraph = super.extract(node, subjectUri)

    // check if page is typed as Person
    if (mappingGraph.exists(q =>
      q.predicate == typeProperty && q.value == personUri
    ))
    {
      // get the page text
      val wikiText: String = node.toWikiText

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

      if (
        maxGender != "" &&
        maxCount > GenderExtractorConfig.minCount &&
        secondCount > 0 &&
        maxCount / secondCount > GenderExtractorConfig.minDifference
      )
      {
        return Seq(
          new Quad(
            context.language,
            DBpediaDatasets.Genders,
            subjectUri,
            genderProperty,
            maxGender,
            node.sourceIri,
            new Datatype("rdf:langString")
          )
        )
      }
    }

    Seq.empty
  }

}
