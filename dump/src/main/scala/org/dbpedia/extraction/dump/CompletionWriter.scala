package org.dbpedia.extraction.dump

import java.io.{BufferedWriter, FileWriter, File}
import _root_.org.dbpedia.extraction.wikiparser.WikiTitle
import java.net.URLDecoder

/**
 * Writes a completion log file.
 * TODO: this class is also used by live extraction. Maybe we should move it to core?
 */
class CompletionWriter(file : File)
{
    private val queue = new CompletionQueue[String]()
  
    private val writer = new BufferedWriter(new FileWriter(file))

    def write(id : Int, title : WikiTitle, success : Boolean) : Unit = synchronized
    {
        val idStr = id.toString
        val leadingZeros = "0" * (10 - idStr.length)
        val line = leadingZeros + idStr + "|" + title.decodedWithNamespace + "|" + success.toString
        
        // add current line
        queue(id) = line
        
        // write whatever is there in correct order
        for (line <- queue) { 
          writer.write(line)
          writer.newLine()
        }
    }

    def close() = writer.close()
}
