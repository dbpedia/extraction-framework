package org.dbpedia.extraction.mappings

import _root_.org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.sources.{FileSource, WikiPage}
import io.Source
import org.dbpedia.extraction.util.Language
import org.junit.Test
import java.io.{FilenameFilter, File}
import java.lang.IllegalStateException

class AbstractExtractorTest// extends FlatSpec with ShouldMatchers
{
//    "AbstractExtractor" should "render nicely" in
//    {
//        val d = parse("AbstractExtractorTest1.wiki")
//        val g = gold("AbstractExtractorTest1-gold.txt")
//        d should equal (g)
//    }
//
//    it should "return false" in
//    {
//        1 should equal (2)
//    }

    private val testDataRootDir = new File("core/src/test/resources/org/dbpedia/extraction/mappings")


    @Test
    def testAll()
    {
        val filter = new FilenameFilter
        {
            def accept(dir: File, name: String) = name endsWith "-gold.txt"
        }

        for(f <- testDataRootDir.listFiles(filter) )
        {
            test(f.getName.replace("-gold.txt", ".wiki"), f.getName)
        }
    }

    def test(fileNameWiki : String, fileNameGold : String)
    {
        println("testing wiki "+fileNameWiki+" and gold "+fileNameGold)
        val d = render(fileNameWiki)
        val g = gold(fileNameGold)
        assert(d == g, "char-diff: " + (d.length-g.length) + "\n" + d + "\n" + g)
    }

    private val context = new {
        def ontology = throw new IllegalStateException("don't need Ontology for testing!!! don't call extract!")
        def language = Language.Default
    }
    private val extractor = new AbstractExtractor(context)

    private val parser = WikiParser()

    private def render(fileName : String) : String =
    {
        val page = new FileSource(testDataRootDir, _ endsWith fileName).head
        val generatedAbstract = extractor.getAbstractWikiText(parser(page))
        extractor.retrievePage(page.title.encoded, generatedAbstract)
    }

    private def gold(fileName : String) : String =
    {
        Source.fromFile(testDataRootDir + "/" + fileName, "UTF-8").getLines().mkString("").replaceAll("\\s+", " ")
    }
}