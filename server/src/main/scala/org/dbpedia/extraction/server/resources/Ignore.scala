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

    if (! Server.instance.managers.contains(language)) throw new WebApplicationException(new Exception("language " + langCode + " not defined in server"), 404)

    private val manager = Server.instance.managers(language)

    private val ignoreList = manager.ignoreList
    
    private def wikiDecode(name: String) : String = WikiUtil.wikiDecode(name, language, capitalize=false)

    private def cookieQuery(sep: Char, show: Int = -1) : String = {
      var vsep = sep
      
      val sb = new StringBuilder
      
      if (Server.instance.adminRights(password)) {
        // TODO: URL encode password
        sb append vsep append "p=" append password
        vsep = '&'
      }
      
      if (show != -1) { 
        sb append vsep append "show=" append show
        vsep = '&' // for future additions below
      }
      
      sb toString
    }
      
    @GET
    @Path("template/")
    @Produces(Array("application/xhtml+xml"))
    def ignoreTemplate(@QueryParam("template") template: String, @QueryParam("ignore") ignore: String, @QueryParam("show") @DefaultValue("20") show: Int = 20) =
    {
        if (Server.instance.adminRights(password))
        {
            if (ignore == "true") ignoreList.addTemplate(wikiDecode(template))
            else ignoreList.removeTemplate(wikiDecode(template))
        }
        
        Response.temporaryRedirect(new URI("/statistics/"+langCode+"/"+cookieQuery('?', show)+"#"+urlEncode(template))).build
    }

    @GET
    @Path("property/")
    @Produces(Array("application/xhtml+xml"))
    def ignoreProperty(@QueryParam("template") template: String, @QueryParam("property") property: String, @QueryParam("ignore") ignore: String) =
    {
        if (Server.instance.adminRights(password))
        {
            // Note: do NOT wikiDecode property names - space and underscore are NOT equivalent for them
            if (ignore == "true") ignoreList.addProperty(wikiDecode(template), property)
            else ignoreList.removeProperty(wikiDecode(template), property)
        }
        
        Response.temporaryRedirect(new URI("/templatestatistics/"+language.wikiCode+"/?template="+template.replace(' ', '_')+cookieQuery('&')+"#"+urlEncode(property))).build
    }

}