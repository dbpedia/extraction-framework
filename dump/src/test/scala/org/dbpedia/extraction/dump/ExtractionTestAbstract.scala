package org.dbpedia.extraction.dump
import scala.io.Source
import java.io.{BufferedWriter, File, FileWriter}
import java.util.concurrent.ConcurrentLinkedQueue
import org.apache.commons.io.FileUtils
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.config.ConfigUtils.{getStrings, parseLanguages}
import org.dbpedia.extraction.dump.TestConfig.{classLoader, date, genericConfig, mappingsConfig, minidumpDir, nifAbstractConfig, plainAbstractConfig, sparkSession, wikidataConfig}
import org.dbpedia.extraction.dump.extract.ConfigLoader
import org.dbpedia.extraction.dump.tags.ExtractionTestTag
import org.dbpedia.extraction.util.{ExtractionRecorder, MediaWikiConnector}
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FunSuite}
import org.slf4j.LoggerFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.{Files, Paths}
import scala.concurrent.{Await, Future}


class ExtractionTestAbstract extends FunSuite with BeforeAndAfterAll {
  println("""    __  ____       _     __                         ______          __
            |   /  |/  (_)___  (_)___/ /_  ______ ___  ____     /_  __/__  _____/ /______
            |  / /|_/ / / __ \/ / __  / / / / __ `__ \/ __ \     / / / _ \/ ___/ __/ ___/
            | / /  / / / / / / / /_/ / /_/ / / / / / / /_/ /    / / /  __(__  ) /_(__  )
            |/_/  /_/_/_/ /_/_/\__,_/\__,_/_/ /_/ /_/ .___/    /_/  \___/____/\__/____/
            |                                      /_/                                   ABSTRACTS""".stripMargin)

  override def beforeAll() {
    minidumpDir.listFiles().foreach(f => {
      val wikiMasque = f.getName + "wiki"
      val targetDir = new File(mappingsConfig.dumpDir, s"${wikiMasque}/$date/")
      // create directories
      targetDir.mkdirs()
      FileUtils.copyFile(
        new File(f + "/wiki.xml.bz2"),
        new File(targetDir, s"${wikiMasque}-$date-pages-articles-multistream.xml.bz2")
      )
    })
  }




test("extract html abstract datasets", ExtractionTestTag) {
    Utils.renameAbstractsDatasetFiles("html")
    println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> html abstract begin")
    val jobsRunning1 = new ConcurrentLinkedQueue[Future[Unit]]()
    var extractRes=extract(nifAbstractConfig, jobsRunning1)
    writeTestResult("MWCREST_final_htmlbstract",extractRes)
    println("> html abstract end")

  }



 test("extract plain abstract datasets", ExtractionTestTag) {
    println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Plain abstract begin")
    Utils.renameAbstractsDatasetFiles("plain")
    val jobsRunning2 = new ConcurrentLinkedQueue[Future[Unit]]()
    var extractRes2=extract(plainAbstractConfig, jobsRunning2)
    writeTestResult("MWC_final_plainAbstract",extractRes2)
    println("> Plain abstract end")
  }

  def writeTestResult(file_name : String, content: Array[Map[String,String]]): Unit ={
    var today = java.time.LocalDate.now.toString
    var list_used_file="../dump/src/test/bash/minidump_file_used.txt"
    var file_name2=""
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    val json = mapper.writeValueAsString(content)

     val targetDir = new File("../dump/test_logs/")
    // create directories
    targetDir.mkdirs()
      
    if( Files.exists(Paths.get(list_used_file))){

      val fileContents = Source.fromFile(list_used_file).getLines.mkString
      file_name2="../dump/test_logs/"+file_name+"_"+fileContents+"_"+today+".log"
    }else{
      file_name2="../dump/test_logs/"+file_name+"_"+"base-list"+today+".log"

    }
    val file = new File(file_name2)
    val bw = new BufferedWriter(new FileWriter(file))
    for (k <- content) {
      //bw.write(k.map{case (k, v) => k + ":" + v}.mkString("|"))
      bw.write(json)
      bw.newLine();
    }
    bw.close()
  }

  def extract(config: Config, jobsRunning: ConcurrentLinkedQueue[Future[Unit]]): Array[Map[String,String]]  ={
    println(">>>>>>>>> EXTRACT  - BEGIN")
    var  map_results = Array[Map[String,String]]()
    val configLoader = new ConfigLoader(config)
    val jobs=configLoader.getExtractionJobs
    println(">>>>>>>>> EXTRACT - NBJOBS > "+jobs.size)
    println("LAUNCH JOBS")
    for (job <- jobs) {

      job.run()
      var lang=job.extractionRecorder.language
      var records=job.extractionRecorder
      println(">>>>>>>>> EXTRACT - LANG > "+lang.wikiCode.toString())

      var status=records.getStatusValues(lang);
      var nbFailed429 = 0;
      var nbFailed503 = 0;
      var nbFailedIOException = 0;
      var nbFailedOutOfMemoryError = 0;
      var nbFailedNullPointerException = 0;
      var map_local= status;

      map_local += "language" -> lang.wikiCode.toString();

      try {
        var listFailedPages_ =  records.listFailedPages(lang)
        for( failed <- listFailedPages_) {
          if(failed.toString().contains("Server returned HTTP response code: 429")){
            nbFailed429 += 1
          }
          if (failed.toString().contains("Server returned HTTP response code: 503")){
            nbFailed503 += 1
          }
          if (failed.toString().contains("java.io.IOException")){
            nbFailedIOException += 1
          }
          if (failed.toString().contains("java.io.OutOfMemoryError")){
            nbFailedOutOfMemoryError += 1
          }
          if (failed.toString().contains("java.io.NullPointerException")){
            nbFailedNullPointerException += 1
          }
        }
      } catch {
        case e: Exception => None
      };

          map_local += "nbFailed429" -> nbFailed429.toString;
          map_local += "nbFailed503" -> nbFailed503.toString;
          map_local += "nbFailedIOException" -> nbFailedIOException.toString;
          map_local += "nbFailedOutOfMemoryError" -> nbFailedOutOfMemoryError.toString;
          map_local += "nbFailedNullPointerException" -> nbFailedNullPointerException.toString;

          //map_results += nb_job.toString -> map_local;
          //jobsRunning.remove(future);
          map_local;

     // }
      map_results= map_results :+ map_local
        //Await.result(future,Duration.Inf)

    }
    while (jobsRunning.size() > 0) {

      Thread.sleep(1000)
    }

    jobsRunning.clear()
    return map_results

  }
}
