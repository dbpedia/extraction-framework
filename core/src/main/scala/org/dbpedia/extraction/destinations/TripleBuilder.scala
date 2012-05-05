package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.util.Language
import java.net.URI

/**
 * Helps to build one triple/quad line in Turtle/Turtle-Quads/N-Triples/N-Quads format.
 */
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
     * Escapes a Unicode string according to N-Triples / Turtle format.
     */
    private def escape(input: String): TripleBuilder =
    {
      val length = input.length
      
      var offset = 0
      while (offset < length)
      {
        val c = input.codePointAt(offset)
        offset += Character.charCount(c)

        // TODO: use a lookup table for c < 0x80
             if (c == '\\') sb append "\\\\"
        else if (c == '\"') sb append "\\\""
        else if (c == '\n') sb append "\\n"
        else if (c == '\r') sb append "\\r";
        else if (c == '\t') sb append "\\t"
        else if (c >= 0x0020 && c < 0x007F) sb append c.toChar
        else if (turtle && c >= 0x00A0 && c <= 0xFFFF) sb append c.toChar
        else if (turtle && c >= 0x10000) sb appendCodePoint c
        else this appendHex c
      }
      
      this
    }
    
    private def appendHex(c: Int) {
      // TODO: use something faster than c.toHexString.toUpperCase and "0" *.
      // Append chars to the string buffer directly. 
      val hex = c.toHexString.toUpperCase
      if (c <= 0xFFFF) sb append "\\u" append "0" * (4 - hex.length)
      else sb append "\\U" append "0" * (8 - hex.length)
      sb append hex
    }
    
}
