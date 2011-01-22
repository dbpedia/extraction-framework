package org.dbpedia.extraction.ontology.datatypes

import scala.util.matching.Regex

/**
 * Represents an enumeration of literals.
 */
//TODO make immutable
class EnumerationDatatype(name : String) extends Datatype(name)
{
	private var literals = List[Literal]()

    /**
     * Adds a new literal to this enumeration
     */
	def addLiteral(name : String, keywords : List[String] = List.empty)
	{
		literals = new Literal(name, keywords) :: literals
	}
	
    def parse(text : String) : Option[String] =
    {
        for( literal <- literals; _ <- literal.regex.findFirstIn(text) )
        {
            return Some(literal.name)
        }

        return None
    }

    private class Literal(val name : String, val keywords : List[String])
    {
        val regex = new Regex("(?iu)\\b(?:" + (name :: keywords).mkString("|") + ")\\b")
    }
}
