package org.dbpedia.extraction.dump

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import org.apache.commons.io.FileUtils
import org.dbpedia.extraction.config.Config2
import org.dbpedia.extraction.dump.extract.ConfigLoader2
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FunSuite, Tag}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object  HistoTestTag extends Tag("HistoricTest")
class ExtractionTest extends FunSuite {

  test("test Historic extraction", HistoTestTag) {
    val jobsRunning = new ConcurrentLinkedQueue[Future[Unit]]()//
    val classLoader: ClassLoader = getClass.getClassLoader
    val histoConfig =     new Config2(classLoader.getResource("extraction-configs/extraction.config.properties").getFile)
    println(classLoader.getResource("extraction-configs/extraction.config.properties").getFile.toString)
    println("BEFORE EXTRACT")
    extract(histoConfig, jobsRunning)
    println("AFTER EXTRACT")
  }

  def extract(config: Config2, jobsRunning: ConcurrentLinkedQueue[Future[Unit]]): Unit = {
    val configLoader = new ConfigLoader2(config)
    val jobs = configLoader.getExtractionJobs
    println(">>>>>>>>> EXTRACT - NBJOBS > " + jobs.size)
    println("LAUNCH JOBS")
    for (job <- jobs) {
      job.run()
    }
      while (jobsRunning.size() > 0) {

        Thread.sleep(1000)
      }

      jobsRunning.clear()
  }
}