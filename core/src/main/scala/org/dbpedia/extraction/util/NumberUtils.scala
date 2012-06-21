package org.dbpedia.extraction.util

import java.lang.StringBuilder

/**
 */
object NumberUtils {
  
  private val chars = "0123456789ABCDEF".toCharArray
    
  /**
   * Returns hex digits for number. Number is treated as unsigned number.
   * Warning: If the number is too large for the given number of hex digits, 
   * its most significant digits are silently ignored (which is probably not what you want).
   * @digits must be >= 0 and <= 8
   */
  def toHex(num: Int, digits: Int): String = toHex(new StringBuilder, num, digits).toString
  
  /**
   * Appends hex digits for number to string builder. Number is treated as unsigned number.
   * Warning: If the number is too large for the given number of hex digits, 
   * its most significant digits are silently ignored (which is probably not what you want).
   * @digits must be >= 0 and <= 8
   */
  def toHex(sb: StringBuilder, num: Int, digits: Int): StringBuilder = {
    require (digits >= 0 && digits <= 8, "digits must be >= 0 and <= 8 but is "+digits)
    var bits = digits << 2 // four bits per digit
    while (bits > 0) {
      bits -= 4
      sb.append(chars((num >> bits) & 0xF))
    }
    sb
  }

  /**
   * Returns hex digits for number. Number is treated as unsigned number.
   * Warning: If the number is too large for the given number of hex digits, 
   * its most significant digits are silently ignored (which is probably not what you want).
   * @digits must be >= 0 and <= 16
   */
  def toHex(num: Long, digits: Int): String = toHex(new StringBuilder, num, digits).toString
  
  /**
   * Appends hex digits for number to string builder. Number is treated as unsigned number.
   * Warning: If the number is too large for the given number of hex digits, 
   * its most significant digits are silently ignored (which is probably not what you want).
   * @digits must be >= 0 and <= 16
   */
  def toHex(sb: StringBuilder, num: Long, digits: Int): StringBuilder = {
    require (digits >= 0 && digits <= 16, "digits must be >= 0 and <= 16 but is "+digits)
    var bits = digits << 2 // four bits per digit
    while (bits > 0) {
      bits -= 4
      sb.append(chars(((num >> bits) & 0xF).asInstanceOf[Int]))
    }
    sb
  }
}