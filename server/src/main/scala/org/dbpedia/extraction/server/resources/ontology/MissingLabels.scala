package org.dbpedia.extraction.server.resources.ontology

import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.OntologyEntity
import scala.xml.{Elem,Text,NodeBuffer}
import javax.ws.rs._

@Path("/ontology/labels/missing/")
class MissingLabels {
  
  private val ontology = Server.instance.extractor.ontology

  private val pagesUrl = Server.instance.paths.pagesUrl

  @GET
  @Produces(Array("application/xhtml+xml"))
  def get: Elem =
  {
    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
    <head>
      <title>Missing labels</title>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    </head>
    <body>
      <h2>Missing labels</h2>
      {
        // we need toArray here to keep languages ordered.
        for(lang <- Server.instance.managers.keys.toArray; code = lang.wikiCode) yield
        {
          <p><a href={code + "/"}>Missing labels for {code}</a></p>
        }
      }
    </body>
    </html>
  }

@GET
  @Path("/{lang}/")
  @Produces(Array("application/xhtml+xml"))
  def forLanguage(@PathParam("lang") langCode : String): Elem = {
    
    // TODO: use ISO codes, not wiki codes.
    val language = Language.getOrElse(langCode, throw new WebApplicationException(new Exception("invalid language "+langCode), 404))

    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
    <head>
      <title>Missing labels for language { langCode }</title>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    </head>
    <body>
      <h2>Missing labels for language { langCode }</h2>
      { missingLabels(language, ontology.classes, "Classes", "OntologyClass") }
      { missingLabels(language, ontology.properties, "Properties", "OntologyProperty") }
      { missingLabels(language, ontology.datatypes, "Datatypes", "Datatype") }
    </body>
    </html>
  }
  
  private def missingLabels(language: Language, items: Map[String, OntologyEntity], header: String, namespace: String): Elem = {
    <div id={header}>
    <h3>{ header }</h3>
    { missingLabels(language, items, namespace) }
    </div>
  }
  
  private def missingLabels(language: Language, items: Map[String, OntologyEntity], namespace: String): NodeBuffer = {
    var nodes = new NodeBuffer()
    var count = 0
    for (item <- items.values.toSeq.sortBy(_.name) if ! item.labels.contains(language)) {
      if (count > 0) nodes += Text(" - ")
      nodes += <a href={pagesUrl+"/"+namespace+":"+item.name}>{item.name}</a>
      count += 1
    }
    nodes.insert(0, <h4>{count} missing labels</h4>)
    nodes
  }
  
}