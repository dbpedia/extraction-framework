package org.dbpedia.extraction.annotations

import java.net.URI

import org.dbpedia.extraction.ontology.{DBpediaNamespace, RdfNamespace}
import org.dbpedia.extraction.util.WikiUtil

/**
  * Created by Chile on 11/14/2016.
  */
case class ExtractorAnnotation(val name: String) extends GeneralDBpediaAnnotation{

  private val encoded = WikiUtil.wikiEncode((if(Option(name).nonEmpty && name.trim.nonEmpty) name.trim else name).replace("-", "_")).toLowerCase

  private val u = new URI(RdfNamespace.fullUri(DBpediaNamespace.EXTRACTOR, encoded))

  override def uri: URI = u

  override def typ: AnnotationType.Value = AnnotationType.Extractor
}
