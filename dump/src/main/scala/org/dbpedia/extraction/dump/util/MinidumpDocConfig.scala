package org.dbpedia.extraction.dump.util
import java.io._
object MinidumpDocConfig {
  val targetNode = "targetNode"
  val subjectOf = "subjectOf"
  val objectOf = "objectOf"
  val issue = "issue"
  val dbpediaUriPrefix = "dbpedia.org/"
  val englishDbpediaUriPrefix = "en.dbpedia.org/"
  val classLoader: ClassLoader = getClass.getClassLoader
  val path = "/Users/mykolamedynsky/Desktop/4semester/GoogleSummerOfCode/extraction-framework/dump/src/test/resources/shaclTestsTable.csv"
  val shaclTestsTableFile = new File("/Users/mykolamedynsky/Desktop/4semester/GoogleSummerOfCode/extraction-framework/dump/src/test/resources/shaclTestsTable.csv" )


}
