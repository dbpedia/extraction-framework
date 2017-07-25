package org.dbpedia.extraction.server.resources

import java.io.InputStream
import javax.ws.rs.core.{MediaType, Response}
import javax.ws.rs.{Produces, _}

import com.fasterxml.jackson.databind
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import dbpedia.dataparsers.ontology.OntologySingleton
import org.dbpedia.extraction.mappings.rml.exception.OntologyPropertyException
import org.dbpedia.extraction.mappings.rml.model.{RMLEditModel, RMLTemplateMapping}
import org.dbpedia.extraction.mappings.rml.model.assembler.TemplateAssembler
import org.dbpedia.extraction.mappings.rml.model.factory.{ConstantTemplateJSONFactory, RMLEditModelJSONFactory, SimplePropertyTemplateJSONFactory}
import org.dbpedia.extraction.mappings.rml.model.resource.RMLUri
import org.dbpedia.extraction.mappings.rml.model.template.{ConstantTemplate, SimplePropertyTemplate}
import org.dbpedia.extraction.mappings.rml.translate.format.RMLFormatter
import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.server.resources.rml.BadRequestException

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

      // validate the input
      checkSimplePropertyInput(input)

      // create the structures
      val mappingNode = getMappingNode(input)
      val mapping = getMapping(mappingNode)
      val template = getSimplePropertyTemplate(input)

      // assemble (side-effects)
      TemplateAssembler.assembleSimplePropertyTemplate(mapping, template, mapping.language, mapping.count(RMLUri.SIMPLEPROPERTYMAPPING))

      // create the response
      val response = createResponse(mapping, mappingNode)
      Response.ok(response, MediaType.APPLICATION_JSON).build()

    } catch {
      case e : OntologyPropertyException => createBadRequestExceptionResponse(e)
      case e : BadRequestException => createBadRequestExceptionResponse(e)
      case e : Exception =>
        e.printStackTrace()
        Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
    }
  }

  /**
    * Takes JSON body input which contains a SimplePropertyTemplate and an RML Mapping dump.
    * This function adds the template to the mapping and returns these if successful.
    *
    * @param input
    * @return
    */
  @POST
  @Path("templates/constant")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def addConstantMapping(input : String) = {
    try {

      // validate the input
      checkConstantInput(input)

      // create the structures
      val mappingNode = getMappingNode(input)
      val mapping = getMapping(mappingNode)
      val template = getConstantTemplate(input)

      // assemble (side-effects)
      TemplateAssembler.assembleConstantTemplate(mapping, template, mapping.language, mapping.count(RMLUri.CONSTANTMAPPING))

      // create the response
      val response = createResponse(mapping, mappingNode)
      Response.ok(response, MediaType.APPLICATION_JSON).build()

    } catch {
      case e : OntologyPropertyException => createBadRequestExceptionResponse(e)
      case e : BadRequestException => createBadRequestExceptionResponse(e)
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
    * Retrieves the parameters JSON node from a POST request
    * @param input
    * @return
    */
  private def getParameterNode(input : String) : JsonNode = {
    val templateNode = getTemplateNode(input)
    templateNode.get("parameters")
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
    * Creates the ConstantTemplate from the input JSON
    * @param input
    */
  private def getConstantTemplate(input: String) : ConstantTemplate = {
    val templateNode = getTemplateNode(input)
    val ontology = Server.instance.extractor.ontology()
    val templateFactory = new ConstantTemplateJSONFactory(templateNode, ontology)
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

  /**
    * Creates a BAD REQUEST respons based on a given exception
    * @param e
    * @return
    */
  private def createBadRequestExceptionResponse(e : Exception) : Response = {
    e.printStackTrace()
    val node = JsonNodeFactory.instance.objectNode()
    node.put("msg", e.getMessage)
    Response.status(Response.Status.BAD_REQUEST).entity(node.toString).`type`(MediaType.APPLICATION_JSON).build()
  }

  /**
    * Checks for the basic validity of a request
    * @param input
    */
  private def checkBasicRequest(input : String) = {
    val mapper = new ObjectMapper()
    val tree = mapper.readTree(input)
    if(!tree.has("mapping")) throw new BadRequestException("Missing field: $.mapping")
    if(!tree.has("template")) throw new BadRequestException("Missing field: $.template")

    val mappingNode = getMappingNode(input)
    if(!mappingNode.has("language")) throw new BadRequestException("Missing field: $.mapping.language")
    if(!mappingNode.has("name")) throw new BadRequestException("Missing field: $.mapping.name")
    if(!mappingNode.has("dump")) throw new BadRequestException("Missing field: $.mapping.dump")

    val templateNode = getTemplateNode(input)
    if(!templateNode.has("name")) throw new BadRequestException("Missing field: $.template.name")
    if(!templateNode.has("parameters")) throw new BadRequestException("Missing field: $.template.parameters")
  }

  /**
    * Checks the validity of a simple property request input
    * @param input
    */
  private def checkSimplePropertyInput(input : String) = {

    checkBasicRequest(input)

    val parameterNode = getParameterNode(input)
    if(!parameterNode.has("property")) throw new BadRequestException("Missing field: $.template.parameters.property")
    if(!parameterNode.has("ontologyProperty")) throw new BadRequestException("Missing field: $.template.parameters.ontologyProperty")

  }

  /**
    * Checks the validity of a constant request input
    * @param input
    */
  private def checkConstantInput(input : String) = {

    checkBasicRequest(input)

    val parameterNode = getParameterNode(input)
    if(!parameterNode.has("ontologyProperty")) throw new BadRequestException("Missing field: $.template.parameters.ontologyProperty")
    if(!parameterNode.has("value")) throw new BadRequestException("Missing field: $.template.parameters.value")


  }

}
