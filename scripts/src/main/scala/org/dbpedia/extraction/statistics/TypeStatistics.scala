package org.dbpedia.extraction.statistics

import java.io.{PrintWriter, File}
import org.dbpedia.extraction.scripts.QuadReader
import org.dbpedia.extraction.util.{RichFile, Language}
import org.dbpedia.extraction.wikiparser.Namespace
import java.util.logging.{Level, Logger}

/**
  * Created by Chile on 3/7/2016.
  */
object TypeStatistics {
  def main(args: Array[String]): Unit = {

    val logger = Logger.getLogger(getClass.getName)

    require(args != null && args.length >= 7,
      "need at least three args: " +
        /*0*/ "base directory, " +
        /*1*/ "input file suffix, " +
        /*2*/ "comma- or space-separated names of input files (e.g. 'instance_types,instance_types_transitive') without suffix and path!" +
        /*3*/ "output file name (note: in json format)" +
        /*4*/ "localized versions - boolean (if false: languages other than english will use the '_en_uris' versions of the files in the input list)" +
        /*5*/ "count also properties" +
        /*6*/ "count also values"
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

    val localized = args(4).toBoolean
    val countProps = args(5).toBoolean
    val countValues = args(6).toBoolean

    val subjectsInLangs = new scala.collection.mutable.HashMap[String, Map[String, Int]]()
    val objectsInLangs = new scala.collection.mutable.HashMap[String, Map[String, Int]]()
    val propertiesInLangs = new scala.collection.mutable.HashMap[String, Map[String, Int]]()

    for(lang <- Namespace.mappings.keySet) //for all mapping languages
    {
      val inputFiles = if(localized) getInputFileList(lang, inputs, "_en_uris") else getInputFileList(lang, inputs, "")
      count(lang.wikiCode, inputFiles, countProps, countValues)
    }

    writeOutput()

    def count(lang: String, files: List[RichFile], countProps: Boolean = true, countValues: Boolean = true): Unit = {

      val subjects = new scala.collection.mutable.HashMap[String, Int]()
      val objects = new scala.collection.mutable.HashMap[String, Int]()
      val props = new scala.collection.mutable.HashMap[String, Int]()

      for(file <- files) {
        logger.log(Level.INFO, file.name)
      }
      for(file <- files) {
        if(file.exists)
        {
          QuadReader.readQuads("statistics", file) { quad =>
            subjects.get(quad.subject) match {
              case Some(s) => subjects += ((quad.subject, s + 1))
              case None => subjects += ((quad.subject, 1))
            }
            objects.get(quad.predicate) match {
              case Some(p) => objects += ((quad.predicate, p + 1))
              case None => objects += ((quad.predicate, 1))
            }
            objects.get(quad.value) match {
              case Some(p) => objects += ((quad.value, p + 1))
              case None => objects += ((quad.value, 1))
            }
          }
        }
      }
      subjectsInLangs += (lang -> subjects.toMap)
      propertiesInLangs += (lang -> props.toMap)
      objectsInLangs += (lang -> objects.toMap)
    }

    def writeOutput() =
    {
      val writer = new PrintWriter(outfile)
      writer.println("{")
      for(lang <- Namespace.mappings.keySet.toList.sortBy(x => x)) //for all mapping languages
      {
        writer.println("\t'" + lang.wikiCode + "': {")
        writer.println("\t\t'subjects': {")
        writeMap(subjectsInLangs(lang.wikiCode), writer)
        writer.println("\t\t} ,")
        writer.println("\t\t'properties': {")
        writeMap(propertiesInLangs(lang.wikiCode), writer)
        writer.println("\t\t} ,")
        writer.println("\t\t'objects': {")
        writeMap(objectsInLangs(lang.wikiCode), writer)
        writer.println("\t\t}")
        writer.println("\t}")
      }
      writer.println("}")
      writer.close()
    }

    def writeMap(map: Map[String, Int], writer: PrintWriter, tabs: Int = 3): Unit =
    {
      val keylist = map.keySet.toList.sortBy(x => x)
      for(i <- 0 until map.size)
      {
        writer.println("'" + keylist(i) + "': " + map.get(keylist(i)).get + (if(i == map.size-1) "" else " ,"))
      }
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
