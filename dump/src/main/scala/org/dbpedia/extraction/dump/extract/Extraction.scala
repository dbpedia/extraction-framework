package org.dbpedia.extraction.dump.extract

import java.util.Properties
import java.io.{FileNotFoundException, FileInputStream, InputStreamReader}

/**
 * Dump extraction script.
 */
object Extraction {
  val Started = "extraction-started"

  val Complete = "extraction-complete"

  def main(args: Array[String]): Unit = {
    require(args != null && args.length >= 1 && args(0).nonEmpty, "missing required argument: config file name")

    // Load properties
    val properties = new Properties()
    try {
      val in = new FileInputStream(args(0))
      try properties.load(new InputStreamReader(in, "UTF-8"))
      finally in.close()
    } catch {
      case fne: FileNotFoundException => println("property file " + args(0) + " not found")
    }

    // overwrite properties with CLI args
    // TODO arguments could be of the format a=b and then property a can be overwritten with "b"

    //Load extraction jobs from configuration
    val jobs = new ConfigLoader(new Config(properties)).getExtractionJobs()

    //Execute the extraction jobs one by one
    for (job <- jobs) job.run()
  }
}
