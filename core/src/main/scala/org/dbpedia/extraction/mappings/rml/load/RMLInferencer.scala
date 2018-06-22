package org.dbpedia.extraction.mappings.rml.load

import java.io._
import java.net.URLDecoder
import java.nio.file.{Files, Path, Paths}

import be.ugent.mmlab.rml.model.RMLMapping
import org.apache.commons.io.IOUtils
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.rulesys.{GenericRuleReasoner, Rule}
import org.apache.jena.util.FileManager
import org.apache.log4j.Logger
import org.dbpedia.extraction.mappings.rml.translate.format.RMLFormatter
import org.dbpedia.extraction.util.Language

/**
  * Created by wmaroy on 10.07.17.
  *
  * TODO: document @wmaroy
  */
object RMLInferencer {

  val LANGUAGE_TEMPLATE = "\\{LANG\\}"

  val logger = Logger.getLogger(this.getClass)

  def loadDir(language: Language, pathToRMLMappingsDir: String): Map[String, RMLMapping] = {

    // create language specific rules from templated file
    val stream = this.getClass.getClassLoader.getResourceAsStream("rules.rule")
    val languageRulesPath = createLanguageRuleFile(language, stream, "./")

    // load the language rules
    val rules = Rule.rulesFromURL(languageRulesPath.toUri.getPath)

    // delete language file
    languageRulesPath.toFile.delete()

    // get the language dir
    val languageDir = getPathToLanguageDir(language, pathToRMLMappingsDir)

    try {

      val infDirTuple = inferenceDir(rules, languageDir, language.isoCode)

      infDirTuple._2.foreach(mapping => {
        logger.debug(mapping)
      })

      val tempDir = infDirTuple._1
      val mappings = RMLProcessorParser.parseFromDir(tempDir.toAbsolutePath.toString)


      // delete temporary dir
      tempDir.toFile.listFiles().foreach(file => file.delete())
      tempDir.toFile.delete()
      languageRulesPath.toFile.delete()

      mappings

    } catch {
      case e: Exception => Map()
    }
  }

  def loadDump(language: Language, dump: String, name: String): (String, RMLMapping) = {
    // create language specific rules from templated file
    val stream = this.getClass.getClassLoader.getResourceAsStream("rules.rule")

    val languageRulesPath = createLanguageRuleFile(language, stream, ".")

    // load the language rules
    val rules = Rule.rulesFromURL(languageRulesPath.toUri.getPath)

    val tempMappingFilePath = createTempMappingFile(name, dump, "./")

    val tmpDir = Files.createTempDirectory(Paths.get("./"), "inferences")
    val inference = inferenceRMLMapping(rules, tempMappingFilePath.toAbsolutePath.toString, tmpDir.toAbsolutePath.toString, language.isoCode)
    val mappings = RMLProcessorParser.parseFromDir(tmpDir.toAbsolutePath.toString)

    // delete temporary files
    tmpDir.toFile.listFiles().foreach(file => file.delete())
    tempMappingFilePath.toFile.delete()
    tmpDir.toFile.delete()
    languageRulesPath.toFile.delete()

    mappings.head
  }

  def loadDumpAsString(language: Language, dump: String, name: String): String = {
    // create language specific rules from templated file
    val stream = this.getClass.getClassLoader.getResourceAsStream("rules.rule")

    val languageRulesPath = createLanguageRuleFile(language, stream, "./")

    // load the language rules
    val rules = Rule.rulesFromURL(languageRulesPath.toUri.getPath)

    val tempMappingFilePath = createTempMappingFile(name, dump, "./")

    val tmpDir = Files.createTempDirectory(Paths.get("./"), "inferences")
    val inference = inferenceRMLMapping(rules, tempMappingFilePath.toAbsolutePath.toString, tmpDir.toAbsolutePath.toString, language.isoCode)

    // delete temporary files
    tmpDir.toFile.deleteOnExit()
    tmpDir.toFile.listFiles().foreach(file => file.delete())
    tempMappingFilePath.toFile.delete()
    languageRulesPath.toFile.delete()

    inference
  }

  /**
    * Replaces all language templated constructs with the given language
    *
    * @param language
    * @param dirPath
    * @return
    */
  private def createLanguageRuleFile(language: Language, stream: InputStream, dirPath: String): Path = {
    val templatedRuleString = IOUtils.toString(stream)
    //new String(Files.readAllBytes(Paths.get(path)), "UTF-8")
    val languageRuleString = templatedRuleString.replaceAll(RMLInferencer.LANGUAGE_TEMPLATE, language.isoCode)
    val file = new File(dirPath)
    val absoluteFile = file.getAbsoluteFile
    val dir = Paths.get(absoluteFile.getParent)
    val tmpFile = Files.createTempFile(dir, language.isoCode + "-rules.rule", null)
    tmpFile.toFile.deleteOnExit()
    Files.write(Paths.get(tmpFile.toUri.getPath), languageRuleString.getBytes)
    tmpFile
  }

  private def createTempMappingFile(name: String, dump: String, dirPath: String): Path = {
    val file = new File(dirPath).getAbsoluteFile
    val dir = Paths.get(file.getParent)
    val tmpFile = Files.createTempFile(dir, name + "-", ".ttl").toFile
    tmpFile.deleteOnExit()
    val writer = new BufferedWriter(new FileWriter(tmpFile))
    writer.write(dump)
    writer.close()
    tmpFile.toPath
  }


  private def inferenceDir(rules: java.util.List[Rule], inputDirPath: String, language: String): (Path, List[String]) = {
    if (inputDirPath == null) return null

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
    val inferences = files.map(file => {
      println("Inference mapping file: " + file.getName)

      inferenceRMLMapping(rules, file.getAbsolutePath, tmpDir.toAbsolutePath.toString, language)
    }).toList

    (tmpDir, inferences)

  }

  /**
    *
    * @param inputPath
    * @param outputPath
    */
  private def inferenceRMLMapping(rules: java.util.List[Rule], inputPath: String, outputPath: String, language: String): String = {

    // create an empty model
    val model = ModelFactory.createDefaultModel()

    // create inputStream for fetching base
    val inBase = createInputStream(inputPath)
    val readerBase = new BufferedReader(
      new InputStreamReader(inBase))
    val firstLine = readerBase.readLine()
    val baseRegex = "http://[^>]*".r
    val base = baseRegex.findFirstMatchIn(firstLine).orNull.toString()

    val in = createInputStream(inputPath)

    // read the InputStream into the model
    model.read(in, base, "TURTLE")

    // create the reasoner
    val reasoner = new GenericRuleReasoner(rules)
    val infModel = ModelFactory.createInfModel(reasoner, model)

    //TODO write to rml/inferenced/{lan}
    val out = new StringWriter()
    infModel.write(out, "TURTLE", base)

    val decodedInfModelString = URLDecoder.decode(out.toString, "UTF-8") //string writer does not decode

    // mapping name regex
    val mappingNameRegex = "Mapping_[^/]+".r
    val fileName = mappingNameRegex.findFirstIn(base).orNull.toString + ".ttl"

    val formatted = RMLFormatter.format(decodedInfModelString, base, language)
    val writer = new BufferedWriter(new FileWriter(outputPath + "/" + fileName))
    writer.write(formatted)
    writer.close()

    formatted
  }

  private def createInputStream(inputPath: String): InputStream = {

    // use the FileManager to find the input file
    val in = FileManager.get().open(inputPath)

    if (in == null) {
      throw new IllegalArgumentException("File not found.")
    }

    in
  }

  /**
    * Creates the path to the mappings dir based on the language and path to all the RML mappings
    *
    * @param language
    * @return
    */
  private def getPathToLanguageDir(language: Language, dir: String): String = {
    dir + (if (!dir.endsWith("/")) "/" else "") + language.isoCode
  }

  private def concatDirAndFileName(dir: String, file: String): String = {
    if (!dir.endsWith("/")) "/" else ""
  }


}
