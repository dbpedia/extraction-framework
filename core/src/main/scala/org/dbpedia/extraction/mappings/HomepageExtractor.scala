package org.dbpedia.extraction.mappings

import java.net.{URI,URISyntaxException}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.config.mappings.HomepageExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, UriUtils}

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
extends Extractor
{
  private val language = context.language.wikiCode

  private val propertyNames = HomepageExtractorConfig.propertyNamesMap(language)
  
  private val official = HomepageExtractorConfig.officialMap(language)
  
  private val externalLinkSections = HomepageExtractorConfig.externalLinkSectionsMap(language)

  private val homepageProperty = context.ontology.properties("foaf:homepage")

  private val listItemStartRegex = ("""(?msiu).*^\s*\*\s*[^^]*(\b""" + official + """\b)?[^^]*\z""").r
  private val officialRegex = ("(?iu)" + official).r
  private val officialAndLineEndRegex = ("""(?msiu)[^$]*\b""" + official + """\b.*$.*""").r
  private val officialAndNoLineEndRegex = ("""(?msiu)[^$]*\b""" + official + """\b[^$]*""").r
  private val lineEndRegex = "(?ms).*$.+".r

  override val datasets = Set(DBpediaDatasets.Homepages)

  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main) return Seq.empty
    
    val list = collectProperties(page).filter(p => propertyNames.contains(p.key.toLowerCase))
    list.foreach((property) => {
      property.children match
      {
        case (textNode @ TextNode(text, _)) :: _ =>
        {
          val url = if (!text.startsWith("http")) "http://" + text else text
          val graph = generateStatement(subjectUri, pageContext, url, textNode)
          if (!graph.isEmpty)
          {
            return graph
          }
        }
        case (linkNode @ ExternalLinkNode(destination, _, _, _)) :: _ =>
        {
          val graph = generateStatement(subjectUri, pageContext, destination.toString, linkNode)
          if (!graph.isEmpty)
          {
            return graph
          }
        }
        case _ =>
      }
    })

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
      case ex: URISyntaxException => // TODO: log
    }
    
    Seq.empty
  }

  private def findLinkTemplateInSection(nodes: List[Node]): Option[(String, Node)] =
  {
    // TODO: use for-loop instead of recursion
    nodes match
    {
      // TODO: use language-specific name
      case (templateNode @ TemplateNode(title, _, _)) :: _
          if ((title.decoded == "Official") || ((context.redirects.map.contains(title.decoded)) && (context.redirects.map(title.decoded) == "Official"))) =>
      {
        templateNode.property("1") match
        {
          case Some(propertyNode) => propertyNode.retrieveText.map(url => (url, propertyNode))
          case _ => None
        }
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
