package org.dbpedia.extraction.mappings
import sys.process._
import org.dbpedia.extraction.dump.extract.Extraction

object Starter {
  def main(args:Array[String]) = {
    if(args.size != 3){
      println("usage: java -jar <xy.jar> <language> <dumpFile> <outputFile>")
    } else {
      val lang = args(0)
      val dumpFile = args(1)
      val outputFile = args(2)
      println("called Wiktionary Extraction Starter with args "+lang+" "+dumpFile+" "+outputFile)

      //invoke mvn
      //todo inject above properties
      Extraction.main(Array())
    }
  }
}
