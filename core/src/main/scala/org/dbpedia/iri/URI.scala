package org.dbpedia.iri

import java.net.URLDecoder

import org.apache.commons.lang3.StringEscapeUtils
import org.apache.jena.iri.IRIFactory
import org.dbpedia.extraction.util.{StringUtils, WikiUtil}

import scala.util.{Failure, Success, Try}

/**
  * Created by chile on 02.10.17.
  */
class URI private[iri](uri: org.apache.jena.iri.IRI) extends IRI(uri){


  def toIRI: IRI = UriUtils.uriToIri(this)
}

object URI{
  protected val uriFactory: IRIFactory = new IRIFactory()
  IRIFactory.setUriImplementation(uriFactory)
  uriFactory.allowUnwiseCharacters()


  def create(uri: String): Try[URI] = {
    Try {
      // unescape all \\u escaped characters
      val input = URLDecoder.decode(StringEscapeUtils.unescapeJava(uri), "UTF-8")

      // Here's the list of characters that we re-encode (see WikiUtil.iriReplacements):
      // "#%<>?[\]^`{|}

      // we re-encode backslashes and we currently can't decode Turtle, so we disallow it
      if (input.contains("\\"))
        throw new IllegalArgumentException("URI contains backslash: [" + input + "]")
      input
      //StringUtils.escape(uri, WikiUtil.iriReplacements)
    } match{
      case Failure(f) => f match {
          case g: IllegalArgumentException => Failure(new IRISyntaxException(null, g))
          case ff => Failure(ff)
        }
      case Success(u) => IRI.prePublischValidation(new URI(URI.uriFactory.construct(u)), allowPathOnly = true).asInstanceOf[Try[URI]]
    }
  }

  def create(uri: URI): Try[URI] = IRI.prePublischValidation(new URI(URI.uriFactory.construct(uri))).asInstanceOf[Try[URI]]
  def create(uri: java.net.URI): Try[URI] = IRI.prePublischValidation(new URI(URI.uriFactory.construct(uri))).asInstanceOf[Try[URI]]
  def createReference(uri: String): Try[URI] = IRI.prePublischValidation(new URI(URI.uriFactory.construct(uri)), allowPathOnly = true).asInstanceOf[Try[URI]]
  def createReference(uri: URI): Try[URI] = IRI.prePublischValidation(new URI(URI.uriFactory.construct(uri)), allowPathOnly = true).asInstanceOf[Try[URI]]
  def createReference(uri: java.net.URI): Try[URI] = IRI.prePublischValidation(new URI(URI.uriFactory.construct(uri)), allowPathOnly = true).asInstanceOf[Try[URI]]
}