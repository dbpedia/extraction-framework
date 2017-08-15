package org.dbpedia.extraction.server.resources.rml

import java.io.File
import java.nio.file.{Files, Paths}

import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.server.util.CommandLineUtils
import java.util.logging.{Level, Logger}

import scala.collection.immutable.HashMap

/**
  * Created by wmaroy on 10.08.17.
  *
  * Object that has methods for accessing the Mappings-Tracker Repo
  *
  */
object MappingsTrackerRepo {

  private val logger = Logger.getLogger(this.getClass.getName)

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Public fields
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  val SERVER_RELATIVE_PATH = "../mappings-tracker/mappings"
  val CORE_RELATIVE_PATH = "../mappings-tracker/mappings"


  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Public methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  /**
    * Pulls from mappings-tracker.
    * This updates at runtime this repository locally.
    * @return success or not as boolean
    */
  def pull() : Boolean = {
    logger.info("Pulling updates from mappings-tracker from the github repository..")
    val result = CommandLineUtils.execute("git --git-dir=../mappings-tracker/.git pull", print = true)
    logger.info("Success: " + result)
  }

  /**
    * Returns a map with language iso code as key and value the folder File object.
    * @return
    */
  def getLanguageDirs : Map[String, File]  = {

    // gets the "mappings" dir in the mappings-tracker repo
    val dir = new File(SERVER_RELATIVE_PATH)
    val listFiles = dir.listFiles

    logger.info("Mapping dirs:")
    logger.info(listFiles.toString)

    // check if language dir exists, if not return empty list
    val files = listFiles match {
      case null => List()
      case _ =>
        listFiles
          .filter(_.isDirectory)
          .toList
    }

    val languageDirs = files.map(file => file.getName -> file).toMap
    languageDirs

  }

  /**
    * Returns the content of the mappings-tracker repo as LanguageRMLModels
    * @return
    */
  def getLanguageRMLModels(updatesPerLanguage : Map[String, Set[String]] = null) : LanguageRMLModels = {

    val languageMappingDumps = getLanguageMappingDumps(updatesPerLanguage)

    logger.info(languageMappingDumps.toString())

    val languageRMLModels = languageMappingDumps.map(entry => {
      val language = entry._1
      val mappingDumps = entry._2
      val rmlModels = mappingDumps.map(mappingDump => {
        val name = mappingDump._2.name
        val dump = mappingDump._2.dump

        // Print out the name the mapping
        val logger = Logger.getLogger(this.getClass.getName)
        logger.info(name)

        name -> RMLModel(language, name, dump)
      })
      language -> rmlModels
    }).toMap

    new LanguageRMLModels(languageRMLModels)

  }

  /**
    * Returns the content of the mappings-tracker repo as a Map with as keyset all the languages and as value a list
    * with all the corresponding mapping files
    * @return
    */
  def getLanguageMappingFiles(languages : Set[String] = null) : Map[String, List[File]] = {

    val languageDirs = getLanguageDirs

    logger.info("Language mapping dirs:")
    logger.info(languageDirs.toString())

    val languageMappingFiles = languageDirs
      .filter( languageDir => {
        // languages set is null continue, else check if the language is needed
        if(languages == null) true else languages.contains(languageDir._1)
      })
      .map(languageDir => {
        val language = languageDir._1
        val dir = languageDir._2
        val mappingFiles = dir.listFiles() match {
          case null => List()
          case _ =>
            dir.listFiles()
              .filter(_.isFile)
              .filter(_.length() > 0)
              .filter(_.getName.contains(".ttl")).toList
        }
        language -> mappingFiles
    })

    languageMappingFiles

  }

  /**
    * Returns the content of the mappings-tracker repo as a LanguageMappingDumps object
    * @return
    */
  def getLanguageMappingDumps(updatesPerLanguage : Map[String, Set[String]] = null) : LanguageMappingDumps = {

    val languageMappingFiles = if(updatesPerLanguage != null) getLanguageMappingFiles(updatesPerLanguage.keys.toSet)
                               else getLanguageMappingFiles()

    logger.info("Mapping files per language:")
    logger.info(languageMappingFiles.toString())

    val languageMappings = languageMappingFiles.map(entry => {
     val language = entry._1
     val files = entry._2
     val dumpMap = files
       // if there are updates, only read updates, else read everything
       .filter(file => if (updatesPerLanguage != null) updatesPerLanguage(language).contains(file.getName) else true)
       .map(file => {

         val fileName = file.getName
         val dump = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath)), "UTF-8")

         val withoutSuffix = fileName.replace(".ttl", "")
         withoutSuffix -> MappingDump(dump, withoutSuffix)

     }).toMap
     language -> dumpMap
    })

    new LanguageMappingDumps(languageMappings)
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Extra classes
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  /**
    * Structure that contains a map with as keyset the languages and as value a Map with as keyset the filenames and
    * as value the corresponding mapping dump.
    * @param languageMappings
    */
  class LanguageMappingDumps(languageMappings: Map[String, Map[String, MappingDump]]) extends Iterable[(String, Map[String, MappingDump])] {

    // returns for a given language a map with mapping filename as key and mapping dump as value.
    def apply(language : String) : Map[String, MappingDump] = {
      languageMappings(language)
    }

    override def iterator: Iterator[(String, Map[String, MappingDump])] = {
      languageMappings.iterator
    }

  }

  /**
    * Structure that contains a map with as keyset the languages and as value a Map with as keyset the filenames and
    * as value the corresponding RMLModel.
    * @param languageRMLModels
    */
  class LanguageRMLModels(languageRMLModels : Map[String, Map[String, RMLModel]]) extends Map[String, Map[String, RMLModel]]{

    override def iterator: Iterator[(String, Map[String, RMLModel])] = {
      languageRMLModels.iterator
    }

    override def +[B1 >: Map[String, RMLModel]](kv: (String, B1)): Map[String, B1] = {
      languageRMLModels + kv
    }

    override def get(key: String): Option[Map[String, RMLModel]] = {
      Option(languageRMLModels(key))
    }

    override def -(key: String): Map[String, Map[String, RMLModel]] = {
      languageRMLModels - key
    }
  }

  /**
    * Represents a mapping dump
    * @param dump raw dump string
    * @param name name of the mapping
    */
  case class MappingDump(dump : String, name : String)


}
