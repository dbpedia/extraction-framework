package org.dbpedia.extraction.annotations

import java.io.File

import org.dbpedia.extraction.ontology.{DBpediaNamespace, RdfNamespace}
import org.dbpedia.extraction.util.{IOUtils, WikiUtil}
import org.dbpedia.extraction.util.RichFile._
import org.dbpedia.iri.IRI

import scala.annotation.StaticAnnotation
import scala.util.{Failure, Success}

/**
  * Created by Chile on 11/14/2016.
  * basic annotation for classes in the DBpedia universe
  */
class SoftwareAgentAnnotation(clazz: Class[_], annotationType: AnnotationType.Value) extends StaticAnnotation

object SoftwareAgentAnnotation{

  private val universe = scala.reflect.runtime.universe

  def getAnnotationIri(clazz: Class[_]): IRI = {
    val className = if(clazz.getName.endsWith("$")) clazz.getName.substring(0, clazz.getName.length-1) else clazz.getName
    val myAnnotatedClass = universe.runtimeMirror(Thread.currentThread().getContextClassLoader).staticClass(className)
    val annotation: Option[universe.Annotation] = myAnnotatedClass.annotations.find(_.tree.tpe.toString == "org.dbpedia.extraction.annotations.SoftwareAgentAnnotation")
    val vals = annotation.get.tree.children.tail.collect({
      case universe.Literal(cc) => "className" -> cc.value.toString
      case universe.Select(name) => "type" -> name._2
    }).toMap
    val name = vals("className").toString
    val typ = vals("type").toString
    var encoded = WikiUtil.wikiEncode((if(Option(name).nonEmpty && name.trim.nonEmpty) name.trim else name).replace("-", "_"))
    encoded = encoded.substring(encoded.lastIndexOf(".")+1)

    def getGitHash: String = {
      IOUtils.readLines(new File("../.git/HEAD")) { branch =>
        IOUtils.readLines(new File("../.git/" + branch.replaceAll("^ref:\\s+", ""))) { head =>
          return head
        }
      }
      throw new IllegalStateException("Git files could not be accessed: ../.git/HEAD")
    }

    val gitHash: String = getGitHash

    DBpediaNamespace.get(typ) match{
      case Some(ns) =>     IRI.create(RdfNamespace.fullUri(ns, encoded) + "?githash=" + gitHash) match {
        case Success(i) => i
        case Failure(f) => throw f
      }
      case None => throw new IllegalArgumentException("No namespace found for " + typ)
    }
  }
}

object AnnotationType extends Enumeration{
  val Extractor = Value(DBpediaNamespace.EXTRACTOR.toString)
  val Transformer = Value(DBpediaNamespace.TRANSFORMER.toString)
  val Dataset = Value(DBpediaNamespace.DATASET.toString)
  val Parser = Value(DBpediaNamespace.PARSER.toString)
  //TODO extend this list
}