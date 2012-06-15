package org.dbpedia.extraction.dump.download

import java.io.File
import java.net.{URL,MalformedURLException}
import scala.collection.mutable.{Set,HashSet,Map,HashMap}
import scala.io.{Source,Codec}
import org.dbpedia.extraction.dump.util.ConfigUtils

class DownloadConfig
{
  var baseUrl: URL = null
  
  var baseDir: File = null
  
  val languages = new HashMap[String, Set[String]]
  
  val ranges = new HashMap[(Int,Int), Set[String]]
  
  var retryMax = 0
  
  var retryMillis = 10000
  
  var unzip = false
  
  var progressPretty = false
  
  /**
   * Parse config in given file. Each line in file must be an argument as explained by usage overview.
   */
  def parse(file: File): Unit = {
    val source = Source.fromFile(file)(Codec.UTF8)
    try parse(file.getParentFile, source.getLines) 
    finally source.close
  }
  
  /**
   * @param dir Context directory. Config file and base dir names will be resolved relative to 
   * this path. If this method is called for a config file, this argument should be the directory
   * of that file. Otherwise, this argument should be the current working directory (or null,
   * which has the same effect).
   */
  def parse(dir: File, args: TraversableOnce[String]): Unit = {
    
    val DumpFiles = new TwoListArg("dump", ":", ",")
    
    for(a <- args; arg = a.trim) arg match
    {
      case Ignored(_) => // ignore
      case Arg("base", url) => baseUrl = toURL(if (url endsWith "/") url else url+"/", arg) // must have slash at end
      case Arg("dir", path) => baseDir = resolveFile(dir, path)
      case Arg("retry-max", count) => retryMax = toInt(count, 1, Int.MaxValue, arg)
      case Arg("retry-millis", millis) => retryMillis = toInt(millis, 0, Int.MaxValue, arg)
      case Arg("unzip", bool) => unzip = toBoolean(bool, arg)
      case Arg("pretty", bool) => progressPretty = toBoolean(bool, arg)
      case Arg("config", path) =>
        val file = resolveFile(dir, path)
        if (! file.isFile) throw Usage("Invalid file "+file, arg)
        parse(file)
      case DumpFiles(keys, files) =>
        if (files.exists(_ isEmpty)) throw Usage("Invalid file name", arg)
        for (key <- keys) key match {
          case ConfigUtils.Range(from, to) => add(ranges, toRange(from, to, arg), files)
          case ConfigUtils.Language(language) => add(languages, language, files)
          case other => throw Usage("Invalid language / range '"+other+"'", arg)
        }
      case _ => throw Usage("Invalid argument '"+arg+"'")
    }
  }
  
  private def add[K](map: Map[K,Set[String]], key: K, values: Array[String]) = 
    map.getOrElseUpdate(key, new HashSet[String]) ++= values
  
  private def toBoolean(s: String, arg: String): Boolean =
    if (s == "true" || s == "false") s.toBoolean else throw Usage("Invalid boolean value", arg) 
  
  private def toRange(from: String, to: String, arg: String): (Int, Int) =
  try {
    ConfigUtils.toRange(from, to)
  }
  catch { case nfe: NumberFormatException => throw Usage("invalid range", arg, nfe) }
  
  private def toInt(str: String, min: Int, max: Int, arg: String): Int =
  try {
    val result = str.toInt
    if (result < min) throw new NumberFormatException(str+" < "+min)
    if (result > max) throw new NumberFormatException(str+" > "+max)
    result
  }
  catch { case nfe: NumberFormatException => throw Usage("invalid integer", arg, nfe) }
  
  private def toURL(s: String, arg: String): URL =
  try new URL(s)
  catch { case mue: MalformedURLException => throw Usage("Invalid URL", arg, mue) }
  
  /**
   * If path is absolute, return it as a File. Otherwise, resolve it against parent.
   * (This method does what the File(File, String) constructor should do. Like URL(URL, String))
   * @param parent may be null
   * @param path must not be null, may be empty
   */
  private def resolveFile(parent: File, path: String): File = {
    val child = new File(path)
    val file = if (child.isAbsolute) child else new File(parent, path)
    // canonicalFile removes '/../' etc.
    file.getCanonicalFile
  }
  
}

object Usage {
  def apply(msg: String, arg: String = null, cause: Throwable = null): Exception = {
    val message = if (arg == null) msg else msg+" in '"+arg+"'"
    
    println(message)
    val usage = /* empty line */ """
Usage (with example values):
config=/example/path/file.cfg
  Path to exisiting UTF-8 text file whose lines contain arguments in the format given here.
  Absolute or relative path. File paths in that config file will be interpreted relative to
  the config file.
base=http://dumps.wikimedia.org/
  Base URL of dump server. Required if dump files are given.
dir=/example/path
  Path to existing target directory. Required.
dump=en,zh-yue,1000-2000,...:file1,file2,...
  Download given files for given languages from server. Each key is either a language code
  or a range. In the latter case, languages with a matching number of articles will be used. 
  If the start of the range is omitted, 0 is used. If the end of the range is omitted, 
  infinity is used. For each language, a new sub-directory is created in the target directory.
  Each file is a file name like 'pages-articles.xml.bz2', to which a prefix like 
  'enwiki-20120307-' will be added. This argument can be used multiple times, for example 
  'dump=en:foo.xml dump=de:bar.xml'
other=http://svn.wikimedia.org/svnroot/mediawiki/trunk/phase3/maintenance/tables.sql
  URL of other file to download to the target directory. Optional. This argument can be used 
  multiple times, for example 'other=http://a.b/c.de other=http://f.g/h.ij'
retry-max=5
  Number of total attempts if the download of a file fails. Default is no retries.
retry-millis=1000
  Milliseconds between attempts if the download of a file fails. Default is 10000 ms = 10 seconds.  
unzip=true
  Should downloaded .gz and .bz2 files be unzipped on the fly? Default is false.
pretty=true
  Should progress printer reuse one line? Doesn't work with log files, so default is false.
Order is relevant - for single-value parameters, values read later overwrite earlier values.
Empty arguments or arguments beginning with '#' are ignored.
""" /* empty line */ 
    println(usage)
    
    new Exception(message, cause)
  }
}

class TwoListArg(key: String, sep1: String, sep2: String)
{
  def unapply(arg: String): Option[(Array[String],Array[String])] = {
    val index = arg.indexOf('=')
    if (index == -1 || arg.substring(0, index).trim != key) return None
    val parts = arg.substring(index + 1).trim.split(sep1, -1)
    if (parts.length != 2) return None
    Some(parts(0).split(sep2, -1).map(_.trim), parts(1).split(sep2, -1).map(_.trim))
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