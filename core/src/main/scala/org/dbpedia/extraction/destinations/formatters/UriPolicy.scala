package org.dbpedia.extraction.destinations.formatters

import java.util.Properties
import java.net.{URISyntaxException, URI}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.ConfigUtils.getStrings
import org.dbpedia.extraction.util.RichString.wrapString
import scala.xml.Utility.{isNameChar,isNameStart}
import scala.collection.Map
import scala.collection.mutable.{ArrayBuffer,HashMap}
import scala.collection.JavaConversions.asScalaSet

/**
 * TODO: use scala.collection.Map[String, String] instead of java.util.Properties?
 */
object UriPolicy {

  /**
   * A policy is a function takes a URI and may return the given URI or a transformed version of it.
   * Human-readable type alias.
   */
  type Policy = URI => URI

  /**
   * PolicyApplicable is a function that decides if a policy should be applied for the given DBpedia URI.
   * Human-readable type alias.
   */
  type PolicyApplicable = URI => Boolean

  // codes for URI positions
  val SUBJECT = 0
  val PREDICATE = 1
  val OBJECT = 2
  val DATATYPE = 3
  val CONTEXT = 4

  // total number of URI positions
  val POSITIONS = 5

  // indicates that a predicate matches all positions
  val ALL = -1

  /*
  Methods to parse the lines for 'uri-policy' in extraction configuration files. These lines
  determine how extracted triples are formatted when they are written to files. For details see
  https://github.com/dbpedia/extraction-framework/wiki/Serialization-format-properties
  */

  /**
   * Key is full policy name, value is triple of priority, position code and factory.
   */
  private val policies: Map[String, (Int, Int, PolicyApplicable => Policy)] = locally {

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
    val policies = Seq[(String, Int, PolicyApplicable => Policy)] (
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

  private val formatters = Map[String, Array[Policy] => Formatter] (
    "trix-triples" -> { new TriXFormatter(false, _) },
    "trix-quads" -> { new TriXFormatter(true, _) },
    "turtle-triples" -> { new TerseFormatter(false, true, _) },
    "turtle-quads" -> { new TerseFormatter(true, true, _) },
    "n-triples" -> { new TerseFormatter(false, false, _) },
    "n-quads" -> { new TerseFormatter(true, false, _) }
  )

  /**
   * Parse all URI policy and format lines.
   * @param uriPolicyPrefix property key prefix, e.g. "uri-policy"
   * @param formatPrefix format key prefix, e.g. "format"
   * @return map from file suffix (without '.' dot) to formatter
   */
  def parseFormats(config: Properties, uriPolicyPrefix: String, formatPrefix: String): Map[String, Formatter] = {
    val policies = parsePolicies(config, uriPolicyPrefix)
    parseFormats(config, formatPrefix, policies)
  }

  /**
   * Parse all format lines.
   * @param prefix format key prefix, e.g. "format"
   * @return map from file suffix (without '.' dot) to formatter
   */
  def parseFormats(config: Properties, prefix: String, policies: Map[String, Array[Policy]]): Map[String, Formatter] = {

    val dottedPrefix = prefix + "."
    val formats = new HashMap[String, Formatter]()

    for (key <- config.stringPropertyNames) {

      if (key.startsWith(dottedPrefix)) {

        val suffix = key.substring(dottedPrefix.length)

        val settings = getStrings(config, key, ';', true)
        require(settings.length == 1 || settings.length == 2, "key '"+key+"' must have one or two values separated by ';' - file format and optional uri policy name")

        val formatter =
          formatters.getOrElse(settings(0), throw error("first value for key '"+key+"' is '"+settings(0)+"' but must be one of "+formatters.keys.toSeq.sorted.mkString("'","','","'")))

        val policy =
          if (settings.length == 1) null
          else policies.getOrElse(settings(1), throw error("second value for key '"+key+"' is '"+settings(1)+"' but must be a configured uri-policy, i.e. one of "+policies.keys.mkString("'","','","'")))

        formats(suffix) = formatter.apply(policy)
      }
    }

    formats
  }

  /**
   * parse all URI policy lines
   * @param prefix property key prefix, e.g. "uri-policy"
   * @return map from URI policy name (including prefix) to an array of policies (with one entry for each position in a quad)
   */
  def parsePolicies(config: Properties, prefix: String): Map[String, Array[Policy]] = {

    val policies = new HashMap[String, Array[Policy]]()
    for (key <- config.stringPropertyNames) {
      if (key.startsWith(prefix)) {
        try policies(key) = parsePolicy(config.getProperty(key))
        catch { case e: Exception => throw error("invalid URI policy: '"+key+"="+config.getProperty(key)+"'", e) }
      }
    }

    policies
  }

  /**
   * Parses a policy list like "uri:en,fr; generic:en"
   */
  def parsePolicy(list: String): Array[Policy] = {

    // empty lists for all positions. each entry is a tuple of priority and policy. 
    val entries = Array.fill(POSITIONS)(new ArrayBuffer[(Int, Policy)])

    // parse a value like "uri:en,fr; xml-safe-predicates:*"
    for (policy <- list.trimSplit(';')) {
      // parse a part like "uri:en,fr" or "xml-safe-predicates:*"
      policy.trimSplit(':') match {
        case Array(name, languages) =>
          // get factory for a name like "xml-safe-predicates"
          policies.get(name) match {
            case Some((prio, position, factory)) => {
              // parse a predicate like "en,fr" or "*", add position
              val policyApplicable = parsePolicyApplicable(languages)
              val entry = (prio -> factory(policyApplicable))
              if (position == ALL) entries.foreach(_ += entry)
              else entries(position) += entry
            }
            case None => throw error("unknown policy name '"+name+"' in '"+policy+"'")
          }
        case _ => throw error("invalid format in '"+policy+"'")
      }
    }

    // order by priority and drop priority
    val ordered = entries.map(_.sortBy(_._1).map(_._2))

    // replace empty policy lists by identity
    ordered.foreach(list => if (list.isEmpty) list += identity)

    // concatenate policy lists into one policy
    ordered.map(_.reduceLeft(_ andThen _))
  }

  /**
   * Parses a list of languages like "en,fr" or "*" or even "en,*,fr"
   */
  private def parsePolicyApplicable(languages: String): PolicyApplicable = {

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

  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }

  /*
  Methods that check URIs at run-time.
  */

  def uri(applicableTo: PolicyApplicable): Policy = {

    iri =>
      if (applicableTo(iri)) {
        new URI(iri.toASCIIString)
      }
      else {
        iri
      }
  }

  def generic(applicableTo: PolicyApplicable): Policy = {

    iri =>
      if (applicableTo(iri)) {

        val scheme = iri.getScheme
        val user = iri.getRawUserInfo
        val host = "dbpedia.org"
        val port = iri.getPort
        val path = iri.getRawPath
        val query = iri.getRawQuery
        val frag = iri.getRawFragment

        uri(scheme, user, host, port, path, query, frag)
      }
      else {
        iri
      }
  }

  // max length (arbitrary choice)
  val MAX_LENGTH = 500

  def rejectLong(applicableTo: PolicyApplicable): Policy = {

    iri =>
      if (applicableTo(iri)) {
        val str = iri.toString
        if (str.length > MAX_LENGTH) throw new URISyntaxException(str, "length "+str.length+" exceeds maximum "+MAX_LENGTH)
      }
      iri
  }

  /**
   * Check if the tail of the URI could be used as an XML element name. If not, attach an
   * underscore (which is a valid XML name). The resulting URI is guaranteed to be usable
   * as a predicate in RDF/XML - it can be split into a valid namespace URI and a valid XML name.
   *
   * Examples:
   *
   * original URI       xml safe URI (may be equal)   possible namespace and name in RDF/XML
   *
   * http://foo/bar          http://foo/bar           http://foo/           bar
   * http://foo/123          http://foo/123_          http://foo/123        _
   * http://foo/%22          http://foo/%22_          http://foo/%22        _
   * http://foo/%C3%BC       http://foo/%C3%BC_       http://foo/%C3%BC     _
   * http://foo/%C3%BCD      http://foo/%C3%BCD       http://foo/%C3%BC     D
   * http://foo/%            http://foo/%22_          http://foo/%22        _
   * http://foo/bar_(fub)    http://foo/bar_(fub)_    http://foo/bar_(fub)  _
   * http://foo/bar#a123     http://foo/bar#a123      http://foo/bar#       a123
   * http://foo/bar#123      http://foo/bar#123_      http://foo/bar#123    _
   * http://foo/bar#         http://foo/bar#_         http://foo/bar#       _
   * http://foo/bar?a123     http://foo/bar?a123      http://foo/bar?       a123
   * http://foo/bar?a=       http://foo/bar?a=_       http://foo/bar?a=     _
   * http://foo/bar?a=b      http://foo/bar?a=b       http://foo/bar?a=     b
   * http://foo/bar?123      http://foo/bar?123_      http://foo/bar?123    _
   * http://foo/bar?         http://foo/bar?_         http://foo/bar?       _
   * http://foo/             http://foo/_             http://foo/           _
   * http://foo              http://foo/_             http://foo/           _
   * http://foo/:            http://foo/:_            http://foo/:          _
   * http://foo/a:           http://foo/a:_           http://foo/a:         _
   * http://foo/a:b          http://foo/a:b           http://foo/a:         b
   */
  def xmlSafe(applicableTo: PolicyApplicable): Policy = {

    iri =>
      if (applicableTo(iri)) {

        val scheme = iri.getScheme
        val user = iri.getRawUserInfo
        val host = iri.getHost
        val port = iri.getPort
        var path = iri.getRawPath
        var query = iri.getRawQuery
        var frag = iri.getRawFragment

        if (frag != null) frag = xmlSafe(frag)
        else if (query != null) query = xmlSafe(query)
        else if (path != null && path.nonEmpty) path = xmlSafe(path)
        else path = "/_" // convert empty path to "/_"

        uri(scheme, user, host, port, path, query, frag)
      }
      else {
        iri
      }
  }

  /**
   * Check if the tail of the string could be used as an XML element name. 
   * If not, attach an underscore (which is a valid XML name).
   */
  private def xmlSafe(tail: String): String = {

    // Go through tail from back to front, find minimal safe part.
    var index = tail.length
    while (index > 0) {

      index -= 1

      // If char is part of a %XX sequence, we can't split the URI into a namespace and a name.
      // Note: We know it's a valid IRI. Otherwise we'd need more checks here. 
      if (index >= 2 && tail.charAt(index - 2) == '%') return tail+'_'

      val ch = tail.charAt(index)

      // If char is not valid as part of a name, we can't use the tail as a name.
      // Note: isNameChar allows ':', but we're stricter.
      if (ch == ':' || ! isNameChar(ch)) return tail+'_'

      // If char is valid as start of a name, we can use this part as a name.
      if (isNameStart(ch)) return tail
    }

    // We can't use the string as an XML name.
    return tail+'_'
  }

  private def uri(scheme: String, user: String, host: String, port: Int, path: String, query: String, frag: String): URI = {

    val sb = new StringBuilder

    if (scheme != null) sb append scheme append ':'

    if (host != null) {
      sb.append("//");
      if (user != null) sb.append(user).append('@');
      sb.append(host);
      if (port != -1) sb.append(':').append(port);
    }

    if (path != null) sb.append(path);
    if (query != null) sb.append('?').append(query);
    if (frag != null) sb.append('#').append(frag);

    new URI(sb.toString)
  }

}