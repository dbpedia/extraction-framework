package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.Quad

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.dataparser._

import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.config.mappings.InfoboxExtractorConfig

import scala.collection.mutable.ArrayBuffer
import org.dbpedia.extraction.config.dataparser.DataParserConfig
import org.dbpedia.iri.UriUtils

import scala.language.reflectiveCalls

/**
 * This extractor extracts all properties from all infoboxes.
 * Extracted information is represented using properties in the http://xx.dbpedia.org/property/
 * namespace (where xx is the language code).
 * The names of the these properties directly reflect the name of the Wikipedia infobox property.
 * Property names are not cleaned or merged.
 * Property types are not part of a subsumption hierarchy and there is no consistent ontology for the infobox dataset.
 * The infobox extractor performs only a minimal amount of property value clean-up, e.g., by converting a value like “June 2009” to the XML Schema format “2009–06”.
 * You should therefore use the infobox dataset only if your application requires complete coverage of all Wikipeda properties and you are prepared to accept relatively noisy data.
 */

class SisterProjectExtractor(
                        context : {
                          def ontology : Ontology
                          def language : Language
                          def redirects : Redirects
                        }
                      )
  extends PageNodeExtractor
{
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Configuration
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private val ontology = context.ontology

  private val language = context.language

  private val wikiCode = language.wikiCode


  private val ignoreTemplates = InfoboxExtractorConfig.ignoreTemplates

  private val ignoreTemplatesRegex = InfoboxExtractorConfig.ignoreTemplatesRegex

  private val ignoreProperties = InfoboxExtractorConfig.ignoreProperties

  private val rdfLangStrDt = ontology.datatypes("rdf:langString")

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Regexes
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // TODO: i18n

  private val SplitWordsRegex = InfoboxExtractorConfig.SplitWordsRegex

  private val TrailingNumberRegex = InfoboxExtractorConfig.TrailingNumberRegex


  private val splitPropertyNodeRegexInfobox = if (DataParserConfig.splitPropertyNodeRegexInfobox.contains(wikiCode))
    DataParserConfig.splitPropertyNodeRegexInfobox.get(wikiCode).get
  else DataParserConfig.splitPropertyNodeRegexInfobox.get("en").get
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Parsers
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  private val sameAsProperty = context.ontology.properties("owl:sameAs")


  /// NEED TO BE EXTENDED TO OTHERS LANGUAGES
  private val regexMap = Map(
  "fr"->List("autres projet.*".r),
  "en"->List("sister project.*".r),
  "de"->List("schwesterprojekte.*".r)

  )


  private val currentRegexList = regexMap(wikiCode)

  private val objectParser = new ObjectParser(context, true)
  //USELESS ?
  private val linkParser = new LinkParser(true)

  private val mapAbrev = Map(
    "c" -> "commons",
    "wikt" -> "wiktionary",
    "n" -> "wikinews",
    "voy" -> "wikivoyage",
    "q" -> "wikiquote",
    "s" -> "wikisource",
    "b" -> "wikibooks",
    "v" -> "wikiversity",
    "wikispecies" -> "species"
  )
  private val mapProjects = Map(
    "commons" -> "http://commons.dbpedia.org/resource/",
    "wiktionary" -> "https://wiktionary.org/wiki/",
    "wikinews" -> "https://wikinews.org/wiki/",
    "wikivoyage" -> "https://wikivoyage.org/wiki/",
    "wikiquote" -> "https://wikiquote.org/wiki/",
    "wikisource" -> "https://wikisource.org/wiki/",
    "wikibooks" -> "https://wikibooks.org/wiki/",
    "wikiversity" -> "https://wikibooks.org/wiki/",
    "species" -> "https://species.wikimedia.org/wiki/"

  )

  override val datasets = Set(DBpediaDatasets.SisterProjectLink)


  override def extract(node : PageNode, subjectUri : String) : Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(node.title)) return Seq.empty
    val quads = new ArrayBuffer[Quad]()

    /** Retrieve all templates on the page which are not ignored */
    for { template <- InfoboxExtractor.collectTemplates(node)
          resolvedTitle = context.redirects.resolve(template.title).decoded.toLowerCase
          if !ignoreTemplates.contains(resolvedTitle)
          if !ignoreTemplatesRegex.exists(regex => regex.unapplySeq(resolvedTitle).isDefined)
          if currentRegexList.exists(regex => regex.findFirstMatchIn(resolvedTitle).isDefined)
          }
    {
      val propertyList = template.children.filterNot(property => ignoreProperties.get(wikiCode).getOrElse(ignoreProperties("en")).contains(property.key.toLowerCase))


        for(property <- propertyList; if (!property.key.forall(_.isDigit))) {

          // TODO clean HTML

          val cleanedPropertyNode = NodeUtil.removeParentheses(property)

          val splitPropertyNodes = NodeUtil.splitPropertyNode(cleanedPropertyNode, splitPropertyNodeRegexInfobox)


          for(splitNode <- splitPropertyNodes; pr <- extractValue(splitNode))
          {
            val propertyUri = getPropertyUri(property.key)
            try
            {

              if (mapProjects.contains(property.key)) {
                val value =  mapProjects(property.key).replace(property.key, language.wikiCode + "." + property.key) + WikiUtil.wikiEncode(pr.value)
                quads += new Quad(language, DBpediaDatasets.SisterProjectLink, subjectUri, sameAsProperty, value, splitNode.sourceIri, null)
              }
              if (mapAbrev.contains(property.key)) {
                val keyProj=mapAbrev(property.key)
                val value = mapProjects(keyProj).replace(keyProj, language.wikiCode + "." + keyProj) + WikiUtil.wikiEncode(pr.value)
                quads += new Quad(language, DBpediaDatasets.SisterProjectLink, subjectUri, sameAsProperty, value, splitNode.sourceIri, null)
              }
            }
            catch
            {
              case ex : IllegalArgumentException => println(ex)
            }
          }
        }
      //}
    }

    quads
  }


  private def extractValue(node: PropertyNode): List[ParseResult[String]] = {

    extractLinks(node) match {
      case links if links.nonEmpty => {
        return links
      }
      case _ =>
    }
    StringParser.parse(node).map(value => ParseResult(value.value, None, Some(rdfLangStrDt))).toList
  }

  private def extractLinks(node : PropertyNode) : List[ParseResult[String]] =
  {
    val splitNodes = NodeUtil.splitPropertyNode(node, """\s*\W+\s*""")

    splitNodes.flatMap(splitNode => objectParser.parse(splitNode)) match
    {
      // TODO: explain why we check links.size == splitNodes.size
      case links if links.size == splitNodes.size => return links

    }

    splitNodes.flatMap(splitNode => linkParser.parse(splitNode)) match
    {
      // TODO: explain why we check links.size == splitNodes.size
      case links if links.size == splitNodes.size => links.map(x => UriUtils.cleanLink(x.value)).collect{case Some(link) => ParseResult(link)}
      case _ => List.empty
    }
  }


  private def getPropertyUri(key : String) : String =
  {
    // convert property key to camelCase
    var result = key.toLowerCase(language.locale).trim
    result = result.toCamelCase(SplitWordsRegex, language.locale)

    // Rename Properties like LeaderName1, LeaderName2, ... to LeaderName
    result = TrailingNumberRegex.replaceFirstIn(result, "")

    result = WikiUtil.cleanSpace(result)

    language.propertyUri.append(result)
  }


}