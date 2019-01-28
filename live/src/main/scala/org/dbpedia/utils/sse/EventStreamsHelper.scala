package org.dbpedia.utils.sse

import java.util

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.javadsl.model.RequestEntity
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling
import akka.stream.{ActorMaterializer, Attributes, scaladsl}
import akka.stream.scaladsl._
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.log4j.Logger
import org.dbpedia.extraction.live.core.LiveOptions
import org.dbpedia.extraction.live.feeder.{EventStreamsFeeder, Feeder}
import org.dbpedia.extraction.live.queue.LiveQueueItem
import org.dbpedia.extraction.live.util.DateUtil
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.collection.JavaConversions._
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
  * @param wikilanguage as configured
  * @param allowedNamespaces as configured. Sharing between Java and Scala is made possible through scala.collection.JavaConversions._
  * @author Lena Schindler, November 2018
  */


class EventStreamsHelper (wikilanguage: String, allowedNamespaces: util.ArrayList[Integer], streams : util.ArrayList[String]) extends  EventStreamUnmarshalling {

  private val logger = Logger.getLogger("EventstreamsHelper")


  override protected def maxLineSize: Int = LiveOptions.options.get("feeder.eventstreams.maxLineSize").toInt

  override protected def maxEventSize: Int = LiveOptions.options.get("feeder.eventstreams.maxEventSize").toInt


  def eventStreamsClient {

    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()

    import system.dispatcher

    val flowData: Flow[ServerSentEvent, String, NotUsed] = Flow.fromFunction(_.getData())
    val flowLiveQueueItem: Flow[String, LiveQueueItem, NotUsed] = Flow.fromFunction(
      eventData => new LiveQueueItem(
        -1,
        parseStringFromJson(eventData, "title"),
        DateUtil.transformToUTC(parseIntFromJson(eventData, "timestamp")),
        false,
        ""
      ))

    val addToQueueSink: Sink[LiveQueueItem, Future[Done]] =
      Sink.foreach[LiveQueueItem](EventStreamsFeeder.addQueueItemCollection(_))


    for (stream <- streams) {
      val sseSource = RestartSource.onFailuresWithBackoff(
        minBackoff = 5.second,
        maxBackoff = 60.second,
        randomFactor = 0.2
      ) { () =>
        Source.fromFutureSource {
          Http().singleRequest(
              HttpRequest(uri = "https://stream.wikimedia.org/v2/stream/" + stream)
            )
            .flatMap(Unmarshal(_).to[Source[ServerSentEvent, NotUsed]])
        }
      }

      sseSource
        .via(flowData).log("dataFlow")
        .filter(data => filterNamespaceAndLanguage(data)).log("filter")
        .via(flowLiveQueueItem).log("livequeueItemFlow")
        .toMat(addToQueueSink)(Keep.right)
        .run()
      }
  }



  def parseStringFromJson(data: String, key: String): String = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val parsedJson = mapper.readValue(data, classOf[Map[String, String]])
    val value = parsedJson.get(key)
    value.getOrElse("")
  }

  def parseIntFromJson(data: String, key: String): Int = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val parsedJson = mapper.readValue(data, classOf[Map[String, Int]])
    parsedJson.get(key).getOrElse(-1)
  }

  //TODO refactor duplication


  def filterNamespaceAndLanguage(data: String): Boolean = {
    var keep = false
    val namespace = parseIntFromJson(data, "namespace")
    val language = parseStringFromJson(data, "wiki")   //the EventStreams API uses this postfix for the language, e.g. "enwiki" for languag "en"
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



}



