package org.dbpedia.extraction.scripts

import java.io._

/**
  * Created by Chile on 2/22/2016.
  */
object StdOutErrRedirect {

  def main(args: Array[String]): Unit =
  {
    val in = new BufferedReader(new InputStreamReader(System.in))
    Stream.continually(in.readLine())
      .takeWhile(_ => true)
      .foreach(x => System.out.println(x))
  }
}
