package org.dbpedia.extraction.mappings.rml.loader

import java.io.File
import java.nio.file.Paths
import java.util

import be.ugent.mmlab.rml.mapdochandler.extraction.std.StdRMLMappingFactory
import be.ugent.mmlab.rml.mapdochandler.retrieval.RMLDocRetrieval
import be.ugent.mmlab.rml.model.{RMLMapping, TriplesMap}
import org.eclipse.rdf4j.rio.RDFFormat

/**
  * Responsible for parsing RML documents using the MapDocHandler
  */


object RMLProcessorParser {

  private val retriever : RMLDocRetrieval = new RMLDocRetrieval()
  private val rmlMappingFactory : StdRMLMappingFactory = new StdRMLMappingFactory()

  /**
    * Parses RML document from a file and returns an RMLMapping object
    */
  def parseFromFile(pathToRmlDocument: String): RMLMapping = {

      val path = convertToAbsolutePath(pathToRmlDocument)
      try {
        val repo = retriever.getMappingDoc(path, RDFFormat.TURTLE)
        val preparedRepo = rmlMappingFactory.prepareExtractRMLMapping(repo)
        val rmlMapping = rmlMappingFactory.extractRMLMapping(preparedRepo)

        rmlMapping
      } catch {
        case e : Exception => new RMLMapping(new util.ArrayList[TriplesMap]())
      }

  }

  /**
    * Retrieves all RML documents in a folder and loads them into a HashMap with as key the name of the mapping
    *
    * @param pathToDir
    * @return
    */
  def parseFromDir(pathToDir : String) : Map[String, RMLMapping] = {
    if(pathToDir == null) return Map()

    val dir = new File(pathToDir)
    val listFiles = dir.listFiles

    // check if language dir exists, if not return empty list
    val files = listFiles match {
      case null => List()
      case _ =>
        listFiles
        .filter(_.isFile)
        .filter(_.length() > 0)
        .filter(_.getName.contains(".ttl")).toList
    }

    val map = files.map(file => file.getName.replace(".ttl", "") -> RMLProcessorParser.parseFromFile(file.getAbsolutePath)).toMap

    map
  }

  /**
    * Converts path to absolute path
    */
  private def convertToAbsolutePath(path: String): String = {
      val relativePath = Paths.get(path)
      val absolutePath = relativePath.toAbsolutePath
      return absolutePath.toString
  }


}
