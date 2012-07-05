package org.dbpedia.extraction.scripts

import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream,BZip2CompressorOutputStream}
import java.util.zip.{GZIPInputStream,GZIPOutputStream}
import java.io.{File,InputStream,OutputStream,Writer,FileInputStream,FileOutputStream,OutputStreamWriter,InputStreamReader,BufferedReader,FileNotFoundException}
import org.dbpedia.extraction.util.{Finder,Language,ConfigUtils,WikiInfo,ObjectTriple}
import org.dbpedia.extraction.util.NumberUtils.{intToHex,longToHex,hexToInt,hexToLong}
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import org.dbpedia.extraction.destinations.DBpediaDatasets
import org.dbpedia.extraction.ontology.{RdfNamespace,DBpediaNamespace}
import scala.collection.immutable.SortedSet
import scala.collection.mutable.{HashSet,HashMap}
import scala.io.{Source,Codec}
import java.util.Arrays.{sort,binarySearch}

private class Title(val language: Language, val title: String)

object ProcessInterLanguageLinks {
  
  private val zippers = Map[String, OutputStream => OutputStream] (
    "gz" -> { new GZIPOutputStream(_) }, 
    "bz2" -> { new BZip2CompressorOutputStream(_) } 
  )
  
  private val unzippers = Map[String, InputStream => InputStream] (
    "gz" -> { new GZIPInputStream(_) }, 
    "bz2" -> { new BZip2CompressorInputStream(_) } 
  )
  
  private def open[T](file: File, opener: File => T, wrappers: Map[String, T => T]): T = {
    val name = file.getName
    val suffix = name.substring(name.lastIndexOf('.') + 1)
    wrappers.getOrElse(suffix, identity[T] _)(opener(file)) 
  }
  
  private def output(file: File) = open(file, new FileOutputStream(_), zippers)
  
  private def input(file: File) = open(file, new FileInputStream(_), unzippers)
  
  // TODO: copy & paste in org.dbpedia.extraction.dump.sql.Import, org.dbpedia.extraction.dump.download.Download, org.dbpedia.extraction.dump.extract.Config
  private def getLanguages(baseDir: File, args: Array[String]): Set[Language] = {
    
    var keys = for(arg <- args; key <- arg.split("[,\\s]"); if (key.nonEmpty)) yield key
        
    var languages = SortedSet[Language]()(Language.wikiCodeOrdering)
    
    val ranges = new HashSet[(Int,Int)]
  
    for (key <- keys) key match {
      case ConfigUtils.Range(from, to) => ranges += ConfigUtils.toRange(from, to)
      case ConfigUtils.Language(language) => languages += Language(language)
      case other => throw new Exception("Invalid language / range '"+other+"'")
    }
    
    // resolve page count ranges to languages
    if (ranges.nonEmpty)
    {
      val listFile = new File(baseDir, WikiInfo.FileName)
      
      // Note: the file is in ASCII, any non-ASCII chars are XML-encoded like '&#231;'. 
      // There is no Codec.ASCII, but UTF-8 also works for ASCII. Luckily we don't use 
      // these non-ASCII chars anyway, so we don't have to unescape them.
      println("parsing "+listFile)
      val wikis = WikiInfo.fromFile(listFile, Codec.UTF8)
      
      // for all wikis in one of the desired ranges...
      for ((from, to) <- ranges; wiki <- wikis; if (from <= wiki.pages && wiki.pages <= to))
      {
        // ...add its language
        languages += Language(wiki.language)
      }
    }
    
    languages
  }
  
  // value copied from InterLanguageLinksExtractor.scala
  private val interLinkUri = DBpediaNamespace.ONTOLOGY.append("wikiPageInterLanguageLink")
  
  private val sameAsUri = RdfNamespace.OWL.append("sameAs")
  
  private val seeAlsoUri = RdfNamespace.RDFS.append("seeAlso")
  
  def main(args: Array[String]) {
    
    /*
    
    Algorithm:
    
    Each URI is a combination of language code and title string. There are only ~9 million 
    unique title strings in the top ~100 languages, so we save space by building an index of title 
    strings and using 24 bits (enough for ~16 million titles) of the index number instead of the 
    title string. We use 8 bits (enough for 256 languages) of the language index instead of the
    language code. Taken together, these 32 bits fit into an Int. The upper 8 bits are the language
    code, the lower 24 bits the title code. -1 is used as the null value.
    
    A link from a page to another page is represented by a Long value which contains the
    concatenation of the Int values for the page titles: the upper 32 bits contain the 'from'
    title, the lower 32 bits contain the 'to' title. All links are stored in one huge array.
    To find an inverse link, we simply swap the upper and lower 32 bits and search the array 
    for the result. To speed up this search, we sort the array and use binary search.
    
    Limitations caused by this bit layout:
    - at most 256 languages (2^8 - 1). We currently use 111.
    - at most ~16 million unique titles (2^24). There currently are ~9 million.
    
    If we use more languages and break these limits, we'll probably need to change the algorithm,
    which won't be easy without letting the memory requirements explode.
    
    Arbitrary limitations:
    - at most ~270 million links (2^28). There currently are ~200 million.
    
    If we break that limit, we can simply increase the array size below and give the JVM more heap.
    
    FIXME: if there are exactly 2^8 languages and exactly 2^24 titles, the 'last' URI will be 
    coded as -1, but -1 is also used as the null value.
    
    */
    
    val uriPrefix = "http://"
    val uriPath = "/resource/"
    
    var domains: Array[String] = null
    var titles: Array[String] = null
    var links: Array[Long] = null
    var linkCount = 0
    
    val baseDir = new File(args(0))
    
    val dumpFile = if (args(1) == "-") null else new File(baseDir, args(1))
    
    // Suffix of DBpedia files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on
    // This script WORKS with .ttl files, SHOULD work with .nt, DOES NOT work with .nq or .tql.
    val fileSuffix = args(2)
    
    // if no languages are given, read dump file
    if (args.length == 3) {
      
      require(dumpFile != null, "no dump file and no languages given")
      
      val dump = readDump(dumpFile)
      domains = dump._1
      titles = dump._2
      links = dump._3
      linkCount = links.length
      
    }
    else {
      
      // Language using generic domain (usually en)
      val generic = if (args(3) == "-") null else Language(args(3))
      
      // Use all remaining args as keys or comma or whitespace separated lists of keys
      val languages = getLanguages(baseDir, args.drop(4)).toArray
      
      domains = new Array[String](1 << 8)
      val domainKeys = new HashMap[String, Int]()
      for (index <- 0 until languages.length) {
        val language = languages(index)
        val domain = if (language == generic) "dbpedia.org" else language.dbpediaDomain
        domains(index) = domain
        domainKeys(domain) = index
      }
      
      // Enough space for ~16 million titles. All languages with 10000+ articles contain about 9 million unique titles.
      titles = new Array[String](1 << 24)
      val titleKeys = new HashMap[String, Int]()
      var titleKey = 0
      
      def parseUri(uri: String): Int = {
        val slash = uri.indexOf('/', uriPrefix.length)
        require(uri.startsWith(uriPath, slash), "bad URI, expected path '"+uriPath+"': "+uri)
        val domain = uri.substring(uriPrefix.length, slash)
        domainKeys.get(domain) match {
          case Some(language) => {
            val title = uri.substring(slash + uriPath.length)
            
            var key = titleKeys.getOrElse(title, -1)
            if (key == -1) {
              key = titleKey
              
              // Note: If there are more than 2^24 titles, this will throw an ArrayIndexOutOfBoundsException
              titles(key) = title
              titleKeys(title) = key
              
              titleKey += 1
            }
            
            // Language key in high bits, title key in low bits. 
            // No need to mask title key - it is less than 2^24.
            language << 24 | key
          }
          case None => -1 // language not in list, ignore this uri
        }
      }
      
      val allStart = System.nanoTime
      var allLines = 0
      var allLinks = 0
      
      // Enough space for ~270 million links. All languages with 10000+ articles contain about 200 million links.
      links = new Array[Long](1 << 28)
      
      for (index <- 0 until languages.length) {
        val language = languages(index)
        val finder = new Finder[File](baseDir, language)
        val name = DBpediaDatasets.InterLanguageLinks.name.replace('_', '-') + fileSuffix
        val file = finder.file(finder.dates(name).last, name)
        
        println(language.wikiCode+": reading "+file+" ...")
        val langStart = System.nanoTime
        var langLines = 0
        var langLinks = 0
        val in = input(file)
        try {
          for (line <- Source.fromInputStream(in, "UTF-8").getLines) {
            line match {
              case ObjectTriple(subjUri, predUri, objUri) => {
                
                val subj = parseUri(subjUri)
                // subj is -1 if its domain was not recognized (generic vs specific?)
                require (subj != -1 && subj >>> 24 == index, "subject has wrong language - expected "+languages(index).wikiCode+", found "+(if (subj == -1) "none" else languages(subj >>> 24).wikiCode)+": "+line)
                
                require (predUri == interLinkUri, "wrong property - expected "+interLinkUri+", found "+predUri+": "+line)
                
                val obj = parseUri(objUri)
                // obj is -1 if its language is not used in this run
                if (obj != -1) {
                  // subject in high 32 bits, object in low 32 bits
                  val link = subj.toLong << 32 | obj.toLong
                  
                  // Note: If there are more than 2^28 links, this will throw an ArrayIndexOutOfBoundsException                
                  links(linkCount) = link
                  linkCount += 1
                  
                  langLinks += 1
                }
              }
              case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match object triple syntax: " + line)
            }
            langLines += 1
            if (langLines % 1000000 == 0) logRead(language.wikiCode, langLines, langLinks, langStart)
          }
        }
        finally in.close()
        logRead(language.wikiCode, langLines, langLinks, langStart)
        allLines += langLines
        allLinks += langLinks
        logRead("total", allLines, allLinks, allStart)
      }
      
      var sortStart = System.nanoTime
      println("sorting "+linkCount+" links...")
      sort(links, 0, linkCount)
      println("sorted "+linkCount+" links in "+prettyMillis((System.nanoTime - sortStart) / 1000000))
      
      if (dumpFile != null) writeDump(dumpFile, domains, titles, links, linkCount)
    }
    
    var writeStart = System.nanoTime
    println("writing "+linkCount+" links...")
    var index = 0
    var sameAs = 0
    while (index < linkCount) {
      val link = links(index)
      val inverse = link >>> 32 | link << 32
      if (binarySearch(links, 0, linkCount, inverse) >= 0) sameAs += 1
      index += 1
      if (index % 10000000 == 0) logWrite(index, sameAs, writeStart)
    }
    logWrite(index, sameAs, writeStart)
  }
  
  private def logRead(name: String, lines: Int, links: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    println(name+": processed "+lines+" lines, found "+links+" links in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
  private def logWrite(links: Int, found: Int, start: Long): Unit = {
    println("wrote "+links+" links, found "+found+" inverse links in "+prettyMillis((System.nanoTime - start) / 1000000))
  }
  
  private def writeDump(dumpFile: File, domains: Array[String], titles: Array[String], links: Array[Long], linkCount: Int): Unit = {
    val writeStart = System.nanoTime
    println("writing dump file...")
    val out = output(dumpFile)
    try {
      val writer = new OutputStreamWriter(out, Codec.UTF8)
      
      println("writing domains...")
      writeStrings(writer, domains)
      
      println("writing titles...")
      writeStrings(writer, titles)
      
      val linkStart = System.nanoTime
      println("writing "+linkCount+" links...")
      writer.write(intToHex(linkCount, 8)+"\n")
      var index = 0
      while (index < linkCount) {
        writer.write(longToHex(links(index), 16)+"\n")
        index += 1
        if (index % 10000000 == 0) logDumpLinks("wrote", index, linkCount, linkStart)
      }
      logDumpLinks("wrote", index, linkCount, linkStart)
      
      writer.close()
    }
    finally out.close()
    println("wrote dump file in "+prettyMillis((System.nanoTime - writeStart) / 1000000))
  }
  
  private def writeStrings(writer: Writer, array: Array[String]): Unit = {
    var length = 0
    for (string <- array) if (string != null) length += 1
    writer.write(intToHex(length, 8)+"\n")
    for (string <- array) if (string != null) writer.write(string+"\n")
  }

  private def readDump(dumpFile: File): (Array[String], Array[String], Array[Long]) = {
    
    var domains, titles: Array[String] = null
    var links: Array[Long] = null
      
    val readStart = System.nanoTime
    println("reading dump file...")
    val in = input(dumpFile)
    try {
      val reader = new BufferedReader(new InputStreamReader(in, Codec.UTF8))
      
      println("reading domains...")
      domains = readStrings(reader)
      
      println("reading titles...")
      titles = readStrings(reader)
      
      val linkStart = System.nanoTime
      val linkCount = hexToInt(reader.readLine())
      println("reading "+linkCount+" links...")
      links = new Array[Long](linkCount)
      var index = 0
      while (index < linkCount) {
        links(index) = hexToLong(reader.readLine())
        index += 1
        if (index % 10000000 == 0) logDumpLinks("read", index, linkCount, linkStart)
      }
      logDumpLinks("read", index, linkCount, linkStart)
      
      reader.close()
    }
    finally in.close()
    println("read dump file in "+prettyMillis((System.nanoTime - readStart) / 1000000))
    
    (domains, titles, links)
  }
  
  private def readStrings(reader: BufferedReader): Array[String] = {
    val length = hexToInt(reader.readLine())
    val array = new Array[String](length)
    var index = 0
    while (index < length) {
      array(index) = reader.readLine()
      index += 1
    }
    array
  }
  
  private def logDumpLinks(did: String, index: Int, count: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    println(did+" "+index+" of "+count+" links ("+(100F * index / count)+" %) in "+prettyMillis(micros / 1000))
  }

}
