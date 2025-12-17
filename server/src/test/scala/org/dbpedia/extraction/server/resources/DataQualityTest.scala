package org.dbpedia.extraction.server.resources

import org.dbpedia.extraction.util.DataQualityMonitor
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class DataQualityTest extends FlatSpec with Matchers with BeforeAndAfter {

  val dataQuality = new DataQuality()

  before {
    DataQualityMonitor.reset()
  }

  "DataQuality resource" should "show empty state" in {
    val html = dataQuality.get

    (html \\ "html").nonEmpty should be(true)
    html.toString should include("No errors logged yet")
  }

  it should "display error summary" in {
    val m1 = DataQualityMonitor.forExtractor("HomepageExtractor")
    val m2 = DataQualityMonitor.forExtractor("AbstractExtractor")

    m1.logInvalidData("Albert_Einstein", "Invalid IRI: malformed URL")
    m1.logInvalidData("Nikola_Tesla", "Invalid IRI: malformed URL")
    m2.logInvalidData("Isaac_Newton", "Missing property")

    val html = dataQuality.get.toString
    html should include("Total Error Types:")
    html should include("HomepageExtractor")
    html should include("AbstractExtractor")
  }

  it should "group by extractor" in {
    DataQualityMonitor.forExtractor("ExtractorA").logInvalidData("Page1", "Error 1")
    DataQualityMonitor.forExtractor("ExtractorB").logInvalidData("Page2", "Error 2")

    val html = dataQuality.get.toString
    html should include("ExtractorA")
    html should include("ExtractorB")
  }

  "Metrics endpoint" should "return JSON" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")
    monitor.logInvalidData("Page1", "Invalid data")

    val json = dataQuality.getMetricsJson
    json should include("totalErrors")
    json should include("TestExtractor")
    json.trim should startWith("{")
  }

  it should "count errors correctly" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")
    (1 to 5).foreach(i => monitor.logInvalidData(s"Page$i", "Test error"))

    dataQuality.getMetricsJson should include("\"totalErrors\": 5")
  }

  "Error details" should "show error table" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")
    monitor.logInvalidData("Albert_Einstein", "Invalid IRI syntax")

    val html = dataQuality.getErrors("TestExtractor:InvalidData", "10").toString
    html should include("<table")
    html should include("Albert_Einstein")
    html should include("Invalid IRI syntax")
  }

  it should "handle missing errors" in {
    val html = dataQuality.getErrors("NonExistent:Type", "10").toString
    html should include("No errors found")
  }

  it should "respect limit" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")
    (1 to 50).foreach(i => monitor.logInvalidData(s"Page$i", "Invalid data error"))

    val html = dataQuality.getErrors("TestExtractor:InvalidData", "10").toString
    html should include("Showing")
  }

  "CSV export" should "export errors" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")
    monitor.logInvalidData("Einstein", "Invalid IRI")

    val csv = dataQuality.exportCsv("TestExtractor:InvalidData", "100")
    csv should include("Extractor,PageTitle,ErrorMessage")
    csv should include("Einstein")
  }

  it should "escape commas" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")
    monitor.logInvalidData("Page1", "Invalid data with, comma, in message")

    val csv = dataQuality.exportCsv("TestExtractor:InvalidData", "100")
    csv should include("Invalid data with; comma; in message")
  }

  it should "respect export limit" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")
    (1 to 200).foreach(i => monitor.logInvalidData(s"Page$i", "Test error"))

    val csv = dataQuality.exportCsv("TestExtractor:InvalidData", "50")
    csv.split("\n").length should be <= 51
  }

  "Integration" should "work with multiple extractors" in {
    val homepage = DataQualityMonitor.forExtractor("HomepageExtractor")
    val abstracts = DataQualityMonitor.forExtractor("AbstractExtractor")
    val infobox = DataQualityMonitor.forExtractor("InfoboxExtractor")

    homepage.logInvalidData("Einstein", "Invalid homepage URL")
    homepage.logInvalidData("Tesla", "Invalid homepage IRI")
    abstracts.logInvalidData("Newton", "Missing abstract")
    infobox.logInvalidData("Curie", "Invalid property value")
    infobox.logInvalidData("Darwin", "Invalid property value")

    val page = dataQuality.get.toString
    page should include("HomepageExtractor")
    page should include("AbstractExtractor")

    dataQuality.getMetricsJson should include("\"totalErrors\": 5")

    val csv = dataQuality.exportCsv("HomepageExtractor:InvalidData", "100")
    csv should include("Einstein")
    csv should include("Tesla")
  }

  it should "handle concurrent access" in {
    val monitor = DataQualityMonitor.forExtractor("ConcurrentTest")

    val threads = (1 to 5).map { i =>
      new Thread {
        override def run(): Unit = {
          for (j <- 1 to 20) {
            monitor.logInvalidData(s"Page_${i}_$j", s"Error from thread $i")
            if (j % 5 == 0) dataQuality.getMetricsJson
          }
        }
      }
    }

    threads.foreach(_.start())
    threads.foreach(_.join())

    dataQuality.getMetricsJson should include("\"totalErrors\": 100")
  }

  it should "accumulate errors" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")

    monitor.logInvalidData("Page1", "Error 1")
    dataQuality.getMetricsJson should include("\"totalErrors\": 1")

    monitor.logInvalidData("Page2", "Error 2")
    monitor.logInvalidData("Page3", "Error 3")
    dataQuality.getMetricsJson should include("\"totalErrors\": 3")
  }
}
