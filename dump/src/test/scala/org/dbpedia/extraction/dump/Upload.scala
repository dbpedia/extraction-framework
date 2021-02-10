package org.dbpedia.extraction.dump

import org.dbpedia.extraction.dump.TestConfig.classLoader

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Upload extends App {
  def loadTestGroupKeys(group: String, path: String)  = {
    val mapGroups: mutable.HashMap[Integer, String] = scala.collection.mutable.HashMap[Integer, String]()
    val file = scala.io.Source.fromFile(classLoader.getResource(path).getFile)

    val firstRow = file.getLines().take(1).mkString.split(",")
    for (i <- 0 until firstRow.length) {
      mapGroups.put(i, firstRow(i))
    }
    val otherRows = file.getLines()
    val result: ArrayBuffer[String] = new ArrayBuffer[String]()
    for (line <- otherRows) {
      val words = line.mkString.split(",")
      for (i <- 0 until words.length) {
        if (mapGroups(i).equals(group) ) {
          if (words(i) == "yes") {
            result += words(0)
          }
        }
      }
    }
    result
  }
  loadTestGroupKeys("GROUP_ALL", "testGroups.csv").foreach(println)
}
