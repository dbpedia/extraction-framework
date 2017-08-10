package org.dbpedia.extraction.server.util

import scala.io.Source

/**
  * Utils for command line
  */
object CommandLineUtils {

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
      if(print) println(commandOutput)
      true

    } catch {
      case e : Exception => e.printStackTrace(); false
    }

  }

}
