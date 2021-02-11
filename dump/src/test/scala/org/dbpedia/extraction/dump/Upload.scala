package org.dbpedia.extraction.dump

import org.dbpedia.extraction.dump.TestConfig.classLoader
import org.scalatest.FunSuite

class Upload extends FunSuite {

  def loadTestGroupsKeys(group: String, path: String): Array[String] = {
    val flag = "yes"
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
        if (row(indexOfGroup) == flag) Array[String](row(0))
        else Array[String]())

      groupsKeys
    }
  }
  val keysGroupAll = loadTestGroupsKeys("GROUP_ALL","testGroups.csv")
  assert(keysGroupAll.nonEmpty)
  assert(keysGroupAll.length == 2)
  assert(keysGroupAll.contains("#Angela_Merkel"))
  assert(keysGroupAll.contains("#IKEA"))

  val keysGroupDev = loadTestGroupsKeys("GROUP_DEV", "testGroups.csv")
  assert(keysGroupDev.nonEmpty)
  assert(keysGroupDev.length == 2)
  assert(keysGroupDev.contains("#Samsung"))
  assert(keysGroupDev.contains("#Food_(disambiguation)_en"))

}
