package org.dbpedia.extraction.dump

import org.scalatest.Suites

class MinidumpTests extends Suites(
  new DownloadTest,
  new ExtractionTest,
  new PostProcessingTest,
  new ConstructValidationTest,
  new ShaclTest
) {
  println("""    __  ____       _     __                         ______          __
            |   /  |/  (_)___  (_)___/ /_  ______ ___  ____     /_  __/__  _____/ /______
            |  / /|_/ / / __ \/ / __  / / / / __ `__ \/ __ \     / / / _ \/ ___/ __/ ___/
            | / /  / / / / / / / /_/ / /_/ / / / / / / /_/ /    / / /  __(__  ) /_(__  )
            |/_/  /_/_/_/ /_/_/\__,_/\__,_/_/ /_/ /_/ .___/    /_/  \___/____/\__/____/
            |                                      /_/                                   """.stripMargin)
}
