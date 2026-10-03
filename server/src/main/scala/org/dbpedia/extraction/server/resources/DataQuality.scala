package org.dbpedia.extraction.server.resources

import javax.ws.rs.{GET, Path, PathParam, Produces, QueryParam}
import org.dbpedia.extraction.util.DataQualityMonitor
import scala.xml.Elem

/**
 * REST resource for viewing Data Quality Monitor metrics
 *
 * Endpoints:
 * - /quality/ - Overview of all metrics
 * - /quality/metrics - JSON format metrics
 * - /quality/errors/{errorType} - Detailed errors for specific type
 * - /quality/export/{errorType} - CSV export
 */
@Path("/quality/")
class DataQuality {

  /**
   * Main page showing all quality metrics
   */
  @GET
  @Produces(Array("application/xhtml+xml"))
  def get: Elem = {
    val globalMetrics = DataQualityMonitor.getGlobalMetrics()

    // Group metrics by extractor
    val metricsByExtractor = globalMetrics.groupBy { case (key, _) =>
      key.split(":").headOption.getOrElse("Unknown")
    }

    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
      {ServerHeader.getHeader("Data Quality Monitoring", true)}
      <body>
        <div class="container">
          <h2>Data Quality Monitoring</h2>
          <p class="lead">Real-time extraction quality metrics and error tracking</p>

          <div class="panel panel-default">
            <div class="panel-heading">
              <h3 class="panel-title">Summary</h3>
            </div>
            <div class="panel-body">
              <p><strong>Total Error Types:</strong> {globalMetrics.size}</p>
              <p><strong>Total Errors:</strong> {globalMetrics.values.sum}</p>
              <p><strong>Extractors Monitored:</strong> {metricsByExtractor.size}</p>
            </div>
          </div>

          {
            if (metricsByExtractor.isEmpty) {
              <div class="alert alert-info">
                <strong>No errors logged yet.</strong> The monitor is running but hasn't recorded any errors.
                Extract some pages to see metrics appear here.
              </div>
            } else {
              for ((extractor, metrics) <- metricsByExtractor.toSeq.sortBy(_._1)) yield {
                val totalErrors = metrics.values.sum
                <div class="panel panel-primary">
                  <div class="panel-heading">
                    <h4 class="panel-title">{extractor}</h4>
                  </div>
                  <div class="panel-body">
                    <p><strong>Total Errors:</strong> {totalErrors}</p>
                    <table class="table table-striped table-hover myTable">
                      <thead>
                        <tr>
                          <th>Error Type</th>
                          <th>Count</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {
                          for ((errorType, count) <- metrics.toSeq.sortBy(-_._2)) yield {
                            <tr>
                              <td>{errorType.split(":").lastOption.getOrElse(errorType)}</td>
                              <td><span class="badge">{count}</span></td>
                              <td>
                                <a href={s"errors?type=$errorType&limit=10"} class="btn btn-sm btn-info">View Details</a>
                                {" "}
                                <a href={s"export?type=$errorType&limit=100"} class="btn btn-sm btn-success">Export CSV</a>
                              </td>
                            </tr>
                          }
                        }
                      </tbody>
                    </table>
                  </div>
                </div>
              }
            }
          }

          <div class="panel panel-info">
            <div class="panel-heading">
              <h4 class="panel-title">API Endpoints</h4>
            </div>
            <div class="panel-body">
              <ul>
                <li><code>GET /quality/</code> - This page</li>
                <li><code>GET /quality/metrics</code> - JSON format metrics</li>
                <li><code>GET /quality/errors?type=ErrorType&amp;limit=10</code> - Error details</li>
                <li><code>GET /quality/export?type=ErrorType&amp;limit=1000</code> - CSV export</li>
              </ul>
            </div>
          </div>
        </div>
      </body>
    </html>
  }

  /**
   * Get metrics in JSON format
   */
  @GET
  @Path("metrics")
  @Produces(Array("application/json"))
  def getMetricsJson: String = {
    val metrics = DataQualityMonitor.getGlobalMetrics()

    val json = new StringBuilder()
    json.append("{\n")
    json.append(s"""  "totalErrors": ${metrics.values.sum},\n""")
    json.append(s"""  "errorTypes": ${metrics.size},\n""")
    json.append("""  "metrics": {""").append("\n")

    val entries = metrics.toSeq
    entries.zipWithIndex.foreach { case ((errorType, count), idx) =>
      json.append(s"""    "$errorType": $count""")
      if (idx < entries.size - 1) json.append(",")
      json.append("\n")
    }

    json.append("  }\n")
    json.append("}")
    json.toString()
  }

  /**
   * Get detailed errors for a specific error type
   */
  @GET
  @Path("errors")
  @Produces(Array("application/xhtml+xml"))
  def getErrors(
    @QueryParam("type") errorType: String,
    @QueryParam("limit") limitParam: String
  ): Elem = {
    val limit = Option(limitParam).map(_.toInt).getOrElse(50)
    val errors = DataQualityMonitor.getErrorDetails(errorType, limit)

    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
      {ServerHeader.getHeader(s"Error Details: $errorType", true)}
      <body>
        <div class="container">
          <h2>Error Details</h2>
          <p class="lead">{errorType}</p>

          <p>
            <a href="/quality/" class="btn btn-default">&larr; Back to Overview</a>
            {" "}
            <a href={s"../export?type=$errorType&limit=1000"} class="btn btn-success">Export to CSV</a>
          </p>

          {
            if (errors.isEmpty) {
              <div class="alert alert-warning">
                <strong>No errors found</strong> for type: {errorType}
              </div>
            } else {
              <div>
                <p><strong>Showing {errors.size} most recent errors</strong></p>
                <table class="table table-striped table-hover myTable">
                  <thead>
                    <tr>
                      <th>Page Title</th>
                      <th>Error Message</th>
                      <th>Exception</th>
                      <th>Timestamp</th>
                    </tr>
                  </thead>
                  <tbody>
                    {
                      for (error <- errors) yield {
                        val timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                          .format(new java.util.Date(error.timestamp))
                        <tr>
                          <td><code>{error.pageTitle}</code></td>
                          <td>{error.message}</td>
                          <td>{error.exceptionType.getOrElse("-")}</td>
                          <td>{timestamp}</td>
                        </tr>
                      }
                    }
                  </tbody>
                </table>
              </div>
            }
          }
        </div>
      </body>
    </html>
  }

  /**
   * Export errors to CSV format
   */
  @GET
  @Path("export")
  @Produces(Array("text/csv"))
  def exportCsv(
    @QueryParam("type") errorType: String,
    @QueryParam("limit") limitParam: String
  ): String = {
    val limit = Option(limitParam).map(_.toInt).getOrElse(1000)
    DataQualityMonitor.exportToCsv(errorType, limit)
  }
}
