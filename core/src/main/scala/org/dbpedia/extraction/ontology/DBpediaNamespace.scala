package org.dbpedia.extraction.ontology

import org.dbpedia.extraction.util.StringUtils.{replacements,escape}
import java.lang.StringBuilder
import scala.collection.mutable.HashMap

class DBpediaNamespace(namespace: String) extends RdfNamespace(null, namespace, true) {
  
  override protected def append(sb: StringBuilder, suffix: String): Unit = {
    escape(sb, suffix, DBpediaNamespace.iriEscapes)
  }
}

object DBpediaNamespace {
  
  // for this list of characters, see RFC 3987 and https://sourceforge.net/mailarchive/message.php?msg_id=28982391
  private val iriEscapes = {
    val chars = ('\u0000' to '\u001F').mkString + "\"#%<>?[\\]^`{|}" + ('\u007F' to '\u009F').mkString
    val replace = replacements('%', chars)
    // don't escape space, replace it by underscore
    replace(' ') = "_"
    replace
  }

  private def ns(namespace: String): DBpediaNamespace = {
    new DBpediaNamespace(namespace)
  }
  
  val ONTOLOGY = ns("http://dbpedia.org/ontology/")
  val DATATYPE = ns("http://dbpedia.org/datatype/")

}
