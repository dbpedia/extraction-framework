package org.dbpedia.extraction.util

import org.dbpedia.extraction.config.dataparser.InfoboxMappingsExtractorConfig._
import org.dbpedia.extraction.wikiparser.PageNode

/**
  * Created by aditya on 6/21/16.
  */
object InfoboxMappingsUtils {
    def extract_property(str : String, typeOfStr : String) : String = {
      var answer = ""
      if ( typeOfStr == "#property") {
        val pattern = """\{\{#property:([0-9A-Za-z]+)\}\}""".r
        answer = str match {
          case pattern(group) => group
          case _ => ""
        }
      } else if (typeOfStr == "#invoke") {
        val list_words = str.split("""\|""")
        if (!(list_words(0) == "Wikidata" || list_words(0) == "PropertyLink"))
          return answer
        val properties = list_words.filter(s => (s.charAt(0) == 'p' || s.charAt(0) == 'P') && isNumber(s.substring(1)))
        for ( p <- properties){
          answer = answer + "/" + p
        }

        if (answer.size > 0)
          answer = answer.substring(1)

      }
      answer
    }

  def isNumber(s : String): Boolean = {s.matches("\\d+")}

  def getAllPropertiesInInfobox(page : PageNode, lang : Language) : scala.collection.mutable.Set[(String, String)] = {
    val templateNodes = ExtractorUtils.collectTemplatesFromNodeTransitive(page)
    val infoboxes = templateNodes.filter(p => p.title.toString().contains(infoboxNameMap.get(lang.wikiCode).getOrElse("Infobox")))
    var answer = scala.collection.mutable.Set[(String, String)]()

    infoboxes.foreach( infobox => {
      val reg = """((p|P)([0-9]+)\})|((p|P)([0-9]+)\|)""".r
      for (m <- reg.findAllIn(page.toWikiText)) {
        answer = answer + new Tuple2(infobox.title.decoded, m.substring(0,m.length -1))
      }

    })

    answer
  }


}
