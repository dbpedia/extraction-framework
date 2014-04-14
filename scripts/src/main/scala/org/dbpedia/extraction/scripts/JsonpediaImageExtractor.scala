package org.dbpedia.extraction.scripts

import scala.Console.err
import java.lang.System
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.http.{HttpMethod, HttpStatus}
import org.json4s._
import org.json4s.jackson.JsonMethods._

case class JsonpediaInnerResult(url: String, description: String, section_idx: Int)
case class JsonpediaResult(filter: String, result: List[JsonpediaInnerResult])

/**
 * Emits picture information for a wikipedia page as triples.
 *
 * Example call:
 * ../run JsonpediaImageExtractor "en:Dog"
 */
object JsonpediaImageExtractor {
  /**
   * Splits a wikipedia article in language and page (if possible).
   * e.g. en:Dog -> ("en", "Dog")
   */
  private val httpClient = new HttpClient()
  httpClient.start()
  implicit val formats = DefaultFormats

  private def extractImagesFrom(wikiPage: String): Seq[String] = {
    val response = httpClient.newRequest(s"http://json.it.dbpedia.org/annotate/resource/json/$wikiPage")
      .method(HttpMethod.GET)
      .param("filter", "url:.*jpg")
      .param("procs", "Extractors,Structure")
      .send()

    response.getStatus match {
      case HttpStatus.OK_200 => parse(response.getContentAsString).extract[JsonpediaResult].result.map(_.url)
      case _ => throw new RuntimeException("error getting response from dbpedia, body was: " + response.getContentAsString)
    }
  }

  private def splitArticle(article: String): (String, String) = article.split(":") match {
      case Array(a,b) => (a, b)
      case _ => throw new IllegalArgumentException("invalid article string")
  }

  private def emitTriples(lang:String, page: String, pictures: Seq[String]) = {
    pictures.map(
      picture => lang match {
        case "en" => s"<http://dbpedia.org/resource/$page> <http://xmlns.com/foaf/0.1/depiction> <$picture> ."
        case _ => s"<http://$lang.dbpedia.org/resource/$page> <http://xmlns.com/foaf/0.1/depiction> <$picture> ."
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
      val images = extractImagesFrom(wikiPage)
      emitTriples(lang, page, images)
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

