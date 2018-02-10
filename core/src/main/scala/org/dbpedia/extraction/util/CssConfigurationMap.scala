package org.dbpedia.extraction.util

import java.net.URL
import scala.collection.convert.decorateAsScala._

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode

/**
  * Created by Chile on 1/30/2017.
  */
class CssConfigurationMap(url: URL) extends JsonConfig(url){

  def getCssSelectors(lang: Language): CssLanguageConfiguration ={
    var default = this.getMap("default")
    val langMap = if(this.keys().toList.contains(lang.wikiCode))
      this.getMap(lang.wikiCode)
    else
      Map[String, JsonNode]()

    for(dv <- default; kv <- langMap){
      if(dv._1 == kv._1)
        default += (kv._1 -> kv._2.asInstanceOf[ArrayNode].addAll(dv._2.asInstanceOf[ArrayNode]))
    }

    new CssLanguageConfiguration(
      findPageEnd = default("nif-find-pageend").asInstanceOf[ArrayNode].iterator().asScala.map(x => x.asText()).toList,
      nextTitle = default("nif-find-next-title").asInstanceOf[ArrayNode].iterator().asScala.map(x => x.asText()).toList,
      findToc = default("nif-find-toc").asInstanceOf[ArrayNode].iterator().asScala.map(x => x.asText()).toList,
      removeElements = default("nif-remove-elements").asInstanceOf[ArrayNode].iterator().asScala.map(x => x.asText()).toList,
      replaceElements = default("nif-replace-elements").asInstanceOf[ArrayNode].iterator().asScala.map(x => x.asText()).toList,
      noteElements = default("nif-note-elements").asInstanceOf[ArrayNode].iterator().asScala.map(x => x.asText()).toList
    )
  }

  class CssLanguageConfiguration(
    val findPageEnd: List[String],
    val nextTitle: List[String],
    val findToc: List[String],
    val removeElements: List[String],
    val replaceElements: List[String],
    val noteElements: List[String]
  )
}
