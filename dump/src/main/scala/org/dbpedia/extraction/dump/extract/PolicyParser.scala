package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.destinations.formatters.{Formatter,TerseFormatter,TriXFormatter}
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.util.Language
import scala.collection.mutable.{HashMap,ArrayBuffer}
import scala.collection.immutable.ListMap
import scala.collection.Map
import scala.collection.JavaConversions.asScalaSet
import java.util.Properties

import PolicyParser._

object PolicyParser {
  
  /**
   * Key is full policy name, value is tuple of priority, position code and factory.
   */
  val policies: Map[String, (Int, Int, Predicate => Policy)] = locally {
    
    /**
     * Triples of prefix, priority and factory.
     * 
     * Priority is important: First convert IRI to URI, then append '_' if necessary,
     * then convert specific domain to generic domain. The second step must happen 
     * after URI conversion (because a URI may need an underscore where a IRI doesn't), 
     * and before the third step (because we need the specific domain to decide which 
     * URIs should be made xml-safe).
     */
    val policies = Seq[(String, Int, Predicate => Policy)] (
      ("uri", 1, uri),
      ("xml-safe", 2, xmlSafe),
      ("generic", 3, generic)
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

}

class PolicyParser(config : Properties)
extends ConfigParser(config)
{
  def parseFormats(): Map[String, Formatter] = {
    val policies = parsePolicies
    parseFormats(policies)
  }
  
  /**
   * parse all URI policy lines
   */
  private def parsePolicies(): Map[String, Array[Policy]] = {
    
    val policies = new HashMap[String, Array[Policy]]()
    for (key <- config.stringPropertyNames) {
      if (key.startsWith("uri-policy")) {
        try policies(key) = parsePolicy(key)
        catch { case e: Exception => throw error("invalid URI policy: '"+key+"="+config.getProperty(key)+"'", e) }
      }
    }
    
    policies
  }
  
  /**
   * Parse all format lines.
   */
  private def parseFormats(policies: Map[String, Array[Policy]]): Map[String, Formatter] = {
    
    val formats = new HashMap[String, Formatter]()
    
    for (key <- config.stringPropertyNames) {
      
      if (key.startsWith("format.")) {
        
        val suffix = key.substring("format.".length)
        
        val settings = splitValue(key, ';')
        require(settings.length == 1 || settings.length == 2, "key '"+key+"' must have one or two values separated by ';' - file format and optional uri policy")
        
        val formatter = formatters.getOrElse(settings(0), throw error("first value for key '"+key+"' is '"+settings(0)+"' but must be one of "+formatters.keys.toSeq.sorted.mkString("'","','","'")))
        
        val policy =
          if (settings.length == 1) null
          else policies.getOrElse(settings(1), throw error("second value for key '"+key+"' is '"+settings(1)+"' but must be a configured uri-policy, i.e. one of "+policies.keys.mkString("'","','","'")))
        
        formats(suffix) = formatter.apply(policy)
      }
    }
    
    formats
  }
  
  /**
   * Parses a list of languages like "en,fr" or "*" or even "en,*,fr"
   */
  private def parsePredicate(languages: String): Predicate = {
    
    val codes = split(languages, ',').toSet
    
    // "*" matches all dbpedia domains
    if (codes("*")) {
      uri => 
        uri.getHost.equals("dbpedia.org") || uri.getHost.endsWith(".dbpedia.org") 
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
  private def parsePolicy(key: String): Array[Policy] = {
    
    val predicates = Array.fill(POSITIONS)(new ArrayBuffer[(Int, Policy)])
    
    // parse a value like "uri:en,fr; xml-safe-predicates:*"
    for (policy <- splitValue(key, ';')) {
      // parse a part like "uri:en,fr" or "xml-safe-predicates:*"
      split(policy, ':') match {
        case List(name, languages) =>
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
  
}