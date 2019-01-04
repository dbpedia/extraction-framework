package org.dbpedia.extraction.annotations

import org.dbpedia.extraction.ontology.DBpediaNamespace
import org.dbpedia.iri.IRI

import scala.annotation.StaticAnnotation

/**
  * Created by Chile on 11/14/2016.
  * basic annotation for classes in the DBpedia universe
  */
trait GeneralDBpediaAnnotation extends StaticAnnotation{

  def uri:IRI

  def typ:AnnotationType.Value

  object AnnotationType extends Enumeration{
    val Extractor = Value(DBpediaNamespace.EXTRACTOR.toString)
    val Transformer = Value(DBpediaNamespace.TRANSFORM.toString)
    val Dataset = Value(DBpediaNamespace.DATASET.toString)
    //TODO extend this list
  }
}
