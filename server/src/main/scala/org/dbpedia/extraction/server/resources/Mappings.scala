package org.dbpedia.extraction.server.resources

import org.dbpedia.extraction.mappings.{MappingsLoader, Redirects}
import org.dbpedia.extraction.util.{Language, WikiApi}
import org.dbpedia.extraction.server.resources.stylesheets.{Log, TriX}
import org.dbpedia.extraction.server.Server
import javax.ws.rs._
import java.util.logging.{Level, Logger}

import org.dbpedia.extraction.wikiparser.{Namespace, WikiParser, WikiTitle}
import org.dbpedia.extraction.server.util.PageUtils
import org.dbpedia.extraction.sources.{WikiSource, XMLSource}
import org.dbpedia.extraction.destinations.{LimitingDestination, WriterDestination}

import xml.{Elem, NodeBuffer}
import java.io.StringWriter
import java.net.URL

import org.dbpedia.extraction.config.ExtractionRecorder
import org.dbpedia.extraction.server.resources.rml.model.factories.RMLTemplateMappingFactory

import scala.reflect.ClassTag

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
          {ServerHeader.getHeader("Mappings")}
          <body>
            <div class="row">
              <div class="col-md-3 col-md-offset-5">
              <h2>Mappings</h2>
              <a href="pages/">Original Source Pages</a><br/>
              <a href="pages/rml">RML Source Pages</a><br/>
              <a href="validate/">Validate Pages</a><br/>
              <a href="extractionSamples/">Retrieve extraction samples</a><br/>
              <a href="redirects/">Redirects</a><br/>
              <a href={"../../statistics/"+language.wikiCode+"/"}>Statistics</a><br/>
              </div>
            </div>
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
          {ServerHeader.getHeader("Mapping pages")}
          <body>
            <div class="row">
             <div class="col-md-3 col-md-offset-5">
            <h2>Mapping pages</h2>
            { Server.instance.extractor.mappingPageSource(language).map(page => PageUtils.relativeLink(parser(page).getOrElse(throw new Exception("Cannot get page: " + page.title.decoded + ". Parsing failed"))) ++ <br/>) }
             </div>
            </div>
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
     * Retrieves a mapping as rml
     */
    @GET
    @Path("pages/rml/")
    @Produces(Array("application/xml"))
    def getRdf(@PathParam("title") title : String) : Elem =
    {
      val pages = Server.instance.extractor.mappingPageSource(language)

      <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
        {ServerHeader.getHeader("Mapping pages")}
        <body>
          <div class="row">
            <div class="col-md-3 col-md-offset-5">
              <h2>RML Mapping pages</h2>
              { pages.map(page => PageUtils.relativeLink(parser(page).getOrElse(throw new Exception("Cannot get page: " + page.title.decoded + ". Parsing failed"))) ++ <br/>) }
            </div>
          </div>
        </body>
      </html>
    }

    /**
     * Retrieves a rml mapping page
     */
    @GET
    @Path("pages/rml/{title: .+$}")
    @Produces(Array("text/turtle"))
    def getRdfMapping(@PathParam("title") title : String) : String =
    {
      logger.info("Get mappings page: " + title)
      val parsed = WikiTitle.parse(title, language)
      val pages = Server.instance.extractor.mappingPageSource(language)
      val page = pages.filter(_.title == parsed)

  //    if(page.size != 1)
  //      if(getAllMappings)
  //        return ""
  //      else
  //        throw new Exception("Cannot get page: " + title + ". Parsing failed")  //TODO??

      // context object that has only this mappingSource
      val lang = Language.getOrElse(langCode, throw new WebApplicationException(new Exception("invalid language "+langCode), 404))
      val context = new {
        val ontology = Server.instance.extractor.ontology()
        val language = lang
        val redirects: Redirects = new Redirects(Map())
        val mappingPageSource = page
        def recorder[T: ClassTag]: ExtractionRecorder[T] = Server.getExtractionRecorder[T](lang)
      }

      //Load mappings
      val factory = new RMLTemplateMappingFactory()
      val rmlMapping = factory.createMapping(parser(page.head).get, language, MappingsLoader.load(context))
      rmlMapping.writeAsTurtle
    }

    /**
     * Retrieves all rml mapping pages
     */
    @GET
    @Path("pages/rdf/all")
    @Produces(Array("text/turtle"))
    def getAllRdfMappings() : String =
    {
      //getAllMappings = true
      val builder = new StringBuilder()
      val titles = Server.instance.extractor.mappingPageSource(language).map(x => x.title.encodedWithNamespace.replace(":", "%3A"))

      for(title <- titles) {
        val zw = try {getRdfMapping(title)}
        catch {
          case x: Throwable => ""
        }
        builder.append(zw)
      }
      //getAllMappings = false
      builder.toString()
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
          {ServerHeader.getHeader("Validate Mappings")}
          <body>
            <div class="row">
             <div class="col-md-3 col-md-offset-5">
              <h2>Validate Mappings</h2>
              <p><a href="*">Validate all mappings</a></p>
             { Server.instance.extractor.mappingPageSource(language).map(page => PageUtils.relativeLink(parser(page).getOrElse(throw new Exception("Cannot validate mapping: " + page.title.decoded + ". Parsing failed"))) ++ <br/>) }
            </div>
           </div>
          </body>
        </html>
    }


    protected val parser = WikiParser.getInstance()

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
            nodes += Server.instance.extractor.validateMapping(XMLSource.fromXML(pagesXML, language), language) // TODO: use Language.Mappings?
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
          {ServerHeader.getHeader("Mapping pages")}
          <body>
           <div class="row">
            <div class="col-md-3 col-md-offset-5">
             <h2>Mapping pages</h2>
             { Server.instance.extractor.mappingPageSource(language).map(page => PageUtils.relativeLink(parser(page).getOrElse(throw new Exception("Cannot read page: " + page.title.decoded + ". Parsing failed"))) ++ <br/>) }
            </div>
            </div>
          </body>
        </html>
    }

    @GET
    @Path("extractionSamples/{title: .+$}")
    @Produces(Array("application/xml"))
    // @Encoded because title.count(_ == '/') must treat "%2F" different from "/"
    // TODO: change to @QueryParam, avoid the count(_ == '/') trick
    def getExtractionSample(
        @PathParam("title") @Encoded title : String,
        @QueryParam("namespace") @DefaultValue("main") arg_namespace: String
    ): String = {
        //Get the title of the mapping as well as its corresponding template on Wikipedia
        val mappingTitle = WikiTitle.parse(title, language) // TODO: use Language.Mappings?
        val templateTitle = new WikiTitle(mappingTitle.decoded, Namespace.Template, language)

        // Determine the namespace; if no such namespace exists for this
        // language, then default to Namespace.Main.
        val namespace = Namespace.get(language, arg_namespace) match {
            case Some(ns) => ns
            case None => Namespace.Main
        }

        //Find pages which use this mapping
        val wikiApiUrl = new URL(language.apiUri)
        val api = new WikiApi(wikiApiUrl, language)
        // TODO: one call to api.php is probably enough - get content of pages, not just titles
        val pageTitles = api.retrieveTemplateUsages(
            title = templateTitle, 
            namespace = namespace, 
            maxCount = 10)

        logger.info("extracting "+pageTitles.size+" pages for '" + templateTitle.encodedWithNamespace + "' language " + language + " namespace " + namespace)
        
        // Extract pages
        // if there are slashes in the title, the stylesheets are further up in the directory tree
        val writer = new StringWriter
        val formatter = TriX.writeHeader(writer, title.count(_ == '/')+3)
        val destination = new WriterDestination(() => writer, formatter)
        // TODO: one call to api.php is probably enough
        val source = WikiSource.fromTitles(pageTitles, wikiApiUrl, language)
        // extract at most 1000 triples
        Server.instance.extractor.extract(source, new LimitingDestination(destination, 1000), language)
        
        writer.toString
    }


    /**
     * Lists all the redirected mapped templates
     */
    @GET
    @Path("redirects/")
    @Produces(Array("application/xhtml+xml"))
    def mappedRedirects : Elem =
    {
      <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
        {ServerHeader.getHeader("Mapped Templates Redirects", true)}
        <body>
          <h2 align="center">Mapped Templates Redirects</h2>
          <table class="tablesorter table myTable" align="center" style="width:500px; margin:auto">
            <thead>
              <tr>
                <th>From</th>
                <th>To</th>
                <th>Simple move</th>
              </tr>
            </thead>
            <tbody>

              {
                val manager = Server.instance.managers(language)
                val statsHolder = manager.holder
                val reversedRedirects = statsHolder.reversedRedirects.map(_.swap)
                var pages = Server.instance.extractor.mappingPageSource(language).map(_.title.decoded).toSet

                for((redirect_from,redirect_to) <- reversedRedirects ) yield
                {
                  val rf = WikiTitle.parse(redirect_from, language)
                  val rt = WikiTitle.parse(redirect_to, language)
                  val simpleMove = if (pages.contains(rt.decoded)) <span >Merge</span>
                  else <a href={ "http://mappings.dbpedia.org/index.php/Special:MovePage/Mapping_" + langCode + ":" +rf.decoded }>Move</a>
                  if (pages.contains(rf.decoded)) {
                    <tr>
                      <td>
                        <a href={"http://mappings.dbpedia.org/index.php/Mapping_" + langCode + ":" +rf.decoded}>{rf.decoded}</a> </td>
                    <td>
                      <a href={"http://mappings.dbpedia.org/index.php/Mapping_" + langCode + ":" + rt.decoded}>{rt.decoded}</a>
                    </td>
                      <td> { simpleMove }
                      </td>
                    </tr>
                  }
                }
              }
            </tbody>
          </table>
        </body>
      </html>
    }
}
