package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.sources.FileSource
import io.Source
import org.dbpedia.extraction.util.Language
import java.io.{FilenameFilter, File}
import java.lang.IllegalStateException
import org.junit.{Ignore, Test}

@Ignore  // unignore to test; MediaWiki server has to be in place
class AbstractExtractorTest
{
    private val testDataRootDir = new File("core/src/test/resources/org/dbpedia/extraction/mappings")

    private val filter = new FilenameFilter
    {
        def accept(dir: File, name: String) = name endsWith "-gold.txt"
    }

    /**
     * Assumes that gold standard files end in "-gold.txt" and input files end in ".wiki"
     */
    @Test
    def testAll()
    {
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
        assert(d == g, "char-diff: " + (d.length-g.length) + "\ngot:  " + d + "\ngold: " + g)
    }

    private val context = new {
        def ontology = throw new IllegalStateException("don't need Ontology for testing!!! don't call extract!")
        def language = Language.English
    }
    private val extractor = new AbstractExtractor(context)

    private val parser = WikiParser.getInstance()

    private def render(fileName : String) : String =
    {
        val page = new FileSource(testDataRootDir, Language.English, _ endsWith fileName).head

      //return empty Abstract in case that the parser Returned None
      parser(page) match {
        case Some(n) =>  val generatedAbstract = extractor.retrievePage(n.title)
                         extractor.retrievePage(page.title/*, generatedAbstract*/)
        case None => ""
      }

    }

    private def gold(fileName : String) : String =
    {
        Source.fromFile(testDataRootDir + "/" + fileName, "UTF-8").getLines().mkString("").replaceAll("\\s+", " ")
    }
}