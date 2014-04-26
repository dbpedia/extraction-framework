package org.dbpedia.extraction.scripts

import scala.Console.err
import java.lang.System
import org.glassfish.jersey.client._
import net.liftweb.json.{parse, DefaultFormats}
import javax.ws.rs.core.MediaType

case class ImageExtractionInnerResult(url: String, description: String, section_idx: Int)
case class ImageExtractionResult(filter: String, result: List[ImageExtractionInnerResult])

/**
 * Emits picture information for a wikipedia page as triples.
 *
 * Example call:
 * ../run JsonpediaImageExtractor "en:Dog"
 */
object JsonpediaImageExtractor {
  private val client = JerseyClientBuilder.createClient()
  implicit val formats = DefaultFormats

  private def extractImagesFrom(wikiPage: String): Seq[String] = {
    val response = client.target(s"http://json.it.dbpedia.org/annotate/resource/json/$wikiPage")
      .queryParam("filter", "url:.*jpg")
      .queryParam("procs", "Extractors,Structure")
      .request(MediaType.APPLICATION_JSON_TYPE)
      .get()

    val responseBody = response.readEntity(classOf[String])

    response.getStatus match {
      case 200 => parse(responseBody).extract[ImageExtractionResult].result.map(_.url)
      case _ => throw new RuntimeException("error getting response from dbpedia, body was: " + responseBody)
    }
  }

  /**
   * Splits a wikipedia article in language and page (if possible).
   * e.g. en:Dog -> ("en", "Dog")
   */
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
    } catch {
      case ia: IllegalArgumentException =>
        err.println(ia.getMessage)
        System.exit(1)
      case e: RuntimeException =>
        err.println(e.getMessage)
        System.exit(2)
    }
  }
}

