package org.dbpedia.extraction.mappings.rml.loader

import java.io.{File, InputStream}
import java.nio.file.{Files, Path, Paths}

import be.ugent.mmlab.rml.model.RMLMapping
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.rulesys.{GenericRuleReasoner, Rule}
import org.apache.jena.util.FileManager
import org.dbpedia.extraction.util.Language

/**
  * Created by wmaroy on 10.07.17.
  */
class RMLInferencer {

  def load(language :Language, pathToRMLMappingsDir : String) : Map[String, RMLMapping] = {

    // create language specific rules from templated file
    val path = this.getClass.getClassLoader.getResource("rules.rule").getPath
    val languageRulesPath = createLanguageRuleFile(language, path)

    // load the language rules
    val rules = Rule.rulesFromURL(languageRulesPath.toUri.getPath)

    // get the language dir
    val languageDir = getPathToLanguageDir(language, pathToRMLMappingsDir)

    val infDir = inferenceDir(rules, languageDir)

    println(infDir)

    //TODO load inferenced dir with RMLLoader

    // return nothing atm
    null
  }

  /**
    * Replaces all language templated constructs with the given language
    * @param language
    * @param path
    * @return
    */
  private def createLanguageRuleFile(language: Language, path : String) : Path = {
    val templatedRuleString = new String(Files.readAllBytes(Paths.get(path)), "UTF-8")
    val languageRuleString = templatedRuleString.replaceAll(RMLInferencer.LANGUAGE_TEMPLATE, language.isoCode)
    val file = new File(path)
    val dir = Paths.get(file.getParent)
    val tmpFile = Files.createTempFile(dir, language.isoCode + "-rules.rule", null)
    Files.write(Paths.get(tmpFile.toUri.getPath), languageRuleString.getBytes)
    tmpFile
  }


  private def inferenceDir(rules : java.util.List[Rule], inputDirPath : String) : String = {
    if(inputDirPath == null) return null

    val dir = new File(inputDirPath)
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

    val tmpDir = Files.createTempDirectory(Paths.get(inputDirPath), "inferences")
    files.foreach(file => inferenceRMLMapping(rules, file.getAbsolutePath, tmpDir.toUri.toString +
                                                      concatDirAndFileName(tmpDir.toUri.toString, file.getName)))


    tmpDir.toUri.toString

  }

  /**
    *
    * @param inputPath
    * @param outputPath
    */
  private def inferenceRMLMapping(rules : java.util.List[Rule], inputPath : String, outputPath : String) = {

    // create an empty model
    val model = ModelFactory.createDefaultModel()

    // create inputStream
    val in = createInputStream(inputPath)

    // read the InputStream into the model
    model.read(in, "http://en.dbpedia.org/resource/Mapping_en:Infobox_martial_artist", "TURTLE")

    // create the reasoner
    val reasoner = new GenericRuleReasoner(rules)
    val infModel = ModelFactory.createInfModel(reasoner, model)

    //TODO write to rml/inferenced/{lan}
    infModel.write(System.out, "TURTLE", "http://en.dbpedia.org/resource/Mapping_en:Infobox_martial_artist/")

    println("Final output would be written to:" + outputPath)

  }

  private def createInputStream(inputPath : String) : InputStream = {

    // use the FileManager to find the input file
    val in = FileManager.get().open(inputPath)

    if (in == null) {
      throw new IllegalArgumentException("File not found.")
    }

    in
  }

  /**
    * Creates the path to the mappings dir based on the language and path to all the RML mappings
    * @param language
    * @return
    */
  private def getPathToLanguageDir(language: Language, dir : String) : String = {
    dir + (if(!dir.endsWith("/"))"/" else "") + language.isoCode
  }

  private def concatDirAndFileName(dir : String, file : String) : String = {
    if (!dir.endsWith("/")) "/" else ""
  }


}

object RMLInferencer {

  val LANGUAGE_TEMPLATE = "\\{LANG\\}"

}
