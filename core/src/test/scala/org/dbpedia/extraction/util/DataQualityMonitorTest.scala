package org.dbpedia.extraction.util

import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

/**
 * Test suite for DataQualityMonitor
 */
class DataQualityMonitorTest extends FlatSpec with Matchers with BeforeAndAfter {

  before {
    // Reset metrics before each test
    DataQualityMonitor.reset()
  }

  "DataQualityMonitor" should "create a monitor for an extractor" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")
    monitor should not be null
    monitor.extractorName should be("TestExtractor")
  }

  it should "record invalid data errors" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")

    monitor.logInvalidData(
      "Albert_Einstein",
      "Invalid IRI syntax",
      Some(new IllegalArgumentException("Test exception")),
      Some("http://malformed url")
    )

    val metrics = monitor.getMetrics()
    metrics should not be empty
    monitor.getTotalErrors() should be(1)
  }

  it should "categorize errors correctly" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")

    // Log different types of errors
    monitor.logInvalidData("Page1", "Invalid data", exception = Some(new IllegalArgumentException()))
    monitor.logInvalidData("Page2", "Malformed URL", exception = None)
    monitor.logInvalidData("Page3", "Missing property", exception = None)

    val metrics = monitor.getMetrics()
    metrics.size should be >= 1
    monitor.getTotalErrors() should be(3)
  }

  it should "track multiple extractors independently" in {
    val monitor1 = DataQualityMonitor.forExtractor("ExtractorA")
    val monitor2 = DataQualityMonitor.forExtractor("ExtractorB")

    monitor1.logInvalidData("Page1", "Error in A")
    monitor1.logInvalidData("Page2", "Another error in A")
    monitor2.logInvalidData("Page3", "Error in B")

    monitor1.getTotalErrors() should be(2)
    monitor2.getTotalErrors() should be(1)
  }

  it should "provide global metrics" in {
    val monitor1 = DataQualityMonitor.forExtractor("ExtractorA")
    val monitor2 = DataQualityMonitor.forExtractor("ExtractorB")

    monitor1.logInvalidData("Page1", "Error 1")
    monitor2.logInvalidData("Page2", "Error 2")
    monitor2.logInvalidData("Page3", "Error 3")

    val globalMetrics = DataQualityMonitor.getGlobalMetrics()
    globalMetrics.values.sum should be(3)
  }

  it should "retrieve error details" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")

    monitor.logInvalidData("Einstein", "Invalid IRI", data = Some("malformed://url"))
    monitor.logInvalidData("Tesla", "Invalid IRI", data = Some("another://bad"))

    val details = DataQualityMonitor.getErrorDetails("TestExtractor:InvalidData", limit = 10)
    details.size should be(2)
    details.head.extractorName should be("TestExtractor")
  }

  it should "export errors to CSV format" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")

    monitor.logInvalidData("Einstein", "Invalid IRI")
    monitor.logInvalidData("Tesla", "Malformed URL")

    val csv = DataQualityMonitor.exportToCsv("TestExtractor:InvalidData", limit = 100)

    csv should include("Extractor,PageTitle,ErrorMessage,Timestamp")
    csv should include("TestExtractor")
    csv should include("Einstein")
  }

  it should "limit stored error details to prevent memory issues" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")

    // Try to log more than the limit (10000)
    for (i <- 1 to 11000) {
      monitor.logInvalidData(s"Page$i", "Error")
    }

    // Should still work without memory issues
    val metrics = monitor.getMetrics()
    monitor.getTotalErrors() should be(11000)

    // But details should be limited
    val details = DataQualityMonitor.getErrorDetails("TestExtractor:InvalidData", limit = 20000)
    details.size should be <= 10000
  }

  it should "log skipped extractions" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")

    // Should not throw exception
    monitor.logSkipped("TestPage", "Not in main namespace")

    // Skipped pages don't count as errors
    monitor.getTotalErrors() should be(0)
  }

  it should "log successful extractions" in {
    val monitor = DataQualityMonitor.forExtractor("TestExtractor")

    // Should not throw exception
    monitor.logSuccess("Einstein", 5)

    // Successful extractions don't count as errors
    monitor.getTotalErrors() should be(0)
  }

  it should "handle concurrent logging safely" in {
    val monitor = DataQualityMonitor.forExtractor("ConcurrentExtractor")

    // Simulate concurrent logging from multiple threads
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

    // Should have logged all errors without data corruption
    monitor.getTotalErrors() should be(1000)
  }

  "Error details" should "include all required fields" in {
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
