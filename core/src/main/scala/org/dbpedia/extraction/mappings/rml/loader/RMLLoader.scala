package org.dbpedia.extraction.mappings.rml.loader

import org.dbpedia.extraction.mappings.rml.translation.model.RMLMapping
import org.dbpedia.extraction.util.Language

/**
  * Created by wmaroy on 30.06.17.
  */
object RMLLoader {

  /**
    * Loads all mappings for one specific language.
    * @param language
    * @return
    */
  def load(language :Language) : Map[String, RMLMapping] = {
    Map()
  }

}
