package org.dbpedia.extraction.statistics

import java.io.{PrintWriter, File}
import org.dbpedia.extraction.scripts.QuadReader
import org.dbpedia.extraction.util.{RichFile, Language}
import org.dbpedia.extraction.wikiparser.Namespace
import java.util.logging.{Level, Logger}

import scala.collection.mutable

/**
  * Created by Chile on 3/7/2016.
  */
object TypeStatistics {
  def main(args: Array[String]): Unit = {

    val logger = Logger.getLogger(getClass.getName)

    require(args != null && args.length >= 7,
      "need at least three args: " +
        /*0*/ "base directory, " +
        /*1*/ "input file suffix (e.g. .ttl.bz2)" +
        /*2*/ "comma- or space-separated names of input files (e.g. 'instance_types,instance_types_transitive') without suffix, language or path!" +
        /*3*/ "output file name (note: in json format, will be saved in the base dir)" +
        /*4*/ "localized / canonicalized - (if 'canonicalized': languages other than english will use the canonical versions of the files in the input list)" +
        /*5*/ "listproperties / not - do not only count all property instances, but list all properties with their pertaining occurrences" +
        /*6*/ "listobjects / not - do not only count all objects instances, but list all objects with their pertaining occurrences" +
        /*7*/ "canonical identifier - (optional) part of the filename which identifies a canonical dataset (default: _en_uris, for 2015-04 its -en-uris)"
    )

    val baseDir = new File(args(0))
    require(baseDir.isDirectory, "basedir is not a directory")

    val inSuffix = args(1)
    require(inSuffix.nonEmpty, "no input file suffix")

    var inputs = args(2).split("[,\\s]").map(_.trim.replace("\\", "/")).filter(_.nonEmpty)
    require(inputs.nonEmpty, "no input file names")
    require(inputs.forall(! _.endsWith(inSuffix)), "input file names shall not end with input file suffix")
    //require(inputs.forall(! _.contains("/")), "input file names shall not contain paths")

    val outfile = new File(baseDir + "/" + args(3))
    if(!outfile.exists())
      outfile.createNewFile()
    require(outfile.isFile && outfile.canWrite, "output file is not writable")
    val writer = new PrintWriter(outfile)
    writer.println("{")

    val localized = if(args(4).toLowerCase == "localized") true else false
    val writeProps = if(args(5).toLowerCase == "listproperties") true else false
    val writeObjects = if(args(6).toLowerCase == "listobjects") true else false

    val canonicalStr = if(args.length == 7) "_en_urs" else args(7).trim

    logger.log(Level.INFO, "starting stats count")

    for(lang <- Namespace.mappings.keySet.toList.sortBy(x => x)) //for all mapping languages
    {
      val inputFiles = if(localized) getInputFileList(lang, inputs, canonicalStr) else getInputFileList(lang, inputs, "")
      count(lang.wikiCode, inputFiles)
    }

    writer.println("}")
    writer.close()
    logger.log(Level.INFO, "finished writing output")

    def count(lang: String, files: List[RichFile]): Unit = {

      val subjects = new mutable.HashMap[String, Int]()
      val objects = new mutable.HashMap[String, Int]()
      val props = new mutable.HashMap[String, Int]()

      var statements = 0

      for(file <- files) {
        if(file.exists)
        {
          QuadReader.readQuads("statistics", file) { quad =>
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
      writeLang(lang, subjects, objects, props, statements)
    }

    def writeLang(lang: String, subjects: mutable.HashMap[String, Int], objects: mutable.HashMap[String, Int], props: mutable.HashMap[String, Int], statements: Int): Unit = {
      writer.println("\t\"" + lang + "\": {")
      writer.println("\t\t\"subjects\": {")
      writeMap(subjects.toMap, writer, false)
      writer.println("\t\t} ,")
      writer.println("\t\t\"properties\": {")
      writeMap(props.toMap, writer, writeProps)
      writer.println("\t\t} ,")
      writer.println("\t\t\"objects\": {")
      writeMap(objects.toMap, writer, writeObjects)
      writer.println("\t\t} ,")
      writer.println("\t\t\"statements\": {")
      writer.println("\"count\": " + statements)
      writer.println("\t\t}")
      writer.println("\t}")
      if (lang != "zh") //TODO should work to figure out the last mapping lang?
        writer.println(",")
    }

    def writeMap(map: Map[String, Int], writer: PrintWriter, writeAll: Boolean = true, tabs: Int = 3): Unit =
    {
      val keymap = map.keySet.toList
      if(writeAll)
        for(i <- 0 until map.size)
        {
          writer.println("\"" + keymap(i).replace("http://", "") + "\": " + map.get(keymap(i)).get + (if(i == map.size-1) "" else " ,"))
        }
      else
        writer.println("\"count\": " + map.size)
    }

    def getInputFileList(lang: Language, inputs: Array[String], append: String): List[RichFile] =
    {
      val appendix = if (lang.iso639_3 == Language.English.iso639_3) "_en" + inSuffix else append + "_" + lang.wikiCode + inSuffix
      val inputFiles = new scala.collection.mutable.MutableList[RichFile]()

      for(file <- inputs)
        inputFiles += new RichFile(new File(baseDir + "/core-i18n/" + lang.wikiCode + "/" + file + appendix))
      inputFiles.toList
    }
  }
}
