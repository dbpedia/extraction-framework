package org.dbpedia.extraction.wikiparser.impl.wikipedia

import java.util.Locale
import org.dbpedia.extraction.wikiparser.{TemplateNode, TextNode, WikiTitle}

/**
 * Handling of flag templates.
 */

object FlagTemplateParser
{
    private val codeMap = Locale.getISOCountries
                          .map(code => new Locale("en", code))
                          .map(locale => (locale.getISO3Country, locale.getDisplayCountry(Locale.US)))
                          .toMap

    def getDestination(templateNode : TemplateNode) : Option[WikiTitle] =
    {
        val templateName = templateNode.title.decoded

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
                        codeMap.get(countryCode).foreach(countryName => return Some(new WikiTitle(countryName)))
                    }
                    case Some(countryName : String) => return Some(new WikiTitle(countryName))
                    case _ =>
                }
            }
        }

        //template name is actually country code for flagicon template
        else if((templateName.length == 3) && (templateName == templateName.toUpperCase))
        {
            codeMap.get(templateName).foreach(countryName => return Some(new WikiTitle(countryName)))
        }

        None
    }

}