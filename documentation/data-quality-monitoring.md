# Data Quality Monitoring

A utility for tracking extraction errors and data quality issues in DBpedia extractors.

## Why?

Many extractors silently drop invalid data without logging. This makes debugging hard and we have no visibility into what's failing. This monitor fixes that.

## Basic Usage

```scala
import org.dbpedia.extraction.util.DataQualityMonitor

// Create a monitor for your extractor
val monitor = DataQualityMonitor.forExtractor("MyExtractor")

// Log an error
monitor.logInvalidData(
  pageTitle = "Some_Page",
  reason = "Malformed URL",
  exception = Some(ex),
  data = Some(badUrl)
)

// Log success (optional)
monitor.logSuccess(pageTitle, triplesCount)

// Log skipped page (optional)
monitor.logSkipped(pageTitle, "Not in main namespace")
```

## Getting Metrics

```scala
// Metrics for one extractor
val metrics = monitor.getMetrics()
// => Map("MyExtractor:IRISyntaxException" -> 42, "MyExtractor:InvalidData" -> 15)

// Total errors
val total = monitor.getTotalErrors()

// Global metrics (all extractors)
val all = DataQualityMonitor.getGlobalMetrics()
```

## Error Details

```scala
// Get detailed error info
val errors = DataQualityMonitor.getErrorDetails("MyExtractor:IRISyntaxException", limit = 100)

errors.foreach { e =>
  println(s"${e.pageTitle}: ${e.message}")
}

// Export to CSV
val csv = DataQualityMonitor.exportToCsv("MyExtractor:IRISyntaxException", limit = 1000)
```

## Web Dashboard

When running the server, access the dashboard at:

```
http://localhost:9999/server/quality/
```

Endpoints:
- `/quality/` - Dashboard with error counts
- `/quality/metrics` - JSON metrics
- `/quality/errors?type=X&limit=10` - Error details
- `/quality/export?type=X` - CSV download

## Example: HomepageExtractor Integration

```scala
class HomepageExtractor(...) extends PageNodeExtractor {
  
  private val monitor = DataQualityMonitor.forExtractor("HomepageExtractor")

  override def extract(page: PageNode, subjectUri: String): Seq[Quad] = {
    if (page.title.namespace != Namespace.Main) {
      monitor.logSkipped(page.title.encoded, "Wrong namespace")
      return Seq.empty
    }
    // ... extraction logic
  }

  private def generateStatement(...): Seq[Quad] = {
    UriUtils.createURI(url) match {
      case Success(u) =>
        monitor.logSuccess(subjectUri, 1)
        Seq(new Quad(...))
      
      case Failure(ex: IRISyntaxException) =>
        monitor.logInvalidData(subjectUri, "Bad IRI", Some(ex), Some(url))
        Seq()
    }
  }
}
```

## Error Categorization

Errors are auto-categorized based on:
- Exception type (if provided): `IRISyntaxException`, `RuntimeException`, etc.
- Message keywords: "invalid" → `InvalidData`, "malformed" → `MalformedData`, "missing" → `MissingData`
- Default: `Other`

## Notes

- Thread-safe (uses `TrieMap` and `AtomicLong`)
- Memory bounded (max 10K errors per type)
- Minimal overhead
- Call `DataQualityMonitor.reset()` to clear metrics (useful for testing)

## Adding to Other Extractors

Find code like this:
```scala
case _ : SomeException => Seq()  // TODO: log
```

Replace with:
```scala
case ex: SomeException =>
  monitor.logInvalidData(page, "reason", Some(ex), Some(data))
  Seq()
```