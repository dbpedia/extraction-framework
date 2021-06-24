package org.dbpedia.extraction.dump

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import org.apache.commons.io.FileUtils
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.dump.TestConfig.{classLoader, date, genericConfig, mappingsConfig, minidumpDir, nifAbstractConfig, plainAbstractConfig, sparkSession, wikidataConfig}
import org.dbpedia.extraction.dump.extract.ConfigLoader
import org.dbpedia.extraction.dump.tags.ExtractionTestTag
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FunSuite}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

@DoNotDiscover
class ExtractionTest extends FunSuite with BeforeAndAfterAll {

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

  test("extract generic datasets", ExtractionTestTag) {
    val jobsRunning = new ConcurrentLinkedQueue[Future[Unit]]()
    extract(genericConfig, jobsRunning)
  }

  test("extract mappings datasets", ExtractionTestTag) {
    val jobsRunning = new ConcurrentLinkedQueue[Future[Unit]]()
    extract(mappingsConfig, jobsRunning)
  }

  test("extract wikidata datasets", ExtractionTestTag) {
    val jobsRunning = new ConcurrentLinkedQueue[Future[Unit]]()
    extract(wikidataConfig, jobsRunning)
  }

  test("extract abstract datasets", ExtractionTestTag) {
    val jobsRunning1 = new ConcurrentLinkedQueue[Future[Unit]]()
    extract(nifAbstractConfig, jobsRunning1)
    Utils.renameAbstractsDatasetFiles("html")
    val jobsRunning2 = new ConcurrentLinkedQueue[Future[Unit]]()
    extract(plainAbstractConfig, jobsRunning2)
    Utils.renameAbstractsDatasetFiles("plain")
  }

  def extractSpark(config: Config, jobsRunning: ConcurrentLinkedQueue[Future[Unit]]): Unit = {
    /**
     * generic extraction
     */
    println("-- generic")
    val configLoader = new ConfigLoader(config)

    // Run extraction jobs
    configLoader.getSparkExtractionJobs.foreach(job => {
      try {
        job.run(sparkSession, config)
      } catch {
        case ex: Throwable =>
          ex.printStackTrace()
      }
    })
    while (jobsRunning.size() > 0) {
      Thread.sleep(1000)
    }
    jobsRunning.clear()
  }

  def extract(config: Config, jobsRunning: ConcurrentLinkedQueue[Future[Unit]]): Unit = {
    val configLoader = new ConfigLoader(config)
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
