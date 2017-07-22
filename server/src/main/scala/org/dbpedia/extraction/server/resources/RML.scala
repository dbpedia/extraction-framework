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
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def addSimplePropertyMapping(input : String) = {
    val response = input
    Response.ok(response, MediaType.APPLICATION_JSON).build()
  }

  @POST
  @Path("templates/geocoordinate")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def addGeocoordinateMapping(input : String) = {
    val json = "{\"response\" : \"test\"}" //convert entity to json
    Response.ok(json, MediaType.APPLICATION_JSON).build()
  }

  @POST
  @Path("templates/startdate")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def addStartDateMapping(input : String) = {
    val json = "{\"response\" : \"test\"}" //convert entity to json
    Response.ok(json, MediaType.APPLICATION_JSON).build()
  }

  @POST
  @Path("templates/enddate")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def addEndDateMapping(input : String) = {
    val json = "{\"response\" : \"test\"}" //convert entity to json
    Response.ok(json, MediaType.APPLICATION_JSON).build()
  }

}
