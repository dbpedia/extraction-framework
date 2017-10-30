package org.dbpedia.extraction.ontology

import java.lang.StringBuilder

import org.dbpedia.extraction.util.StringUtils.{escape, replacements}

class DBpediaNamespace (namespace: String) extends RdfNamespace(null, namespace, true) {
  
  override protected def append(sb: StringBuilder, suffix: String): Unit = {
    escape(sb, suffix, DBpediaNamespace.iriEscapes)
  }

  override def toString: String = namespace

}

object DBpediaNamespace {

  private var nsMap = Map[String, DBpediaNamespace]()
  // for this list of characters, see RFC 3987 and https://sourceforge.net/mailarchive/message.php?msg_id=28982391
  private val iriEscapes = {
    val chars = ('\u0000' to '\u001F').mkString + "\"#%<>?[\\]^`{|}" + ('\u007F' to '\u009F').mkString
    val replace = replacements('%', chars)
    // don't escape space, replace it by underscore
    replace(' ') = "_"
    replace
  }

  def get(namespace: String): Option[DBpediaNamespace] = nsMap.get(namespace.toLowerCase.trim)

  private def ns(namespace: String, short: String = null): DBpediaNamespace = {
    val str = if(short != null)
                namespace.toLowerCase().trim
              else
                "http://dbpedia.org/" + namespace.toLowerCase().trim
    val ns = new DBpediaNamespace(str + (if(str.endsWith("/")) "" else "/"))
    nsMap += (if(short != null) short.toLowerCase.trim else namespace.toLowerCase().trim) -> ns
    ns
  }

  val ONTOLOGY: DBpediaNamespace = ns("ontology")
  val DATATYPE: DBpediaNamespace = ns("datatype")
  val DATASET: DBpediaNamespace = ns("dataset")
  val PARSER: DBpediaNamespace = ns("parser")
  val EXTRACTOR: DBpediaNamespace = ns("extractor")
  val TRANSFORMER: DBpediaNamespace = ns("transformer")
  val PROVENANCE: DBpediaNamespace = ns("http://prov.dbpedia.org/", "provenance")

}
