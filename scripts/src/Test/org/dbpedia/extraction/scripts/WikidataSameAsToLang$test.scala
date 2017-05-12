package org.dbpedia.extraction.scripts

import java.io.{BufferedReader, File}

import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.scalatest._

import scala.collection.Map
import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
  * Created by Termilion on 10/5/2017.
  */
class WikidataSameAsToLang$test extends FunSuite {
  private var baseDir : File = null
  private var date : String = null
  private var suffix : String = null
  private var output_1 : String = "test_1"
  private var output_2 : String = "test_2"
  private var language : Array[Language] = null
  private var formats : Map[String, Formatter] = null
  private var input : String = "test-input"
  private var wikiDataFile: RichFile = null

  // TODO: come up with a better performing solution for the concurrency issue => maybe compare graphs instead of files?
  /**
    * The test assures that the new version of WikidataSameAsToLanguageLinks is faster and both outputs contain the same quads.
    * False alarms caused by concurrency are handled, but that could use some memory when testing on big files.
    */
  test("Compare WdSATLL_test and WdSATLL") {
    loadConfig("process.wikidata.sameas.all.properties")
    //Execute old
    val oldCode = new WikidataSameAsToLanguageLinks(baseDir, wikiDataFile, output_1, language, formats)
    var start = System.currentTimeMillis()
    oldCode.processLinks()
    var time1 = System.currentTimeMillis() - start
    info("Old Code finished in: " + time1 + " ms")
      // TODO: RAM Usage ?
    //Execute new
    start = System.currentTimeMillis()
    val newCode = new WikidataSameAsToLanguageLinks_test(baseDir, wikiDataFile, output_2, language, formats)
    newCode.processLinks()
    var time2 = System.currentTimeMillis() - start
    info("New Code finished in: " + time2 + " ms")
      // TODO: RAM Usage ?
      //Find Files
    val testfileFinder = new Finder[File](baseDir, Language.English, "wiki")
    val date = testfileFinder.dates().last
      //Read Files
    var i = 0
    var diff = 0
    var diff_list1 = ListBuffer[String]()
    var diff_list2 = ListBuffer[String]()
    val iterator_1 = Source.fromInputStream(
      IOUtils.inputStream(testfileFinder.file(date, output_1 + suffix).get)).getLines()
    val iterator_2 = Source.fromInputStream(
      IOUtils.inputStream(testfileFinder.file(date, output_2 + suffix).get)).getLines()
    while(iterator_1.hasNext && iterator_2.hasNext){
      //Compare Files
      i += 1
      val line_1 = iterator_1.next()
      val line_2 = iterator_2.next()
      if(!line_1.equals(line_2)){
        // Lines are different! => could still be concurrency caused
        diff_list1 += line_1
        diff_list2 += line_2
        if(diff_list1.contains(line_2)){
          // We found line_2 in the first file before => false alarm
          diff_list1 -= line_2
          diff -= 1
        }
        if(diff_list2.contains(line_1)){
          // We found line_1 in the second file before => false alarm
          diff_list2 -= line_1
          diff -= 1
        }
        // Output different Lines
        if(!line_1.trim.startsWith("#")) {
          // Line is not head or tail comment
          info("DIFFERENCE in line: " + i + "\nline 1: " + line_1 + "\nline 2: " + line_2)
          diff += 1
        }
      }
    }
    //Output gathered Data
    info("\n\nOld Time: "+time1+"  New Time: "+time2+"\nDifference in Time (NEW - OLD): " + (time2 - time1) + "ms\nNumber of Lines that differed (without the concurrency differences!): " + diff)
    assert(diff == 0, "Test Failed: the Files are different!")
    assert(time2 <= time1, "Test Failed: the new Code is slower!")
  }

  private def loadConfig(fileName : String): Unit = {
    require(fileName != "", "missing required argument: config file name")

    val config = new Config(fileName)
    baseDir = config.dumpDir
    if (!baseDir.exists) {
    throw error("dir " + baseDir + " does not exist")
  }

    val inputFinder = new Finder[File](baseDir, Language.Wikidata, "wiki")
    date = inputFinder.dates().last

    suffix = config.inputSuffix match{
    case Some(x) => x
    case None => throw new IllegalArgumentException("Please provide a 'suffix' attribute in your properties configuration")
  }

    language = config.languages

    formats = config.formats

    // find the input wikidata file
    wikiDataFile = inputFinder.file(date, input + suffix).get
  }
}
