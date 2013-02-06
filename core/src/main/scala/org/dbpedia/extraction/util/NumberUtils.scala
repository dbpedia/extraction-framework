package org.dbpedia.extraction.util

import java.lang.StringBuilder

/**
 */
object NumberUtils {
  
  private val hexChars = "0123456789ABCDEF".toCharArray
  
  private val intHexValues = {
    val values = new Array[Int](128)
    for (i <- 0 until 128) values(i) = -1
    for (c <- '0' to '9') values(c) = c - '0'
    for (c <- 'A' to 'F') values(c) = c - 'A' + 10
    for (c <- 'a' to 'f') values(c) = c - 'a' + 10
    values
  }
    
  private val longHexValues = {
    val values = new Array[Long](128)
    for (i <- 0 until 128) values(i) = -1
    for (c <- '0' to '9') values(c) = c - '0'
    for (c <- 'A' to 'F') values(c) = c - 'A' + 10
    for (c <- 'a' to 'f') values(c) = c - 'a' + 10
    values
  }
    
  /**
   * Returns hex digits for number. Number is treated as unsigned number.
   * Warning: If the number is too large for the given number of hex digits, 
   * its most significant digits are silently ignored (which is probably not what you want).
   * @digits must be >= 0 and <= 8
   */
  def intToHex(num: Int, digits: Int): String = intToHex(new StringBuilder, num, digits).toString
  
  /**
   * Appends hex digits for number to string builder. Number is treated as unsigned number.
   * Warning: If the number is too large for the given number of hex digits, 
   * its most significant digits are silently ignored (which is probably not what you want).
   * @digits must be >= 0 and <= 8
   */
  def intToHex(sb: StringBuilder, num: Int, digits: Int): StringBuilder = {
    if (digits < 0 || digits > 8) throw new IllegalArgumentException("digits must be >= 0 and <= 8 but is "+digits)
    var bits = digits << 2 // four bits per digit
    while (bits > 0) {
      bits -= 4
      sb.append(hexChars((num >> bits) & 0xF))
    }
    sb
  }

  /**
   * Returns hex digits for number. Number is treated as unsigned number.
   * Warning: If the number is too large for the given number of hex digits, 
   * its most significant digits are silently ignored (which is probably not what you want).
   * @digits must be >= 0 and <= 16
   */
  def longToHex(num: Long, digits: Int): String = longToHex(new StringBuilder, num, digits).toString
  
  /**
   * Appends hex digits for number to string builder. Number is treated as unsigned number.
   * Warning: If the number is too large for the given number of hex digits, 
   * its most significant digits are silently ignored (which is probably not what you want).
   * @digits must be >= 0 and <= 16
   */
  def longToHex(sb: StringBuilder, num: Long, digits: Int): StringBuilder = {
    if (digits < 0 || digits > 16) throw new IllegalArgumentException("digits must be >= 0 and <= 16 but is "+digits)
    var bits = digits << 2 // four bits per digit
    while (bits > 0) {
      bits -= 4
      sb.append(hexChars(((num >> bits) & 0xF).asInstanceOf[Int]))
    }
    sb
  }
  
  def hexToInt(str: String): Int = hexToInt(str, 0, str.length)
    
  def hexToInt(str: String, start: Int, end: Int): Int = {
    if (end < start || end - start > 8) throw new IllegalArgumentException("length must be >= 0 and <= 8 but is "+(end - start)+" (start = "+start+", end = "+end+")")
    var result = 0
    var index = start
    while (index < end) {
      val ch = str.charAt(index)
      val value = if (ch < 128) intHexValues(ch) else -1
      if (value == -1) throw new IllegalArgumentException("invalid hex digit ["+ch+"] in string ["+str+"] at index "+index)
      result = result << 4 | value
      index += 1
    }
    result
  }

  def hexToLong(str: String): Long = hexToLong(str, 0, str.length)
    
  def hexToLong(str: String, start: Int, end: Int): Long = {
    if (end < start || end - start > 16) throw new IllegalArgumentException("length must be >= 0 and <= 16 but is "+(end - start)+" (start = "+start+", end = "+end+")")
    var result = 0L
    var index = start
    while (index < end) {
      val ch = str.charAt(index)
      val value = if (ch < 128) longHexValues(ch) else -1L
      if (value == -1L) throw new IllegalArgumentException("invalid hex digit ["+ch+"] in string ["+str+"] at index "+index)
      result = result << 4 | value
      index += 1
    }
    result
  }
  
}