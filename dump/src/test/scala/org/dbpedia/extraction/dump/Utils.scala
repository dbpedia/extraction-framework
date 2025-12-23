package org.dbpedia.extraction.dump

import org.dbpedia.extraction.dump.TestConfig.{classLoader, date}

import java.io.File
import java.util.Properties

object Utils {
  def loadTestGroupsKeys(group: String, path: String, option: String = "yes"): Array[String] = {
    println(
      s"""##############
         | GROUP $group
         |##############""".stripMargin)

    val filePath = classLoader.getResource(path).getFile
    val file = scala.io.Source.fromFile(filePath)

    val table: Array[Array[String]] = file.getLines().map(_.split(",")).toArray
    val columnsNames: Array[String] = table.head

    if (!columnsNames.contains(group)) {
      Array[String]()
    }
    else {
      val indexOfGroup = columnsNames.indexOf(group)
      val groupsKeys: Array[String] = table.tail.flatMap(row =>
        if (row(indexOfGroup) == option) Array[String](row(0))
        else Array[String]())
      groupsKeys
    }
  }

  def getGroup(testName: String): String = {
    val resourceInputStream = Option(getClass.getClassLoader.getResourceAsStream("properties-from-pom.properties"))
    val properties = new Properties()
    resourceInputStream match {
      case Some(inputStream) => properties.load(inputStream)
      case None => return TestConfig.defaultTestGroup
    }
    val groupOption = Option(properties.getProperty(testName))
    groupOption match {
      case Some(group) => group
      case None => TestConfig.defaultTestGroup
    }
  }

  def renameAbstractsDatasetFiles(datasetName: String): Unit = {
    val minidumpDir = new File("./target/minidumptest/base")
    minidumpDir.listFiles().foreach(f => {
      val longAbstractsFile = new File( s"./target/minidumptest/base/${f.getName}/$date/${f.getName}-$date-long-abstracts.ttl.bz2")
      if (longAbstractsFile.exists()) {
        longAbstractsFile.renameTo(new File(s"./target/minidumptest/base/${f.getName}/$date/${f.getName}-$date-long-abstracts-$datasetName.ttl.bz2"))
      }
      val shortAbstractsFile = new File( s"./target/minidumptest/base/${f.getName}/$date/${f.getName}-$date-short-abstracts.ttl.bz2")
      if (shortAbstractsFile.exists()) {
        shortAbstractsFile.renameTo(new File(s"./target/minidumptest/base/${f.getName}/$date/${f.getName}-$date-short-abstracts-$datasetName.ttl.bz2"))
      }
    })
  }
}
