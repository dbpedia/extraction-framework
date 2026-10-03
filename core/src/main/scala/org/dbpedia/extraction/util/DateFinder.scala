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

  def byName(name: String, auto: Boolean = false, required: Boolean = true): Option[T] = {
    if (_date == null) {
      if (! auto)
        throw new IllegalStateException("date not set")
      val dates = finder.dates(name, required)
      if (dates.isEmpty) return None
      _date = dates.last
    }
    finder.file(_date, name)
  }

  def byPattern (pattern: String, auto: Boolean = false, required: Boolean = true): Seq[T] = {
    if (_date == null) {
      if (! auto) throw new IllegalStateException("date not set")
      val dates = finder.dates(pattern, required, isSuffixRegex = true)
      if (dates.isEmpty) return Seq.empty
      _date = dates.last
    }
    finder.matchFiles(_date, pattern).toSeq
  }
}
