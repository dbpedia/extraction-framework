package org.dbpedia.extraction.mappings

import java.io.File

import org.dbpedia.extraction.destinations.WriterDestination
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.nif.WikipediaNifExtractor
import org.dbpedia.extraction.util.{Config, IOUtils, Language, RichFile}
import org.dbpedia.extraction.wikiparser.{Namespace, WikiPage, WikiTitle}
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

class NifExtractorTest extends FunSuite {


  private val context = new {
    def ontology = throw new IllegalStateException("don't need Ontology for testing!!! don't call extract!")
    def language = Language.map.get("en").get
    def configFile = new Config("C:\\Users\\Chile\\IdeaProjects\\extraction-framework-temp\\dump\\extraction.nif.abstracts.properties")
  }
  private val wikipageextractor = new NifExtractor(context)
  private val outFile = new RichFile(new File("C:\\Users\\Chile\\Desktop\\Dbpedia\\nif-abstracts.ttl"))
  private val dest = new WriterDestination(() => IOUtils.writer(outFile), new TerseFormatter(false,true))
  //private val titles = List("Dana_Ferguson", "Doum,_Central_African_Republic", "Robert_A._Baines", "1962_Copa_del_Generalísimo_Final")
  private val titles = List("George_Washington")

  test("testExtractNif") {
    dest.open()
    for(title <- titles){
      val wt = new WikiTitle(title, Namespace.Main, context.language)
      val wp = new WikiPage(wt, null, 3273623938l, 3273623939l, 3273623939l, "")
      val extractor = new WikipediaNifExtractor(context, wp)
      val html = getHtml(wt)
      dest.write(extractor.extractNif(html)(tt => System.err.println(tt.errorMsg)))
    }
    dest.close()
  }

  private def getHtml(title:WikiTitle): String={
    wikipageextractor.retrievePage(title, 0l) match{
      case Some(pc) => wikipageextractor.postProcess(title, pc)
      case None => ""
    }
  }

  override def convertToLegacyEqualizer[T](left: T): LegacyEqualizer[T] = ???

  override def convertToLegacyCheckingEqualizer[T](left: T): LegacyCheckingEqualizer[T] = ???
}

