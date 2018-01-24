package org.dbpedia.extraction.dump.extract

import java.io.{File, Reader, Writer}

import org.apache.log4j.{Level, LogManager}
import org.apache.spark.{SparkConf, SparkContext}
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.sources.{Source, XMLSource}
import org.dbpedia.extraction.util.{ExtractorUtils, Finder, IOUtils}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.wikiparser.{Namespace, WikiPage}

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object SparkExtraction {
  val logger = LogManager.getRootLogger()

  def main(args: Array[String]): Unit = {
    logger.setLevel(Level.INFO)
    val chunkSize = 100

    // Load configuration
    val sources = getInput(new Config(args.head))

    val pageQueue = mutable.Queue[WikiPage]()

    val sparkContext = new SparkContext("local[4]","Test Extraction")
    var prodFin = false

    sources.foreach(src => {
      Future {
        src.foreach(page => pageQueue.enqueue(page))
      }.onComplete(_ => prodFin = true)

      while(!(prodFin && pageQueue.isEmpty)) {
        if(!prodFin) {
          if(pageQueue.length >= chunkSize) {
            val t = System.nanoTime()
            sparkContext.parallelize((0 until chunkSize).map(_ => pageQueue.dequeue())).map(_.title.decoded).toLocalIterator.foreach(_ => {})
            logger.warn(s"${((System.nanoTime() - t) / chunkSize)/1000} µs per page")
          } else {
            Thread.sleep(10)
          }
        } else {
          if(pageQueue.nonEmpty) {
            sparkContext.parallelize(pageQueue).map(_.title.decoded).toLocalIterator.foreach(println)
          }
        }
      }
    })

//    val rdd = sparkContext.textFile("/data/2016-10/dewiki/20161020/dewiki-20161020-pages-articles.xml.bz2")
//
//    val mapped = rdd.flatMap(line => {
//      if(line.trim.startsWith("<title>"))
//        Some(line.split("<title>")(1).split("</title>")(0))
//      else
//        None
//    }).toLocalIterator.foreach(println)
  }

  def getInput(config: Config): Seq[Source] = {
    config.extractorClasses.toList.map(input => {
      val lang = input._1
      val extractors = input._2
      val finder = new Finder[File](config.dumpDir, lang, config.wikiName)

      val articlesReaders = config.source.flatMap(x => readers(x, finder, latestDate(config, finder)))

      XMLSource.fromReaders(articlesReaders, lang,
        title => title.namespace == Namespace.Main || title.namespace == Namespace.File ||
          title.namespace == Namespace.Category || title.namespace == Namespace.Template ||
          title.namespace == Namespace.WikidataProperty || ExtractorUtils.titleContainsCommonsMetadata(title))
    })
  }

  private def latestDate(config: Config, finder: Finder[_]): String = {
    val isSourceRegex = config.source.startsWith("@")
    val source = if (isSourceRegex) config.source.head.substring(1) else config.source.head
    val fileName = if (config.requireComplete) Config.Complete else source
    finder.dates(fileName, isSuffixRegex = isSourceRegex).last
  }

  private def writer(file: File): () => Writer = {
    () => IOUtils.writer(file)
  }

  private def reader(file: File): () => Reader = {
    () => IOUtils.reader(file)
  }

  private def files(source: String, finder: Finder[File], date: String): List[File] = {

    val files = if (source.startsWith("@")) { // the articles source is a regex - we want to match multiple files
      finder.matchFiles(date, source.substring(1))
    } else List(finder.file(date, source)).collect{case Some(x) => x}

    logger.info(s"Source is $source - ${files.size} file(s) matched")

    files
  }

  private def readers(source: String, finder: Finder[File], date: String): List[() => Reader] = {
    files(source, finder, date).map(reader)
  }
}
