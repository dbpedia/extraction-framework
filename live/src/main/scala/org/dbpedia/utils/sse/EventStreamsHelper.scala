package org.dbpedia.utils.sse


import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.dbpedia.extraction.live.core.LiveOptions
import org.dbpedia.extraction.live.feeder.EventStreamsFeeder
import org.dbpedia.extraction.live.queue.LiveQueueItem
import org.dbpedia.extraction.live.util.DateUtil
import org.slf4j.LoggerFactory
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * This class is used in order to consume the Wikimedia EventStreamsAPI that replaced the RCStream API during spring of 2018.
  * EventStreams follows the Server Sent Event (SSE) protocol.
  * Akka SSE offers the implementation of a SSE Client used here.
  * Akka Streams is used in order to process the stream data.
  * Processing means filtering by configured language and namespaces, and use the wiki page title and timestamp in order to create LiveQueueItems.
  *
  * Documentation on the EventStreams API can be found here: https://wikitech.wikimedia.org/wiki/EventStreams
  * EventStreams is available here: https://stream.wikimedia.org/v2/stream/recentchange
  * Documentation on the Akka SSE implementation: https://doc.akka.io/docs/akka-http/current/sse-support.html
  * The schema of the EventStreams data can be found here: https://github.com/wikimedia/mediawiki-event-schemas/blob/master/jsonschema/mediawiki/recentchange/2.yaml
  *
  * @author Lena Schindler, November 2018
  */


class EventStreamsHelper () extends  EventStreamUnmarshalling {

  private val logger = LoggerFactory.getLogger("EventStreamsHelper")

  private val baseURL = LiveOptions.options.get("eventstreams.baseURL")
  private val streams = LiveOptions.options.get("eventstreams.streams").split("\\s*,\\s*").toList
  private val allowedNamespaces  = LiveOptions.options.get("feeder.eventstreams.allowedNamespaces").split("\\s*,\\s*").toList.map((s:String )=> s.toInt)
  private val wikilanguage = LiveOptions.options.get("language")
  private val minBackoffFactor = LiveOptions.options.get("eventstreams.minBackoffFactor").toInt.second
  private val maxBackoffFactor = LiveOptions.options.get("eventstreams.maxBackoffFactor").toInt.second

  private val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  override protected def maxLineSize: Int = LiveOptions.options.get("feeder.eventstreams.maxLineSize").toInt
  override protected def maxEventSize: Int = LiveOptions.options.get("feeder.eventstreams.maxEventSize").toInt


  def eventStreamsClient {

    implicit val system = ActorSystem("EventStreamsActorSystem")
    implicit val mat = ActorMaterializer()

    import system.dispatcher

    val flowData: Flow[ServerSentEvent, String, NotUsed] = Flow.fromFunction(_.getData())
    val flowLiveQueueItem: Flow[String, LiveQueueItem, NotUsed] = Flow.fromFunction(eventData =>
      new LiveQueueItem(
        -1,
        parseStringFromJson(eventData, "title"),
        DateUtil.transformToUTC(parseIntFromJson(eventData, "timestamp")),
        false,
        ""))
    val sinkAddToQueue: Sink[LiveQueueItem, Future[Done]] =
      Sink.foreach[LiveQueueItem](EventStreamsFeeder.addQueueItemCollection(_))


    for (stream <- streams) {
      val sseSource = RestartSource.onFailuresWithBackoff(
        minBackoff = minBackoffFactor,
        maxBackoff = maxBackoffFactor,
        randomFactor = 0.2
      ) { () =>
        Source.fromFutureSource {
          Http().singleRequest(
            HttpRequest(uri = baseURL + stream))
            .flatMap(event => Unmarshal(event).to[Source[ServerSentEvent, NotUsed]])
        }
      }

      sseSource
        .via(flowData).log("dataFlow")
        .filter(data => filterNamespaceAndLanguage(data)).log("filter")
        .via(flowLiveQueueItem).log("livequeueItemFlow")
        .toMat(sinkAddToQueue)(Keep.right)
        .run()
      }
  }

  def filterNamespaceAndLanguage(data: String): Boolean = {
    var keep = false

    //the EventStreams API uses the postfix "wiki" for the language, e.g. "enwiki" for languag "en"
    val namespace = parseIntFromJson(data, "namespace")
    val language = parseStringFromJson(data, "wiki")

    for(nspc <- allowedNamespaces){
      if (nspc == namespace ){
        keep = true
      }
    }
    if (parseIntFromJson(data, "timestamp") == -1){
      keep = false
    }
    keep && language == wikilanguage + "wiki"
  }

  
  def parseStringFromJson(data: String, key: String): String = {
    mapper.readValue(data, classOf[Map[String, String]]).getOrElse(key, "")
  }

  def parseIntFromJson(data: String, key: String): Int = {
    mapper.readValue(data, classOf[Map[String, Int]]).getOrElse(key, -1)
  }
  
}



