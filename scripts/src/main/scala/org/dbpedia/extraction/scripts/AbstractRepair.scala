package org.dbpedia.extraction.scripts

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.dbpedia.extraction.destinations.{CompositeDestination, WriterDestination}
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import org.dbpedia.extraction.util.RichFile.wrapFile

import scala.collection.convert.decorateAsScala._

import scala.Console._
import org.dbpedia.extraction.util._

/**
  * Created by Chile on 10/11/2016.
  */
object AbstractRepair {
  private val  suffix: String = ".tql.bz2"
  private var  baseDir: File = null
  private var oldFilePathPattern : String = null
  private var abstractFile : String = null
  private val abstracts = new ConcurrentHashMap[Language, scala.collection.mutable.Map[String, Int]]().asScala


  val workers = SimpleWorkers(1.0, 1.0) { language: Language =>
    val oldFile = new File(oldFilePathPattern.replaceAll("%langcode", language.wikiCode).replaceAll("%abstracts", abstractFile).replaceAll("%suffix", suffix))
    if(oldFile.exists()) {
      val templateString = Namespaces.names(language).get(Namespace.Template.code) match {
        case Some(x) => x
        case None => "Template"
      }

      val finder = new DateFinder(baseDir, language)
      val faultyFile = finder.byName(abstractFile + suffix, auto = true).get
      val langMap = abstracts.get(language).get
      val destination = new CompositeDestination(
        new WriterDestination(() => IOUtils.writer(finder.byName(abstractFile + "-repaired" + suffix).get), new TerseFormatter(quads = true, turtle = true, null)),
        new WriterDestination(() => IOUtils.writer(finder.byName(abstractFile + "-repaired" + suffix.replace("tql", "ttl")).get), new TerseFormatter(quads = false, turtle = true, null))
      )
      QuadMapper.mapQuads(language.wikiCode, new RichFile(faultyFile), destination, required = true, closeWriter = false) { quad =>
        if (quad.value.indexOf(templateString + ":") >= 0) {
          langMap.put(quad.subject, 0)
          Seq()
        }
        else
          Seq(quad)
      }
      QuadMapper.mapQuads(language.wikiCode, new RichFile(oldFile), destination, required = true) { quad =>
        langMap.get(quad.subject) match {
          case Some(x) => {
            Seq(quad)
          }
          case None => Seq()
        }
      }
    }
    else
      err.println("Error: could not map langauge " + language.name + " since previous abstract file was found: " + oldFile.getAbsolutePath)
  }

  def main(args: Array[String]): Unit = {
    baseDir = new File(args(0))
    require(baseDir.isDirectory, "basedir is not a directory")

    abstractFile = args(1)
    require(abstractFile.contains("abstracts"), "Please specify the abstracts file in queston ('short-abstracts' or 'long-abstracts'.")

    oldFilePathPattern = args(2)
    require(oldFilePathPattern.contains("%langcode"), "Please specify a valid path for the old abstracts files containing '%langcode' for every instance of a wiki language code.")

    val languages = parseLanguages(baseDir, args(3).split(",").map(_.trim).filter(_.nonEmpty))

    for(lang <- languages)
      abstracts.put(lang, new ConcurrentHashMap[String, Int]().asScala)

    Workers.work(workers, languages.toList)
  }
}
