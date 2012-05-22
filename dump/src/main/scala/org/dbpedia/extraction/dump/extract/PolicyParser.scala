package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.destinations.formatters.{Formatter,TerseFormatter,TriXFormatter}
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.util.Language
import scala.collection.mutable.{HashMap,ArrayBuffer}
import scala.collection.Map
import scala.collection.JavaConversions.asScalaSet // implicit
import java.util.Properties

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
  private def parsePolicies(): Map[String, Policy] = {
    
    val policies = new HashMap[String, Policy]()
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
  private def parseFormats(policies: Map[String, Policy]): Map[String, Formatter] = {
    
    val formats = new HashMap[String, Formatter]()
    
    for (key <- config.stringPropertyNames) {
      
      if (key.startsWith("format.")) {
        
        val suffix = key.substring("format.".length)
        
        val settings = splitValue(key, ';')
        require(settings.length == 1 || settings.length == 2, "key '"+key+"' must have one or two values separated by ';' - file format and optional uri policy")
        
        val formatter = formatters.getOrElse(settings(0), throw error("first value for key '"+key+"' is '"+settings(0)+"' but must be one of "+formatters.keys.toSeq.sorted.mkString("'","','","'")))
        
        val policy =
          if (settings.length == 1) identity
          else policies.getOrElse(settings(1), throw error("second value for key '"+key+"' is '"+settings(1)+"' but must be a configured uri-policy, i.e. one of "+policies.keys.mkString("'","','","'")))
        
        formats(suffix) = formatter.apply(policy)
      }
    }
    
    formats
  }
    
  /**
   * Order is important here: First convert IRI to URI, then append '_' if necessary,
   * then convert specific domain to generic domain. The second step must happen 
   * after URI conversion (because a URI may need an underscore where a IRI doesn't), 
   * and before the third step (because we need the specific domain to decide which 
   * URIs should be made xml-safe).
   * 
   * In each tuple, the key is the policy name. The value is a policy-factory that
   * takes a predicate. The predicate decides for which DBpedia URIs a policy
   * should be applied. We pass a predicate to the policy-factory to get the policy.
   */
  private val policyFactories = Seq[(String, Predicate => Policy)] (
    "uris" -> uris,
    "xml-safe" -> xmlSafe,
    "generic" -> generic
  )

  private val formatters = Map[String, Policy => Formatter] (
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
    
    val codes = split(languages, ',').toSet
    val domains = codes.map { code => if (code == "*") "*" else Language(code).dbpediaDomain }
    
    if (domains("*")) { uri => uri.getHost.equals("dbpedia.org") || uri.getHost.endsWith(".dbpedia.org") }
    else { uri => domains(uri.getHost) }
  }
  
  /**
   * Parses a policy line like "uri-policy.main=uris:en,fr; generic:en"
   */
  private def parsePolicy(key: String): Policy = {
    
    val predicates = new HashMap[String, Predicate]()
    
    // parse all predicates
    for (policy <- splitValue(key, ';')) {
      split(policy, ':') match {
        case List(key, languages) => {
          require(! predicates.contains(key), "duplicate policy '"+key+"'")
          predicates(key) = parsePredicate(languages)
        }
        case _ => throw error("invalid format: '"+policy+"'")
      }
    }
    
    require(predicates.nonEmpty, "found no URI policies")
    
    val policies = new ArrayBuffer[Policy]()
    
    // go through known policies in correct order, get predicate, and create policy
    for ((key, factory) <- policyFactories; predicate <- predicates.remove(key)) {
      policies += factory(predicate)
    }
    
    require(predicates.isEmpty, "unknown URI policies "+predicates.keys.mkString("'","','","'"))
    
    // The resulting policy is the concatenation of all policies.
    (iri, pos) => {
      var result = iri
      for (policy <- policies) result = policy(result, pos)
      result
    }
  }
  
}