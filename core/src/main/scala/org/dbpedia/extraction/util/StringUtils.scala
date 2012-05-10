package org.dbpedia.extraction.util

import java.lang.StringBuilder

/**
 * Defines additional methods on strings, which are missing in the standard library.
 */
object StringUtils
{
    /** 
     * Displays minutes, seconds and milliseconds.
     */
    def prettyMillis(millis : Long) = zeros(2, millis / 60000)+':'+zeros(2, millis % 60000 / 1000)+'.'+zeros(3, millis % 1000)+'s'
    
    private val zeros = "0000000000000000000000000"
    
    /**
     * Build a copy of the first string in which all occurrences of chars from the third string
     * have been converted to UTF-8 hex escapes where each two-digit hex byte is prefixed by the 
     * given escape character.
     * 
     * This method is pretty inefficient. If it is repeatedly used with the same chars, 
     * you should use replacements() and escape() below.
     * 
     * TODO: This method cannot replace code points > 0xFFFF.
     * 
     * @param chars list of characters that should be escaped if they occur in the string.
     * @param target If null, the method may return null or a new StringBuilder. 
     * Otherwise, the method will return the given target.
     * @return null if target was null and no characters had to be escaped
     */
    def escape(target: StringBuilder, str: String, esc: Char, chars: String): StringBuilder = {
      
      var sb = target
      
      var last = 0
      var pos = 0
      
      while (pos < str.length)
      {
        val c = str.charAt(pos)
        
        if (chars.indexOf(c) != -1) {
          
          if (sb == null) sb = new StringBuilder
          sb.append(str, last, pos)
          last = pos + 1
          
          if (c < 0x80) {
            sb append esc
            NumberUtils.toHex(sb, c, 2)
          } else {
            val bytes = new String(Array(c)).getBytes("UTF-8")
            for (b <- bytes) {
              sb append esc
              NumberUtils.toHex(sb, b, 2)
            }
          }
          
        }
        pos += 1
      }
      
      if (sb != null) sb.append(str, last, str.length)
      
      sb
    }
    
    /**
     * Produces a replacement array that can be used for escape() below. Call this method once,
     * save its result and use it to call escape() below repeatedly.
     */
    def replacements(esc: Char, chars: String): Array[String] = {
      if (chars.isEmpty) {
        new Array[String](0)
      } else {
        var max = chars.max(Ordering.Char)
        val replace = new Array[String](max + 1)
        var pos = 0
        while (pos < chars.length)
        {
          val c = chars.charAt(pos)
          val sb = new StringBuilder
          if (c < 0x80) {
            sb append esc
            NumberUtils.toHex(sb, c, 2)
          } else {
            val bytes = new String(Array(c)).getBytes("UTF-8")
            for (b <- bytes) {
              sb append esc
              NumberUtils.toHex(sb, b, 2)
            }
          }
          replace(c) = sb.toString
          pos += 1
        }
        replace
      }
    }
    
    /**
     * Build a copy of the given string, replacing some chars by replacement strings. 
     * The replacement array is indexed by character value. Only characters for which
     * the replacement array contains a non-null value will be replaced.
     * 
     * TODO: This method cannot replace code points > 0xFFFF.
     * 
     * @param replace mapping from characters to replacement strings.
     * @param target If null, the method may return null or a new StringBuilder. 
     * Otherwise, the method will return the given target.
     * @return null if target was null and no characters had to be escaped
     */
    def escape(target: StringBuilder, str: String, replace: Array[String]): StringBuilder = {
      
      var sb = target
      val chars = str.toCharArray
      
      var last = 0
      var pos = 0
      
      while (pos < chars.length)
      {
        val c = chars(pos)
        if (c < replace.length) {
          val rep = replace(c)
          if (rep != null) {
            if (sb == null) sb = new StringBuilder
            sb.append(str, last, pos)
            last = pos + 1
            sb.append(rep)
          }
        }
        pos += 1
      }
      
      if (sb != null) sb.append(chars, last, chars.length)
      
      sb
    }
    
    /**
     * Build a copy of the given string in which all occurrences of chars from the second string
     * have been replaced by the corresponding char from the third string. If there is no
     * corresponding char (i.e. the third string is shorter than the second one), the affected
     * char is removed.
     * 
     * TODO: This method cannot replace code points > 0xFFFF.
     * 
     * @param chars list of characters that should be replaced if they occur in the string.
     * @param target If null, the method may return null or a new StringBuilder. 
     * Otherwise, the method will return the given target.
     * @return null if target was null and no characters had to be escaped
     */
    def replaceChars(target: StringBuilder, str: String, chars : String, rep : String) : StringBuilder = {
      
      var sb = target
      var last = 0
      var pos = 0
      
      while (pos < str.length)
      {
        val ch = str.charAt(pos)
        val index = chars.indexOf(ch)
        if (index != -1)
        {
          if (sb == null) sb = new StringBuilder()
          sb.append(str, last, pos)
          if (index < rep.length) sb.append(rep.charAt(index))
          last = pos + 1
        }
        
        pos += 1
      }
      
      if (sb != null) sb.append(str, last, str.length)
      
      sb
    }
    
    /**
     * Zeropad given number.
     * undefined result for more than 20 or so zeros. May even loop forever because of integer overflow.
     */
    private def zeros(z: Int, n: Long) : String = {
        var p = 10
        var e = 1
        while (e < z) {
          if (n < p) return zeros.substring(0, z - e)+n
          p *= 10
          e += 1
        }
        n.toString
    }
    
    object IntLiteral
    {
        def apply(x : Int) = x.toString

        def unapply(x : String) : Option[Int] =  try { Some(x.toInt) } catch { case _ => None }
    }

    object BooleanLiteral
    {
        def apply(x : Boolean) = x.toString

        def unapply(x : String) : Option[Boolean] =  try { Some(x.toBoolean) } catch { case _ => None }
    }
}
