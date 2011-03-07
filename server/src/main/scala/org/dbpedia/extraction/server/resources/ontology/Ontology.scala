package org.dbpedia.extraction.server.resources.ontology

import org.dbpedia.extraction.server.Server
import javax.ws.rs._
import xml.Elem
import org.dbpedia.extraction.ontology.io.OntologyOWLWriter
import org.dbpedia.extraction.server.resources.Base

@Path("/ontology")
class Ontology extends Base
{
    /**
     * Retrieves an overview page
     */
    @GET
    @Produces(Array("application/xhtml+xml"))
    def get : Elem =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <body>
            <h2>Ontology</h2>
            <a href="ontology/pages">Source Pages</a><br/>
            <a href="ontology/validate">Validate</a><br/>
            <a href="ontology/classes">Classes</a><br/>
            <a href="ontology/export">Ontology (OWL)</a><br/>
          </body>
        </html>
    }

    /**
     * Exports the ontology as OWL.
     */
    @GET
    @Path("/export")
    @Produces(Array("application/rdf+xml"))
    def export =
    {
        new OntologyOWLWriter().write(Server.extractor.ontology)
    }
}
