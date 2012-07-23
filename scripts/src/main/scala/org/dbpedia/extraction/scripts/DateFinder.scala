package org.dbpedia.extraction.scripts

import java.io.File
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.{Finder,Language}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import IOUtils._

/**
 */
class DateFinder(val baseDir: File, val language: Language, private var _date: String = null) {
  
  private val finder = new Finder[File](baseDir, language)
  
  def date = if (_date != null) _date else throw new IllegalStateException("date not set")
  
  def find(name: String, auto: Boolean = false): File = {
    if (_date == null) {
      if (! auto) throw new IllegalStateException("date not set")
      _date = finder.dates(name).last
    }
    finder.file(_date, name)
  }
      
}