package org.dbpedia.extraction.util

/**
 * Matches a line containing three URIs. Usage example:
 * 
 * line.trim match {
 *   case ObjectTriple(subj, pred, obj) => { ... }
 * }
 * 
 * WARNING: there are several deviations from the N-Triples / Turtle specifications.
 */
object ObjectTriple extends UriTriple(3)

/**
 * Matches a line containing two URIs and a literal value. Usage example:
 * 
 * line.trim match {
 *   case DatatypeTriple(subj, pred, obj) => { ... }
 * }
 * 
 * WARNING: there are several deviations from the N-Triples / Turtle specifications.
 */
object DatatypeTriple extends UriTriple(2)

/**
 * Do not use this class directly. Use objects ObjectTriple or DatatypeTriple.
 * 
 * TODO: Clean up this code a bit. Fix the worst deviations from Turtle/N-Triples spec, 
 * clearly document the others. Unescape \U stuff while parsing the line. Return Quad objects 
 * instead of arrays. Throw exceptions instead of returning None.
 * 
 * @param uris must be 
 */
sealed class UriTriple private[util](uris: Int) {
  
  private def skipSpace(line: String, start: Int): Int = {
    var index = start
    while (index < line.length && line.charAt(index) == ' ') {
      index += 1 // skip space
    } 
    index
  }
  
  /** @param line must not start or end with whitespace - use line.trim. */
  def unapplySeq(line: String) : Option[Seq[String]] =  {
    var count = 0
    var index = 0
    val triple = new Array[String](3)
    
    while (count < uris) {
      if (index == line.length || line.charAt(index) != '<') return None
      val end = line.indexOf('>', index + 1)
      if (end == -1) return None
      triple(count) = line.substring(index + 1, end)
      count += 1
      index = skipSpace(line, end + 1)
    }
    
    if (uris == 2) { // literal
      if (index == line.length || line.charAt(index) != '"') return None
      var end = index + 1
      while (line.charAt(end) != '"') {
        if (line.charAt(end) == '\\') end += 1
        end += 1
        if (end >= line.length) return None
      } 
      triple(2) = line.substring(index + 1, end)
      index = end + 1
      if (index == line.length) return None
      val ch = line.charAt(index)
      if (ch == '@') { 
        // FIXME: This code matches: @[a-z][a-z0-9-]*
        // NT spec says: '@' [a-z]+ ('-' [a-z0-9]+ )*
        // Turtle spec says: "@" [a-zA-Z]+ ( "-" [a-zA-Z0-9]+ )*
        index += 1 // skip '@'
        if (index == line.length) return None
        var c = line.charAt(index)
        if (c < 'a' || c > 'z') return None
        do {
          index += 1 // skip last lang char
          if (index == line.length) return None
          c = line.charAt(index)
        } while (c == '-' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z'))
      }
      else if (ch == '^') { // type uri: ^^<...>
        if (! line.startsWith("^^<", index)) return None
        index = line.indexOf('>', index + 3)
        if (index == -1) return None
        index += 1 // skip '>'
      } 
      else if (ch != ' ' && ch != '.') {
        return None
      }
      index = skipSpace(line, index)
    }
    
    if (line.charAt(index) != '.') return None
    index = skipSpace(line, index + 1)
    if (index != line.length) return None
    Some(triple)
  }
  
}
