package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.mappings.GenderExtractorConfig
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._
import util.matching.Regex
import scala.language.reflectiveCalls
/**
 * Extracts the grammatical gender of people using a pronoun-based heuristic.
 */
class GenderExtractor(
  context: {
    def mappings: Mappings
    def ontology: Ontology
    def language: Language
    def redirects: Redirects
  }
) extends MappingExtractor(context) {
  
  /** Language code (en, de, fr, etc.) */
  private val language: String =
    context.language.wikiCode
  /** Pronoun → gender map (from config) */
  private val pronounMap: Map[String, String] =
    GenderExtractorConfig.pronounsMap(language)

  /** Ontology-based properties & classes */
  private val genderProperty =
    context.ontology.properties("foaf:gender")

  private val typeProperty =
    context.ontology.properties("rdf:type")

  private val personClass =
    context.ontology.classes("Person")

  private val langStringDatatype =
    new Datatype("rdf:langString")

  override val datasets = Set(DBpediaDatasets.Genders)

  override def extract(node: PageNode, subjectUri: String): Seq[Quad] = {

    /** First pass: extract mappings to detect rdf:type */
    val mappingGraph: Seq[Quad] =
      super.extract(node, subjectUri)

    /** Check if entity is a dbo:Person */
    val isPerson: Boolean =
      mappingGraph.exists(q =>
        q.predicate == typeProperty.uri &&
        q.value == personClass.uri
      )

    if (!isPerson) return Seq.empty

    /** Get full wiki text */
    val wikiText: String =
      node.toWikiText

    /** Count pronouns by gender */
    var genderCounts: Map[String, Int] =
      Map.empty.withDefaultValue(0)

    for ((pronoun, gender) <- pronounMap) {
      val regex =
        new Regex("(?i)\\b" + Regex.quote(pronoun) + "\\b")

      val count =
        regex.findAllIn(wikiText).size

      genderCounts =
        genderCounts.updated(gender, genderCounts(gender) + count)
    }

    if (genderCounts.isEmpty) return Seq.empty

    /** Find dominant gender */
    val sorted =
      genderCounts.toSeq.sortBy(-_._2)

    val (maxGender, maxCount) =
      sorted.head

    val secondCount: Double =
      if (sorted.size > 1) sorted(1)._2.toDouble else 0.0

    /** Avoid division-by-zero */
    val differenceOk: Boolean =
      secondCount == 0.0 ||
        (maxCount.toDouble / secondCount) >
          GenderExtractorConfig.minDifference

    /** Threshold checks */
    if (
      maxGender.nonEmpty &&
      maxCount > GenderExtractorConfig.minCount &&
      differenceOk
    ) {
      Seq(
        new Quad(
          context.language,
          DBpediaDatasets.Genders,
          subjectUri,
          genderProperty,
          maxGender,
          node.sourceIri,
          langStringDatatype
        )
      )
    } else {
      Seq.empty
    }
  }
}
