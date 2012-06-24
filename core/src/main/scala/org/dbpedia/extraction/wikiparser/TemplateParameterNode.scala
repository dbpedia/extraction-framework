package org.dbpedia.extraction.wikiparser

/**
 * Represents a template parameter.
 *
 * @param parameter The key by which this parameter is identified in the template.
 * @param opt true if this parameter is optional (if it has a default value - which is not accessible yet)
 * @param line The source line number of this property
 */
case class TemplateParameterNode(parameter : String, opt : Boolean, override val line : Int) extends Node(List.empty, line)
{
    def toWikiText() : String = "{{{" + parameter + (if (opt == true) "|" else "") + "}}}"

}
