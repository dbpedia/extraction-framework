package org.dbpedia.extraction.server.resources.ontology

import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.sources.{XMLSource, WikiSource}
import org.dbpedia.extraction.util.Language
import java.net.URL
import org.dbpedia.extraction.wikiparser.WikiTitle
import javax.ws.rs._
import java.util.logging.Logger
import xml.{NodeBuffer, Elem}

@Path("/ontology/validate/")
class Validate
{
    val logger = Logger.getLogger(classOf[Validate].getName)

    /**
     * Validate the ontology
     */
    @GET
    @Produces(Array("application/xml"))
    def validate =
    {
        var nodes = new NodeBuffer()
        nodes += <?xml-stylesheet type="text/xsl" href="../../stylesheets/log.xsl"?>
        nodes += Server.instance.extractor.validateOntologyPages()
        nodes
    }

    /**
     * Validates an ontology page.
     */
    @POST
    @Path("/{title}")
    @Consumes(Array("application/xml"))
    @Produces(Array("application/xml"))
    def validatePage(@PathParam("title") @Encoded title : String, pageXML : Elem) =
    {
        try
        {
            var nodes = new NodeBuffer()
            nodes += <?xml-stylesheet type="text/xsl" href="../../stylesheets/log.xsl"?>
            nodes += Server.instance.extractor.validateOntologyPages(XMLSource.fromXML(pageXML, Language.Mappings).toList)
            logger.info("Validated ontology page: " + title)
            nodes
        }
        catch
        {
            case ex : Exception =>
            {
                logger.warning("Error validating ontology page: " + title + ". Details: " + ex.getMessage)
                throw ex
            }
        }
    }
}
