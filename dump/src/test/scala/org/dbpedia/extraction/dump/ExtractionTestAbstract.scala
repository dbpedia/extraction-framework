package org.dbpedia.extraction.dump
import scala.io.Source
import java.io.{BufferedWriter, File, FileWriter}
import java.util.concurrent.ConcurrentLinkedQueue
import org.apache.commons.io.FileUtils
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.dump.TestConfig.{ date, mappingsConfig, minidumpDir, nifAbstractConfig, plainAbstractConfig}
import org.dbpedia.extraction.dump.extract.ConfigLoader
import org.dbpedia.extraction.dump.tags.ExtractionTestTag
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FunSuite}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.{Files, Paths}
import scala.concurrent.Future

@DoNotDiscover
class ExtractionTestAbstract extends FunSuite with BeforeAndAfterAll {
  override def beforeAll() {
    minidumpDir.listFiles().foreach(f => {
      val wikiMasque = f.getName + "wiki"
      val targetDir = new File(mappingsConfig.dumpDir, s"$wikiMasque/$date/")
      // create directories
      targetDir.mkdirs()
      FileUtils.copyFile(
        new File(f + "/wiki.xml.bz2"),
        new File(targetDir, s"$wikiMasque-$date-pages-articles-multistream.xml.bz2")
      )
    })
  }

  ignore("extract html abstract datasets", ExtractionTestTag) {
   // Utils.renameAbstractsDatasetFiles("html")
    println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> html abstract begin")
    val jobsRunning1 = new ConcurrentLinkedQueue[Future[Unit]]()
    val extractRes = extract(nifAbstractConfig, jobsRunning1)
    writeTestResult("MWC_ro_html_rest_only",extractRes)
    println("> html abstract end")
  }

/*test("extract plain abstract datasets", ExtractionTestTag) {
    println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Plain abstract begin")
    Utils.renameAbstractsDatasetFiles("plain")
    val jobsRunning2 = new ConcurrentLinkedQueue[Future[Unit]]()
    val extractRes2=extract(plainAbstractConfig, jobsRunning2)
    writeTestResult("MWC_ro_plain_only",extractRes2)
    println("> Plain abstract end")
  }*/

  def writeTestResult(fileName : String, content: Array[Map[String,String]]): Unit ={
    val today = java.time.LocalDate.now.toString
    val urisListUsed="../dump/src/test/bash/minidump_file_used.txt"
    var fileName2=""
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    val json = mapper.writeValueAsString(content)

    val targetDir = new File("../dump/test_logs/")
    // create directories
    targetDir.mkdirs()
      
    if(Files.exists(Paths.get(urisListUsed))){
      val Urifile = Source.fromFile(urisListUsed)
      val fileContents = Urifile.getLines.mkString
      Urifile.close()
      fileName2="../dump/test_logs/"+fileName+"_"+fileContents+"_"+today+".log"
    } else {
      fileName2="../dump/test_logs/"+fileName+"_"+"base-list"+today+".log"
    }
    val file = new File(fileName2)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(json)
    bw.close()
  }

  def extract(config: Config, jobsRunning: ConcurrentLinkedQueue[Future[Unit]]): Array[Map[String,String]] ={
    println(">>>>>>>>> EXTRACT  - BEGIN")
    var  mapResults = Array[Map[String,String]]()
    val configLoader = new ConfigLoader(config)
    val parallelProcesses = 1
    println(parallelProcesses)
    val jobs=configLoader.getExtractionJobs
    println(">>>>>>>>> EXTRACT - NBJOBS > "+jobs.size)
    println("LAUNCH JOBS")
    for (job <- jobs) {
      job.run()
      val lang=job.extractionRecorder.language
      val records=job.extractionRecorder
      println(">>>>>>>>> EXTRACT - LANG > " + lang.wikiCode)
      val status = records.getStatusValues(lang);
      var numberOfFailedPages429 = 0
      var numberOfFailedPages503 = 0
      var numberOfFailedPagesIOException = 0
      var numberOfFailedPagesOutOfMemoryError = 0
      var numberOfFailedPagesNullPointerException = 0
      var mapLocal= status

      mapLocal += "language" -> lang.wikiCode.toString();

      try {
        val listFailedPages_ = records.listFailedPages(lang)
        for( failed <- listFailedPages_) {
          if (failed.toString().contains("Server returned HTTP response code: 429")){
            numberOfFailedPages429 += 1
          }
          if (failed.toString().contains("Server returned HTTP response code: 503")){
            numberOfFailedPages503 += 1
          }
          if (failed.toString().contains("java.io.IOException")){
            numberOfFailedPagesIOException += 1
          }
          if (failed.toString().contains("java.io.OutOfMemoryError")){
            numberOfFailedPagesOutOfMemoryError += 1
          }
          if (failed.toString().contains("java.io.NullPointerException")){
            numberOfFailedPagesNullPointerException += 1
          }
        }
      } catch {
        case e: Exception =>  None
      }

      mapLocal += "numberOfFailedPages429" -> numberOfFailedPages429.toString
      mapLocal += "numberOfFailedPages503" -> numberOfFailedPages503.toString
      mapLocal += "numberOfFailedPagesIOException" -> numberOfFailedPagesIOException.toString
      mapLocal += "numberOfFailedPagesOutOfMemoryError" -> numberOfFailedPagesOutOfMemoryError.toString
      mapLocal += "numberOfFailedPagesNullPointerException" -> numberOfFailedPagesNullPointerException.toString

      mapResults = mapResults :+ mapLocal
    }

    while (jobsRunning.size() > 0) {
      Thread.sleep(1000)
    }

    jobsRunning.clear()
    mapResults
  }
}
