package org.dbpedia.extraction.util

/**
 */
class DateFinder[T](baseDir: T, language: Language, wikiName: String = "wiki")(implicit wrap: T => FileLike[T])
  extends Finder[T](baseDir, language, wikiName)(wrap){
  
  def this(f: Finder[T])(implicit wrap: T => FileLike[T]) =
    this(f.baseDir, f.language, f.wikiName)
  
  private var _date: String = _
  
  def date = if (_date == null) {
    _date = this.dates().last
    _date
  }
  else
    _date

  def byName(name: String, auto: Boolean = false): Option[T] = {
    if (_date == null) {
      if (! auto)
        throw new IllegalStateException("date not set")
      _date = this.dates(name).last
    }
    this.file(_date, name)
  }

  def byPattern (pattern: String, auto: Boolean = false): Seq[T] = {
    if (_date == null) {
      if (! auto) throw new IllegalStateException("date not set")
      _date = this.dates(pattern, true, true).last
    }
    this.matchFiles(_date, pattern)
  }
}
