package org.dbpedia.extraction.dump.util

import java.io.{BufferedInputStream, File, FileInputStream}

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.rdf.model.{Model, ModelFactory}

import scala.collection.mutable.ListBuffer

object MinidumpDoc extends App {

  sealed trait Target
  case class TarNode(s: String) extends Target
  case class TarSubject(p: String) extends Target
  case class TarObject(p: String) extends Target

  case class TestDefinition(id: String, target: Target)

  // Get SHACL tests
  val prefixSHACL = "PREFIX sh: <http://www.w3.org/ns/shacl#> "

  val ontologySHACL = ModelFactory.createDefaultModel()
  ontologySHACL.read("http://www.w3.org/ns/shacl#")

  val shapesSHACL = ModelFactory.createDefaultModel()
  shapesSHACL.read(args(0))

  val testModel = ModelFactory.createRDFSModel(ontologySHACL, shapesSHACL)

  val exec = QueryExecutionFactory.create(
    prefixSHACL +
      """SELECT * {
        |  ?shape a sh:Shape .
        |  OPTIONAL { ?shape sh:targetNode ?targetNode . }
        |  OPTIONAL { ?shape sh:targetSubjectsOf ?subjectOf . }
        |  OPTIONAL { ?shape sh:targetObjectsOf ?objectOf . }
        |}
        |""".stripMargin, testModel)

  val rs = exec.execSelect()

  val testsBuffer = new ListBuffer[TestDefinition]
  while (rs.hasNext) {
    val qs = rs.next()
    val targetNode = {
      if (qs.contains("targetNode")) {
        println(s"tests ${Some(TarNode(qs.get("targetNode").asResource().getURI))} on ${Some(TarNode(qs.get("targetNode").asResource().getURI))}")
        None
        //TODO
        //Some(TarNode(qs.get("targetNode").asResource().getURI))
      } else if (qs.contains("subjectOf")) {
        Some(TarSubject(qs.get("subjectOf").asResource().getURI))
      } else if (qs.contains("objectOf")) {
        Some(TarObject(qs.get("objectOf").asResource().getURI))
      } else {
        None
      }
    }
    val shape = qs.get("shape").asResource()
    if (shape.isURIResource && targetNode.isDefined) {
      testsBuffer.append(TestDefinition(shape.getURI,targetNode.get))
    }
  }

  // Select From MiniExtraction
  val miniExtractionBaseDir = new File(args(1))
  if(! testsBuffer.isEmpty) {
    val miniExtraction = loadMiniExtraction(miniExtractionBaseDir)
    testsBuffer.foreach({
      testDef =>

        val queryString = new StringBuilder
        queryString.append("SELECT DISTINCT ?t { ")
        testDef.target match {
          case TarNode(s) => queryString.append(s"VALUES ?t { <$s> } ?t ?p ?o . }")
          case TarSubject(p) => queryString.append(s"?t <$p> ?o . }")
          case TarObject(p) => queryString.append(s"?s <$p> ?t . }")
        }

        val exec = QueryExecutionFactory.create(queryString.toString(),miniExtraction)

        val rs = exec.execSelect()

        while (rs.hasNext) {
          val qs = rs.next
          val t = qs.get("t").asResource().getURI
          if(t.contains("dbpedia.org/"))
          println(s"tests ${testDef.target} on $t")
        }
    })
  }

  def loadMiniExtraction(d: File) = {
    val filesToBeValidated = recursiveListFiles(d).filter(_.isFile)
      .filter(_.toString.endsWith(".ttl.bz2"))
      .toList

    val model: Model = ModelFactory.createDefaultModel()
    for (file <- filesToBeValidated) {
      model.read(new BZip2CompressorInputStream(new FileInputStream(file)),null, "TTL")
    }
    model
  }


  def recursiveListFiles(d: File): Array[File] = {
    val these = d.listFiles
    these ++
      these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }
}
