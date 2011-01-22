package org.dbpedia.extraction.dump

import java.io.{BufferedWriter, FileWriter, File}
import _root_.org.dbpedia.extraction.wikiparser.WikiTitle

/**
 * Writes a completion log file.
 */
class CompletionWriter(file : File)
{
    val writer = new BufferedWriter(new FileWriter(file))

    def write(id : Int, title : WikiTitle, success : Boolean) : Unit = synchronized
    {
        val idStr = id.toString
        val leadingZeros = "0" * (10 - idStr.length)
        writer.write(leadingZeros + idStr + " " + title.encodedWithNamespace + " " + success.toString)
        writer.newLine() 
    }

    def close() = writer.close()
}
