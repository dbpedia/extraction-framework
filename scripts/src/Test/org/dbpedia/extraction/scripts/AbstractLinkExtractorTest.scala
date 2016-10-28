package org.dbpedia.extraction.scripts

import java.io.File

import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.destinations.{WriterDestination, Quad}
import org.dbpedia.extraction.mappings.{AbstractExtractor, AbstractLinkExtractor}
import org.dbpedia.extraction.util.{DateFinder, IOUtils, RichFile, Language}
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import org.dbpedia.extraction.wikiparser.{Namespace, WikiTitle, PageNode}
import org.scalatest.FunSuite
import org.dbpedia.extraction.util.RichFile.wrapFile

/**
  * Created by Chile on 10/17/2016.
  */
class AbstractLinkExtractorTest extends FunSuite {

  private val context = new {
    def ontology = throw new IllegalStateException("don't need Ontology for testing!!! don't call extract!")
    def language = Language.map.get("en").get
  }
  private val extractor = new AbstractLinkExtractor(context)
  private val outFile = new RichFile(new File("C:\\Users\\Chile\\Desktop\\Dbpedia\\core-i18n\\enwiki\\20160305\\linked-abstracts.ttl"))
  private val dest = new WriterDestination(() => IOUtils.writer(outFile), new TerseFormatter(false,true))
  //private val titles = List("Tom_Pettitt", "George_Washington","President_of_the_United_States","Electoral_College_(United_States)")
  private val titles = List("James_Wilkinson", "George_Washington")

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
