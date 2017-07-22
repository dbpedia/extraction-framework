package org.dbpedia.extraction.server.resources

import java.io.InputStream
import javax.ws.rs.core.{MediaType, Response}
import javax.ws.rs.{Produces, _}

import com.fasterxml.jackson.databind.ObjectMapper

import scala.xml.Elem

/**
  * Created by wmaroy on 22.07.17.
  */

@Path("rml/")
class RML {

  @GET
  @Produces(Array("application/xhtml+xml"))
  def get: Elem = {
    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
      {ServerHeader.getHeader("RML API")}
      <body>
        <h3>RML API 1.0.0</h3>
      </body>
    </html>
  }

  @POST
  @Path("templates/simpleproperty")
  @Consumes(Array("application/json"))
  @Produces(Array("application/json"))
  def addSimplePropertyMapping(json : String) = {
    //println(simplePropertyMapping)
    print(json)
    val response = "{\"response\" : \"test\"}" //convert entity to json
    Response.ok(response, MediaType.APPLICATION_JSON).build()
  }

  @POST
  @Path("templates/geocoordinate")
  @Produces(Array("application/json"))
  def addGeocoordinateMapping = {
    val json = "{\"response\" : \"test\"}" //convert entity to json
    Response.ok(json, MediaType.APPLICATION_JSON).build()
  }

  @POST
  @Path("templates/startdate")
  @Produces(Array("application/json"))
  def addStartDateMapping = {
    val json = "{\"response\" : \"test\"}" //convert entity to json
    Response.ok(json, MediaType.APPLICATION_JSON).build()
  }

  @POST
  @Path("templates/enddate")
  @Produces(Array("application/json"))
  def addEndDateMapping = {
    val json = "{\"response\" : \"test\"}" //convert entity to json
    Response.ok(json, MediaType.APPLICATION_JSON).build()
  }

}
