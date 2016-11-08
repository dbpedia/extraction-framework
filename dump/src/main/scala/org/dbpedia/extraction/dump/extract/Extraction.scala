package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.util.{Config, ProxyAuthenticator, ConfigUtils}
import java.net.Authenticator

/**
 * Dump extraction script.
 */
object Extraction {
  
  val Started = "extraction-started"

  val Complete = "extraction-complete"

  def main(args: Array[String]): Unit = {
    require(args != null && args.length >= 1 && args(0).nonEmpty, "missing required argument: config file name")
    Authenticator.setDefault(new ProxyAuthenticator())
    
    // Load properties
    val configProperties = ConfigUtils.loadConfig(args(0), "UTF-8")

    // overwrite properties with CLI args
    // TODO arguments could be of the format a=b and then property a can be overwritten with "b"

    //Load extraction jobs from configuration
    val config = new Config(configProperties)
    val configLoader = new ConfigLoader(config)

    //Execute the extraction jobs one by one
    for (job <- configLoader.getExtractionJobs) {
      job.run()
    }
  }
}
