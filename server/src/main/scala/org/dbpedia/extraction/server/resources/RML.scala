package org.dbpedia.extraction.server.resources

import java.io.InputStream
import javax.ws.rs.core.{MediaType, Response}
import javax.ws.rs.{Produces, _}

import com.fasterxml.jackson.databind
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import dbpedia.dataparsers.ontology.OntologySingleton
import org.dbpedia.extraction.mappings.rml.model.{RMLEditModel, RMLTemplateMapping}
import org.dbpedia.extraction.mappings.rml.model.assembler.TemplateAssembler
import org.dbpedia.extraction.mappings.rml.model.factory.{RMLEditModelJSONFactory, SimplePropertyTemplateJSONFactory}
import org.dbpedia.extraction.mappings.rml.model.template.SimplePropertyTemplate
import org.dbpedia.extraction.mappings.rml.translate.format.RMLFormatter
import org.dbpedia.extraction.server.Server

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
  @Path("templates")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def getTemplates(input : String) = {

  }

  /**
    * Takes JSON body input which contains a SimplePropertyTemplate and an RML Mapping dump.
    * This function adds the template to the mapping and returns these if successful.
    *
    * @param input
    * @return
    */
  @POST
  @Path("templates/simpleproperty")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def addSimplePropertyMapping(input : String) = {
    try {

      val mappingNode = getMappingNode(input)
      val mapping = getMapping(mappingNode)
      val template = getSimplePropertyTemplate(input)

      // assemble (side-effects)
      TemplateAssembler.assembleSimplePropertyTemplate(mapping, template, mapping.language, mapping.countSimpleProperties)

      val response = createResponse(mapping, mappingNode)
      Response.ok(response, MediaType.APPLICATION_JSON).build()
    } catch {
      case e : Exception => {
        e.printStackTrace()
        Response.serverError().build()
      }
    }
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

  /**
    * Retrieves the mapping dump from the input JSON string from a POST request
    *
    * @param input
    * @return
    */
  private def getMappingDump(input : String) : String = {
    val mapper = new ObjectMapper()
    val tree = mapper.readTree(input)
    val dump = tree.get("mapping").get("dump").asText()
    dump
  }

  /**
    * Retrieves the mapping JSON node from a POST request
    *
    * @param input
    * @return
    */
  private def getMappingNode(input : String) : JsonNode = {
    val mapper = new ObjectMapper()
    val tree = mapper.readTree(input)
    val mapping = tree.get("mapping")
    mapping
  }

  /**
    * Retrieves the template JSON node from a POST request
    *
    * @param input
    * @return
    */
  private def getTemplateNode(input : String) : JsonNode = {
    val mapper = new ObjectMapper()
    val tree = mapper.readTree(input)
    val templateNode = tree.get("template")
    templateNode
  }

  /**
    * Creates the RMLEditModel from the input JSON
    * @return
    */
  private def getMapping(mappingNode : JsonNode) : RMLEditModel = {
    val mappingFactory = new RMLEditModelJSONFactory(mappingNode)
    val mapping = mappingFactory.create
    mapping
  }

  /**
    * Creates the SimplePropertyTemplate from the input JSON
    * @param input
    */
  private def getSimplePropertyTemplate(input: String) : SimplePropertyTemplate = {
    val templateNode = getTemplateNode(input)
    val ontology = Server.instance.extractor.ontology()
    val templateFactory = new SimplePropertyTemplateJSONFactory(templateNode, ontology)
    val template = templateFactory.createTemplate
    template
  }

  /**
    * Creates a JSON response
    * Updates the "dump" field
    * @param mapping
    * @return
    */
  private def createResponse(mapping : RMLEditModel, mappingNode: JsonNode) : String = {
    val updatedMapping = RMLFormatter.format(mapping, mapping.base)
    val responseNode = mappingNode.asInstanceOf[ObjectNode].put("dump", updatedMapping)
    val response = responseNode.toString
    response
  }

}
