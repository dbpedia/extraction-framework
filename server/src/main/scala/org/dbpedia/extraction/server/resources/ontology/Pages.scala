package org.dbpedia.extraction.server.resources.ontology

import xml.Elem
import org.dbpedia.extraction.wikiparser.WikiTitle
import javax.ws.rs._
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.server.Server
import java.util.logging.Logger
import org.dbpedia.extraction.server.util.PageUtils
import org.dbpedia.extraction.util.Language

@Path("/ontology/pages/")
class Pages
{
    val logger = Logger.getLogger(classOf[Pages].getName)

    /**
     * Retrieves an overview page
     */
    @GET
    @Produces(Array("application/xhtml+xml"))
    def getPages : Elem =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
          </head>
          <body>
            <h2>Ontology pages</h2>
            { Server.extractor.ontologyPages.values.toArray.sortBy(_.title.decodedWithNamespace).map(PageUtils.relativeLink(_) ++ <br/>) }
          </body>
        </html>
    }

    /**
     * Retrieves an ontology page
     */
    @GET
    @Path("/{title}")
    @Produces(Array("application/xml"))
    def getPage(@PathParam("title") @Encoded title : String) : Elem =
    {
        logger.info("Get ontology page: " + title)
        Server.extractor.ontologyPages(WikiTitle.parse(title, Language.Default)).toXML
    }

    /**
     * Writes an ontology page
     */
    @PUT
    @Path("/{title}")
    @Consumes(Array("application/xml"))
    def putPage(@PathParam("title") @Encoded title : String, pageXML : Elem)
    {
        try
        {
            for(page <- XMLSource.fromXML(pageXML))
            {
                Server.extractor.updateOntologyPage(page)
                logger.info("Updated ontology page: " + title)
            }
        }
        catch
        {
            case ex : Exception =>
            {
                logger.warning("Error updating ontology page: " + title + ". Details: " + ex.getMessage)
                throw ex
            }
        }
    }

    /**
     * Deletes an ontology page
     */
    @DELETE
    @Path("/{title}")
    @Consumes(Array("application/xml"))
    def deletePage(@PathParam("title") @Encoded title : String)
    {
        Server.extractor.removeOntologyPage(WikiTitle.parse(title, Language.Default))
        logger.info("Deleted ontology page: " + title)
    }
}
