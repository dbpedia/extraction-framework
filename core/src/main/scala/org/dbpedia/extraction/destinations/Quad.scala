package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.util.Language
import java.net.URI

/**
 * Represents a statement.
 */
// FIXME Handle URI vs IRI stuff only in this class. Use IRIs everywhere else.
class Quad( val language : Language,
            val dataset : Dataset,
            val subject : String,
            val predicate : String,
            val value : String,
            val context : String,
            var datatype : Datatype )
{
    //Validate input
    if (subject == null) throw new NullPointerException("subject")
    if (predicate == null) throw new NullPointerException("predicate")
    if (value == null) throw new NullPointerException("value")
    if (context == null) throw new NullPointerException("context")

    if (value.isEmpty) throw new IllegalArgumentException("Value is empty")

    // Validate URIs
//    new URI(subject)
//    new URI(predicate)
//    new URI(context)
//    if (datatype == null) new URI(value)
    
    def this(language : Language,
             dataset : Dataset,
             subject : String,
             predicate : OntologyProperty,
             value : String,
             context : String,
             datatype : Datatype = null) = this(language, dataset, subject, predicate.uri, value, context, Quad.getType(datatype, predicate))

    def renderNTriple = render(false, false)

    def renderNQuad = render(false, true)

    def renderTurtleTriple = render(true, false)

    def renderTurtleQuad = render(true, true)

    override def toString = renderTurtleQuad

    private def render(turtle: Boolean, quad: Boolean) : String =
    {
      val triple = new TripleBuilder(turtle)
      triple.uri(subject).uri(predicate).value(value, datatype, language)
      if (quad) triple.uri(context)
      triple.close().toString()
    }
}

class TripleBuilder(turtle: Boolean)
{
    // Scala's StringBuilder doesn't have appendCodePoint
    private val sb = new java.lang.StringBuilder
    
    // FIXME Handle URI vs IRI stuff only in this method. Use IRIs in all other classes and methods.
    def uri(uri: String): TripleBuilder = {
      this append '<' escape uri append "> "
      this
    }
    
    def value(value: String, datatype: Datatype, language: Language): TripleBuilder = {
      if (datatype == null) this uri value
      else {
        this append '"' escape value append '"' 
        if (datatype.name == "xsd:string") this append '@' append language.isoCode append ' '
        else this append "^^" uri datatype.uri
      }
      this
    }
    
    def close(): TripleBuilder = {
      // use UNIX EOL. N-Triples and Turtle don't care:
      // http://www.w3.org/TR/rdf-testcases/#eoln and http://www.w3.org/TR/turtle/#term-turtle2-WS
      // and it's probably better to be consistent instead of using the EOL of the platform
      // where the file was generated. These files are moved around a lot anyway.
      sb append ".\n"
      this
    }
      
    override def toString: String = {
      sb toString
    }
    
    private def append(s: String) = { 
      sb append s
      this 
    }
    
    private def append(c: Char) = { 
      sb append c
      this 
    }
    
    /**
     * Escapes an unicode string according to N-Triples / Turtle format
     */
    private def escape(input: String): TripleBuilder =
    {
      val length = input.length
      
      var offset = 0
      while (offset < length)
      {
        val c = input.codePointAt(offset)
        offset += Character.charCount(c)

        if (c == '\\' || c == '"') sb append '\\' append c.toChar
        else if (c == '\n') sb append "\\n"
        else if (c == '\r') sb append "\\r";
        else if (c == '\t') sb append "\\t"
        else if (c >= 0x0020 && c < 0x00FF) sb append c.toChar
        else if (turtle && c >= 0x00A0 && c <= 0xFFFF) sb append c.toChar
        else if (turtle && c >= 0x10000) sb appendCodePoint c
        else this appendHex c
      }
      
      this
    }
    
    private def appendHex(c: Int) {
      val hex = c.toHexString.toUpperCase
      if (c <= 0xFFFF) sb append "\\u" append "0" * (4 - hex.length)
      else sb append "\\U" append "0" * (8 - hex.length)
      sb append hex
    }
    
}

object Quad
{
  private def getType(datatype : Datatype, predicate : OntologyProperty): Datatype =
  {
    if (datatype != null) datatype
    else predicate.range match {
      case datatype: Datatype => datatype
      case _ => null
    }
  }
}
