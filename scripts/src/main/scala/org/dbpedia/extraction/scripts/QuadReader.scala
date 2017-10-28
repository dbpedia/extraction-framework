package org.dbpedia.extraction.scripts

import java.io.File

import org.dbpedia.extraction.config.{ExtractionRecorder, RecordEntry, RecordCause}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.util.StringUtils.prettyMillis

import scala.Console.err
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success}

/**
 */
class QuadReader(log: FileLike[File] = null, preamble: String = null) {

  private val recorder: ExtractionRecorder[Quad] =     Option(log) match{
    case Some(f) => new ExtractionRecorder[Quad](IOUtils.writer(f, append = true), 100000, preamble)
    case None => new ExtractionRecorder[Quad](null, 100000, preamble)
  }

  def this(){
    this(null, null)
  }

  def getRecorder = recorder

  def addQuadRecord(quad: Quad, lang: Language, errorMsg: String = null, error: Throwable = null): Unit ={
    if(errorMsg == null && error == null)
      recorder.record(new RecordEntry[Quad](quad, RecordCause.Info, lang, errorMsg, error))
    else if(error != null)
      recorder.record(new RecordEntry[Quad](quad, RecordCause.Exception, lang, errorMsg, error))
  }

  /**
   * @param input file name, e.g. interlanguage-links-same-as.nt.gz
   * @param proc process quad
   */
  def readQuads[T <% FileLike[T]](finder: DateFinder[T], input: String, auto: Boolean = false)(proc: Quad => Unit): Unit = {
    readQuads(finder.language, finder.byName(input, auto).get)(proc)
  }

  /**
    * @param pattern regex of filenemes
    * @param proc process quad
    */
  def readQuadsOfMultipleFiles[T <% FileLike[T]](finder: DateFinder[T], pattern: String, auto: Boolean = false)(proc: Quad => Unit): Unit = {
    for(file <- finder.byPattern(pattern, auto))
      readQuads(finder.language, file)(proc)
  }

  def readSortedQuads[T <% FileLike[T]](language: Language, file: FileLike[_])(proc: Traversable[Quad] => Unit): Unit = {
    //TODO needs extraction-recorder syntax!
    var lastSubj = ""
    var seq = ListBuffer[Quad]()
    readQuads(language, file) { quad =>
      if(!lastSubj.equals(quad.subject))
      {
        lastSubj = quad.subject
        proc(seq.toList)
        seq.clear()
        seq += quad
      }
      else{
        seq += quad
      }
    }
    proc(seq.toList)
  }

  /**
   * @param language for logging
   * @param file input file
   * @param proc process quad
   */
  def readQuads(language: Language, file: FileLike[_])(proc: Quad => Unit): Unit = {
    val dataset = "(?<=(.*wiki-\\d{8}-))([^\\.]+)".r.findFirstIn(file.toString) match {
      case Some(x) => DBpediaDatasets.getDataset(x, language) match{
        case Success(d) => Some(d)
        case Failure(f) => None
      }
      case None => None
    }

    getRecorder.initialize(language, "Processing Quads", if(dataset.nonEmpty) Seq(dataset.get) else Seq())

    IOUtils.readLines(file) { line =>
      line match {
        case null => // ignore last value
        case Quad(quad) => {
          val copy = quad.copy (
              dataset = if(dataset.nonEmpty) dataset.get.encoded else null
          )
          proc(copy)
          addQuadRecord(copy, language)
        }
        case str => if (str.nonEmpty && !str.startsWith("#"))
          addQuadRecord(null, language, null, new IllegalArgumentException("line did not match quad or triple syntax: " + line))
      }
    }
    addQuadRecord(null, language, "reading quads completed with {page} pages", null)
  }
  
  private def logRead(tag: String, lines: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    err.println(tag+": read "+lines+" lines in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
}