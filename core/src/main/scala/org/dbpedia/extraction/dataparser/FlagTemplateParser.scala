package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.dataparser.FlagTemplateParserConfig
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
 * Handling of flag templates.
 */
@SoftwareAgentAnnotation(classOf[FlagTemplateParser], AnnotationType.Parser)
class FlagTemplateParser( extractionContext : { def language : Language } ) extends DataParser[WikiTitle]
{
    private val templates = FlagTemplateParserConfig.templateMap.getOrElse(extractionContext.language.wikiCode, FlagTemplateParserConfig.templateMap("en"))

    private[dataparser] override def parse(node : Node) : Option[ParseResult[WikiTitle]] =
    {
        node match
        {
            case templateNode : TemplateNode =>
            {
                val templateName = templateNode.title.decoded

                if (templates.contains(templateName.toLowerCase))
                {
                    for (countryNameNode <- templateNode.property("1"))
                    {
                        countryNameNode.children.collect{case TextNode(text, _, _) => text}.headOption match
                        {
                            case Some(countryCode : String) if(countryCode.length == 2||countryCode.length == 3)&&(countryCode == countryCode.toUpperCase) =>
                            {
                                //getCodeMap returns en if language code is not configured
                                val langCodeMap = FlagTemplateParserConfig.getCodeMap(extractionContext.language.wikiCode)
                                langCodeMap.get(countryCode).foreach(countryName => return Some(ParseResult(new WikiTitle(countryName, Namespace.Main, extractionContext.language))))
                            }
                            case Some(countryName : String) => return Some(ParseResult(new WikiTitle(countryName, Namespace.Main, extractionContext.language)))
                            case _ =>
                        }
                    }
                }

                //template name is actually country code for flagicon template
                else if ((templateName.length == 2 || templateName.length == 3) && (templateName == templateName.toUpperCase))
                {
                    val langCodeMap = FlagTemplateParserConfig.getCodeMap(extractionContext.language.wikiCode)
                    langCodeMap.get(templateName).foreach(countryName => return Some(ParseResult(new WikiTitle(countryName, Namespace.Main, extractionContext.language))))
                }
                
                else
                {
                    val fullCountryNames = FlagTemplateParserConfig.getFullCountryNames(extractionContext.language.wikiCode)
                    if (fullCountryNames.contains(templateName))
                    {
                        return Some(ParseResult(new WikiTitle(templateName, Namespace.Main, extractionContext.language)))
                    }
                }

                None
            }
            case _ => None
        }
    }
}