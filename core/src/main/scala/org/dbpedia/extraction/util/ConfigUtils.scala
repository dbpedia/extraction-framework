package org.dbpedia.extraction.util

object ConfigUtils {
  /**
   * Simple regex matching Wikipedia language codes.
   * Language codes have at least two characters, start with a lower-case letter and contain only 
   * lower-case letters and dash, but there are also dumps for "wikimania2005wiki" etc.
   */
  val Language = """([a-z][a-z0-9-]+)""".r
    
  /**
   * Regex for numeric range, both limits optional
   */
  val Range = """(\d*)-(\d*)""".r
  
  def toRange(from: String, to: String): (Int, Int) = {
    val lo: Int = if (from isEmpty) 0 else from.toInt
    val hi: Int = if (to isEmpty) Int.MaxValue else to.toInt
    if (lo > hi) throw new NumberFormatException
    (lo, hi)
  }
  
  /**
   * TODO: move this method to Finder?
   */
  def latestDate(finder: Finder[_], fileName: String): String = {
    val dates = finder.dates(fileName)
    if (dates.isEmpty) throw new IllegalArgumentException("found no directory with file '"+finder.wikiName+"-[YYYYMMDD]-"+fileName+"'")
    dates.last
  }
  
}