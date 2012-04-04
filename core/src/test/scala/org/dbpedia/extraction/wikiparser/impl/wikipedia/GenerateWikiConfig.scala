package org.dbpedia.extraction.wikiparser.impl.wikipedia

import scala.io.{Source, Codec}
import javax.xml.stream.XMLInputFactory
import scala.collection.mutable
import org.dbpedia.extraction.util.WikiConfigDownloader
import java.io.{IOException, OutputStreamWriter, FileOutputStream, Writer}

object GenerateWikiConfig {
  
  def main(args: Array[String]) : Unit = {
    
    val errors = mutable.LinkedHashMap[String, String]()
    
    val namespaceMap = mutable.LinkedHashMap[String, mutable.Map[String, Int]]()
    
    val aliasMap = mutable.LinkedHashMap[String, mutable.Map[String, Int]]()
    
    val redirectMap = mutable.LinkedHashMap[String, mutable.Set[String]]()
    
    for (language <- getLanguages) {
      print(language)
      try
      {
        val (namespaces, aliases, redirects) = new WikiConfigDownloader(language).download()
        namespaceMap.put(language, namespaces)
        aliasMap.put(language, aliases)
        redirectMap.put(language, redirects)
        println(" - OK")
      } catch {
        case ioex : IOException => {
          errors.put(language, ioex.getMessage)
          println(" - "+ioex.getMessage)
        }
      }
    }
    
    // pattern for insertion point lines
    val Insert = """// @ insert (\w+) here @ //""".r
  
    val nsSrc = Source.fromFile("src/test/resources/org/dbpedia/extraction/wikiparser/impl/wikipedia/Namespaces.scala.txt")(Codec.UTF8)
    try {
      val nsDst = new OutputStreamWriter(new FileOutputStream("src/main/scala/org/dbpedia/extraction/wikiparser/impl/wikipedia/Namespaces.scala"), "UTF-8")
      try
      {
        for (line <- nsSrc.getLines) line match {
          case Insert("namespaces") => insertNamespaces(nsDst, "namespaces", namespaceMap)
          case Insert("aliases") => insertNamespaces(nsDst, "aliases", aliasMap)
          case Insert("errors") => insertErrors(nsDst, errors)
          case Insert(_) => throw new Exception("unknown insertion point "+line)
          case _ => nsDst.write(line); nsDst.write('\n')
        }
      } finally nsDst.close
    } 
    finally nsSrc.close
    
    val reSrc = Source.fromFile("src/test/resources/org/dbpedia/extraction/wikiparser/impl/wikipedia/Redirect.scala.txt")(Codec.UTF8)
    try {
      val reDst = new OutputStreamWriter(new FileOutputStream("src/main/scala/org/dbpedia/extraction/wikiparser/impl/wikipedia/Redirect.scala"), "UTF-8")
      try
      {
        for (line <- reSrc.getLines) line match {
          case Insert("redirects") => insertRedirects(reDst, redirectMap)
          case Insert(_) => throw new Exception("unknown insertion point "+line)
          case _ => reDst.write(line); reDst.write('\n')
        }
      } finally reDst.close
    } 
    finally reSrc.close
  }
  
  def getLanguages() : List[String] = {
    val source = Source.fromURL("http://noc.wikimedia.org/conf/langlist")(Codec.UTF8)
    try source.getLines.toList finally source.close
  }

  private def insertNamespaces(dst : Writer, suffix : String, map : mutable.Map[String, mutable.Map[String, Int]] ) : Unit = {
    
    var firstLang = true
    dst.write("    Map(\n")
    for ((language, namespaces) <- map) {
      if (firstLang) firstLang = false else dst.write(",\n") 
      dst.write("        \""+language+"\" -> "+language.replace('-', '_')+'_'+suffix)
    }
    dst.write("\n    )\n")
    
    for ((language, namespaces) <- map) {
      // We used to generate the map as one huge value, but then constructor code is generated 
      // that is so long that the JVM  doesn't load it. So we have to use separate functions.
      dst.write("\n    private def "+language.replace('-','_')+'_'+suffix+" = Map(\n")
      var firstNS = true
      for ((name, code) <- namespaces) {
        if (firstNS) firstNS = false else dst.write(",\n")
        if (name.isEmpty) dst.write("//")
        dst.write("        \""+name+"\" -> "+code);
      }
      dst.write("\n    )\n")
    }
    dst.write("\n")
  }
  
  private def insertErrors(dst : Writer, map : mutable.Map[String, String]) : Unit = {
    for ((language, message) <- map) {
      dst.write("// "); dst.write(language); dst.write(" - "); dst.write(message); dst.write("\n")
    }
  }
  
  private def insertRedirects(dst : Writer, map : mutable.Map[String, mutable.Set[String]] ) : Unit = {
    var firstLang = true
    for ((language, redirects) <- map) {
      if (firstLang) firstLang = false else dst.write(",\n") 
      dst.write("        \""); dst.write(language); dst.write("\" -> Set(")
      var firstRe = true
      for (name : String <- redirects) {
        if (firstRe) firstRe = false else dst.write(",")
        dst.write("\""); dst.write(name); dst.write("\"");
      }
      dst.write(")")
    }
    dst.write("\n")
  }
  
}

