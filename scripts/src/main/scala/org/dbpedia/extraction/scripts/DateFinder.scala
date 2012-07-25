package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.{Finder,FileLike,Language}

/**
 */
class DateFinder[T](finder: Finder[T]) {
  
  def this(baseDir: T, language: Language)(implicit wrap: T => FileLike[T]) = this(new Finder[T](baseDir, language))
  
  def baseDir = finder.baseDir
  
  def language = finder.language
  
  private var _date: String = null
  
  def date = if (_date != null) _date else throw new IllegalStateException("date not set")
  
  def find(name: String, auto: Boolean = false): T = {
    if (_date == null) {
      if (! auto) throw new IllegalStateException("date not set")
      _date = finder.dates(name).last
    }
    finder.file(_date, name)
  }
      
}