package org.dbpedia.extraction.dump.sql

import java.io.File
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.Namespace

object Import {
  
  def main(args: Array[String]) : Unit = {
    
    val file = new File("/home/release/wikipedia/ptwiki/20120601/ptwiki-20120601-pages-articles.xml")
    val source = XMLSource.fromFile(file, Language("pt"), _.namespace == Namespace.Template)
    
    val conn = new com.mysql.jdbc.Driver().connect("jdbc:mysql:///test", null)
    try {
      new Importer(conn).process(source)
    }
    finally conn.close()
  }
}

