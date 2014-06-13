package org.dbpedia.extraction.server.resources.ontology

import org.dbpedia.extraction.server.Server
import javax.ws.rs._
import xml.Elem
import org.dbpedia.extraction.ontology.io.OntologyOWLWriter
import org.dbpedia.extraction.server.resources.serverHeader

@Path("/ontology/")
class Ontology
{
    /**
     * Retrieves an overview page
     */
    @GET
    @Produces(Array("application/xhtml+xml"))
    def get : Elem =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          {serverHeader.getheader()}
          <body>
            <div class="row">
              <div class="col-md-3 col-md-offset-5">
                <h2>Ontology</h2>
                <a href="pages/">Source Pages</a><br/>
                <a href="validate/">Validate</a><br/>
                <a href="classes/">Classes</a><br/>
                <a href="labels/missing/">Missing Labels</a><br/>
                <a href="wikidata/missing/">Missing Wikidata Properties</a><br/>
                <a href="dbpedia.owl">Ontology (OWL)</a><br/>
              </div>
            </div>
          </body>
        </html>
    }

    /**
     * Exports the ontology as OWL. Also match "export" for backwards compatibility with old links.
     */
    @GET
    @Path("{dummy:dbpedia.owl|export}")
    @Produces(Array("application/rdf+xml"))
    def ontology =
    {
        new OntologyOWLWriter("latest-snapshot").write(Server.instance.extractor.ontology)
    }
}
