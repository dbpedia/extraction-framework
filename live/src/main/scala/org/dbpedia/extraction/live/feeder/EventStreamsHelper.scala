package org.dbpedia.extraction.live.feeder

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
import org.dbpedia.extraction.live.config.LiveOptions
import org.dbpedia.extraction.live.queue.LiveQueueItem
import org.dbpedia.extraction.live.util.DateUtil
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * This class is used in order to consume the Wikimedia EventStreamsAPI
  * that replaced the RCStream API during spring of 2018.
  * EventStreams follows the Server Sent Event (SSE) protocol.
  * Akka SSE offers the implementation of a SSE Client used here.
  * Akka Streams is used in order to process the stream data.
  * Processing means filtering by configured language and namespaces, and use
  * the wiki page title and timestamp in order to create LiveQueueItems.
  *
  * Documentation on the EventStreams API can be found here: https://wikitech.wikimedia.org/wiki/EventStreams
  * EventStreams is available here: https://stream.wikimedia.org/v2/stream/recentchange
  * Documentation on the Akka SSE implementation: https://doc.akka.io/docs/akka-http/current/sse-support.html
  * The schema of the EventStreams data can be found here: https://github.com/wikimedia/mediawiki-event-schemas/blob/master/jsonschema/mediawiki/recentchange/2.yaml
  *
  * @author Lena Schindler, November 2018
  */


class EventStreamsHelper(val since: String) extends EventStreamUnmarshalling {

  private val logger = LoggerFactory.getLogger("EventStreamsHelper")

  private val baseURL = LiveOptions.options.get("feeder.eventstreams.baseURL")
  private val stream = LiveOptions.options.get("feeder.eventstreams.streams").split("\\s*,\\s*").toList
  private val allowedNamespaces: List[Int] = LiveOptions.options.get("feeder.eventstreams.allowedNamespaces").split("\\s*,\\s*").toList.map((s: String) => s.toInt)
  private val wikilanguages = LiveOptions.languages
  private val minBackoffFactor = LiveOptions.options.get("feeder.eventstreams.minBackoffFactor").toInt.second
  private val maxBackoffFactor = LiveOptions.options.get("feeder.eventstreams.maxBackoffFactor").toInt.second

  private val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  // maxLineSize and maxEventSize belong to the trait EventStreamUnmarshalling
  // and the defaults of  4096 and  8192 respectively are to small for us
  override protected def maxLineSize: Int = LiveOptions.options.get("feeder.eventstreams.maxLineSize").toInt

  override protected def maxEventSize: Int = LiveOptions.options.get("feeder.eventstreams.maxEventSize").toInt

  /**
    * Defines and starts an akka graph that consumes the Wikimedia EventStream
    *
    */

  def eventStreamsClient {

    implicit val system = ActorSystem("EventStreamsActorSystem")
    implicit val mat = ActorMaterializer()

    import system.dispatcher

    val flowData: Flow[ServerSentEvent, String, NotUsed] = Flow.fromFunction(_.getData())

    val flowLiveQueueItem: Flow[String, LiveQueueItem, NotUsed] = Flow.fromFunction(eventData =>
      new LiveQueueItem(
        //language
        parseStringFromJson(eventData, "wiki").replace("wiki", ""), //TODO implement multilanguage
        //itemID
        -1,
        //Title
        parseStringFromJson(eventData, "title"),
        //timestamp
        DateUtil.transformUnixTimestampToUTC(parseIntFromJson(eventData, "timestamp")),
        //deleted
        false,
        //xml
        ""))

    val sinkAddToQueue: Sink[LiveQueueItem, Future[Done]] =
      Sink.foreach[LiveQueueItem](EventStreamsFeeder.addQueueItemToBuffer(_))


    val sseSource: Source[ServerSentEvent, NotUsed] = RestartSource.onFailuresWithBackoff(
      minBackoff = minBackoffFactor,
      maxBackoff = maxBackoffFactor,
      randomFactor = 0.2
    ) { () =>
      Source.fromFutureSource {
        Http().singleRequest(
          //HttpRequest(uri = baseURL + stream.head + "?since=" + since))
          HttpRequest(uri = baseURL + stream.head))
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

  /**
    * Takes a JSON String and returns true, if namespace and language matches, false otherwise
    * See the schema of the JSON at https://github.com/wikimedia/mediawiki-event-schemas/blob/master/jsonschema/mediawiki/recentchange/2.yaml
    *
    * @param data a JSON String
    * @return boolean: match the configured namespace and language
    */
  def filterNamespaceAndLanguage(data: String): Boolean = {
    var keep = false

    //the EventStreams API uses the postfix "wiki" for the language, e.g. "enwiki" for languag "en"
    val namespace = parseIntFromJson(data, "namespace")
    val language = parseStringFromJson(data, "wiki").replace("wiki", "")
    val timestamp: Int = parseIntFromJson(data, "timestamp")

    allowedNamespaces.contains(namespace) && (timestamp != -1) && wikilanguages.contains(language)

    /*for(nspc <- allowedNamespaces){
      if (nspc == namespace ){
        keep = true
      }
    }
    if (parseIntFromJson(data, "timestamp") == -1){
      keep = false
    }
    keep && wikilanguages.contains(language)*/
  }


  def parseStringFromJson(data: String, key: String): String = {
    mapper.readValue(data, classOf[Map[String, String]]).getOrElse(key, "")
  }

  def parseIntFromJson(data: String, key: String): Int = {
    mapper.readValue(data, classOf[Map[String, Int]]).getOrElse(key, -1)
  }

}

//println("eventstreamshelper: wikilanguages:  " + wikilanguages.toString + ", language: " + language)

