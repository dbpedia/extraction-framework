package org.dbpedia.extraction.server.resources.ontology

import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.OntologyEntity
import org.dbpedia.extraction.server.util.PageUtils.languageList
import scala.xml.{Elem,Text,NodeBuffer}
import javax.ws.rs._

@Path("/ontology/labels/missing/")
class MissingLabels {
  
  private val ontology = Server.instance.extractor.ontology

  private val pagesUrl = Server.instance.paths.pagesUrl

  @GET
  @Produces(Array("application/xhtml+xml"))
  def get: Elem = languageList("DBpedia Missing Ontology Labels", "Missing Ontology labels", "Missing labels for")

  @GET
  @Path("/{lang}/")
  @Produces(Array("application/xhtml+xml"))
  def forLanguage(@PathParam("lang") langCode : String): Elem = {
    
    // TODO: use ISO codes? Language uses wiki codes.
    val language = Language.getOrElse(langCode, throw new WebApplicationException(new Exception("invalid language "+langCode), 404))

    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
    <head>
      <title>Missing labels for language { langCode }</title>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    </head>
    <body>
      <h2>Missing labels for language { langCode }</h2>
      { listLabels(language, ontology.classes, "Classes", "OntologyClass") }
      { listLabels(language, ontology.properties, "Properties", "OntologyProperty") }
      { listLabels(language, ontology.datatypes, "Datatypes", "Datatype") }
    </body>
    </html>
  }
  
  private def listLabels(language: Language, items: Map[String, OntologyEntity], header: String, namespace: String): Elem = {
    <div id={header}>
    <h3>{ header }</h3>
    { 
      val nodes = new NodeBuffer()
      var count = 0
      for (item <- items.values.toSeq.sortBy(_.name) if ! item.labels.contains(language)) {
        if (count > 0) nodes += Text(" - ")
        nodes += <a href={pagesUrl+"/"+namespace+":"+item.name}>{item.name}</a>
        count += 1
      }
      nodes.insert(0, <h4>{count} missing labels</h4>)
      
      nodes
    }
    </div>
  }
  
}