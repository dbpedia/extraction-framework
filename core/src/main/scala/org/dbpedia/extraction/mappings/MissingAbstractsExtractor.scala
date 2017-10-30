package org.dbpedia.extraction.mappings

import java.io._
import java.net.URL
import java.util.logging.{Level, Logger}

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.util.text.ParseExceptionIgnorer
import org.dbpedia.util.text.html.{HtmlCoder, XmlCodes}

import scala.collection.mutable
import scala.io.Source
import scala.language.reflectiveCalls
import scala.xml.XML

/**
 * Extracts page abstracts which are not yet extracted. For each page which is a candidate for extraction
 *
 * From now on we use MobileFrontend for MW <2.21 and TextExtracts for MW > 2.22
 * The patched mw instance is no longer needed except from minor customizations in LocalSettings.php
 * TODO: we need to adapt the TextExtracts extension to accept custom wikicode syntax.
 * TextExtracts now uses the article entry and extracts the abstract. The retional for
 * the new extension is that we will not need to load all articles in MySQL, just the templates
 * At the moment, setting up the patched MW takes longer than the loading of all articles in MySQL :)
 * so, even this way it's way better and cleaner ;)
 * We leave the old code commented since we might re-use it soon
 */

@SoftwareAgentAnnotation(classOf[MissingAbstractsExtractor], AnnotationType.Extractor)
class MissingAbstractsExtractor(
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
    //TODO make this configurable
    protected def apiUrl: String = "http://localhost/mediawiki/api.php"

    private val maxRetries = 3

    /** timeout for connection to web server, milliseconds */
    private val connectMs = 2000

    /** timeout for result from web server, milliseconds */
    private val readMs = 8000

    /** sleep between retries, milliseconds, multiplied by CPU load */
    private val sleepFactorMs = 4000

    private val language = context.language.wikiCode

    private val logger = Logger.getLogger(classOf[AbstractExtractor].getName)

    //private val apiParametersFormat = "uselang="+language+"&format=xml&action=parse&prop=text&title=%s&text=%s"
    private val apiParametersFormat = "uselang="+language+"&format=xml&action=query&prop=extracts&exintro=&explaintext=&titles=%s"

    // lazy so testing does not need ontology
    private lazy val shortProperty = context.ontology.properties("rdfs:comment")

    // lazy so testing does not need ontology
    private lazy val longProperty = context.ontology.properties("abstract")

    private lazy val longQuad = QuadBuilder(context.language, DBpediaDatasets.MissingLongAbstracts, longProperty, null) _
    private lazy val shortQuad = QuadBuilder(context.language, DBpediaDatasets.MissingShortAbstracts, shortProperty, null) _

    override val datasets = Set(DBpediaDatasets.MissingLongAbstracts, DBpediaDatasets.MissingShortAbstracts)

    private val osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean()

    private val availableProcessors = osBean.getAvailableProcessors()

    override def extract(pageNode : PageNode, subjectUri : String): Seq[Quad] =
    {
      // only run extraction if subjectUri is not in list of extracted data
        if (MissingAbstractsExtractor.existingAbstracts(subjectUri)) {
          Seq.empty
        }
        else {
          //Only extract abstracts for pages from the Main namespace
          if (pageNode.title.namespace != Namespace.Main) {
            return Seq.empty
          }

          //Don't extract abstracts from redirect and disambiguation pages
          if (pageNode.isRedirect || pageNode.isDisambiguation) {
            return Seq.empty
          }

          println(s"Detected missing abstract for '$subjectUri'")

          //Reproduce wiki text for abstract
          //val abstractWikiText = getAbstractWikiText(pageNode)
          // if(abstractWikiText == "") return Seq.empty

          //Retrieve page text
          var text = retrievePage(pageNode.title /*, abstractWikiText*/)

          text = postProcess(pageNode.title, text)

          if (text.trim.isEmpty) {
            logger.info(s"Empty abstract for subject $subjectUri")
            return Seq.empty
          }

          //Create a short version of the abstract
          val shortText = short(text)

          //Create statements
          val quadLong = longQuad(subjectUri, text, pageNode.sourceIri)
          val quadShort = shortQuad(subjectUri, shortText, pageNode.sourceIri)

          if (shortText.isEmpty) {
            Seq(quadLong)
          }
          else {
            Seq(quadLong, quadShort)
          }
        }
    }


    /**
     * Retrieves a Wikipedia page.
     *
     * @param pageTitle The encoded title of the page
     * @return The page as an Option
     */
    def retrievePage(pageTitle : WikiTitle/*, pageWikiText : String*/) : String =
    {
      // The encoded title may contain some URI-escaped characters (e.g. "5%25-Klausel"),
      // so we can't use URLEncoder.encode(). But "&" is not escaped, so we do this here.
      // TODO: there may be other characters that need to be escaped.
      var titleParam = pageTitle.encodedWithNamespace
      List(
        (";", "%3B"),
        ("/", "%2F"),
        ("?", "%3F"),
        (":", "%3A"),
        ("@", "%40"),
        ("&", "%26"),
        ("=", "%3D"),
        ("+", "%2B"),
        (",", "%2C"),
        ("$", "%24")
      ) foreach { case (search, replacement) =>
        titleParam = titleParam.replace(search, replacement);
      }


      // Fill parameters
      val parameters = apiParametersFormat.format(titleParam/*, URLEncoder.encode(pageWikiText, "UTF-8")*/)

      val url = new URL(apiUrl)

      for(counter <- 1 to maxRetries)
      {
        try
        {
          // Send data
          val conn = url.openConnection
          conn.setDoOutput(true)
          conn.setConnectTimeout(connectMs)
          conn.setReadTimeout(readMs)
          val writer = new OutputStreamWriter(conn.getOutputStream)
          writer.write(parameters)
          writer.flush()
          writer.close()

          // Read answer
          return readInAbstract(conn.getInputStream)
        }
        catch
        {
          case ex: Exception => {

            // The web server may still be trying to render the page. If we send new requests
            // at once, there will be more and more tasks running in the web server and the
            // system eventually becomes overloaded. So we wait a moment. The higher the load,
            // the longer we wait.

            var loadFactor = Double.NaN
            var sleepMs = sleepFactorMs

            // if the load average is not available, a negative value is returned
            val load = osBean.getSystemLoadAverage()
            if (load >= 0) {
              loadFactor = load / availableProcessors
              sleepMs = (loadFactor * sleepFactorMs).toInt
            }

            if (counter < maxRetries) {
              logger.log(Level.INFO, "Error retrieving abstract of " + pageTitle + ". Retrying after " + sleepMs + " ms. Load factor: " + loadFactor, ex)
              Thread.sleep(sleepMs)
            }
            else {
              ex match {
                case e : java.net.SocketTimeoutException => logger.log(Level.INFO,
                  "Timeout error retrieving abstract of " + pageTitle + " in " + counter + " tries. Giving up. Load factor: " +
                    loadFactor, ex)
                case _ => logger.log(Level.INFO,
                  "Error retrieving abstract of " + pageTitle + " in " + counter + " tries. Giving up. Load factor: " +
                    loadFactor, ex)
              }
            }
          }
        }

      }

      throw new Exception("Could not retrieve abstract for page: " + pageTitle)
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

    /**
     * Get the parsed and cleaned abstract text from the MediaWiki instance input stream.
     * It returns
     * <api> <query> <pages> <page> <extract> ABSTRACT_TEXT <extract> <page> <pages> <query> <api>
     *  ///  <api> <parse> <text> ABSTRACT_TEXT </text> </parse> </api>
     */
    private def readInAbstract(inputStream : InputStream) : String =
    {
      // for XML format
      val xmlAnswer = Source.fromInputStream(inputStream, "UTF-8").getLines().mkString("")
      //val text = (XML.loadString(xmlAnswer) \ "parse" \ "text").text.trim
      val text = (XML.loadString(xmlAnswer) \ "query" \ "pages" \ "page" \ "extract").text.trim
      decodeHtml(text)
    }

    private def postProcess(pageTitle: WikiTitle, text: String): String =
    {
      val startsWithLowercase =
      if (text.isEmpty) {
        false
      } else {
        val firstLetter = text.substring(0,1)
        firstLetter != firstLetter.toUpperCase(context.language.locale)
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

    def decodeHtml(text: String): String = {
      val coder = new HtmlCoder(XmlCodes.NONE)
      coder.setErrorHandler(ParseExceptionIgnorer.INSTANCE)
      coder.code(text)
    }

}

object MissingAbstractsExtractor {
  private val logger = Logger.getLogger(classOf[MissingAbstractsExtractor].getName)

  /**
   * List of all characters which are reserved in a query component according to RFC 2396
   * with their escape sequences as determined by the JavaScript function encodeURIComponent.
   */
  val CHARACTERS_TO_ESCAPE = List(
    (";", "%3B"),
    ("/", "%2F"),
    ("?", "%3F"),
    (":", "%3A"),
    ("@", "%40"),
    ("&", "%26"),
    ("=", "%3D"),
    ("+", "%2B"),
    (",", "%2C"),
    ("$", "%24")
  )

  lazy val existingAbstracts = {
    val file = new File("existing-abstracts.tsv")
    logger.info(s"Starting to read list of existing abstracts from file '${file.getAbsolutePath}'")

    val reader = try {
      new BufferedReader(new FileReader("existing-abstracts.tsv"))
    }
    catch {
      case e: FileNotFoundException => logger.severe(s"Unable to find file '${file.getAbsolutePath}'." +
        s"Please generate it and put it in the given location.")
        throw e
      case e : Throwable => throw e
    }

    val set: mutable.HashSet[String] = mutable.HashSet()

    var line: String = null
    var first = true
    while ( {
      line = reader.readLine(); line != null
    }) {
      if (first) {
        first = false
      }
      else {
        val parts = line.split("\t")
        set.add(parts(1))
      }
    }

    reader.close()

    logger.info(s"Done reading existing abstract names: ${set.size} abstracts already existing")
    set
  }
}
