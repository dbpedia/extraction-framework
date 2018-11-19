package org.dbpedia.utils.sse

import java.util
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.dbpedia.extraction.live.feeder.{EventStreamsFeeder, Feeder}
import org.dbpedia.extraction.live.queue.LiveQueueItem
import org.dbpedia.extraction.live.util.DateUtil
import scala.concurrent.Future
import scala.collection.JavaConversions._

//https://stream.wikimedia.org/v2/stream/recentchange

class EventStreamsHelper (wikilanguage: String, allowedNamespaces: util.ArrayList[Integer]) {

  def eventStreamsClient {

    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()

    import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._
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

    val addToQueueSink : Sink[LiveQueueItem, Future[Done]] =
      Sink.foreach[LiveQueueItem](EventStreamsFeeder.addQueueItemCollection(_))

    Http()
      .singleRequest(Get("https://stream.wikimedia.org/v2/stream/recentchange"))
      .flatMap(Unmarshal(_).to[Source[ServerSentEvent, NotUsed]])
      .map(source => source
        .via(flowData)
        .filter(data => filterNamespaceAndLanguage(data))
        .via(flowLiveQueueItem)
        .toMat(addToQueueSink)(Keep.right)
        .run())
     }


  def parseStringFromJson(data: String, key: String): String = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val parsedJson = mapper.readValue(data, classOf[Map[String, String]])
    val value = parsedJson.get(key)
    value.getOrElse("")
    //TODO decide on default value, consider title and language
  }

  def parseIntFromJson(data: String, key: String): Int = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val parsedJson = mapper.readValue(data, classOf[Map[String, Int]])
    parsedJson.get(key).getOrElse(-1)
    //TODO decide on default value, namespace and timestamp use this at the moment
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
    keep && language == wikilanguage + "wiki"
  }



}



