package org.dbpedia.extraction.scripts

import scala.Console.err
import java.lang.System
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.http.{HttpMethod, HttpStatus}
import org.json4s._
import org.json4s.jackson.JsonMethods._


case class AlbumExtractionResult(filter: String, result: List[String])

/**
 * Emits album information for a wikipedia page as triples
 *
 * Example call:
 * ../run JsonpediaAlbumExtractor "en:Queen_(band)"
 */
object JsonpediaAlbumExtractor {

  private val httpClient = new HttpClient()
  httpClient.start()
  implicit val formats = DefaultFormats

  /**
   * Splits a wikipedia article in language and page (if possible).
   * e.g. en:Dog -> ("en", "Dog")
   */
  private def extractAlbumFrom(wikiPage: String): Seq[String] = {
    val response = httpClient.newRequest(s"http://json.it.dbpedia.org/annotate/resource/json/$wikiPage")
      .method(HttpMethod.GET)
      .param("filter", "__type:section,title:Discography>__type:list_item>label")
      .param("procs", "Extractors,Structure")
      .send()

    response.getStatus match {
      case HttpStatus.OK_200 => parse(response.getContentAsString).extract[AlbumExtractionResult].result.map(_.trim().replace(' ', '_'))
      case _ => throw new RuntimeException("error getting response from dbpedia, body was: " + response.getContentAsString)
    }
  }

  private def splitArticle(article: String): (String, String) = article.split(":") match {
      case Array(a,b) => (a, b)
      case _ => throw new IllegalArgumentException("invalid article string")
  }

  private def emitTriples(lang:String, page: String, albums: Seq[String]) = {
    albums.map(
      album => lang match {
        case "en" => s"<http://dbpedia.org/resource/$page> <http://dbpedia.org/property/discography> <http://dbpedia.org/resource/$album> ."
        case _ => s"<http://$lang.dbpedia.org/resource/$page> <http://dbpedia.org/property/discography> <http://$lang.dbpedia.org/resource/$album> ."
      }
    ).foreach(println)
  }

  def main(args: Array[String]): Unit = {
    require(args != null && args.length == 1,
      "need 1 argument: wikipedia page to process (e.g. 'en:Queen_(band)')"
    )
    val wikiPage = args(0)

    try {
      val (lang, page) = splitArticle(wikiPage)
      val albums = extractAlbumFrom(wikiPage)
      emitTriples(lang, page, albums)
      httpClient.stop()
    } catch {
      case ia: IllegalArgumentException => {
        err.println(ia.getMessage)
        System.exit(1)
      }
      case e: RuntimeException => {
        err.println(e.getMessage)
        System.exit(2)
      }
    }
  }
}

