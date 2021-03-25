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
//      for (line <- file.getLines()) {
//        println(line)
//      }
      val markdownPrintWriter = new PrintWriter(markdownFile)
      markdownPrintWriter.write(firstLine.replaceAll(",", "|"))
      val numberOfColumns = firstLine.count(x => x==',')
      markdownPrintWriter.write("\n")
      for (i <- 0 until numberOfColumns) {
        markdownPrintWriter.write("-|")
      }
      markdownPrintWriter.write("-\n")

      for (line <- lines) {
        markdownPrintWriter.write(line.replaceAll(",", "|"))
        markdownPrintWriter.write("\n")
      }

      markdownPrintWriter.close()
    }
  def loadTestGroupsKeys(group: String, path: String): Array[String] = {
    println(
      s"""##############
         | GROUP $group
         |##############""".stripMargin)
    val flag = "yes"
    val filePath = MinidumpDocConfig.path
    val file = scala.io.Source.fromFile(filePath)

    val table: Array[Array[String]] = file.getLines().map(_.split(",")).toArray
    val columnsNames: Array[String] = table.head

    if (!columnsNames.contains(group)) {
      Array[String]()
    }
    else {
      val indexOfGroup = columnsNames.indexOf(group)
      val groupsKeys: Array[String] = table.tail.flatMap(row =>
        if (row(indexOfGroup) == flag) Array[String](row(0))
        else Array[String]())

      groupsKeys
    }
  }
    createMarkdownFile()
}
