package org.dbpedia.extraction.dump.extract

import java.net.Authenticator

import org.dbpedia.extraction.util.{Config, ProxyAuthenticator}

/**
 * Dump extraction script.
 */
object Extraction {
  
  val Started = "extraction-started"

  val Complete = "extraction-complete"

  def main(args: Array[String]): Unit = {
    require(args != null && args.length >= 1 && args(0).nonEmpty, "missing required argument: config file name")
    Authenticator.setDefault(new ProxyAuthenticator())


    // overwrite properties with CLI args
    // TODO arguments could be of the format a=b and then property a can be overwritten with "b"

    //Load extraction jobs from configuration
    val config = new Config(args.head)
    val configLoader = new ConfigLoader(config)

    //Execute the extraction jobs one by one
    for (job <- configLoader.getExtractionJobs) {
      job.run()
    }
    configLoader.getExtractionRecorder.finalize()
  }
}
