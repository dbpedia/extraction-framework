package org.dbpedia.iri

import org.apache.jena.iri.IRIFactory

/**
  * Created by chile on 02.10.17.
  */
class URI(uri: org.apache.jena.iri.IRI) extends IRI(uri){

  def this(uri: String) = this(URI.uriFactory.construct(uri))
  def this(uri: URI) = this(URI.uriFactory.construct(uri))
  def this(uri: java.net.URI) = this(URI.uriFactory.construct(uri))

  def toIRI: IRI = UriUtils.uriToIri(this)
}

object URI{
  protected val uriFactory: IRIFactory = new IRIFactory()
  IRIFactory.setUriImplementation(uriFactory)
  uriFactory.allowUnwiseCharacters()
}