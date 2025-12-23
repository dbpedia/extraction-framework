package org.dbpedia.extraction.ontology.datatypes

import org.dbpedia.extraction.dataparser.ParseResult

import scala.util.matching.Regex

/**
 * Represents an enumeration of literals.
 */
//TODO make immutable
class EnumerationDatatype(name : String) extends Datatype(name) with java.io.Serializable
{
    private var literals = List[Literal]()

    /**
     * Adds a new literal to this enumeration
     */
    def addLiteral(name : String, keywords : List[String] = List.empty)
    {
        literals = new Literal(name, keywords) :: literals
    }
    
    def parse(text : String) : Option[ParseResult[String]] =
    {
        for( literal <- literals; _ <- literal.regex.findFirstIn(text) )
        {
            return Some(ParseResult(literal.name))
        }

        None
    }

    private class Literal(val name : String, val keywords : List[String]) extends java.io.Serializable
    {
        val regex = new Regex("(?iu)\\b(?:" + (name :: keywords).mkString("|") + ")\\b")
    }
}
