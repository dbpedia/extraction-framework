package org.dbpedia.extraction.mappings

import java.util.logging.Logger

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.{Language, MediaWikiConnector}
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
 * Extracts wiki texts like abstracts or sections in html.
  * NOTE: This class is not only used for abstract extraction but for extracting wiki text of the whole page
  * The NifAbstract Extractor is extending this class.
  * All configurations are now outsourced to //extraction-framework/core/src/main/resources/mediawikiconfig.json
  * change the 'publicParams' entries for tweaking endpoint and time parameters
 *
 * From now on we use MobileFrontend for MW <2.21 and TextExtracts for MW > 2.22
 * The patched mw instance is no longer needed except from minor customizations in LocalSettings.php
 * TextExtracts now uses the article entry and extracts the abstract. The retional for
 * the new extension is that we will not need to load all articles in MySQL, just the templates
 * At the moment, setting up the patched MW takes longer than the loading of all articles in MySQL :)
 * so, even this way it's way better and cleaner ;)
 * We leave the old code commented since we might re-use it soon
 */

@deprecated("replaced by NifExtractor.scala: which will extract the whole page content including the abstract", "2016-10")
@SoftwareAgentAnnotation(classOf[AbstractExtractor], AnnotationType.Extractor)
class AbstractExtractor(
  context : {
    def ontology : Ontology
    def language : Language
    def configFile : Config
  }
)
extends WikiPageExtractor
{
  protected val logger = Logger.getLogger(classOf[AbstractExtractor].getName)
  this.getClass.getClassLoader.getResource("myproperties.properties")


  /** protected params ... */

  protected val language = context.language.wikiCode

    //private val apiParametersFormat = "uselang="+language+"&format=xml&action=parse&prop=text&title=%s&text=%s"
  protected val apiParametersFormat = context.configFile.abstractParameters.abstractQuery

    // lazy so testing does not need ontology
  protected lazy val shortProperty = context.ontology.properties(context.configFile.abstractParameters.shortAbstractsProperty)

    // lazy so testing does not need ontology
  protected lazy val longProperty = context.ontology.properties(context.configFile.abstractParameters.longAbstractsProperty)

  protected lazy val longQuad = QuadBuilder(context.language, DBpediaDatasets.LongAbstracts, longProperty, null) _
  protected lazy val shortQuad = QuadBuilder(context.language, DBpediaDatasets.ShortAbstracts, shortProperty, null) _

  override val datasets = Set(DBpediaDatasets.LongAbstracts, DBpediaDatasets.ShortAbstracts)

  private val mwConnector = new MediaWikiConnector(context.configFile.mediawikiConnection, context.configFile.abstractParameters.abstractTags.split(","))


    override def extract(pageNode : WikiPage, subjectUri: String): Seq[Quad] =
    {
        //Only extract abstracts for pages from the Main namespace
        if(pageNode.title.namespace != Namespace.Main)
          return Seq.empty

        //Don't extract abstracts from redirect and disambiguation pages
        if(pageNode.isRedirect || pageNode.isDisambiguation)
          return Seq.empty

        //Reproduce wiki text for abstract
        //val abstractWikiText = getAbstractWikiText(pageNode)
        // if(abstractWikiText == "") return Seq.empty

        //Retrieve page text
        val text = mwConnector.retrievePage(pageNode.title, apiParametersFormat) match{
          case Some(t) => AbstractExtractor.postProcessExtractedHtml(pageNode.title, replacePatterns(t))
          case None => return Seq.empty
        }

        //Create a short version of the abstract
        val shortText = short(text)

        //Create statements
        val quadLong = longQuad(pageNode.uri, text, pageNode.sourceIri)
        val quadShort = shortQuad(pageNode.uri, shortText, pageNode.sourceIri)

        if (shortText.isEmpty)
        {
            Seq(quadLong)
        }
        else
        {
            Seq(quadLong, quadShort)
        }
    }

    /**
     * Returns the first sentences of the given text that have less than 500 characters.
     * A sentence ends with a dot followed by whitespace.
     * TODO: probably doesn't work for most non-European languages.
     * TODO: analyse ActiveAbstractExtractor, I think this works  quite well there,
     * because it takes the first two or three sentences
      *
      * @param text
     * @param max max length
     * @return result string
     */
    def short(text : String, max : Int = 500) : String =
    {
        if (text.size < max) return text

        val builder = new StringBuilder()
        var size = 0

        for(sentence <- text.split("""(?<=\.\s)"""))
        {
            if(size + sentence.size > max)
            {
                if (builder.isEmpty)
                {
                    return sentence
                }
                return builder.toString().trim
            }

            size += sentence.size
            builder.append(sentence)
        }

        builder.toString().trim
    }


    private def replacePatterns(abst: String): String= {
      var ret = abst
      for ((regex, replacement) <- AbstractExtractor.patternsToRemove) {
        val matches = regex.pattern.matcher(ret)
        if (matches.find()) {
          ret = matches.replaceAll(replacement)
        }
      }
      ret
    }


    //private val destinationNamespacesToRender = List(Namespace.Main, Namespace.Template)

    /*
    private def renderNode(node : Node) = node match
    {
        case InternalLinkNode(destination, _, _, _) => destinationNamespacesToRender contains destination.namespace
        case ParserFunctionNode(_, _, _) => false
        case _ => true
    }
    */


    /**
     * Get the wiki text that contains the abstract text.
     */
    /*
    def getAbstractWikiText(pageNode : PageNode) : String =
    {
        // From first TextNode
        val start = pageNode.children.indexWhere{
            case TextNode(text, _) => text.trim != ""
            case InternalLinkNode(destination, _, _, _) => destination.namespace == Namespace.Main
            case _ => false
        }

        // To first SectionNode (exclusive)
        var end = pageNode.children.indexWhere{
            case sectionNode : SectionNode => true
            case _ => false
        }

        // If there is no SectionNode, To last non-empty TextNode (inclusive)
        if(end == -1)
        {
            val reverseLastTextIndex = pageNode.children.reverse.indexWhere{
                case TextNode(text, _) => text.trim != ""
                case _ => false
            }
            if(reverseLastTextIndex != -1)
            {
                end = pageNode.children.length - reverseLastTextIndex
            }
        }

        // No result if there is no TextNode or no text before a SectionNode
        if(start == -1 || end == -1 || start >= end)
        {
            return ""
        }

        // Re-generate wiki text for found range of nodes
        val text = pageNode.children.slice(start, end)
                .filter(renderNode)
                .map(_.toWikiText)
                .mkString("").trim
        
        // decode HTML entities - the result is plain text
        decodeHtml(text)
    }
    */

}

object AbstractExtractor {

  //TODO check if this function is still relevant
  def postProcessExtractedHtml(pageTitle: WikiTitle, text: String): String =
  {
    val startsWithLowercase =
      if (text.isEmpty) {
        false
      } else {
        val firstLetter = text.substring(0,1)
        firstLetter != firstLetter.toUpperCase(pageTitle.language.locale)
      }

    //HACK
    if (startsWithLowercase)
    {
      val decodedTitle = pageTitle.decoded.replaceFirst(" \\(.+\\)$", "")

      if (! text.toLowerCase.contains(decodedTitle.toLowerCase))
      {
        // happens mainly for Japanese names (abstract starts with template)
        return decodedTitle + " " + text
      }
    }

    text
  }

  val patternsToRemove = List(
    """<div style=[^/]*/>""".r -> " ",
    """</div>""".r -> " "
  )
}
