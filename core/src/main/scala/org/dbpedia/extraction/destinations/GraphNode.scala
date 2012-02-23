package org.dbpedia.extraction.destinations

import java.net.URI
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.OntologyEntity
import java.io.CharConversionException


/**
 * used by Quad, NTriple/NQuad Syntax used
 */
trait GraphNode {
  def render : String

  /**
	 * Escapes an unicode string according to N-Triples format
	 */
	def escapeString( input : String) : StringBuilder =
	{
    val sb = new StringBuilder()
        // iterate over code points (http://blogs.sun.com/darcy/entry/iterating_over_codepoints)
        val inputLength = input.length
        var offset = 0

        while (offset < inputLength)
        {
            val c = input.codePointAt(offset)
            offset += Character.charCount(c)

    		//Ported from Jena's NTripleWriter
			if (c == '\\' || c == '"')
			{
				sb append '\\' append c.toChar
			}
			else if (c == '\n')
			{
				sb append "\\n"
			}
			else if (c == '\r')
			{
				sb append "\\r";
			}
			else if (c == '\t')
			{
				sb append "\\t"
			}
			else if (c >= 32 && c < 127)
			{
				sb append c.toChar
			}
			else
			{
				val hexStr = c.toHexString.toUpperCase
                val hexStrLen = hexStr.length

                if (c <= 0xffff)
                {
                    // 16-bit code point
                    sb append "\\u"
                    sb append "0" * (4 - hexStrLen)  // leading zeros
                }
                else if (c <= 0x10ffff)  // biggest representable code point
                {
                    // 32-bit code point
                    sb append "\\U"
                    sb append "0" * (8 - hexStrLen)  // leading zeros
                }
                else
                {
                    throw new CharConversionException("code point "+c+" outside of range (0x0000..0x10ffff)")
                }

				sb append hexStr
			}
		}
		return sb
	}
}

class IriRef (val uri : String) extends GraphNode {
  def this(uriObj : URI) = this(uriObj.toString)
  def this(oe : OntologyEntity) = this(oe.uri)

   override def render : String = "<" + escapeString ( uri.toString ) + ">"
}

class PlainLiteral (val value : String) extends GraphNode {
   override def render : String = "\"" + escapeString (value)  + "\""
}

class TypedLiteral (override val value : String, val dataType : Datatype) extends PlainLiteral(value){
  require(dataType != null, "datatype cannot be null")
  override def render : String = super.render + "^^" + "<" + dataType.uri.toString + ">"
}

class LanguageLiteral (override val value : String, val language : String = "en") extends PlainLiteral(value){
  require(language != null && language.length != 0, "language must be non-empty")
  override def render : String = super.render + "@" + language
}
