package org.dbpedia.extraction.util

import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class DataQualityMonitorTest extends FlatSpec with Matchers with BeforeAndAfter {

  before {
    DataQualityMonitor.reset()
  }

  "DataQualityMonitor" should "create monitor" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")
    monitor should not be null
    monitor.extractorName should be("TestExtractor")
  }

  it should "record errors" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")
    monitor.logInvalidData(
      "Albert_Einstein",
      "Invalid IRI syntax",
      Some(new IllegalArgumentException("Test exception")),
      Some("http://malformed url")
    )

    monitor.getMetrics() should not be empty
    monitor.getTotalErrors() should be(1)
  }

  it should "categorize errors" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")

    monitor.logInvalidData("Page1", "Invalid data", exception = Some(new IllegalArgumentException()))
    monitor.logInvalidData("Page2", "Malformed URL", exception = None)
    monitor.logInvalidData("Page3", "Missing property", exception = None)

    monitor.getMetrics().size should be >= 1
    monitor.getTotalErrors() should be(3)
  }

  it should "track extractors independently" in {
    val m1 = DataQualityMonitor.forExtractor("ExtractorA")
    val m2 = DataQualityMonitor.forExtractor("ExtractorB")

    m1.logInvalidData("Page1", "Error in A")
    m1.logInvalidData("Page2", "Another error in A")
    m2.logInvalidData("Page3", "Error in B")

    m1.getTotalErrors() should be(2)
    m2.getTotalErrors() should be(1)
  }

  it should "provide global metrics" in {
    val m1 = DataQualityMonitor.forExtractor("ExtractorA")
    val m2 = DataQualityMonitor.forExtractor("ExtractorB")

    m1.logInvalidData("Page1", "Error 1")
    m2.logInvalidData("Page2", "Error 2")
    m2.logInvalidData("Page3", "Error 3")

    DataQualityMonitor.getGlobalMetrics().values.sum should be(3)
  }

  it should "retrieve error details" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")

    monitor.logInvalidData("Einstein", "Invalid IRI", data = Some("malformed://url"))
    monitor.logInvalidData("Tesla", "Invalid IRI", data = Some("another://bad"))

    val details = DataQualityMonitor.getErrorDetails("TestExtractor:InvalidData", limit = 10)
    details.size should be(2)
    details.head.extractorName should be("TestExtractor")
  }

  it should "export to CSV" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")

    monitor.logInvalidData("Einstein", "Invalid IRI")
    monitor.logInvalidData("Tesla", "Malformed URL")

    val csv = DataQualityMonitor.exportToCsv("TestExtractor:InvalidData", limit = 100)
    csv should include("Extractor,PageTitle,ErrorMessage")
    csv should include("Einstein")
  }

  it should "limit stored details" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")
    (1 to 11000).foreach(i => monitor.logInvalidData(s"Page$i", "Error"))

    monitor.getTotalErrors() should be(11000)
    val details = DataQualityMonitor.getErrorDetails("TestExtractor:InvalidData", limit = 20000)
    details.size should be <= 10000
  }

  it should "log skipped extractions" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")
    monitor.logSkipped("TestPage", "Not in main namespace")
    monitor.getTotalErrors() should be(0)
  }

  it should "log successful extractions" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")
    monitor.logSuccess("Einstein", 5)
    monitor.getTotalErrors() should be(0)
  }

  it should "handle concurrent logging" in {
    val monitor = DataQualityMonitor.forExtractor("ConcurrentExtractor")

    val threads = (1 to 10).map { i =>
      new Thread {
        override def run(): Unit = {
          for (j <- 1 to 100) {
            monitor.logInvalidData(s"Page_${i}_$j", s"Error from thread $i")
          }
        }
      }
    }

    threads.foreach(_.start())
    threads.foreach(_.join())
    monitor.getTotalErrors() should be(1000)
  }

  "Error details" should "include required fields" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")

    monitor.logInvalidData(
      "TestPage",
      "Test error message",
      Some(new IllegalArgumentException("Test")),
      Some("test data")
    )

    val details = DataQualityMonitor.getErrorDetails("TestExtractor:IllegalArgumentException", limit = 1)
    details should have size 1

    val error = details.head
    error.extractorName should be("TestExtractor")
    error.pageTitle should be("TestPage")
    error.message should be("Test error message")
    error.exceptionType should be(Some("IllegalArgumentException"))
    error.timestamp should be > 0L
  }
}
