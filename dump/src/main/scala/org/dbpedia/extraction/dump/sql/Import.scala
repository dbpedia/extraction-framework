package org.dbpedia.extraction.dump.sql

import java.io._

import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.wikiparser.{Namespace, PageNode}

import scala.io.Codec
import java.util.Properties

import org.dbpedia.extraction.config.Config

import scala.collection.mutable
import scala.io.Source

object Import {
  
  def main(args: Array[String]) : Unit = {

    assert(args.length == 1,"Importer needs a single parameter: the path to the pertaining properties file (see: mysql.import.properties).")
    val config = new Config(args.head)

    val baseDir = config.dumpDir
    val tablesFile = config.getArbitraryStringProperty("tables-file").getOrElse(throw new IllegalArgumentException("tables-file entry is missing in the properties file"))
    val url = config.getArbitraryStringProperty("jdbc-url").getOrElse(throw new IllegalArgumentException("jdbc-url entry is missing in the properties file"))
    val fileName = config.source.head
    val importThreads = config.parallelProcesses
    val languages = config.languages
    
    val source = Source.fromFile(tablesFile)(Codec.UTF8)
    val tables =
      try source.getLines.mkString("\n")
      finally source.close()

    val namespaces = mutable.Set(Namespace.Template, Namespace.Category, Namespace.Main, Namespace.Module)
    val namespaceList = namespaces.map(_.name).mkString("[",",","]")

      org.dbpedia.extraction.util.Workers.work(SimpleWorkers(importThreads, importThreads){ language : Language =>      //loadfactor: think about disk read speed and mysql processes

        val info = new Properties()
        info.setProperty("allowMultiQueries", "true")
        val conn = new com.mysql.jdbc.Driver().connect(url, info)
        try {
          val finder = new Finder[File](baseDir, language, "wiki")
          val date = finder.dates(fileName).last

          if (config.isDownloadComplete(language)) {
            finder.file(date, fileName) match {
              case None =>
              case Some(file) =>
                val database = finder.wikiName

                println(language.wikiCode + ": importing pages in namespaces " + namespaceList + " from " + file + " to database " + database + " on server URL " + url)

                /*
            Ignore the page if a different namespace is also given for the title.
            http://gd.wikipedia.org/?curid=4184 and http://gd.wikipedia.org/?curid=4185&redirect=no
            have the same title "Teamplaid:Gàidhlig", but they are in different namespaces: 4184 is
            in the template namespace (good), 4185 is in the main namespace (bad). It looks like
            MediaWiki can handle this somehow, but when we try to import both pages from the XML dump
            into the database, MySQL rightly complains about the duplicate title. As a workaround,
            we simply reject pages for which the <ns> namespace doesn't fit the <title> namespace.
            */
                val source = XMLSource.fromReader(() => IOUtils.reader(file), language, title => title.otherNamespace == null && namespaces.contains(title.namespace))

                val stmt = conn.createStatement()
                try {
                  stmt.execute("DROP DATABASE IF EXISTS " + database + "; CREATE DATABASE " + database + " CHARACTER SET binary; USE " + database + ";")
                  stmt.execute(tables)
                }
                finally stmt.close()

                val recorder = config.getDefaultExtractionRecorder[PageNode](language, 2000)
                recorder.initialize(language, "import")
                val pages = new Importer(conn, language, recorder).process(source)

                println(language.wikiCode + ": imported " + pages + " pages in namespaces " + namespaceList + " from " + file + " to database " + database + " on server URL " + url)
            }
          }
          else
            println(language.name + " could not be imported! Download was not complete.")
        }
          finally conn.close()
      }, languages.toList)
  }
}

