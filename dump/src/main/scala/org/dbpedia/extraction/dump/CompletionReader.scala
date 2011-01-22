package org.dbpedia.extraction.dump

import _root_.org.dbpedia.extraction.wikiparser.WikiTitle
import _root_.org.dbpedia.extraction.util.StringUtils._

import java.io.{BufferedReader, FileReader, File}
import java.util.logging.Logger

/**
 * Reads a completion log file.
 */
class CompletionReader(file : File)
{
    val reader =
    {
        if(file.exists)
        {
            new BufferedReader(new FileReader(file))
        }
        else
        {
            Logger.getLogger(classOf[CompletionReader].getName).info("No completion log found. Doing complete extraction")
            null
        }
    }

    def read(expectedID : Int, expectedTitle : WikiTitle) : Boolean = synchronized
    {
        if(reader == null) return false

        //Read the next line
        val line = reader.readLine()
        if(line == null) return false

        //Parse the line
        val IntLiteral(id) :: title :: BooleanLiteral(success) :: Nil = line.split(' ').toList

        //Check if page matches expected page
        if(id != expectedID) throw new Exception("Invalid id: " + id + " (Expected: " + expectedID + ")")
        if(title != expectedTitle.encodedWithNamespace) throw new Exception("Invalid title: " + title + " (Expected: " + expectedTitle.encodedWithNamespace + ")")

        success
    }

    def close() = reader.close()
}