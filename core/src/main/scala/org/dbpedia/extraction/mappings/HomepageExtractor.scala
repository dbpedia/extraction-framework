package org.dbpedia.extraction.mappings

import java.net.URI
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad}
import org.dbpedia.extraction.config.mappings.HomepageExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, UriUtils}

/**
 * Extracts links to the official homepage of an instance.
 */
class HomepageExtractor( context : {
                             def ontology : Ontology
                             def language : Language
                             def redirects : Redirects } ) extends Extractor
{
    private val language = context.language.wikiCode

    require(HomepageExtractorConfig.supportedLanguages.contains(language),"Homepage Extractor supports the following languages: " + HomepageExtractorConfig.supportedLanguages.mkString(", ")+"; not "+language)

    private val propertyNames = HomepageExtractorConfig.propertyNamesMap.getOrElse(language, HomepageExtractorConfig.propertyNamesMap("en"))
    private val official = HomepageExtractorConfig.officialMap.getOrElse(language, HomepageExtractorConfig.officialMap("en"))
    private val externalLinkSections = HomepageExtractorConfig.externalLinkSectionsMap.getOrElse(language, HomepageExtractorConfig.externalLinkSectionsMap("en"))


    private val homepageProperty = context.ontology.getProperty("foaf:homepage").get

    private val listItemStartRegex = ("""(?msiu).*^\s*\*\s*[^^]*(\b""" + official + """\b)?[^^]*\z""").r
    private val officialRegex = ("(?iu)" + official).r
    private val officialAndLineEndRegex = ("""(?msiu)[^$]*\b""" + official + """\b.*$.*""").r
    private val officialAndNoLineEndRegex = ("""(?msiu)[^$]*\b""" + official + """\b[^$]*""").r

    private val lineEndRegex = "(?ms).*$.+".r

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != Namespace.Main) return new Graph()
        
        val list = collectProperties(node).filter(p => propertyNames.contains(p.key.toLowerCase))
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

        for(externalLinkSectionChildren <- collectExternalLinkSection(node.children))
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

        new Graph()
    }

    private def generateStatement(subjectUri : String, pageContext : PageContext, url : String, node: Node) : Graph =
    {
        try
        {
            for(link <- UriUtils.cleanLink(URI.create(url)))
            {
                return new Graph(new Quad(context.language, DBpediaDatasets.Homepages, subjectUri, homepageProperty, link, node.sourceUri) :: Nil)
            }
        }
        catch
        {
            case ex: IllegalArgumentException =>
        }
        new Graph()
    }

    private def findLinkTemplateInSection(nodes : List[Node]) : Option[(String, Node)] =
    {
        nodes match
        {
            case (templateNode @ TemplateNode(title, _, _)) :: _
                if ((title.encoded == "Official") || ((context.redirects.map.contains(title.decoded)) && (context.redirects.map(title.decoded) == "Official"))) =>
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

    private def findLinkInSection(nodes : List[Node]) : Option[(String, Node)] =
    {
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

    private def findExternalLinkNodeInLine(nodes : List[Node], officialMatch : Boolean, link : String = null) : Option[(String, Node)] =
    {
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

    private def collectExternalLinkSection(nodes : List[Node]) : Option[List[Node]] =
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
