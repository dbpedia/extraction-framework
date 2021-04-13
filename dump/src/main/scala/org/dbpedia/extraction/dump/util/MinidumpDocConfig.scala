package org.dbpedia.extraction.dump.util

object MinidumpDocConfig {
  val targetNode = "targetNode"
  val subjectOf = "subjectOf"
  val objectOf = "objectOf"
  val issue = "issue"
  val shape = "shape"
  val dbpediaUriPrefix = "dbpedia.org/"
  val englishDbpediaUriPrefix = "en.dbpedia.org/"
  val classLoader: ClassLoader = getClass.getClassLoader
  val shaclTestsTableMarkdownPath = "src/test/resources/shaclTestsCoverageTable.md"
  val shaclTestsFolderPath = "src/test/resources/shacl-tests"
  val urisFilePath = "src/test/bash/uris.lst"
  val miniExtractionBaseDirPath = "target/minidumptest/base"
}
