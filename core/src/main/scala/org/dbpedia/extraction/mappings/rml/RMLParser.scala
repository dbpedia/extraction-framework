package org.dbpedia.extraction.mappings.rml
import java.nio.file
import java.nio.file.{Path, Paths}

import be.ugent.mmlab.rml.mapdochandler.extraction.std.StdRMLMappingFactory
import be.ugent.mmlab.rml.mapdochandler.retrieval.RMLDocRetrieval
import be.ugent.mmlab.rml.model.RMLMapping
import org.openrdf.rio.RDFFormat

/**
  * Responsible for parsing RML documents using the MapDocHandler
  */


object RMLParser {

  private val retriever : RMLDocRetrieval = new RMLDocRetrieval()
  private val rmlMappingFactory : StdRMLMappingFactory = new StdRMLMappingFactory()

  /**
    * Parses RML document from a file and returns an RMLMapping object
    */
  def parseFromFile(pathToRmlDocument: String): RMLMapping = {

      val path = convertToAbsolutePath(pathToRmlDocument)
      val repo = retriever.getMappingDoc(path, RDFFormat.TURTLE)
      val preparedRepo = rmlMappingFactory.prepareExtractRMLMapping(repo)
      val rmlMapping = rmlMappingFactory.extractRMLMapping(preparedRepo)

      rmlMapping

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
