package org.dbpedia.extraction.live.export

import java.io.{File, Writer}
import java.util
import java.util.concurrent._
import scala.collection.JavaConversions._

import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.destinations.formatters.{TerseFormatter, UriPolicy}
import org.dbpedia.extraction.live.core.LiveOptions
import org.dbpedia.extraction.live.storage._
import org.dbpedia.extraction.util.RichFile._
import org.dbpedia.extraction.util.{IOUtils}
import org.slf4j.{Logger, LoggerFactory}


/**
 * Description
 *
 * @author Dimitris Kontokostas
 * @since 9/18/14 4:33 PM
 * Modified in 2015/06/17 by André Pereira
 */
class DumpExport(val filename: String, val threads: Integer) {
  val logger: Logger = LoggerFactory.getLogger(classOf[DumpExport])

  val destination: Destination = new WriterDestination(writer(new File(filename)), new TerseFormatter(false, true, policies))

  // Max threads in thread pool queu 4 x running threads
  val linkedBlockingDeque: BlockingQueue[Runnable] = new LinkedBlockingDeque[Runnable](threads * 4);
  val executorService: ExecutorService = new ThreadPoolExecutor(threads, threads, 30,
      TimeUnit.SECONDS, linkedBlockingDeque, new ThreadPoolExecutor.CallerRunsPolicy());

  def export() {
    destination.open()

    try {
      val all:util.List[String] = MongoUtil.getAll()

      for (json <- all.toList){
        executorService.execute(new QuadProcessWorker(destination,json))
      }
    } catch {
      case e: Exception => {
        logger.warn(e.getMessage(), e);
      }
    }
    finally {
      executorService.shutdown();
      try {
        executorService.awaitTermination(Long.MaxValue, TimeUnit.NANOSECONDS);
      } catch {
        case e : InterruptedException  => {
          logger.error("Error in thread termination", e)
        }
      }
      destination.close()
    }
  }

  val policies = {
    UriPolicy.parsePolicy(LiveOptions.options.get("uri-policy.main"))
  }


  private def writer(file: File): () => Writer = {
    () => IOUtils.writer(wrapFile(file))
  }


}

class QuadProcessWorker(val destination: Destination, val jsonString: String) extends Runnable {

  override def run(): Unit = {
    val quads = JSONCache.getTriplesFromJson(new String(jsonString))
    destination.write(quads)
  }
}

object DumpExport {


  def main(args: scala.Array[String]): Unit = {

    require(args != null && args.length == 2 && args(0).nonEmpty && args(1).nonEmpty, "missing required argument: $ {dump file name} {threads Number}")
    val threads: Int = Integer.parseInt(args(1))

    new DumpExport(args(0), threads).export()
  }
}
