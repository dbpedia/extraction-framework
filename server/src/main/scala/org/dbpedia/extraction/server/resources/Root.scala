package org.dbpedia.extraction.server.resources

import javax.ws.rs.{GET, Path, Produces}

import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.server.util.PageUtils._
import org.dbpedia.extraction.util.Language

import scala.xml.Elem

@Path("/")
class Root
{
    @GET @Produces(Array("application/xhtml+xml"))
    def get =
    {
      <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
        {serverHeader.getheader()}
          <body>
            <h2 align="center">DBpedia Server</h2>
            <p align="center">
            <a href="ontology/">Ontology</a>
            </p>
            <p align ="center">
            <a href="statistics/">Statistics</a> -
            <a href="mappings/">Mappings</a> -
            <a href="extraction/">Extractors</a> -
            <a href="ontology/labels/missing/">Missing labels</a>
            </p>
            <table class="tablesorter table myTable" align="center" style="width:500px; margin:auto">
            <thead>
            <tr>
              <th> Language</th>
              <th> Action</th>
            </tr>
            </thead>
            <tbody>

            {
              // we need toArray here to keep languages ordered.
              for(lang <- Server.instance.managers.keys.toArray; code = lang.wikiCode) yield
              {
                <tr>
                <td> {code} </td>
                <td>
                <a href={"statistics/" + code + "/"}>Statistics </a> -
                <a href={"mappings/" + code + "/"}>Mappings </a> -
                <a href={"extraction/" + code + "/"}>Extractor </a> -
                <a href={"ontology/labels/missing/" + code + "/"}>Missing labels </a>
                </td>
                </tr>
              }
            }
            </tbody>
            </table>
          </body>
        </html>

    }
    
    /**
     * List of all languages. Currently for the sprint code, may be useful for others.
     */
    @GET @Path("languages/") @Produces(Array("text/plain"))
    def languages = Server.instance.managers.keys.toArray.map(_.wikiCode).mkString(" ")
    
    @GET @Path("statistics/") @Produces(Array("application/xhtml+xml"))
    def statistics: Elem = {
      // we need toBuffer here to keep languages ordered.
      val links = Server.instance.managers.keys.toBuffer[Language].map(lang => (lang.wikiCode+"/", "Mapping Statistics for "+lang.wikiCode))
      links.insert(0, ("*/", "Mapping Statistics for all languages"))
      linkList("DBpedia Mapping Statistics", "Statistics", links)
    }

    @GET @Path("mappings/") @Produces(Array("application/xhtml+xml"))
    def mappings = languageList("DBpedia Template Mappings", "Mappings", "Mappings for")
    
    @GET @Path("extraction/") @Produces(Array("application/xhtml+xml"))
    def extraction = languageList("DBpedia Test Extractors", "Extractors", "Extractor for")

}
object serverHeader {
  def getheader()={
    <head>
      <title>DBpedia Server</title>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
      <link href="//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css" rel="stylesheet" type="text/css" />
      <link href="http://cdnjs.cloudflare.com/ajax/libs/jquery.tablesorter/2.16.4/css/theme.default.css" rel="stylesheet" type ="text/css" />
      <script type="text/javascript" src="http://code.jquery.com/jquery-1.11.0.min.js"></script>
      <script type="text/javascript" src="http://mottie.github.com/tablesorter/js/jquery.tablesorter.js"></script>
      <script type="text/javascript">
        //{scala.xml.PCData("""
                        $(document).ready(function()
                          {
                               $(".myTable").tablesorter();
                            }
                          );
             // """)}
      </script>
    </head>
  }
}