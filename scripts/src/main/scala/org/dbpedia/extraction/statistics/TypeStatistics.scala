package org.dbpedia.extraction.statistics

import java.io.{File, PrintWriter}
import java.util.concurrent.ConcurrentHashMap

import org.apache.log4j.{Level, Logger}
import org.dbpedia.extraction.config.{ExtractionLogger, ExtractionRecorder}
import org.dbpedia.extraction.scripts.QuadMapper
import org.dbpedia.extraction.util.{Language, RichFile, SimpleWorkers, Workers}
import org.dbpedia.extraction.wikiparser.Namespace

import scala.collection.convert.decorateAsScala._
import scala.collection.mutable

/**
  * Created by Chile on 3/7/2016.
  */
object TypeStatistics {

  private var localized = false
  private var writeProps = false
  private var writeObjects = false
  private val logger = ExtractionLogger.getLogger(getClass, Language.None)

  def main(args: Array[String]): Unit = {

    require(args != null && args.length >= 7,
      "need at least 7 args: " +
        /*0*/ "base directory, " +
        /*1*/ "input file suffix (e.g. .ttl.bz2)" +
        /*2*/ "comma- or space-separated names of input files (e.g. 'instance_types,instance_types_transitive') without suffix, language or path!" +
        /*3*/ "output file name (note: in json format, will be saved in the statistics directory under the base dir)" +
        /*4*/ "localized / canonicalized - (if 'canonicalized': languages other than english will use the canonical versions of the files in the input list)" +
        /*5*/ "listproperties / not - do not only count all property instances, but list all properties with their pertaining occurrences" +
        /*6*/ "listobjects / not - do not only count all objects instances, but list all objects with their pertaining occurrences" +
        /*7*/ "canonical identifier - (optional) part of the filename which identifies a canonical dataset (default: _en_uris, for 2015-04 its -en-uris)"
    )


    val baseDir = new File(args(0))
    require(baseDir.isDirectory, "basedir is not a directory")

    val inSuffix = args(1)
    require(inSuffix.nonEmpty, "no input file suffix")

    val inputs = args(2).split("[,\\s]").map(_.trim.replace("\\", "/")).filter(_.nonEmpty)
    require(inputs.nonEmpty, "no input file names")
    require(inputs.forall(! _.endsWith(inSuffix)), "input file names shall not end with input file suffix")
    //require(inputs.forall(! _.contains("/")), "input file names shall not contain paths")

    val statisticsDir = new File(baseDir, "statistics")
    if(!statisticsDir.exists())
      statisticsDir.mkdir()
    val outfile = new File(statisticsDir, args(3))
    if(!outfile.exists())
      outfile.createNewFile()
    require(outfile.isFile && outfile.canWrite, "output file is not writable")

    localized = args(4).toLowerCase == "localized"
    writeProps = args(5).toLowerCase == "listproperties"
    writeObjects = args(6).toLowerCase == "listobjects"

    val canonicalStr = if(args.length < 8) "_wkd_uris" else args(7).trim

    val results = new ConcurrentHashMap[String, String]().asScala

    def getInputFileList(lang: Language, inputs: Array[String], append: String): List[RichFile] =
    {
      val appendix = if (lang.iso639_3 == Language.English.iso639_3) "_en" + inSuffix else append + "_" + lang.wikiCode + inSuffix
      val inputFiles = new scala.collection.mutable.MutableList[RichFile]()

      for(file <- inputs)
        inputFiles += new RichFile(new File(baseDir + "/core-i18n/" + lang.wikiCode + "/" + file + appendix))
      inputFiles.toList
    }
    logger.log(Level.INFO, "starting stats count")

    //for all mapping languages
    Workers.work(SimpleWorkers(1.5, 1.0) {lang: Language =>
      val inputFiles = if(localized) getInputFileList(lang, inputs, "") else getInputFileList(lang, inputs, canonicalStr)
      val subjects = new mutable.HashMap[String, Int]()
      val objects = new mutable.HashMap[String, Int]()
      val props = new mutable.HashMap[String, Int]()

      var statements = 0

      for(file <- inputFiles) {
        if(file.exists)
        {
          new QuadMapper().readQuads(lang, file) { quad =>
            statements = statements +1
            subjects.get(quad.subject) match {
              case Some(s) => subjects += ((quad.subject, s + 1))
              case None => subjects += ((quad.subject, 1))
            }
            props.get(quad.predicate) match {
              case Some(s) => props += ((quad.predicate, s + 1))
              case None => props += ((quad.predicate, 1))
            }
            objects.get(quad.value) match {
              case Some(p) => objects += ((quad.value, p + 1))
              case None => objects += ((quad.value, 1))
            }
          }
        }
      }
      results.put(lang.wikiCode, writeLang(lang.wikiCode, statements, subjects, props, objects))
    }, Namespace.mappingLanguages.toList.sortBy(x => x))

    logger.log(Level.INFO, "finished calculations")

    val sb = new StringBuilder()
    for(langEntry <- results)
    {
      if(sb.isEmpty)
        sb.append("{\n")
      else
        sb.append(",\n")
      sb.append(langEntry._2)
    }

    val writer = new PrintWriter(outfile)
    writer.write(sb.toString())
    writer.write("\n}")
    writer.close()
    logger.log(Level.INFO, "finished writing output")
  }

  def writeLang(lang: String, statements: Int, subjects: mutable.HashMap[String, Int], props: mutable.HashMap[String, Int], objects: mutable.HashMap[String, Int]): String = {
    val sb = new StringBuilder()
    sb.append("\t\"" + lang + "\": {")
    sb.append("\t\t\"subjects\": {")
    writeMap(subjects.toMap, sb, false)
    sb.append("\t\t} ,")
    sb.append("\t\t\"properties\": {")
    writeMap(props.toMap, sb, writeProps)
    sb.append("\t\t} ,")
    sb.append("\t\t\"objects\": {")
    writeMap(objects.toMap, sb, writeObjects)
    sb.append("\t\t} ,")
    sb.append("\t\t\"statements\": {")
    sb.append("\"count\": " + statements)
    sb.append("\t\t}")
    sb.append("\t}\n")
    sb.toString()
  }

  def writeMap(map: Map[String, Int], sb: StringBuilder, writeAll: Boolean = true, tabs: Int = 3): Unit =
  {
    val keymap = map.keySet.toList
    if(writeAll)
      for(i <- 0 until map.size)
      {
        sb.append("\"" + keymap(i).replace("http://", "") + "\": " + map.get(keymap(i)).get + (if(i == map.size-1) "" else " ,"))
      }
    else
      sb.append("\"count\": " + map.size)
  }
}
