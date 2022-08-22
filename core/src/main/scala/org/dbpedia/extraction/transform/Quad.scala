package org.dbpedia.extraction.transform

import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.{OntologyProperty, OntologyType}
import org.dbpedia.extraction.transform.Quad._
import org.dbpedia.extraction.util.Language
import org.dbpedia.iri.UriUtils

/**
 * Represents a statement.
 * 
 * @param language ISO code, may be null
 * @param dataset DBpedia dataset name, may be null
 * @param subject URI/IRI, must not be null
 * @param predicate URI/IRI, must not be null
 * @param value URI/IRI or literal, must not be null
 * @param context URI/IRI, may be null
 * @param datatype may be null, which means that value is a URI/IRI
 * 
 * TODO: the order of the parameters is confusing. As in Turtle/N-Triple/N-Quad files, it should be
 * 
 * dataset
 * subject
 * predicate
 * value
 * datatype
 * language
 * context
 */
class Quad(
  val language: String,
  val dataset: String,
  val subject: String,
  val predicate: String,
  val value: String,
  val context: String,
  val datatype: String
)
extends Ordered[Quad]
with Equals with Serializable
{
  //updated for allowing addition of Wikidata String properties with unknown language
  //try to use this constructor: when using DatasetDestination we need the exact name of the DBpedia dataset!
  def this(
    language: Language,
    dataset: Dataset,
    subject: String,
    predicate: String,
    value: String,
    context: String,
    datatype: Datatype
  ) = this(
    if (language == null) null else language.isoCode,
    if (dataset == null) null else dataset.encoded,
      subject,
      predicate,
      value,
      context,
      if (datatype == null) null else datatype.uri
    )

  def this(
    language: Language,
    dataset: Dataset,
    subject: String,
    predicate: OntologyProperty,
    value: String,
    context: String,
    datatype: Datatype = null
  ) = this(
      language,
      dataset,
      subject,
      predicate.uri,
      value,
      context,
      findType(datatype, predicate.range)
    )


  // Validate input
  if (subject == null) throw new NullPointerException("subject")
  if (predicate == null) throw new NullPointerException("predicate")
  if (value == null) throw new NullPointerException("value")
  
  def copy(
    dataset: String = this.dataset,
    subject: String = this.subject,
    predicate: String = this.predicate,
    value: String = this.value,
    datatype: String = this.datatype,
    language: String = this.language,
    context: String = this.context
  ) = new Quad(
    language,
    dataset,
    subject,
    predicate,
    value,
    context,
    datatype
  )
  
  override def toString: String = {
   "Quad("+
   "dataset="+dataset+","+
   "subject="+subject+","+
   "predicate="+predicate+","+
   "value="+value+","+
   "language="+language+","+
   "datatype="+datatype+","+
   "context="+context+
   ")"
  }

  /**
   * sub classes that add new fields should override this method 
   */
  def compare(that: Quad): Int = {
    var c = 0
    c = this.subject.compareTo(that.subject)
    if (c != 0) return c
    c = this.predicate.compareTo(that.predicate)
    if (c != 0) return c
    c = this.value.compareTo(that.value)
    if (c != 0) return c
    c = safeCompare(this.datatype, that.datatype)
    if (c != 0) return c
    c = safeCompare(this.language, that.language)
    if (c != 0) return c
    // ignore dataset and context
    0
  }
  
  /**
   * sub classes that add new fields should override this method 
   */
  override def canEqual(obj: Any): Boolean = {
    obj.isInstanceOf[Quad]
  }

  /**
   * sub classes that add new fields should override this method 
   */
  override def equals(other: Any): Boolean = other match {
    case that: Quad =>
      this.eq(that) ||
      that.canEqual(this) &&
      this.subject == that.subject &&
      this.predicate == that.predicate &&
      this.value == that.value &&
      this.datatype == that.datatype &&
      this.language == that.language
      // ignore dataset and context
    case _ => false
  }
  
  /**
   * sub classes that add new fields should override this method 
   */
  override def hashCode(): Int = {
    val prime = 41
    var hash = 1
    hash = prime * hash + subject.hashCode
    hash = prime * hash + predicate.hashCode
    hash = prime * hash + value.hashCode
    hash = prime * hash + safeHash(datatype)
    hash = prime * hash + safeHash(language)
    // ignore dataset and context
    hash
  }

  def hasObjectPredicate: Boolean =
  {
    datatype == null && language == null && UriUtils.createURI(value).get.isAbsolute
  }
}

object Quad
{
  /**
   * null-safe comparison. null is equal to null and less than any non-null string.
   */
  private def safeCompare(s1: String, s2: String): Int =
  {
    if (s1 == null && s2 == null) 0
    else if (s1 == null) -1
    else if (s2 == null) 1
    else s1.compareTo(s2)
  }
  
  private def safeHash(s: String): Int =
  {
    if (s == null) 0 else s.hashCode
  }
  
  private def findType(datatype: Datatype, range: OntologyType): Datatype =
  {
    if (datatype != null) datatype
    else range match {
      case datatype1: Datatype => datatype1
      case _ => null
    }
  }

  /**
   * Matches a line containing a triple or quad. Usage example:
   * 
   * line.trim match {
   *   case Quad(quad) => { ... }
   * }
   * 
   * WARNING: there are several deviations from the N-Triples / Turtle specifications.
   * 
   * TODO: Clean up this code a bit. Fix the worst deviations from Turtle/N-Triples spec, 
   * clearly document the others. Unescape \U stuff while parsing the line?
   * 
   * TODO: Move this to its own TerseParser class, make it configurable:
   * - N-Triples or Turtle syntax?
   * - Unescape \U stuff or not?
   * - triples or quads?
   */
  def unapply(line: String): Option[Quad] =  {
    val length = line.length
    var index = 0
    
    var language: String = null
    var datatype: String = null
    
    index = skipSpace(line, index)
    val subject = findUri(line, index)
    if (subject != null) index += subject.length + 2
    else return None
    
    // TODO: N-Triples requires space here. Not sure about Turtle.
    index = skipSpace(line, index)
    val predicate = findUri(line, index)
    if (predicate != null) index += predicate.length + 2
    else return None
    
    // TODO: N-Triples requires space here. Not sure about Turtle.
    index = skipSpace(line, index)
    var value = findUri(line, index)
    if (value != null) index += value.length + 2
    else { // literal
      if (index == length || line.charAt(index) != '"') return None
      index += 1 // skip "
      if (index == length) return None
      var start = index
      while (line.charAt(index) != '"') {
        if (line.charAt(index) == '\\') index += 1
        index += 1
        if (index >= length) return None
      } 
      value = line.substring(start, index)
      index += 1 // skip "
      if (index == length) return None
      datatype = "http://www.w3.org/2001/XMLSchema#string" // set default type
      var c = line.charAt(index)
      if (c == '@') {
        // FIXME: This code matches: @[a-z][a-z0-9-]*
        // NT spec says: '@' [a-z]+ ('-' [a-z0-9]+ )*
        // Turtle spec says: "@" [a-zA-Z]+ ( "-" [a-zA-Z0-9]+ )*
        index += 1 // skip @
        start = index
        if (index == length) return None
        c = line.charAt(index)
        if (c < 'a' || c > 'z') return None
        do {
          index += 1 // skip last lang char
          if (index == length) return None
          c = line.charAt(index)
        } while (c == '-' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z'))
        language = line.substring(start, index)
        datatype = "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString" // when there is a language we have an rdf:langString
      }
      else if (c == '^') { // type uri: ^^<...>
        if (! line.startsWith("^^<", index)) return None
        start = index + 3 // skip ^^<
        index = line.indexOf('>', start)
        if (index == -1) return None
        datatype = line.substring(start, index)
        index += 1 // skip '>'
      } 
    }
    
    index = skipSpace(line, index)
    val context = findUri(line, index)
    if (context != null) index += context.length + 2
    
    index = skipSpace(line, index)
    if (index == length || line.charAt(index) != '.') return None
    index += 1 // skip .
    
    index = skipSpace(line, index)
    if (index != length) return None
    
    Some(new Quad(language, null, subject, predicate, value, context, datatype))
  }
  
  private def skipSpace(line: String, start: Int): Int = {
    val length = line.length
    var index = start
    while (index < length) {
      val c = line.charAt(index)
      if (c != ' ' && c != '\t') return index
      index += 1
    } 
    index
  }
  
  private def findUri(line: String, start: Int): String = {
    if (start == line.length || line.charAt(start) != '<') return null
    val end = line.indexOf('>', start + 1) // TODO: turtle allows escaping > as \>
    if (end == -1) null else line.substring(start + 1, end)
  }
  
}
