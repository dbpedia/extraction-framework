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
    // TODO: remove this method. Clients should get the ontology pages directly from the
    // mappings wiki, not from here. We don't want to keep all ontology pages in memory.
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
            { Server.instance.extractor.ontologyPages.values.toArray.sortBy(_.title.decodedWithNamespace).map(PageUtils.relativeLink(_) ++ <br/>) }
          </body>
        </html>
    }

    /**
     * Retrieves an ontology page
     */
    // TODO: remove this method. Clients should get the ontology pages directly from the
    // mappings wiki, not from here. We don't want to keep all ontology pages in memory.
    @GET
    @Path("/{title}")
    @Produces(Array("application/xml"))
    // FIXME: Why @Encoded? Probably wrong.
    def getPage(@PathParam("title") @Encoded title : String) : Elem =
    {
        logger.info("Get ontology page: " + title)
        Server.instance.extractor.ontologyPages(WikiTitle.parse(title, Language.Mappings)).toDumpXML
    }

    /**
     * Writes an ontology page
     */
    @PUT
    @Path("/{title}")
    @Consumes(Array("application/xml"))
    // FIXME: Why @Encoded? Probably wrong.
    def putPage(@PathParam("title") @Encoded title : String, pageXML : Elem)
    {
        try
        {
            for(page <- XMLSource.fromXML(pageXML, Language.Mappings))
            {
                Server.instance.extractor.updateOntologyPage(page)
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
    // FIXME: Why @Encoded? Probably wrong.
    def deletePage(@PathParam("title") @Encoded title : String)
    {
        Server.instance.extractor.removeOntologyPage(WikiTitle.parse(title, Language.Mappings))
        logger.info("Deleted ontology page: " + title)
    }
}
