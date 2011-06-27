package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser.{TemplateNode, TextNode, WikiTitle}
import org.dbpedia.extraction.config.dataparser.FlagTemplateParserConfig
import org.dbpedia.extraction.util.Language

/**
 * Handling of flag templates.
 */
//TODO test after re-factor
class FlagTemplateParser( extractionContext : { def language : Language } )
{
    private val langCodeMap = FlagTemplateParserConfig.getCodeMap(extractionContext.language.wikiCode)

    def getDestination(templateNode : TemplateNode) : Option[WikiTitle] =
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
                        langCodeMap.get(countryCode).foreach(countryName => return Some(new WikiTitle(countryName)))
                    }
                    case Some(countryName : String) => return Some(new WikiTitle(countryName))
                    case _ =>
                }
            }
        }

        //template name is actually country code for flagicon template
        else if((templateName.length == 3) && (templateName == templateName.toUpperCase))
        {
            langCodeMap.get(templateName).foreach(countryName => return Some(new WikiTitle(countryName)))
        }

        None
    }
}