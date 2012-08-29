package org.dbpedia.extraction.dump.extract

import java.util.Properties
import java.io.{FileInputStream, InputStreamReader}

/**
 * Dump extraction script.
 */
object Extraction
{
  val Started = "extraction-started"

  val Complete = "extraction-complete"

  def main(args : Array[String]): Unit =
  {
    require(args != null && args.length == 1 && args(0).nonEmpty, "missing argument: config file name")
    
    // Load properties
    val properties = new Properties()
    val in = new FileInputStream(args(0))
    try properties.load(new InputStreamReader(in, "UTF-8"))
    finally in.close()

    //Load extraction jobs from configuration
    val jobs = new ConfigLoader(new Config(properties)).getExtractionJobs()

    //Execute the extraction jobs one by one
    for (job <- jobs) job.run()
  }
}
