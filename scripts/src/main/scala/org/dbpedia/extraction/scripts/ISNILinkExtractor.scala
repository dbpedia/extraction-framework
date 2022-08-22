package org.dbpedia.extraction.scripts

import java.io.{File, FileWriter}

import org.dbpedia.extraction.destinations.{DestinationUtils, WriterDestination}
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{IOUtils, RichFile}
import org.dbpedia.iri.{IRI, URI}
import org.jsoup.Jsoup

import scala.collection.mutable.ListBuffer
import scala.collection.convert.decorateAsScala._
import scala.util.{Failure, Success}
import scala.util.matching.Regex

/**
  * Created by chile on 05.10.17.
  */
object ISNILinkExtractor {

  private val breakMsg = "break!"
  private val isniBaseUri = "http://www.isni.org/isni/"
  private var destination: WriterDestination = _
  private var errorIds: FileWriter = _

  private val idQuery = ".right5 > input[value]"
  private val dataError = "span + span.left5"
  private val linksQuery = "a.link_gen[href]"
  private val linkUri = "http://www.w3.org/2002/07/owl#sameAs"
  private val baseUriMap = Map[String, (Regex, String)](
    "http://viaf.org"           -> ("^".r, ""),
    "http://catalogue.bnf.fr"   -> ("^".r, ""),
    "http://d-nb.info"          -> ("^".r, ""),
    "http://www.worldcat.org"   -> ("^".r, ""),
    "http://catalogo.bne.es"    -> ("http://catalogo\\.bne\\.es/.*id=(.*)".r, "http://datos.bne.es/resource/$1"),
    "http://id.loc.gov"         -> ("^".r, ""),
    "http://libris.kb.se"       -> ("^".r, ""),
    "http://opac.sbn.it"        -> ("^".r, ""),
    "http://www.getty.edu"      -> ("http://www\\.getty\\.edu/.*subjectid=(.*)".r, "http://vocab.getty.edu/ulan/$1"),  //TODO not sure if /ulan/ is always the case
    "http://www.idref.fr"       -> ("^".r, "")
  )

  private def parseAndEvaluateDocument(html: String): List[SourceLink] ={
    val list = new ListBuffer[SourceLink]()
    val doc = Jsoup.parse(html.replaceAll("\n", ""))

    val id = doc.select(idQuery).asScala.head.attr("value").substring(4)
    val error= doc.select(dataError)
    if(error.isEmpty || !error.text().contains("data limit")){
      val links = Jsoup.parse(html.replaceAll("\n", "")).select(linksQuery).asScala
      for(node <- links){
        URI.create(node.attr("href")) match {
          case Success(link) =>
            baseUriMap.get(link.getScheme + "://" + link.getAuthority) match {
              case Some(regex) =>
                val target = regex._1.replaceAllIn(link.toString, regex._2)
                list.append(SourceLink(id, error = false, target))
              case None =>
            }
          case Failure(f) =>
        }
      }
    }
    else
      list.append(SourceLink(id, error = true, null))

    list.toList
  }

  private def readRawApiResultFile(filename: String): Unit ={
    var sb = new StringBuilder()
    var inBody = false
    IOUtils.readLines(new RichFile(new File(filename))){ line: String =>
      if(inBody){
        sb.append(line + "\n")
        if(line.trim.startsWith("</body>")){
          inBody = false
          for(link <- parseAndEvaluateDocument(sb.toString())){
            if(link.error)
              errorIds.write("\"" + link.id + "\"\n")
            else
              destination.write(Some(new Quad(null.asInstanceOf[String], null, isniBaseUri + link.id, linkUri, link.link.toString, null, null)))
          }
        }
      }
      else{
        if(line != null && line.trim.startsWith("<body")){
          inBody = true
          sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n")
          sb.append(line)
        }
      }
    }
  }

  def main(args: Array[String]): Unit = {

    val name = if(args.head.trim.endsWith(".bz2")) args.head else args.head + ".bz2"
    val file = new RichFile(new File(name))
    destination = DestinationUtils.createWriterDestination(file)
    val of = new File(name.replaceAll("\\.\\w+.bz2", ".csv"))
    errorIds = new FileWriter(of)
    destination.open()
    for (file <- args.drop(1)) {
      System.out.println("reading file: " + file)
      readRawApiResultFile(file)
    }
    destination.close()
    errorIds.close()
  }

  case class SourceLink(id: String, error: Boolean, link: String)
}
