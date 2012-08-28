package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.config.dataparser.FlagTemplateParserConfig
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

/**
 * Handling of flag templates.
 */
class FlagTemplateParser( extractionContext : { def language : Language } ) extends DataParser
{
    override def parse(node : Node) : Option[WikiTitle] =
    {
        node match
        {
            case templateNode : TemplateNode =>
            {
                val templateName = templateNode.title.decoded
                //getCodeMap return en if language code is not configured

                if((templateName equalsIgnoreCase "flagicon")              //{{flagicon|countryname|variant=|size=}}
                        || (templateName equalsIgnoreCase "flag")          //{{flag|countryname|variant=|size=}}
                        || (templateName equalsIgnoreCase "flagcountry"))  //{{flagcountry|countryname|variant=|size=|name=}}  last parameter is alternative name
                {
                    for (countryNameNode <- templateNode.property("1"))
                    {
                        countryNameNode.children.collect{case TextNode(text, _) => text}.headOption match
                        {
                            case Some(countryCode : String) if(templateName.length == 3)&&(templateName == templateName.toUpperCase) =>
                            {
                                val langCodeMap = FlagTemplateParserConfig.getCodeMap(extractionContext.language.wikiCode)
                                langCodeMap.get(countryCode).foreach(countryName => return Some(new WikiTitle(countryName, Namespace.Main, extractionContext.language)))
                            }
                            case Some(countryName : String) => return Some(new WikiTitle(countryName, Namespace.Main, extractionContext.language))
                            case _ =>
                        }
                    }
                }

                //template name is actually country code for flagicon template
                else if((templateName.length == 3) && (templateName == templateName.toUpperCase))
                {
                    val langCodeMap = FlagTemplateParserConfig.getCodeMap(extractionContext.language.wikiCode)
                    langCodeMap.get(templateName).foreach(countryName => return Some(new WikiTitle(countryName, Namespace.Main, extractionContext.language)))
                }

                None
            }
            case _ => None
        }
    }
}