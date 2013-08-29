package org.dbpedia.extraction.server.resources.ontology

import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.OntologyEntity
import org.dbpedia.extraction.server.util.PageUtils.languageList
import scala.xml.{Elem,Text,NodeBuffer}
import javax.ws.rs._
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.ontology.RdfNamespace

@Path("/ontology/wikidata/missing/")
class MissingWikidata {
  
  private val ontology = Server.instance.extractor.ontology

  private val pagesUrl = Server.instance.paths.pagesUrl

  @GET
  @Produces(Array("application/xhtml+xml"))
  def get: Elem = {
    
    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
    <head>
      <title>Missing equivalent Wikidata properties</title>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    </head>
    <body>
      <h2>Missing equivalent Wikidata properties</h2>
      { 
        val nodes = new NodeBuffer()
        var count = 0
        for (item <- ontology.properties.values.toArray.sortBy(_.name) if ! item.equivalentProperties.exists(_.uri.startsWith(RdfNamespace.WIKIDATA.namespace))) {
          if (count > 0) nodes += Text(" - ")
          nodes += <a href={pagesUrl+"/OntologyProperty:"+item.name}>{item.name}</a>
          count += 1
        }
        nodes.insert(0, <h4>{count} missing Wikidata properties</h4>)
        nodes
      }
    </body>
    </html>
  }
  
}