package org.dbpedia.extraction.mappings
import sys.process._
import org.dbpedia.extraction.dump.extract.Extraction

object Starter {
  def main(args:Array[String]) = {
  
	Extraction.main(args)	
   
   /* if(args.size != 4){
        println("usage: java -jar <xy.jar> <configfile> <language> <dumpFile> <outputFile>")
      } else {
        val configfile = args(0)
        val lang = args(1)
        val dumpFile = args(2)
        val outputFile = args(3)
        println("called Wiktionary Extraction Starter with args "+configfile+" "+lang+" "+dumpFile+" "+outputFile)
	}
	*/
      
   
  }
}
