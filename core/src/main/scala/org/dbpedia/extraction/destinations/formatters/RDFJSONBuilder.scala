package org.dbpedia.extraction.destinations.formatters

import scala.util.parsing.json.JSONFormat
import UriPolicy._

/*
* Serializes a single quad to JDF/JSON
* */
class RDFJSONBuilder(policies: Array[Policy] = null)
  extends UriTripleBuilder(policies) {

  // Scala's StringBuilder doesn't have appendCodePoint
  private val sb = new java.lang.StringBuilder

  override def start(context: String): Unit = {
    /* nothing to do */
  }

  override def subjectUri(subj: String): Unit = {
    this add "{" generateUri(subj, SUBJECT) add " : "
  }

  override def predicateUri(pred: String): Unit = {
    this add "{ " generateUri(pred, SUBJECT)
  }

  override def objectUri(obj: String): Unit = {
    this add " : [ { \"value\" : " generateUri(obj, SUBJECT) add ", \"type\" : \"uri\" } ]"
  }

  override def uri(uri: String, pos: Int): Unit = {
    // Do nothing
  }

  override def plainLiteral(value: String, lang: String): Unit = {
    this add ": [ { \"value\" : \"" escape value add "\", "
    this add "\"type\" : \"literal\""
    if (lang != null)
      this add ", \"lang\" : \"" add lang add "\""
    this add " } ] "
  }

  override def typedLiteral(value: String, datatype: String): Unit = {
    this add ": [ { \"value\" : \"" escape value add "\", "
    this add "\"type\" : \"literal\", "
    this add "\"datatype\" : \"" escape datatype add "\""
    this add " } ] "
  }

  def generateUri(str: String, pos: Int): RDFJSONBuilder = {
    this add '"' escape (parseUri(str, pos)) add '"'
    this
  }

  override def end(context: String): Unit = {
    this add " } },\n"
  }

  override def result = sb.toString

  private def add(s: String): RDFJSONBuilder = {
    sb append s
    this
  }

  private def add(c: Char): RDFJSONBuilder = {
    sb append c
    this
  }

  /**
   * Escapes a Unicode string according to JSON Format
   */
  private def escape(input: String): RDFJSONBuilder = {
    sb append JSONFormat.quoteString(input)
    this
  }

}
