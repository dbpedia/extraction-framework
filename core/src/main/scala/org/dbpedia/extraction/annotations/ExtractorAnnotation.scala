package org.dbpedia.extraction.annotations

import java.net.URI

import org.apache.jena.iri.IRI
import org.dbpedia.extraction.ontology.{DBpediaNamespace, RdfNamespace}
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.iri.UriUtils

/**
  * Created by Chile on 11/14/2016.
  */
case class ExtractorAnnotation(val name: String) extends GeneralDBpediaAnnotation{

  private val encoded = WikiUtil.wikiEncode((if(Option(name).nonEmpty && name.trim.nonEmpty) name.trim else name).replace("-", "_")).toLowerCase

  private val u = UriUtils.createIri(RdfNamespace.fullUri(DBpediaNamespace.EXTRACTOR, encoded)).get

  override def uri: IRI = u

  override def typ: AnnotationType.Value = AnnotationType.Extractor
}
