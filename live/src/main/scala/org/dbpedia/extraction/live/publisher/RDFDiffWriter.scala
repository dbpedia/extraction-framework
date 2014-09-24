package org.dbpedia.extraction.live.publisher

import java.io._

import org.apache.log4j.Logger
import org.dbpedia.extraction.destinations.formatters.{TerseFormatter, UriPolicy}
import org.dbpedia.extraction.destinations.{Destination, Quad, WriterDestination}
import org.dbpedia.extraction.live.core.LiveOptions
import org.dbpedia.extraction.util.IOUtils
import org.dbpedia.extraction.util.RichFile._

import scala.collection.JavaConversions._

/**
 * Helper object that writes a set of Quads to a file,
 * ATM used for diff publishing
 *
 * @author Dimitris Kontokostas
 * @since 9/24/14 1:16 PM
 */

object RDFDiffWriter {

  private final val logger: Logger = Logger.getLogger(classOf[Publisher])


  /**
   * Writes a set of Quads in a file in TURTLE format. If zip == true it will be compressed with a .gz suffix
   * @param quads A Set og Quads to write
   * @param added Whether the model contains triples that should be added or removed
   * @param baseName  The base file name
   * @param zip   Whether the file should be compressed or not
   */
  def writeAsTurtle(quads: java.util.Set[Quad], added: Boolean, baseName: String, zip: Boolean) {

    val filename = baseName + (if (added)  ".added" else ".removed") + ".nt" + (if (zip) ".gz" else "")
    val destination: Destination = new WriterDestination(writer(new File(filename)), new TerseFormatter(false, true, policies))

    logger.info("Writing diff: " + filename)
    destination.open()
    destination.write(quads)
    destination.close()


  }

  val policies = {
    UriPolicy.parsePolicy(LiveOptions.options.get("uri-policy.main"))
  }


  private def writer(file: File): () => Writer = {
    () => IOUtils.writer(wrapFile(file))
  }
}

