package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.OntologyProperty
import java.io.CharConversionException
import org.openrdf.model._
import java.io.CharConversionException
import java.io.CharConversionException
import org.dbpedia.extraction.util.Language

/**
 * Represents a statement in the N-Quads format
 * @see http://sw.deri.org/2008/07/n-quads/
 */
//TODO write out equivalent properties
class Quad(	val language : Language,
val dataset : Dataset,
val subject : String,
val predicate : String,
val value : String,
val context : String,
val datatype : Datatype )
{

  //a constructor for OntologyProperty
  def this(language : Language,
           dataset : Dataset,
           subject : String,
           predicate : OntologyProperty,
           value : String,
           context : String,
           datatype : Datatype = null) = this(language, dataset, subject, Quad.validatePredicate(predicate, datatype), value, context, Quad.getType(predicate, datatype))

  //a constructor for openrdf
  def this(language : Language,
           dataset : Dataset,
           subject : Resource,
           predicate : URI,
           value : Value,
           context : Resource) = {
      this(
          language, 
          dataset, 
          subject.stringValue, 
          predicate.stringValue, 
          value.stringValue, 
          context.stringValue, 
          if(value.isInstanceOf[Literal] && value.asInstanceOf[Literal].getDatatype != null){
            new Datatype(value.asInstanceOf[Literal].getDatatype.toString)
          } else if(value.isInstanceOf[Literal]) {
            new Datatype("xsd:string")
          } else {
            null
          }
      )
  }

  //Validate input
	if(subject == null) throw new NullPointerException("subject")
	if(predicate == null) throw new NullPointerException("predicate")
	if(value == null) throw new NullPointerException("value")
	if(context == null) throw new NullPointerException("context")

    if(value.isEmpty) throw new IllegalArgumentException("Value is empty")

	//what does that do? is it just to trigger exceptions?
	//new URI(subject)
	//new URI(context)
	//if(datatype == null) new URI(value)

  def renderNTriple = render(false)
    
  def renderNQuad = render(true)
    
  override def toString = renderNQuad
    
  private def render(includeContext : Boolean) : String =
  {
    "<" + subject + "> <" + predicate + "> " +
	  (if (datatype != null) {
			if (datatype.uri == "http://www.w3.org/2001/XMLSchema#string"){
				"\"" +
				escapeString(value) + 
				"\"" +
				"@" + language.locale.getLanguage
			} else {
				"\"" +
				escapeString(value) + 
				"\"" +
				"^^<" + datatype.uri + ">"
			}
		} else {
				"<" +
				(if(predicate == "http://xmlns.com/foaf/0.1/homepage"){
					escapeString(value) 
				} else {
					value
				}) +
				">"
    })    +  " " +
    (if (includeContext){ "<" + context +  "> "  } else {""}) + ". "
  }

    /**
	 * Escapes an unicode string according to N-Triples format
	*/
	private def escapeString(input : String) : String =
	{
		val sb = new StringBuilder
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
		sb.toString
	}

    override def equals(obj:Any) : Boolean = {
        obj.isInstanceOf[Quad] && obj.asInstanceOf[Quad].renderNQuad.equals(this.renderNQuad)
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
