package org.dbpedia.extraction.dump.extract

import java.net.Authenticator
import java.util.concurrent.ConcurrentLinkedQueue

import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.util.ProxyAuthenticator

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Dump extraction script.
 */
object Extraction {

  def main(args: Array[String]): Unit = {
    require(args != null && args.length >= 1 && args(0).nonEmpty, "missing required argument: config file name")
    Authenticator.setDefault(new ProxyAuthenticator())

    //Load extraction jobs from configuration
    val config = new Config(args.head)
    val configLoader = new ConfigLoader(config)

    val parallelProcesses = if(config.runJobsInParallel) config.parallelProcesses else 1
    val jobsRunning = new ConcurrentLinkedQueue[Future[Unit]]()
    //Execute the extraction jobs one by one
    for (job <- configLoader.getExtractionJobs) {
      while(jobsRunning.size() >= parallelProcesses)
        Thread.sleep(1000)

      val future = Future{job.run()}
      jobsRunning.add(future)
      future.onComplete {
        case Failure(f) => throw f
        case Success(_) => jobsRunning.remove(future)
      }
    }

    while(jobsRunning.size() > 0)
      Thread.sleep(1000)
  }
}
