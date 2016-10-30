package org.dbpedia.extraction.scripts

import java.io.File

import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.destinations.WriterDestination
import org.dbpedia.extraction.mappings.NifAbstractExtractor
import org.dbpedia.extraction.util.{IOUtils, RichFile, Language}
import org.dbpedia.extraction.wikiparser.{Namespace, WikiTitle}
import org.scalatest.FunSuite

/**
  * Created by Chile on 10/17/2016.
  * Test for the NifAbstractExtractor's extractNif function, returning all Nif triples generated.
  * Perquisites:
  *   1. mediawiki with a fully imported language edition
  *   2. edit the path for th outFile below
  *   3. enter all Wikipdia/DBpedia titles of interest in the titles list below
  *   4. configure the //extraction-framework/core/src/main/resources/mediawikiconfig.json file:
  *    - change the apiUri to the mediawiki endpoint
  *    - configure the rest of the publicParams to your liking
  *    - switch isTestRun to true (no ontology is loaded, short/long-abstracts datasets are skipped)
  *    - for debugging switch writeNifStrings to true so that every nif instance has a string representation (turn this to false in the extraction!!!)
  */
class NifAbstractExtractorTest extends FunSuite {

  private val context = new {
    def ontology = throw new IllegalStateException("don't need Ontology for testing!!! don't call extract!")
    def language = Language.map.get("en").get
  }
  private val extractor = new NifAbstractExtractor(context)
  private val outFile = new RichFile(new File("/home/extractor/2016-04-extraction/nif-abstracts.ttl"))
  private val dest = new WriterDestination(() => IOUtils.writer(outFile), new TerseFormatter(false,true))
  private val titles = List("Biological_warfare", "Bahá'í_Faith")

  test("testExtractNif") {
    dest.open()
    for(title <- titles)
      dest.write(extractor.extractNif("https://wikipedia.org/wiki/" + title, "http://dbpedia.org/resource/" + title, getHtml(title)))
    dest.close()
  }

  private def getHtml(title:String): String={
    val wt = new WikiTitle(title, Namespace.Main, context.language)
    val html = extractor.postProcess(wt, extractor.retrievePage(wt))
    html
  }
}
