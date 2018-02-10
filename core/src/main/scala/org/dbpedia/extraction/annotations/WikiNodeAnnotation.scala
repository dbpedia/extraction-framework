package org.dbpedia.extraction.annotations

import java.util.MissingResourceException

import org.dbpedia.iri.IRI

import scala.annotation.StaticAnnotation
import scala.util.{Failure, Success, Try}

class WikiNodeAnnotation(clazz: Class[_]) extends StaticAnnotation

object WikiNodeAnnotation{

  def getAnnotationIri[T](implicit clazz: Class[T]): IRI = Try {
    SoftwareAgentAnnotation.getAnnotationIri[T]
  } match{
    case Success(s) => s
    case Failure(f) => f match{
      case msa: MissingSoftwareAgentAnnotation => throw new MissingWikiNodeAnnotation(msa.getMessage.replace("SoftwareAgentAnnotation", "WikiNodeAnnotation"))
      case f: Throwable => throw f
    }
  }
}

class MissingWikiNodeAnnotation(msg: String)
  extends MissingResourceException(msg, SoftwareAgentAnnotation.getClass.getName, "MissingWikiNodeAnnotation")