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
 //TODO i refactored this class: no extraction context needed anymore (was used for language tags of literals) and use meaningfull classes for s,p,o
class Quad(	val dataset : Dataset,
            val subject : IriRef,
		    val predicate : IriRef,
		    val value : GraphNode,
		    val context : IriRef)
{
  //various constructors
  def this( dataset : Dataset,
                subject : IriRef,
            predicate : OntologyProperty,
            value : GraphNode,
            context : IriRef ) = this(dataset, subject, new IriRef(predicate.uri), value, context)

  //subject, predicate and context can be string because they can only be IriRefs
  def this( dataset : Dataset,
                subject : String,
            predicate : String,
            value : GraphNode,
            context : String ) = this(dataset, new IriRef(subject), new IriRef(predicate), value, new IriRef(context))

    //Validate input
	if(subject == null) throw new NullPointerException("subject")
	if(predicate == null) throw new NullPointerException("predicate")
	if(value == null) throw new NullPointerException("value")
	if(context == null) throw new NullPointerException("context")

  //what does that do? is it just to trigger exceptions?
	//new URI(subject)
	//new URI(context)



    def renderNTriple = render(false)
    
    def renderNQuad = render(true)
    
    override def toString = renderNQuad
    
    private def render(includeContext : Boolean) : String =
    {
      //TODO use ntriple abbreviation for repeated subject and subject-predicate-pairs
    	val sb = new StringBuilder
        
        sb append subject.render

        sb append " "

        sb append predicate.render

        sb append " "
        
        sb append value.render

        sb append " "
        
        if (includeContext)
        {
            sb append context.render
            sb append " "
        }
        
        sb append ". "
        
        return sb.toString
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
