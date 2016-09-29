package org.dbpedia.extraction.util


/**
 */
class DateFinder[T](val finder: Finder[T]){
  
  def this(baseDir: T, language: Language)(implicit wrap: T => FileLike[T]) = this(new Finder[T](baseDir, language, "wiki"))
  
  def baseDir = finder.baseDir
  
  def language = finder.language
  
  private var _date: String = null
  
  def date =
    if (_date != null)
    _date
  else throw new IllegalStateException("date not set")

  def byName(name: String, auto: Boolean = false): T = {
    if (_date == null) {
      if (! auto) throw new IllegalStateException("date not set")
      _date = finder.dates(name).last
    }
    finder.file(_date, name)
  }

  def byPattern (pattern: String, auto: Boolean = false): Seq[T] = {
    if (_date == null) {
      if (! auto) throw new IllegalStateException("date not set")
      _date = finder.dates(pattern, true, true).last
    }
    finder.matchFiles(_date, pattern).toSeq
  }
}
