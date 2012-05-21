package org.dbpedia.extraction.server.resources

import org.dbpedia.extraction.util.{Language, WikiApi}
import org.dbpedia.extraction.server.resources.stylesheets.{TriX,Log}
import org.dbpedia.extraction.server.Server
import javax.ws.rs._
import java.util.logging.{Logger,Level}
import org.dbpedia.extraction.wikiparser.{Namespace,WikiTitle,WikiParser}
import org.dbpedia.extraction.server.util.PageUtils
import org.dbpedia.extraction.sources.{WikiSource, XMLSource}
import org.dbpedia.extraction.destinations.{WriterDestination,LimitingDestination}
import java.net.{URI, URL}
import java.lang.Exception
import xml.{ProcInstr, XML, NodeBuffer, Elem}
import java.io.StringWriter

/**
 * TODO: merge Extraction.scala and Mappings.scala
 */
@Path("mappings/{lang}/")
class Mappings(@PathParam("lang") langCode : String)
{
    private val logger = Logger.getLogger(classOf[Mappings].getName)

    private val language = Language.getOrElse(langCode, throw new WebApplicationException(new Exception("invalid language "+langCode), 404))

    if(!Server.instance.managers.contains(language))
        throw new WebApplicationException(new Exception("language "+langCode+" not configured in server"), 404)

    /**
     * Retrieves an overview page
     */
    // TODO: remove this method. Clients should get the mapping pages directly from the
    // mappings wiki, not from here. We don't want to keep all ontology pages in memory.
    @GET
    @Produces(Array("application/xhtml+xml"))
    def get : Elem =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
          </head>
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
    // TODO: remove this method. Clients should get the mapping pages directly from the
    // mappings wiki, not from here. We don't want to keep all ontology pages in memory.
    @GET
    @Path("pages/")
    @Produces(Array("application/xhtml+xml"))
    def getPages : Elem =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
          </head>
          <body>
            <h2>Mapping pages</h2>
            { Server.instance.extractor.mappingPageSource(language).map(page => PageUtils.relativeLink(page) ++ <br/>) }
          </body>
        </html>
    }

    /**
     * Retrieves a mapping page
     */
    @GET
    @Path("pages/{title: .+$}")
    @Produces(Array("application/xml"))
    def getPage(@PathParam("title") title : String) : Elem =
    {
        logger.info("Get mappings page: " + title)
        val parsed = WikiTitle.parse(title, language)
        val pages = Server.instance.extractor.mappingPageSource(language)
        pages.find(_.title == parsed).getOrElse(throw new Exception("No mapping found for " + parsed)).toDumpXML
    }

    /**
     * Writes a mapping page
     */
    @PUT
    @Path("pages/{title: .+$}")
    @Consumes(Array("application/xml"))
    def putPage(@PathParam("title") title : String, pageXML : Elem)
    {
        try
        {
            for(page <- XMLSource.fromXML(pageXML, language)) // TODO: use Language.Mappings?
            {
                Server.instance.extractor.updateMappingPage(page, language)
                logger.info("Updated mapping page: " + page.title)
            }
        }
        catch
        {
            case ex : Exception =>
            {
                logger.log(Level.WARNING, "Error updating mapping page: " + title, ex)
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
    def deletePage(@PathParam("title") title : String)
    {
        // TODO: use Language.Mappings?
        Server.instance.extractor.removeMappingPage(WikiTitle.parse(title, language), language)
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
          <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
          </head>
          <body>
            <h2>Validate Mappings</h2>
            <p><a href="*">Validate all mappings</a></p>
            { Server.instance.extractor.mappingPageSource(language).map(page => PageUtils.relativeLink(page) ++ <br/>) }
          </body>
        </html>
    }


    protected val parser = WikiParser()

    /**
     * Validates a mapping page from the Wiki, or all mapping pages if title is "*".
     */
    @GET
    @Path("validate/{title: .+$}")
    @Produces(Array("application/xml"))
    // @Encoded because title.count(_ == '/') must treat "%2F" different from "/"
    // TODO: change to @QueryParam, avoid the count(_ == '/') trick
    def validateExistingPage(@PathParam("title") @Encoded title: String) =
    {
      var pages = Server.instance.extractor.mappingPageSource(language)
      
      if (title != "*") {
        // TODO: use Language.Mappings?
        val wikiTitle = WikiTitle.parse(title, language)
        pages = pages.filter(_.title == wikiTitle)
      }
    
      var nodes = new NodeBuffer()
      nodes += Log.header(title.count(_ == '/') + 3)
      nodes += Server.instance.extractor.validateMapping(pages, language)
      
      nodes
    }

    /**
     * Validates a mapping source.
     */
    @POST
    @Path("validate/{title: .+$}")
    @Consumes(Array("application/xml"))
    @Produces(Array("application/xml"))
    // @Encoded because title.count(_ == '/') must treat "%2F" different from "/"
    // TODO: change to @QueryParam, avoid the count(_ == '/') trick
    def validatePage(@PathParam("title") @Encoded title : String, pagesXML : Elem) =
    {
        try
        {
            var nodes = new NodeBuffer()
            nodes += Log.header(title.count(_ == '/') + 3)
            nodes += Server.instance.extractor.validateMapping(XMLSource.fromXML(pagesXML, language).map(parser), language) // TODO: use Language.Mappings?
            logger.info("Validated mapping page: " + title)
            nodes
        }
        catch
        {
            case ex : Exception =>
            {
                logger.log(Level.WARNING, "Error validating mapping page: " + title, ex)
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
          <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
          </head>
          <body>
            <h2>Mapping pages</h2>
            { Server.instance.extractor.mappingPageSource(language).map(page => PageUtils.relativeLink(page) ++ <br/>) }
          </body>
        </html>
    }

    @GET
    @Path("extractionSamples/{title: .+$}")
    @Produces(Array("application/xml"))
    // @Encoded because title.count(_ == '/') must treat "%2F" different from "/"
    // TODO: change to @QueryParam, avoid the count(_ == '/') trick
    def getExtractionSample(@PathParam("title") @Encoded title : String) : String =
    {
        //Get the title of the mapping as well as its corresponding template on Wikipedia
        val mappingTitle = WikiTitle.parse(title, language)
        val templateTitle = new WikiTitle(mappingTitle.decoded, Namespace.Template, language)

        //Find pages which use this mapping
        val wikiApiUrl = new URL(language.apiUri)
        val api = new WikiApi(wikiApiUrl, language)
        val pageTitles = api.retrieveTemplateUsages(templateTitle, 10)

        logger.info("extracting sample for '" + templateTitle.encodedWithNamespace + "' language " + language)
        
        // Extract pages
        // if there are slashes in the title, the stylesheets are further up in the directory tree
        val writer = new StringWriter
        val formatter = TriX.writeHeader(writer, title.count(_ == '/')+3)
        val destination = new WriterDestination(writer, formatter)
        val source = WikiSource.fromTitles(pageTitles, wikiApiUrl, language)
        // extract at most 1000 triples
        Server.instance.extractor.extract(source, new LimitingDestination(destination, 1000), language)
        destination.close()
        
        writer.toString
    }
}
