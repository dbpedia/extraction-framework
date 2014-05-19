package org.dbpedia.extraction.server

import java.util.Properties
import org.dbpedia.extraction.util.ConfigUtils._
import java.io.File
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.extraction.util.{WikiInfo, Language}
import scala.collection.{SortedMap, Map}
import org.dbpedia.extraction.mappings.Extractor
import scala.collection.mutable.HashMap
import scala.util.matching.Regex
import scala.io.Codec

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

  val mappingTestExtractorClasses = "";
  val allExtractorClasses = "";

}
