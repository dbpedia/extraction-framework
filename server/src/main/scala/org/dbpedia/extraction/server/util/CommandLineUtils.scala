package org.dbpedia.extraction.server.util

import java.util.logging.Logger

import scala.io.Source

/**
  * Utils for command line
  */
object CommandLineUtils {

  private val logger = Logger.getLogger(this.getClass.getName)

  /**
    * Execute commands
    *
    * @param command Must be " " delimited
    * @param print
    * @return returns True if success, False otherwise
    */
  def execute(command : String, print: Boolean = true) : Boolean = {

    try {

      val rt = Runtime.getRuntime
      val pr = rt.exec(command)
      val is = pr.getInputStream
      val commandOutput = Source.fromInputStream(is).mkString
      pr.waitFor()
      if(print) logger.info(commandOutput)
      true

    } catch {
      case e : Exception => e.printStackTrace(); false
    }

  }

}
