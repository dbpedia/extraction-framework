package org.dbpedia.extraction.server

import org.dbpedia.extraction.config.ConfigUtils._
import java.io.File

import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.mappings.Extractor
import org.dbpedia.extraction.util.{ExtractorUtils, Language}

/**
 * User: Dimitris Kontokostas
 * server config
 */
class ServerConfiguration(configPath: String) extends Config(configPath) {

  val mappingsUrl: String = getString(this, "mappingsUrl", required = true)

  val localServerUrl: String = getString(this, "localServerUrl", required = true)

  val serverPassword: String = getString(this, "serverPassword", required = true)
  val statisticsDir: File = getValue(this, "statisticsDir", required = true)(new File(_))

  val mappingTestExtractorClasses: Seq[Class[_ <: Extractor[_]]] = ExtractorUtils.loadExtractorClassSeq(getStrings(this, "mappingsTestExtractors", ","))
  val customTestExtractorClasses: Map[Language, Seq[Class[_ <: Extractor[_]]]] = ExtractorUtils.loadExtractorsMapFromConfig(languages, this)

}
