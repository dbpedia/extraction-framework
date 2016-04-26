package org.dbpedia.extraction.server

import java.util.Properties
import org.dbpedia.extraction.util.ConfigUtils._
import java.io.File
import org.dbpedia.extraction.util.ExtractorUtils

/**
 * User: Dimitris Kontokostas
 * confi
 */
private class ServerConfiguration(config: Properties) {

  /** Dump directory */
  val mappingsUrl = getString(config, "mappingsUrl", true)

  val localServerUrl = getString(config, "localServerUrl", true)

  val serverPassword = getString(config, "serverPassword", true)
  val statisticsDir = getValue(config, "statisticsDir", true)(new File(_))
  val ontologyFile = getValue(config, "ontologyFile", false)(new File(_))
  val mappingsDir = getValue(config, "mappingsDir", false)(new File(_))

  val _languages = getString(config, "languages",true)
  val languages = parseLanguages(null,Seq(_languages))

  val mappingTestExtractorClasses = ExtractorUtils.loadExtractorClassSeq(getStrings(config, "mappingsTestExtractors", ',', false))
  val customTestExtractorClasses = ExtractorUtils.loadExtractorsMapFromConfig(languages, config)

}
