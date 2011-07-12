package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.sources.FileSource
import io.Source
import org.dbpedia.extraction.util.Language
import java.io.{FilenameFilter, File}
import java.lang.IllegalStateException
import org.junit.{Ignore, Test}

@Ignore  // uncomment to test; MediaWiki server has to be in place
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