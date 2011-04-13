package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.ontology.datatypes.Datatype
import java.net.URI
import org.dbpedia.extraction.mappings.ExtractionContext
import org.dbpedia.extraction.ontology.{OntologyProperty}
import java.io.CharConversionException

/**
 * Represents a statement in the N-Quads format
 * see: http://sw.deri.org/2008/07/n-quads/
 */
 //TODO should we use URI instead of String?
//FIXME Made the extraction Context public in order to retrieve the language - Is that a problem? - Claus
class Quad(	val extractionContext : ExtractionContext,
            val dataset : Dataset,
            val subject : String,
		    val predicate : String,
		    val value : String,
		    val context : String,
		    val datatype : Datatype )
{
    //Validate input
	if(subject == null) throw new NullPointerException("subject")
	if(predicate == null) throw new NullPointerException("predicate")
	if(value == null) throw new NullPointerException("value")
	if(context == null) throw new NullPointerException("context")

    if(value.isEmpty) throw new IllegalArgumentException("Value is empty")

    //TODO validate them on creation, now can be either URI/IRI
	//new URI(subject)
	//new URI(context)
	//if(datatype == null) new URI(value)

    def this( extractionContext : ExtractionContext,
              dataset : Dataset,
              subject : String,
		      predicate : OntologyProperty,
		      value : String,
		      context : String,
		      datatype : Datatype = null ) = this(extractionContext, dataset, subject, Quad.validatePredicate(predicate, datatype), value, context, Quad.getType(predicate, datatype))

    def renderNTriple = render(false)

    def renderNQuad = render(true)

    override def toString = renderNQuad

    private def render(includeContext : Boolean) : String =
    {
    	val sb = new StringBuilder

        sb append "<" append subject append "> "

        sb append "<" append predicate append "> "

        if (datatype != null)
        {
            if (datatype.uri == "http://www.w3.org/2001/XMLSchema#string")
            {
            	sb append '"'
            	escapeString(sb, value)
            	sb append "\""

                sb append "@" + extractionContext.language.locale.getLanguage + " "
            }
            else
            {
                sb append '"'
                escapeString(sb, value)
                sb append "\"^^<" append datatype.uri append "> "
            }
        }
        else
        {
            sb append '<'
            escapeString(sb, value)
            //sb append value
            sb append "> "
        }

        if (includeContext)
        {
            sb append '<' append context append "> "
        }

        sb append '.'

        return sb.toString
    }

	/**
	 * Escapes an unicode string according to N-Triples format
	 */
	private def escapeString(sb : StringBuilder, input : String) : StringBuilder =
	{
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

object Quad
{
    private def validatePredicate(predicate : OntologyProperty, datatype : Datatype) : String = predicate.uri //TODO

    // TODO: why check this special case here, but not others? We should
	// check that the given type is a sub type of the predicate range.
	//	if ($this->predicate instanceof \dbpedia\ontology\OntologyDataTypeProperty)
	//	{
	//		if (($this->type instanceof \dbpedia\ontology\dataTypes\DimensionDataType))
	//		{
	//			$this->validationErrors[] = "- Ontology Property Range is not a Unit: '".$this->type->getName()."'";
	//			$valid = false;
	//		}
	//	}

    private def getType(predicate : OntologyProperty, datatype : Datatype) : Datatype =
    {
        if(datatype != null)
        {
             datatype
        }
        else
        {
            predicate.range match
            {
                case dt : Datatype => dt
                case _ => null
            }
        }
    }
}
