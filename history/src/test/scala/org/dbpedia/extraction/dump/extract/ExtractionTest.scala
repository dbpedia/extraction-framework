package org.dbpedia.extraction.dump

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import org.apache.commons.io.FileUtils
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.dump.extract.ConfigLoader2
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FunSuite, Tag}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object  HistoTestTag extends Tag("HistoricTest")
class ExtractionTest extends FunSuite {

  test("test Historic extraction", HistoTestTag) {
    val jobsRunning = new ConcurrentLinkedQueue[Future[Unit]]()

    val classLoader: ClassLoader = getClass.getClassLoader

    val histoConfig =     new Config(classLoader.getResource("extraction-configs/extraction.config.properties").getFile)
    println("BEFORE")
    extract(histoConfig, jobsRunning)
    println("AFTER")
  }

  def extract(config: Config, jobsRunning: ConcurrentLinkedQueue[Future[Unit]]): Unit = {
    val configLoader = new ConfigLoader2(config)
    val parallelProcesses = if (config.runJobsInParallel) config.parallelProcesses else 1

    //Execute the extraction jobs one by one
    for (job <- configLoader.getExtractionJobs) {
      while (jobsRunning.size() >= parallelProcesses) {
        Thread.sleep(1000)
      }

      val future = Future {
        job.run()
      }
      jobsRunning.add(future)
      future.onComplete {
        case Failure(f) => throw f
        case Success(_) => jobsRunning.remove(future)
      }
    }

    while (jobsRunning.size() > 0) {
      Thread.sleep(1000)
    }
    jobsRunning.clear()
  }
}