package org.dbpedia.extraction.server.resources

import _root_.org.dbpedia.extraction.util.{Language, WikiApi}
import ontology.Ontology
import org.dbpedia.extraction.server.Server
import javax.ws.rs._
import java.util.logging.Logger
import org.dbpedia.extraction.wikiparser.{Namespace,WikiTitle}
import org.dbpedia.extraction.server.util.PageUtils
import org.dbpedia.extraction.sources.{WikiSource, XMLSource}
import org.dbpedia.extraction.destinations.{StringDestination,ThrottlingDestination}
import org.dbpedia.extraction.destinations.formatters.TriXFormatter
import java.net.{URI, URL}
import java.lang.Exception
import xml.{ProcInstr, XML, NodeBuffer, Elem}

/**
 * TODO: merge Extraction.scala and Mappings.scala
 */
@Path("mappings/{lang}/")
class Mappings(@PathParam("lang") langCode : String)
{
    private val logger = Logger.getLogger(classOf[Ontology].getName)

    private val language = Language.getOrElse(langCode, throw new WebApplicationException(new Exception("invalid language "+langCode), 404))

    if(!Server.config.languages.contains(language))
        throw new WebApplicationException(new Exception("language "+langCode+" not configured in server"), 404)

    /**
     * Retrieves an overview page
     */
    @GET
    @Produces(Array("application/xhtml+xml"))
    def get : Elem =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <body>
            <h2>Mappings</h2>
            <a href="pages/">Source Pages</a><br/>
            <a href="validate/">Validate Pages</a><br/>
            <a href="extractionSamples/">Retrieve extraction samples</a><br/>
            <a href={"../../statistics/"+language.wikiCode+"/"}>Statistics</a><br/>
          </body>
        </html>
    }

    /**
     * Retrieves a mapping page
     */
    @GET
    @Path("pages/")
    @Produces(Array("application/xhtml+xml"))
    def getPages : Elem =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <body>
            <h2>Mapping pages</h2>
            { Server.extractor.mappingPageSource(language).map(page => PageUtils.relativeLink(page) ++ <br/>) }
          </body>
        </html>
    }

    /**
     * Retrieves a mapping page
     */
    @GET
    @Path("pages/{title: .+$}")
    @Produces(Array("application/xml"))
    def getPage(@PathParam("title") @Encoded title : String) : Elem =
    {
        logger.info("Get mappings page: " + title)
        Server.extractor.mappingPageSource(language).find(_.title == WikiTitle.parse(title, language))
                                                 .getOrElse(throw new Exception("No mapping found for " + title)).toXML
    }

    /**
     * Writes a mapping page
     */
    @PUT
    @Path("pages/{title: .+$}")
    @Consumes(Array("application/xml"))
    def putPage(@PathParam("title") @Encoded title : String, pageXML : Elem)
    {
        try
        {
            for(page <- XMLSource.fromXML(pageXML))
            {
                Server.extractor.updateMappingPage(page, language)
                logger.info("Updated mapping page: " + page.title)
            }
        }
        catch
        {
            case ex : Exception =>
            {
                logger.warning("Error updating mapping page: " + title + ". Details: " + ex.getMessage)
                throw ex
            }
        }
    }

    /**
     * Deletes a mapping page
     */
    @DELETE
    @Path("pages/{title: .+$}")
    @Consumes(Array("application/xml"))
    def deletePage(@PathParam("title") @Encoded title : String)
    {
        Server.extractor.removeMappingPage(WikiTitle.parse(title, language), language)
        logger.info("Deleted mapping page: " + title)
    }

    /**
     * Retrieves the validation overview page
     */
    @GET
    @Path("validate/")
    @Produces(Array("application/xhtml+xml"))
    def validate : Elem =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <body>
            <h2>Mapping pages</h2>
            { Server.extractor.mappingPageSource(language).map(page => PageUtils.relativeLink(page) ++ <br/>) }
          </body>
        </html>
    }


    /**
     * Validates a mapping page from the Wiki.
     */
    @GET
    @Path("validate/{title: .+$}")
    @Produces(Array("application/xml"))
    def validateExistingPage(@PathParam("title") @Encoded title : String) =
    {
        var nodes = new NodeBuffer()
        val stylesheetUri = "../" * title.count(_ == '/') + "../../../stylesheets/log.xsl"  // if there are slashes in the title, the stylesheets are further up in the directory tree
        nodes += new ProcInstr("xml-stylesheet", "type=\"text/xsl\" href=\"" + stylesheetUri + "\"")  // <?xml-stylesheet type="text/xsl" href="{logUri}"?>
        nodes += Server.extractor.validateMapping(WikiSource.fromTitles(WikiTitle.parse(title, language) :: Nil, Server.config.wikiApiUrl), language)
        nodes
    }

    /**
     * Validates a mapping source.
     */
    @POST
    @Path("validate/{title: .+$}")
    @Consumes(Array("application/xml"))
    @Produces(Array("application/xml"))
    def validatePage(@PathParam("title") @Encoded title : String, pagesXML : Elem) =
    {
        try
        {
            var nodes = new NodeBuffer()
            val stylesheetUri = "../" * title.count(_ == '/') + "../../../stylesheets/log.xsl"  // if there are slashes in the title, the stylesheets are further up in the directory tree
            nodes += new ProcInstr("xml-stylesheet", "type=\"text/xsl\" href=\"" + stylesheetUri + "\"")  // <?xml-stylesheet type="text/xsl" href="{logUri}"?>
            nodes += Server.extractor.validateMapping(XMLSource.fromXML(pagesXML), language)
            logger.info("Validated mapping page: " + title)
            nodes
        }
        catch
        {
            case ex : Exception =>
            {
                logger.warning("Error validating mapping page: " + title + ". Details: " + ex.getMessage)
                throw ex
            }
        }
    }

    /**
     * Retrieves the extraction overview page
     */
    @GET
    @Path("extractionSamples/")
    @Produces(Array("application/xhtml+xml"))
    def extractionSamples : Elem =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <body>
            <h2>Mapping pages</h2>
            { Server.extractor.mappingPageSource(language).map(page => PageUtils.relativeLink(page) ++ <br/>) }
          </body>
        </html>
    }

    @GET
    @Path("extractionSamples/{title: .+$}")
    @Produces(Array("application/xml"))
    def getExtractionSample(@PathParam("title") @Encoded title : String) : String =
    {
        //Get the title of the mapping as well as its corresponding template on Wikipedia
        val mappingTitle = WikiTitle.parse(title, language)
        val templateTitle = new WikiTitle(mappingTitle.decoded, Namespace.Template, mappingTitle.language)

        //Find pages which use this mapping
        val wikiApiUrl = new URL("http://" + language.wikiCode + ".wikipedia.org/w/api.php")
        val api = new WikiApi(wikiApiUrl, language)
        val pageTitles = api.retrieveTemplateUsages(templateTitle, 10)

        logger.info("extracting sample for '" + templateTitle.encodedWithNamespace + "' language " + language)
        
        //Extract pages
        val stylesheetUri = new URI(("../" * title.count(_ == '/')) + "../../../stylesheets/trix.xsl")  // if there are slashes in the title, the stylesheets are further up in the directory tree
        val destination = new StringDestination(new TriXFormatter(stylesheetUri))
        val source = WikiSource.fromTitles(pageTitles, wikiApiUrl, language)
        // extract at most 1000 triples
        Server.extractor.extract(source, new ThrottlingDestination(destination, 1000), language)
        destination.close()
        destination.toString
    }
}
