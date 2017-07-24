package org.dbpedia.extraction.mappings.rml.load

import be.ugent.mmlab.rml.model.RMLMapping
import org.dbpedia.extraction.util.Language

/**
  * Created by wmaroy on 30.06.17.
  */
object RMLProcessorLoader {

  /**
    * Loads all mappings for one specific language.
    * @param language
    * @return
    */
  def load(language :Language, pathToRMLMappingsDir : String) : Map[String, RMLMapping] = {

    val pathToLanguageDir = getPathToLanguageDir(language, pathToRMLMappingsDir)
    RMLProcessorParser.parseFromDir(pathToLanguageDir)

  }

  /**
    * Creates the path to the mappings dir based on the language and path to all the RML mappings
    * @param language
    * @param pathToRMLMappingsDir
    * @return
    */
  private def getPathToLanguageDir(language: Language, pathToRMLMappingsDir : String) : String = {
    pathToRMLMappingsDir + "/" + language.isoCode
  }

}
