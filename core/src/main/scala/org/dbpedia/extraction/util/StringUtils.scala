package org.dbpedia.extraction.util

import java.lang.StringBuilder
import org.dbpedia.extraction.util.NumberUtils.intToHex
import java.text.{DateFormat,SimpleDateFormat}
import java.util.TimeZone
import java.security.MessageDigest

/**
 */
object StringUtils
{
  // SimpleDateFormat is expensive but not thread-safe
  private val timestampFormat = new ThreadLocal[DateFormat] {
    override def initialValue() = {
      // Note: the 'X' at the end of the format string and timezone "UTC" are important
      val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
      format.setTimeZone(TimeZone.getTimeZone("UTC"))
      format
    }
  }
  
  def formatCurrentTimestamp = formatTimestamp(System.currentTimeMillis)

  def formatTimestamp(timestamp: Long): String = {
    if (timestamp < 0) ""
    else timestampFormat.get.format(timestamp)
  }

  def parseTimestamp(str: String): Long = {
    if (str == null || str.isEmpty) -1
    else timestampFormat.get.parse(str).getTime
  }
  
  /** 
   * Displays minutes, seconds and milliseconds.
   */
  def prettyMillis(millis : Long) = zeros(2, millis / 60000)+':'+zeros(2, millis % 60000 / 1000)+'.'+zeros(3, millis % 1000)+'s'
  
  private val zeros = "0000000000000000000000000"
  
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
  
  /**
   * Build a copy of the first string in which all occurrences of chars from the third string
   * have been converted to UTF-8 hex escapes where each two-digit hex byte is prefixed by the 
   * given escape character.
   * 
   * This method is pretty inefficient. If it is repeatedly used with the same chars, 
   * you should use replacements() and escape() below.
   * 
   * TODO: don't double-escape existing escape sequences
   * 
   * TODO: This method cannot replace code points > 0xFFFF.
   * 
   * @param rep list of replaced characters that should be escaped if they occur in the string.
   * @param target If null, the method may return null or a new StringBuilder. 
   * Otherwise, the method will return the given target.
   * @return null if target was null and no characters had to be escaped
   */
  def escape(target: StringBuilder, str: String, esc: Char, replace: String): StringBuilder = {
    
    var sb = target
    val chars = str.toCharArray
    
    var last = 0
    var pos = 0
    
    while (pos < chars.length)
    {
      val c = chars(pos)
      
      if (replace.indexOf(c) != -1) {
        
        if (sb == null) sb = new StringBuilder
        sb.append(chars, last, pos - last)
        last = pos + 1
        
        if (c < 0x80) {
          // ASCII chars need no expensive UTF-8 encoding
          sb append esc
          intToHex(sb, c, 2)
        } else {
          // TODO: do it like java.net.URI.encode(), probably faster
          val bytes = new String(Array(c)).getBytes("UTF-8")
          for (b <- bytes) {
            sb append esc
            intToHex(sb, b, 2)
          }
        }
        
      }
      pos += 1
    }
    
    if (sb != null) sb.append(chars, last, chars.length - last)
    
    sb
  }
  
  /**
   * Produces a replacement array that can be used for escape() below. Call this method once,
   * save its result and use it to call escape() below repeatedly.
   * @param limit highest allowed code point. Safety catch - if someone gave us a string 
   * containing a very high code point, we would allocate an array whose length is that 
   * code point. If that is desired, at least force the user to make it explicit.
   */
  def replacements(esc: Char, chars: String, limit: Int = 256): Array[String] = {
    if (chars.isEmpty) {
      new Array[String](0)
    } else {
      val max = chars.max(Ordering.Char)
      if (max >= limit) throw new IllegalArgumentException("highest code point "+max.toInt+" exceeds limit "+limit)
      val replace = new Array[String](max + 1)
      var pos = 0
      while (pos < chars.length)
      {
        val c = chars.charAt(pos)
        val sb = new StringBuilder
        if (c < 0x80) {
          // ASCII chars need no expensive UTF-8 encoding
          sb append esc
          intToHex(sb, c, 2)
        } else {
          // TODO: do it like java.net.URI.encode(), probably faster
          val bytes = new String(Array(c)).getBytes("UTF-8")
          for (b <- bytes) {
            sb append esc
            intToHex(sb, b, 2)
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
   * TODO: don't double-escape existing escape sequences
   * 
   * TODO: This method cannot replace code points > 0xFFFF.
   * 
   * @param input input string
   * @param replace mapping from characters to replacement strings.
   * @return new string if characters had to be escaped, input string otherwise
   */
  def escape(input: String, replace: Array[String]): String = {
      val sb = StringUtils.escape(null, input, replace)
      if (sb == null) input else sb.toString
  }
    
  /**
   * Build a copy of the given string, replacing some chars by replacement strings. 
   * The replacement array is indexed by character value. Only characters for which
   * the replacement array contains a non-null value will be replaced.
   * 
   * TODO: don't double-escape existing escape sequences
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
          sb.append(chars, last, pos - last)
          last = pos + 1
          sb.append(rep)
        }
      }
      pos += 1
    }
    
    if (sb != null) sb.append(chars, last, chars.length - last)
    
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
   * @param replace list of replaced characters
   * @param by list of replacement characters
   * @param target If null, the method may return null or a new StringBuilder. 
   * Otherwise, the method will return the given target.
   * @return null if target was null and no characters had to be escaped
   */
  def replaceChars(target: StringBuilder, str: String, replace: String, by: String) : StringBuilder = {
    
    var sb = target
    val chars = str.toCharArray
    
    var last = 0
    var pos = 0
    
    while (pos < chars.length)
    {
      val ch = chars(pos)
      val index = replace.indexOf(ch)
      if (index != -1)
      {
        if (sb == null) sb = new StringBuilder()
        sb.append(chars, last, pos - last)
        if (index < by.length) sb.append(by.charAt(index))
        last = pos + 1
      }
      
      pos += 1
    }
    
    if (sb != null) sb.append(chars, last, chars.length - last)
    
    sb
  }

  /**
   * Caclulate the MD5 sum of the input String
   *
   * @param input the string we want to calculate the MD5 sum for
   * @return the md5sum hash of the input string
   */
  def md5sum(input: String): String = {
    val inputBytes = input.getBytes("UTF-8")
    val md5 = MessageDigest.getInstance("MD5")
    // TODO: Do we really have to call reset? We just created a new object...
    md5.reset()
    md5.update(inputBytes)

    md5.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft(""){_ + _}
  }
  
  object IntLiteral {
    def apply(x : Int) = x.toString
    def unapply(x : String) : Option[Int] =  try Some(x.toInt) catch { case _ : NumberFormatException => None }
  }

  object BooleanLiteral {
    def apply(x : Boolean) = x.toString
    def unapply(x : String) : Option[Boolean] =  try Some(x.toBoolean) catch { case _ : IllegalArgumentException => None }
  }
}
