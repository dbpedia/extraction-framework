package org.dbpedia.extraction.server.resources

import javax.ws.rs._
import javax.ws.rs.core.Response
import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.server.util.StringUtils.urlEncode
import org.dbpedia.extraction.util.{WikiUtil, Language}
import java.net.URI

@Path("/ignore/{lang}/")
class Ignore (@PathParam("lang") langCode: String, @QueryParam("p") password: String) {
  
    private val language = Language.getOrElse(langCode, throw new WebApplicationException(new Exception("invalid language " + langCode), 404))

    if (! Server.languages.contains(language)) throw new WebApplicationException(new Exception("language " + langCode + " not defined in server"), 404)

    private val manager = Server.statsManager(language)

    private val ignoreList = manager.ignoreList
    
    private def cookieQuery(sep: Char, all: Boolean = false) : String = {
      val sb = new StringBuilder
      
      var vsep = sep
      if (Server.adminRights(password)) {
        sb append sep append "p=" append password
        vsep = '&'
      }
      
      if (all) sb append vsep append "all=true"
      
      sb toString
    }
      
    @GET
    @Path("template/")
    @Produces(Array("application/xhtml+xml"))
    def ignoreTemplate(@QueryParam("template") template: String, @QueryParam("ignore") ignore: String, @QueryParam("all") all: Boolean) =
    {
        if (Server.adminRights(password))
        {
            if (ignore == "true") ignoreList.addTemplate(WikiUtil.wikiDecode(template,language,false))
            else ignoreList.removeTemplate(WikiUtil.wikiDecode(template, language,false))
        }
        
        Response.temporaryRedirect(new URI("/statistics/"+langCode+"/"+cookieQuery('?', all)+"#"+urlEncode(template))).build
    }

    @GET
    @Path("property/")
    @Produces(Array("application/xhtml+xml"))
    def ignoreProperty(@QueryParam("template") template: String, @QueryParam("property") property: String, @QueryParam("ignore") ignore: String) =
    {
        if (Server.adminRights(password))
        {
            // Note: do NOT wikiDecode property names - space and underscore are NOT equivalent for them
            if (ignore == "true") ignoreList.addProperty(WikiUtil.wikiDecode(template,language,false), property)
            else ignoreList.removeProperty(WikiUtil.wikiDecode(template,language,false), property)
        }
        
        Response.temporaryRedirect(new URI("/templatestatistics/"+language.wikiCode+"/?template="+template+cookieQuery('&')+"#"+urlEncode(property))).build
    }

}