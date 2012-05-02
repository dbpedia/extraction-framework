package org.dbpedia.extraction.wikiparser.impl.simple

private final class Matcher(val userTags : List[String], matchStdTags : Boolean = false)
{
    private val stdTags = if(matchStdTags) List("[[", "[", "http", "{{{", "{{", "{|", "\n=", "<!--", "<ref", "<math", "<code", "<source") else List()

    /** Indicates whether all tags start with a special char (see: isSpecialChar) */
    private val onlySpecialChars = (stdTags ::: userTags).map(tag => tag(0)).forall(isSpecialChar)

    /**
     * True, if the given character is one of: \n, ' ', <, =, >, [, ], {, |, }, h.
     * These characters are usually used as tokens in MediaWiki.
     */
    @inline
    private def isSpecialChar(c : Char) : Boolean =
    {
       c == 'h' || c == 10 || c == 33 || (c >= 60 && c <= 62) || c == 91 || c == 93 || (c >= 123 && c <= 125)
    }

    def execute(source : String, startPos : Int) : MatchResult =
    {
        var pos = startPos
        var tagIndex = -1
        var tagLength = 0

        //Handle special case when a section begins in the first line
        if(matchStdTags && pos == 0 && source.nonEmpty && source(0) == '=')
        {
            return new MatchResult(true, 1, stdTags.indexOf("\n="), "\n=", true)
        }

        while(pos < source.length)
        {
            if(!onlySpecialChars || isSpecialChar(source(pos)))
            {
                // check for standard tags (if set)
                tagIndex = 0
                for(tag <- stdTags)
                {
                    tagLength = tag.length;
                    if(pos + tagLength <= source.length && source.regionMatches(pos, tag, 0, tagLength))
                    {
                        pos += tagLength;
                        return new MatchResult(true, pos, tagIndex, tag, true)
                    }

                    tagIndex += 1
                }

                // check for actually searched for tags
                tagIndex = 0
                for(tag <- userTags)
                {
                    tagLength = tag.length;
                    if(pos + tagLength <= source.length && source.regionMatches(pos, tag, 0, tagLength))
                    {
                        pos += tagLength;
                        return new MatchResult(true, pos, tagIndex, tag, false)
                    }

                    tagIndex += 1
                }
            }
            
            pos += 1;
        }

        new MatchResult(false, pos)
    }
}

private final class MatchResult( val matched : Boolean,
                                 val pos : Int,
                                 val tagIndex : Int = 0,
                                 val tag : String = null,
                                 val isStdTag : Boolean = true )
