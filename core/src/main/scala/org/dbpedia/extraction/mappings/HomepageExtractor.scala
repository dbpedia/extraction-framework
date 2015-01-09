package org.dbpedia.extraction.mappings

import java.net.{MalformedURLException, URI, URISyntaxException}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.config.mappings.HomepageExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, UriUtils}
import scala.language.reflectiveCalls

/**
 * Extracts links to the official homepage of an instance.
 */
class HomepageExtractor(
  context : {
    def ontology : Ontology
    def language : Language
    def redirects : Redirects
  }
)
extends PageNodeExtractor
{
  private val language = context.language.wikiCode

  private val propertyNames = HomepageExtractorConfig.propertyNames(language)
  
  private val official = HomepageExtractorConfig.official(language)
  
  private val externalLinkSections = HomepageExtractorConfig.externalLinkSections(language)

  private val templateOfficialWebsite = HomepageExtractorConfig.templateOfficialWebsite(language)

  private val homepageProperty = context.ontology.properties("foaf:homepage")

  private val listItemStartRegex = ("""(?msiu).*^\s*\*\s*[^^]*(\b""" + official + """\b)?[^^]*\z""").r
  private val officialRegex = ("(?iu)" + official).r
  private val officialAndLineEndRegex = ("""(?msiu)[^$]*\b""" + official + """\b.*$.*""").r
  private val officialAndNoLineEndRegex = ("""(?msiu)[^$]*\b""" + official + """\b[^$]*""").r
  private val lineEndRegex = "(?ms).*$.+".r
  // Similar to org.dbpedia.extraction.config.dataparser.DataParserConfig.splitPropertyNodeRegexLink - without '/' and ';'
  private val splitPropertyNodeLinkStrict = """<br\s*\/?>|\n| and | or |,| """

  override val datasets = Set(DBpediaDatasets.Homepages)

  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main) return Seq.empty

    val list = collectProperties(page).filter(p => propertyNames.contains(p.key.toLowerCase)).flatMap {
      NodeUtil.splitPropertyNode(_, splitPropertyNodeLinkStrict, true)
    }

    list.foreach((property) =>

      // Find among children
      for (child <- property.children) {
        child match
        {
          case (textNode @ TextNode(text, _)) =>
          {
            val cleaned = cleanProperty(text)
            if (cleaned.nonEmpty) { // do not proceed if the property value is not a valid candidate
              val url = if (UriUtils.hasKnownScheme(cleaned)) cleaned else "http://" + cleaned
              val graph = generateStatement(subjectUri, pageContext, url, textNode)
              if (!graph.isEmpty)
              {
                return graph
              }
            }
          }
          case (linkNode @ ExternalLinkNode(destination, _, _, _)) =>
          {
            val graph = generateStatement(subjectUri, pageContext, destination.toString, linkNode)
            if (!graph.isEmpty)
            {
              return graph
            }
          }
          case _ =>
        }
      }
    )

    for(externalLinkSectionChildren <- collectExternalLinkSection(page.children))
    {
      for((url, sourceNode) <- findLinkTemplateInSection(externalLinkSectionChildren))
      {
        val graph = generateStatement(subjectUri, pageContext, url, sourceNode)
        if (!graph.isEmpty) return graph
      }
      for((url, sourceNode) <- findLinkInSection(externalLinkSectionChildren))
      {
        val graph = generateStatement(subjectUri, pageContext, url, sourceNode)
        if (!graph.isEmpty) return graph
      }
    }

    Seq.empty
  }

  private def cleanProperty(text: String) : String = {

    val candidateUrl = text.stripLineEnd.trim // remove ending new line

    // While it is perfectly legal to have hostnames without dots in URLs
    // it is very unlikely that such URLs will be present in Wikipedia
    // Most of the times such values represent texts inserted by editors
    // to convey a "missing homepage" info, such as None, N/A, missing, down etc.
    if (candidateUrl.matches(""".*\w\.\w.*""")) candidateUrl
    else ""
  }

  private def generateStatement(subjectUri: String, pageContext: PageContext, url: String, node: Node): Seq[Quad] =
  {
    try
    {
      for(link <- UriUtils.cleanLink(new URI(url)))
      {
        return Seq(new Quad(context.language, DBpediaDatasets.Homepages, subjectUri, homepageProperty, link, node.sourceUri))
      }
    }
    catch
    {
      case _ : URISyntaxException => // TODO: log
    }
    
    Seq.empty
  }

  private def extractUrlFromProperty(node: PropertyNode): Option[String] = {

    /*
    It could be:
    1) {{template | key = example.com }}
    2) {{template | key = http://example.com }}

    In 1) => PropertyNode("key", List(TextNode("example.com", _))
    In 2) => PropertyNode("key", List(ExternalLinkNode(URI("http://example.com"), ...)))
     */
    val url = node.children.collect {
      case TextNode(t, _) => t
      case ExternalLinkNode(destination, _, _, _) => destination.toString
    }.mkString.trim

    if (url.isEmpty) {
      None
    } else {
      try {
        // UriUtils.encode fails if not scheme is provided
        val urlWithScheme = if (UriUtils.hasKnownScheme(url)) url else ("http://" + url)
        Some(new URI(urlWithScheme).toString)
      } catch {
        case _ : Exception => None
      }
    }
  }

  private def findLinkTemplateInSection(nodes: List[Node]): Option[(String, Node)] =
  {
    // TODO: use for-loop instead of recursion
    nodes match
    {
      case (templateNode @ TemplateNode(title, _, _, _)) :: tail =>
      {
        val templateRedirect = context.redirects.resolve(title).decoded
        if (templateOfficialWebsite.contains(templateRedirect)) {
          templateNode.property(templateOfficialWebsite(templateRedirect)) match
          {
            case Some(propertyNode) => extractUrlFromProperty(propertyNode).map(url => (url, propertyNode))
            case None => findLinkTemplateInSection(tail) // do not stop the recursion - there might be other templates
          }
        }
        else findLinkTemplateInSection(tail)
      }
      case head :: tail => findLinkTemplateInSection(tail)
      case Nil => None
    }
  }

  private def findLinkInSection(nodes: List[Node]): Option[(String, Node)] =
  {
    // TODO: use for-loop instead of recursion
    nodes match
    {
      case TextNode(listItemStartRegex(officialMatch), _) :: tail =>
      {
        findExternalLinkNodeInLine(tail, officialMatch != null) match
        {
          case Some(linkAndNode) => Some(linkAndNode)
          case _ => findLinkInSection(tail)
        }
      }
      case head :: tail => findLinkInSection(tail)
      case _ => None
    }
  }

  private def findExternalLinkNodeInLine(nodes: List[Node], officialMatch: Boolean, link: String = null): Option[(String, Node)] =
  {
    // TODO: use for-loop instead of recursion
    nodes match
    {
      case ExternalLinkNode(destination, TextNode(label, _) :: Nil, _, _) :: tail =>
      {
        if (officialRegex.findFirstIn(label).isDefined)
        {
          Some((destination.toString, nodes.head))
        }
        else
        {
          findExternalLinkNodeInLine(tail, false, destination.toString)
        }
      }
      case TextNode(officialAndLineEndRegex(), _) :: tail =>
      {
        if (link != null)
        {
          Some((link, nodes.head))
        }
        else
        {
          findExternalLinkNodeInLine(tail, true)
        }
      }
      case TextNode(officialAndNoLineEndRegex(), _) :: tail =>
      {
        if (link != null)
        {
          Some((link, nodes.head))
        }
        else
        {
          findExternalLinkNodeInLine(tail, true)
        }
      }
      case TextNode(lineEndRegex, _) :: _ => None
      case head :: tail => findExternalLinkNodeInLine(tail, officialMatch, link)
      case _ => None
    }
  }

  private def collectExternalLinkSection(nodes: List[Node]): Option[List[Node]] =
  {
    nodes match
    {
      case SectionNode(name, level, _, _) :: tail if name.matches(externalLinkSections) => Some(collectSectionChildNodes(tail, level))
      case _ :: tail => collectExternalLinkSection(tail)
      case Nil => None
    }
  }

  private def collectSectionChildNodes(nodes : List[Node], sectionLevel : Int) : List[Node] =
  {
    nodes match
    {
      case SectionNode(name, level, _, _) :: tail if (level <= sectionLevel) => Nil
      case head :: tail => head :: collectSectionChildNodes(tail, sectionLevel)
      case Nil => Nil
    }
  }

  private def collectProperties(node : Node) : List[PropertyNode] =
  {
    node match
    {
      case propertyNode : PropertyNode => List(propertyNode)
      case _ => node.children.flatMap(collectProperties)
    }
  }
}
