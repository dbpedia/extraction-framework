package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.RichString.wrapString
import scala.collection.mutable.{HashMap,ArrayBuffer}
import scala.collection.immutable.ListMap
import scala.collection.Map
import scala.collection.JavaConversions.asScalaSet
import java.util.Properties

/**
 * Methods to parse the lines for 'uri-policy' and 'format' in extraction
 * configuration files. These lines determine how extracted triples are
 * formatted when they are written to files. For details see
 * https://github.com/dbpedia/extraction-framework/wiki/Serialization-format-properties
 */
object PolicyParser {
  
  /**
   * Key is full policy name, value is tuple of priority, position code and factory.
   */
  val policies: Map[String, (Int, Int, Predicate => Policy)] = locally {
    
    /**
     * Triples of prefix, priority and factory.
     * 
     * Priority is important:
     * 
     * 1. check length
     * 2. convert IRI to URI
     * 3. append '_' if necessary
     * 4. convert specific domain to generic domain.
     * 
     * The length check must happen before the URI conversion, because for a non-Latin IRI the URI
     * may be several times as long, e.g. one Chinese character has several UTF-8 bytes, each of
     * which needs three characters after percent-encoding.
     *  
     * The third step must happen after URI conversion (because a URI may need an underscore where
     * a IRI doesn't), and before the last step (because we need the specific domain to decide which 
     * URIs should be made xml-safe).
     */
    val policies = Seq[(String, Int, Predicate => Policy)] (
      ("reject-long", 1, rejectLong),
      ("uri", 2, uri),
      ("xml-safe", 3, xmlSafe),
      ("generic", 4, generic)
    )

    /**
     * Tuples of suffix and position code.
     */
    val positions = Seq[(String, Int)] (
      ("", ALL),
      ("-subjects", SUBJECT),
      ("-predicates", PREDICATE),
      ("-objects", OBJECT),
      ("-datatypes", DATATYPE),
      ("-contexts", CONTEXT)
    )
    
    val product = for ((prefix, prio, factory) <- policies; (suffix, position) <- positions) yield {
      prefix+suffix -> (prio, position, factory)
    }

    product.toMap
  }
  
  val formatters = Map[String, Array[Policy] => Formatter] (
    "trix-triples" -> { new TriXFormatter(false, _) },
    "trix-quads" -> { new TriXFormatter(true, _) },
    "turtle-triples" -> { new TerseFormatter(false, true, _) },
    "turtle-quads" -> { new TerseFormatter(true, true, _) },
    "n-triples" -> { new TerseFormatter(false, false, _) },
    "n-quads" -> { new TerseFormatter(true, false, _) }
  )

  /**
   * Parses a list of languages like "en,fr" or "*" or even "en,*,fr"
   */
  private def parsePredicate(languages: String): Predicate = {
    
    val codes = languages.trimSplit(',').toSet
    
    // "*" matches all dbpedia domains
    if (codes("*")) {
      uri =>
        // host can be null for some URIs, e.g. java.net.URI doesn't understand IDN
        val host = uri.getHost
        host != null && (host.equals("dbpedia.org") || host.endsWith(".dbpedia.org")) 
    }
    else { 
      val domains = codes.map(Language(_).dbpediaDomain)
      uri =>
        domains(uri.getHost) 
    }
  }
  
  /**
   * Parses a policy line like "uri-policy.main=uri:en,fr; generic:en"
   */
  def parsePolicyValue(value: String): Array[Policy] = {
    
    val predicates = Array.fill(POSITIONS)(new ArrayBuffer[(Int, Policy)])
    
    // parse a value like "uri:en,fr; xml-safe-predicates:*"
    for (policy <- value.trimSplit(';')) {
      // parse a part like "uri:en,fr" or "xml-safe-predicates:*"
      policy.trimSplit(':') match {
        case Array(name, languages) =>
          // get factory for a name like "xml-safe-predicates"
          policies.get(name) match {
            case Some((prio, position, factory)) => {
              // parse a predicate like "en,fr" or "*", add position
              val predicate = parsePredicate(languages)
              val entry = (prio -> factory(predicate))
              if (position == ALL) predicates.foreach(_ += entry)
              else predicates(position) += entry
            }
            case None => throw error("unknown policy name in '"+policy+"'")
          }
        case _ => throw error("invalid format in '"+policy+"'")
      }
    }
    
    // order by priority and drop priority
    val ordered = predicates.map(_.sortBy(_._1).map(_._2))
    
    // replace empty policy lists by identity
    ordered.foreach(list => if (list.isEmpty) list += identity)
    
    // concatenate policy lists into one policy
    ordered.map(_.reduceLeft(_ andThen _))
  }
  
  protected def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }
    
}
