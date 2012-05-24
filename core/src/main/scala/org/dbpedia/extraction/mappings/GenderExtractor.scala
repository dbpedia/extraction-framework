package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.mappings.GenderExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import util.matching.Regex
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.datatypes.Datatype

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

  private val pronounMap: Map[String, String] = GenderExtractorConfig.pronounsMap(language)

  // FIXME: don't use string constant, use context.ontology (or at least RdfNamespace.FOAF)
  private val genderProperty = "http://xmlns.com/foaf/0.1/gender"
  // FIXME: don't use string constant, use context.ontology (or at least RdfNamespace.RDF)
  private val typeProperty = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
  // FIXME: don't use string constant, use context.ontology (or at least DBpediaNamespace.ONTOLOGY)
  private val personUri = "http://dbpedia.org/ontology/Person"

  override val datasets = Set(DBpediaDatasets.Genders)

  override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    // apply mappings
    // FIXME: what a waste! we extract all mapped properties a second time and throw them away.
    // Find a better solution. For example: Make sure that this extractor runs after the 
    // MappingExtractor. In the MappingExtractor, set the page type as an attriute.
    // Even better: in the first extraction pass, extract all types. Use them in the second pass.
    val mappingGraph = super.extract(node, subjectUri, pageContext)

    // if this page is mapped onto Person
    if (mappingGraph.exists(q => q.predicate == typeProperty && q.value == personUri))
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

      // output triple for maximum gender
      if (maxGender != "" && maxCount > GenderExtractorConfig.minCount && maxCount/secondCount > GenderExtractorConfig.minDifference)
      {
        return Seq(new Quad(context.language, DBpediaDatasets.Genders, subjectUri, genderProperty, maxGender, node.sourceUri, new Datatype("xsd:string")))
      }
    }

    Seq.empty
  }

}
