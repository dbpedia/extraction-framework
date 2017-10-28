package org.dbpedia.extraction.mappings

import java.io.File

import org.dbpedia.extraction.config.{Config, ExtractionRecorder}
import org.dbpedia.extraction.destinations.WriterDestination
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.nif.WikipediaNifExtractor
import org.dbpedia.extraction.util.{MediaWikiConnector, _}
import org.dbpedia.extraction.wikiparser.{Namespace, WikiPage, WikiTitle}
import org.scalatest.FunSuite

import scala.language.reflectiveCalls
import scala.reflect.ClassTag


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
    def language = Language.map("eu")
    def configFile = new Config("C:\\Users\\Chile\\IdeaProjects\\extraction-framework-temp\\dump\\extraction.nif.abstracts.properties")
    def recorder[T: ClassTag] = new ExtractionRecorder[T]
  }
  private val wikipageextractor = new NifExtractor(context)
  private val outFile = new RichFile(new File("C:\\Users\\Chile\\Desktop\\Dbpedia\\nif-abstracts.ttl"))
  private val dest = new WriterDestination(() => IOUtils.writer(outFile), new TerseFormatter(false,true))
  //private val titles = List("Antimon", "Alkalimetalle", "Apostilb", "Doldenblütler")
  private val titles = List("Trancrainville")

  private val mwConnector = new MediaWikiConnector(context.configFile.mediawikiConnection, context.configFile.nifParameters.nifTags.split(","))

  test("testExtractNif") {
    dest.open()
    for(title <- titles){
      val wt = new WikiTitle(title, Namespace.Main, context.language, false, null, true, None)
      val wp = new WikiPage(wt, 4548, 4548, 4548, "")
      val extractor = new WikipediaNifExtractor(context, wp)
      val html = getHtml(wt)
      dest.write(extractor.extractNif(html)(tt => System.err.println(tt.msg)))
    }
    dest.close()
  }

  private def getHtml(title:WikiTitle): String={
    mwConnector.retrievePage(title, context.configFile.nifParameters.nifQuery) match{
      case Some(pc) => AbstractExtractor.postProcessExtractedHtml(title, pc)
      case None => ""
    }
  }

  override def convertToLegacyEqualizer[T](left: T): LegacyEqualizer[T] = ???

  override def convertToLegacyCheckingEqualizer[T](left: T): LegacyCheckingEqualizer[T] = ???
}

