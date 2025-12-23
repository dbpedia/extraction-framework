package org.dbpedia.extraction.wikiparser.impl.simple

import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.WikiParserException

/**
 * Internal class which is used by the WikiParser to represent the source and keep track of the current position of the parser.
 * 
 * @param source The source the page
 * @parm language The language of the source
 */
private final class Source(source : String, val language : Language)
{
    //TODO create new class Position
    var pos = 0
    var line = 1

    /** Error counter */
    var errors = 0

    def length = source.length

    def seek(count : Int) : Boolean =
    {
        if(count >= 0)
        {
            if(pos + count <= source.length)
            {
                for(c <- source.substring(pos, pos + count) if c == '\n') line += 1
                pos += count

                true
            }
            else
            {
                for(c <- source.substring(pos) if c == '\n') line += 1
                pos = source.length - 1

                false
            }
        }
        else
        {
            if(pos + count >= 0)
            {
                for(c <- source.substring(pos + count, pos) if c == '\n') line -= 1
                pos += count

                true
            }
            else
            {
                for(c <- source.substring(0, pos) if c == '\n') line -= 1
                pos = 0

                false
            }
        }
    }

    /**
     * Advances the current position to the next match of a given matcher.
     *
     * @param $matcher The matcher
     */
    def find(matcher : Matcher, throwIfNoMatch : Boolean = true) : MatchResult =
    {
        val oldPos = pos

        val result = matcher.execute(source, pos)
        pos = result.pos
        
        if(pos != oldPos)
        {
            for(c <- source.substring(oldPos, pos) if c == '\n') line += 1
        }

        if(!result.matched && throwIfNoMatch)
        {
            throw new WikiParserException("Closing tag not found", line, findLine(line))
        }

        return result
    }
    
    def nextTag(tag : String) : Boolean = 
    {
        return pos + tag.length <= source.length && source.regionMatches(pos, tag, 0, tag.length)
    }

    def lastTag(tag : String) : Boolean =
    {
        return pos >= tag.length && source.regionMatches(pos - tag.length, tag, 0, tag.length)
    }

    /**
     *  Retrieves a section of the source text.
     *
     * @param startPos - the beginning position, inclusive.
     * @param endPos - the ending position, exclusive or null if the current position should denote the end of the section.
     * @return A string containing the specified section.
     */
    def getString(startPos : Int, endPos : Int = -1) : String =
    {
        if(endPos == -1)
        {
            return source.substring(startPos, pos);
        }
        else
        {
            return source.substring(startPos, endPos);
        }
    }

    /**
     * Finds a specific line in the source.
     *
     * @param line The line number
     * @return String The line
     */
    def findLine(lineNumber : Int) : String = 
    {
        //Find line beginning
        var curLine = lineNumber
        var begin = 0
        while(begin < source.length - 1 && curLine > 1)
        {
            if(source(begin) == '\n')
            {
                curLine -= 1
            }

            begin += 1
        }

        //Find line ending
        var end = begin;
        while(end < source.length - 1 && source(end + 1) != '\n')
        {
            end += 1;
        }

        return source.substring(begin, end + 1);
    }
}
