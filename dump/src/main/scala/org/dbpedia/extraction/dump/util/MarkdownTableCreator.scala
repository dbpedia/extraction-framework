package org.dbpedia.extraction.dump.util

import java.io.{File, PrintWriter}

object MarkdownTableCreator extends App{
    def createMarkdownFile(): Unit = {

      val markdownFile = new File("../extraction-framework/dump/src/test/resources/markdown.md")

      if (!markdownFile.exists()) {
        markdownFile.createNewFile()
      }
      val filePath = MinidumpDocConfig.path
      val file = scala.io.Source.fromFile(filePath)
      val lines = file.getLines().toArray
      val firstLine = lines.head

      val markdownPrintWriter = new PrintWriter(markdownFile)
      markdownPrintWriter.write(firstLine.replaceAll(",", "|"))
      val numberOfColumns = firstLine.count(x => x==',')
      markdownPrintWriter.write("\n")
      for (i <- 0 until numberOfColumns) {
        markdownPrintWriter.write("-|")
      }
      markdownPrintWriter.write("-\n")

      for (line <- lines.tail) {
        markdownPrintWriter.write(line.replaceAll(",", "|"))
        markdownPrintWriter.write("\n")
      }

      markdownPrintWriter.close()
    }
    createMarkdownFile()
}
