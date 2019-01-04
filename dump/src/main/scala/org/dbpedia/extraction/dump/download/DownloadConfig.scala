package org.dbpedia.extraction.dump.download

import java.io.File
import java.net.{MalformedURLException, URL}

import org.dbpedia.extraction.config.{Config, ConfigUtils}
import org.dbpedia.extraction.util.{Finder, Language}

import scala.collection.mutable
import scala.util.matching.Regex
import org.dbpedia.extraction.util.RichFile.wrapFile

class DownloadConfig(path: String) extends Config(path)
{
  val baseUrl: URL = this.getArbitraryStringProperty("base-url") match {
    case Some(u) => new URL(u)
    case None => throw new IllegalArgumentException("Properties file is lacking this property: " + "base-url")
  }
  
  var dumpCount = 1
  
  val retryMax: Int = this.getArbitraryStringProperty("retry-max") match {
    case Some(u) => Integer.parseInt(u)
    case None => throw new IllegalArgumentException("Properties file is lacking this property: " + "retry-max")
  }

  val retryMillis: Int = this.getArbitraryStringProperty("retry-millis") match {
    case Some(u) => Integer.parseInt(u)
    case None => throw new IllegalArgumentException("Properties file is lacking this property: " + "retry-millis")
  }

  val unzip: Boolean = this.getArbitraryStringProperty("unzip") match {
    case Some(u) => u.toBoolean
    case None => throw new IllegalArgumentException("Properties file is lacking this property: " + "unzip")
  }

  val dumpDate: Option[String] = this.getArbitraryStringProperty("dump-date") match {
    case Some(u) if u.matches("\\d{8,8}") => Some(u)
    case _ => None
  }
  
  private def add[K](map: mutable.Map[K,mutable.Set[(String, Boolean)]], key: K, values: Array[(String, Boolean)]) =
    map.getOrElseUpdate(key, new mutable.HashSet[(String, Boolean)]) ++= values

  def getDownloadStarted(lang:Language): Option[File] ={
    if(requireComplete){
      val finder = new Finder[File](dumpDir, lang, wikiName)
      val date = finder.dates(source.head).last
      finder.file(date, DownloadConfig.Started) match {
        case None => finder.file(date, DownloadConfig.Started)
        case Some(x) => Some(x)
      }
    }
    else
      None
  }
}

object DownloadConfig
{
  /** name of marker file in wiki directory */
  private val Started = "download-started"

  def toBoolean(s: String, arg: String): Boolean =
    if (s == "true" || s == "false") s.toBoolean else throw Usage("Invalid boolean value", arg) 
  
  def toRange(from: String, to: String, arg: String): (Int, Int) =
  try {
    ConfigUtils.toRange(from, to)
  }
  catch { case nfe: NumberFormatException => throw Usage("invalid range", arg, nfe) }
  
  val DateRange: Regex = """(\d{8})?(?:-(\d{8})?)?""".r
  
  def parseDateRange(range: String, arg: String): (String, String) = {
    range match {
      case DateRange(from, to) =>
        // "" and "-" are invalid
        if ((from == null || from.isEmpty) && (to == null || to.isEmpty)) throw Usage("invalid date range", arg)
        // "-to" means "min-to"
        val lo = if (from == null || from.isEmpty) "00000000" else from
        // "from" means "from-from", "from-" means "from-max"
        val hi = if (to == null) lo else if (to.isEmpty) "99999999" else to
        if (lo > hi) throw Usage("invalid date range", arg)
        (lo, hi)
      case _ => throw Usage("invalid date range", arg)
    }
  }
  
  def toInt(str: String, min: Int, max: Int, arg: String): Int = {
    try {
      val result = str.toInt
      if (result < min) throw new NumberFormatException(str + " < " + min)
      if (result > max) throw new NumberFormatException(str + " > " + max)
      result
    }
    catch {
      case nfe: NumberFormatException => throw Usage("invalid integer", arg, nfe)
    }
  }

  def toURL(s: String, arg: String): URL = {
    try new URL(s)
    catch {
      case mue: MalformedURLException => throw Usage("Invalid URL", arg, mue)
    }
  }

  /**
   * If path is absolute, return it as a File. Otherwise, resolve it against parent.
   * (This method does what the File(File, String) constructor should do. Like URL(URL, String))
    *
    * @param parent may be null
   * @param path must not be null, may be empty
   */
  def resolveFile(parent: File, path: String): File = {
    val child = new File(path)
    val file = if (child.isAbsolute) child else new File(parent, path)
    // canonicalFile removes '/../' etc.
    file.getCanonicalFile
  }
}

object Usage {
  def apply(msg: String, arg: String = null, cause: Throwable = null): Exception = {
    val message = if (arg == null) msg else msg+" in '"+arg+"'"

    new Exception(message, cause)
  }
}

class TwoListArg(key: String, sep1: String, sep2: String, partsIndicator: String)
{
  def unapply(arg: String): Option[(Array[String],Array[(String, Boolean)])] = {
    val index = arg.indexOf('=')
    if (index == -1 || arg.substring(0, index).trim != key) return None
    val parts = arg.substring(index + 1).trim.split(sep1, -1)
    if (parts.length != 2) return None

    Some(parts(0).split(sep2, -1).map(_.trim), parts(1).split(sep2, -1).map { s =>
      (if (s.trim.startsWith(partsIndicator)) s.trim.substring(partsIndicator.length()) else s.trim, s.trim.startsWith(partsIndicator))
    })
  }
}

object Arg {
  def unapply(arg: String): Option[(String, String)] =  {
    val index = arg.indexOf('=')
    if (index == -1) None else Some((arg.substring(0, index).trim, arg.substring(index + 1).trim))
  }
}

object Ignored {
  def unapply(arg: String): Option[String] = if (arg.trim.isEmpty || arg.trim.startsWith("#")) Some(arg) else None
}
