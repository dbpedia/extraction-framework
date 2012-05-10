package org.dbpedia.extraction.util

import java.lang.StringBuilder

/**
 * TODO: add toHex methods for Long.
 */
object NumberUtils {
  
  private val chars = "0123456789ABCDEF".toCharArray
    
  def toHex(num: Int, digits: Int): String = toHex(new StringBuilder, num, digits).toString
  
  /**
   * Appends hex digits for number to string builder. Number is treated as unsigned number.
   * @digits must be >= 0 and <= 8
   */
  def toHex(sb: StringBuilder, num: Int, digits: Int): StringBuilder = {
    require (digits >= 0 && digits <= 8, "digits must be >= 0 and <= 8 but is "+digits)
    var sh = digits << 2
    while (sh > 0) {
      sh -= 4
      sb.append(chars((num >> sh) & 0xF))
    }
    sb
  }

}