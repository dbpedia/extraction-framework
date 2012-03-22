package org.dbpedia.extraction.dump.download

import java.io.File
import java.net.{URL,MalformedURLException}
import scala.collection.mutable.{Set,HashSet,HashMap}
import scala.io.{Source,Codec}

class Config
{
  var baseUrl : URL = null
  
  var baseDir : File = null
  
  var csvUrl : URL = null
  
  val languages = new HashMap[String, Set[String]]
  
  val ranges = new HashMap[(Int,Int), Set[String]]
  
  val others = new HashSet[URL]
  
  var retryMax = 0
  
  var retryMillis = 10000
  
  var unzip : Boolean = false
  
  def validate =
  {
    if (baseDir == null) throw Usage("No target directory")
    if ((languages.nonEmpty || ranges.nonEmpty) && baseUrl == null) throw Usage("No base URL")
    if (ranges.nonEmpty && csvUrl == null) throw Usage("No CSV file URL")
    if (languages.isEmpty && ranges.isEmpty && others.isEmpty) throw Usage("No files to download")
  }

  /**
   * Parse config in given file. Each line in file must be an argument as explained by usage overview.
   */
  def parse( file : File ) : Unit =
  {
    val source = Source.fromFile(file)(Codec.UTF8)
    try
    {
      parse(file.getParentFile, source.getLines)
    } 
    finally source.close
  }
  
  /**
   * @param dir Context directory. Config file and base dir names will be resolved relative to 
   * this path. If this method is called for a config file, this argument should be the directory
   * of that file. Otherwise, this argument should be the current working directory.
   */
  def parse( dir : File, args : TraversableOnce[String] ) : Unit =
  {
    // range, both limits optional
    val Range = """(\d*)-(\d*)""".r
    
    for(a <- args; arg = a.trim)
    {
      if (arg startsWith "config=")
      {
        val file = resolveFile(dir, arg.substring("config=".length))
        if (! file.isFile) throw Usage("Invalid file "+file, arg)
        parse(file)
      }
      else if (arg startsWith "base=")
      {
        var url = arg.substring("base=".length)
        if (! url.endsWith("/")) url += "/" // make sure that base URL ends with slash
        baseUrl = toURL(url, arg)
      }
      else if (arg startsWith "dir=")
      {
        baseDir = resolveFile(dir, arg.substring("dir=".length))
      }
      else if (arg startsWith "csv=")
      {
        csvUrl = toURL(arg.substring("csv=".length), arg)
      }
      else if (arg startsWith "dump=")
      {
        val values = arg.substring("dump=".length).split(":", -1)
        if (values.length == 1) throw Usage("No ':'", arg)
        if (values.length > 2) throw Usage("More than one ':'", arg)
        
        val files = values(1).split(",", -1)
        if (files(0).isEmpty) throw Usage("No files", arg)
        
        val keys = values(0).split(",", -1)
        for (key <- keys)
        {
          if (key.isEmpty) throw Usage("Empty language / range", arg)
          var set = key match
          {
            case Range(from, to) => ranges.getOrElseUpdate(toRange(from, to, arg), new HashSet[String])
            case WikiInfo.Language(_) => languages.getOrElseUpdate(key, new HashSet[String])
            case other => throw Usage("Invalid language / range '"+other+"'", arg)
          }
          set ++= files
        }
      }
      else if (arg startsWith "other=") 
      {
        others += toURL(arg.substring("other=".length), arg)
      }
      else if (arg startsWith "retry-max=") 
      {
        retryMax = toInt(arg.substring("retry-max=".length), 0, Int.MaxValue, arg)
      }
      else if (arg startsWith "retry-millis=") 
      {
        retryMillis = toInt(arg.substring("retry-millis=".length), 0, Int.MaxValue, arg)
      }
      else if (arg startsWith "unzip=") 
      {
        unzip = toBoolean(arg.substring("unzip=".length), arg)
      }
      else if (arg.nonEmpty && ! arg.startsWith("#"))
      {
        throw Usage("Invalid argument '"+arg+"'")
      }
    }
  }
  
  private def toBoolean(s : String, arg : String) : Boolean =
  {
    if (s != "true" && s != "false") throw Usage("Invalid boolean value", arg)
    s.toBoolean
  }
  
  private def toRange(from : String, to : String, arg : String) : (Int, Int) =
  {
    try
    {
      val lo : Int = if (from isEmpty) 0 else from.toInt
      val hi : Int = if (to isEmpty) Int.MaxValue else to.toInt
      if (lo > hi) throw new NumberFormatException
      (lo, hi)
    }
    catch
    {
      case nfe : NumberFormatException => throw Usage("invalid range", arg, nfe)
    }
  }
  
  private def toInt(str : String, min : Int, max : Int, arg : String) : Int =
  {
    try
    {
      val result = str.toInt
      if (result < min) throw new NumberFormatException(str+" < "+min)
      if (result > max) throw new NumberFormatException(str+" > "+max)
      result
    }
    catch
    {
      case nfe : NumberFormatException => throw Usage("invalid integer", arg, nfe)
    }
  }
  
  private def toURL(s : String, arg : String) : URL =
  {
    try
    {
      new URL(s)
    }
    catch
    {
      case mue : MalformedURLException => throw Usage("Invalid URL", arg, mue)
    }
  }
  
  /**
   * If path is absolute, return it as a File. Otherwise, resolve it against parent.
   * (This method does what the File(File, String) constructor should do. Like URL(URL, String))
   * @param parent may be null
   * @param path must not be null, may be empty
   */
  private def resolveFile(parent : File, path : String) : File =
  {
    val child = new File(path)
    if (child.isAbsolute) child else new File(parent, path).getAbsoluteFile
  }
}


object Usage
{
  def apply( msg : String, arg : String = null, cause : Throwable = null ) : Exception =
  {
    val message = if (arg == null) msg else msg+" in '"+arg+"'"
    
    println(message)
    val usage = /* empty line */ """
    |Usage (with example values):
    |config=/example/path/file.cfg
    |  Path to exisiting UTF-8 text file whose lines contain arguments in the format given here.
    |  Absolute or relative path. File paths in that config file will be interpreted relative to
    |  the config file.
    |base=http://dumps.wikimedia.org/
    |  Base URL of dump server. Required if dump files are given.
    |dir=/example/path
    |  Path to existing target directory. Required.
    |csv=http://s23.org/wikistats/wikipedias_csv.php
    |  URL of csv file containing list of wikipedias with article count. First line is header,
    |  third column is language code, sixth column is article count. Required if ranges are used.
    |dump=en,zh-yue,1000-2000,...:file1,file2,...
    |  Download given files for given languages from server. Each key is either a language code
    |  or a range. In the latter case, languages with a matching number of articles will be used. 
    |  If the start of the range is omitted, 0 is used. If the end of the range is omitted, 
    |  infinity is used. For each language, a new sub-directory is created in the target directory.
    |  Each file is a file name like 'pages-articles.xml.bz2', to which a prefix like 
    |  'enwiki-20120307-' will be added. This argument can be used multiple times, for example 
    |  'dump=en:foo.xml dump=de:bar.xml'
    |other=http://svn.wikimedia.org/svnroot/mediawiki/trunk/phase3/maintenance/tables.sql
    |  URL of other file to download to the target directory. Optional. This argument can be used 
    |  multiple times, for example 'other=http://a.b/c.de other=http://f.g/h.ij'
    |retry-max=5
    |  Number of total attempts if the download of a file fails. Default is no retries.
    |retry-millis=1000
    |  Milliseconds between attempts if the download of a file fails. Default is 10000 ms = 10 seconds.  
    |unzip=true
    |  Should downloaded .gz and .bz2 files be unzipped on the fly? Default is false.
    |Order is relevant - for single-value parameters, values read later overwrite earlier values.
    |Empty arguments or arguments beginning with '#' are ignored.
    |""" /* empty line */ 
    println(usage.stripMargin)
    
    new Exception(message, cause)
  }
}
