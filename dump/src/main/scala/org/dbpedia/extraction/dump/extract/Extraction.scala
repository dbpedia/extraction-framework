package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.util.ProxyAuthenticator
import java.net.Authenticator
import org.dbpedia.extraction.util.ConfigUtils

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
    val config = ConfigUtils.loadConfig(args(0), "UTF-8")

    // overwrite properties with CLI args
    // TODO arguments could be of the format a=b and then property a can be overwritten with "b"

    //Load extraction jobs from configuration
    val jobs = new ConfigLoader(new Config(config)).getExtractionJobs()

    //Execute the extraction jobs one by one
    for (job <- jobs) job.run()
  }
}
