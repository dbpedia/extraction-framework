package org.dbpedia.extraction.server.resources

import java.io.{PrintWriter, StringWriter}
import javax.ws.rs.core.{MediaType, Response}
import javax.ws.rs.{Produces, _}

import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import org.dbpedia.extraction.mappings.rml.exception.OntologyPropertyException
import org.dbpedia.extraction.mappings.rml.model.RMLEditModel
import org.dbpedia.extraction.mappings.rml.model.assembler.TemplateAssembler
import org.dbpedia.extraction.mappings.rml.model.factory.{JSONBundle, JSONTemplateFactory, RMLEditModelJSONFactory}
import org.dbpedia.extraction.mappings.rml.model.resource.RMLUri
import org.dbpedia.extraction.mappings.rml.model.template.{ConstantTemplate, GeocoordinateTemplate, SimplePropertyTemplate, StartDateTemplate}
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
      val msg = "SimplePropertyMapping succesfully added."
      val response = createResponse(mapping, mappingNode, msg)
      Response.ok(response, MediaType.APPLICATION_JSON).build()

    } catch {
      case e : OntologyPropertyException => createBadRequestExceptionResponse(e)
      case e : BadRequestException => createBadRequestExceptionResponse(e)
      case e : Exception =>
        e.printStackTrace()
        createInternalServerErrorResponse(e)
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
      val msg = "Constant Mapping successfully added."
      val response = createResponse(mapping, mappingNode, msg)
      Response.ok(response, MediaType.APPLICATION_JSON).build()

    } catch {
      case e : OntologyPropertyException => createBadRequestExceptionResponse(e)
      case e : BadRequestException => createBadRequestExceptionResponse(e)
      case e : Exception => {
        e.printStackTrace()
        createInternalServerErrorResponse(e)
      }
    }
  }

  @POST
  @Path("templates/geocoordinate")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def addGeocoordinateMapping(input : String) = {


    try {
      // validate input
      checkGeocoordinateInput(input)

      // create the structures
      val mappingNode = getMappingNode(input)
      val mapping = getMapping(mappingNode)
      val template = getGeocoordinateTemplate(input)

      TemplateAssembler.assembleGeocoordinateTemplate(mapping, template, mapping.language, mapping.count(RMLUri.LATITUDEMAPPING))

      // create the response
      val msg = "Geocoordinate Mapping succesfully added."
      val response = createResponse(mapping, mappingNode, msg)
      Response.ok(response, MediaType.APPLICATION_JSON).build()

    } catch {
      case e : OntologyPropertyException => createBadRequestExceptionResponse(e)
      case e : BadRequestException => createBadRequestExceptionResponse(e)
      case e : Exception => {
        e.printStackTrace()
        createInternalServerErrorResponse(e)
      }
    }


  }

  @POST
  @Path("templates/startdate")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def addStartDateMapping(input : String) = {
    try {

      // validate the input
      checkStartDateInput(input)

      // create the structures
      val mappingNode = getMappingNode(input)
      val mapping = getMapping(mappingNode)
      val template = getStartDateTemplate(input)

      // assemble (side-effects)
      TemplateAssembler.assembleStartDateTemplate(mapping, template, mapping.language, mapping.count(RMLUri.STARTDATEMAPPING))

      // create the response
      val msg = "Start Date Mapping successfully added."
      val response = createResponse(mapping, mappingNode, msg)
      Response.ok(response, MediaType.APPLICATION_JSON).build()

    } catch {
      case e : OntologyPropertyException => createBadRequestExceptionResponse(e)
      case e : BadRequestException => createBadRequestExceptionResponse(e)
      case e : Exception => {
        e.printStackTrace()
        createInternalServerErrorResponse(e)
      }
    }
  }

  @POST
  @Path("templates/enddate")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def addEndDateMapping(input : String) = {
    Response.noContent()
  }

  @POST
  @Path("templates/conditional")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def addConditionalMapping(input : String) = {
    Response.noContent()
  }

  @POST
  @Path("templates/intermediate")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def addIntermediateMapping(input : String) = {
    Response.noContent()
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
    *
    * @param input
    * @return
    */
  private def getParameterNode(input : String) : JsonNode = {
    val templateNode = getTemplateNode(input)
    templateNode.get("parameters")
  }

  /**
    * Creates the RMLEditModel from the input JSON
    *
    * @return
    */
  private def getMapping(mappingNode : JsonNode) : RMLEditModel = {
    val mappingFactory = new RMLEditModelJSONFactory(mappingNode)
    val mapping = mappingFactory.create
    mapping
  }

  /**
    * Creates the SimplePropertyTemplate from the input JSON
    *
    * @param input
    */
  private def getSimplePropertyTemplate(input: String) : SimplePropertyTemplate = {
    val templateNode = getTemplateNode(input)
    val ontology = Server.instance.extractor.ontology()
    val template = JSONTemplateFactory.createSimplePropertyTemplate(JSONBundle(templateNode, ontology))
    template
  }

  /**
    * Creates the ConstantTemplate from the input JSON
    *
    * @param input
    */
  private def getConstantTemplate(input: String) : ConstantTemplate = {
    val templateNode = getTemplateNode(input)
    val ontology = Server.instance.extractor.ontology()
    val template= JSONTemplateFactory.createConstantTemplate(JSONBundle(templateNode, ontology))
    template
  }

  /**
    * Creates the GeocoordinatesTemplate from the input JSON
    *
    * @param input
    */
  private def getGeocoordinateTemplate(input: String) : GeocoordinateTemplate = {
    val templateNode = getTemplateNode(input)
    val ontology = Server.instance.extractor.ontology()
    val template= JSONTemplateFactory.createGeocoordinateTemplate(JSONBundle(templateNode, ontology))
    template
  }

  /**
    * Creates the StartDateTemplate from the input JSON
    *
    * @param input
    */
  private def getStartDateTemplate(input: String) : StartDateTemplate = {
    val templateNode = getTemplateNode(input)
    val ontology = Server.instance.extractor.ontology()
    val template= JSONTemplateFactory.createStartDateTemplate(JSONBundle(templateNode, ontology))
    template
  }

  /**
    * Creates a JSON response
    * Updates the "dump" field
    *
    * @param mapping
    * @return
    */
  private def createResponse(mapping : RMLEditModel, mappingNode: JsonNode, msg : String) : String = {
    val updatedMapping = RMLFormatter.format(mapping, mapping.base)
    mappingNode.asInstanceOf[ObjectNode].put("dump", updatedMapping)
    val responseNode = JsonNodeFactory.instance.objectNode()
    responseNode.set("mapping", mappingNode)
    responseNode.put("msg", msg)
    val response = responseNode.toString
    response
  }

  /**
    * Creates a BAD REQUEST respons based on a given exception
    *
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
    * Creates a INTERNAL SERVER ERROR
    *
    * @param e
    * @return
    */
  private def createInternalServerErrorResponse(e : Exception) : Response = {

    val node = JsonNodeFactory.instance.objectNode()
    node.put("msg", e.getMessage)
    node.put("exception", e.toString)

    val stacktrace = getStacktrace(e)
    node.put("stacktrace", stacktrace)
    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(node.toString).`type`(MediaType.APPLICATION_JSON).build()
  }

  /**
    * Retrieves the stacktrace of an Exception
    * @param e
    * @return
    */
  private def getStacktrace(e : Exception) : String = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    e.printStackTrace(pw)
    sw.toString
  }

  /**
    * Checks for the basic validity of a request
    *
    * @param input
    */
  private def checkBasicRequest(input : String) = {
    val mapper = new ObjectMapper()
    val tree = mapper.readTree(input)
    if(!tree.has("mapping")) throw new BadRequestException("Missing field: $.mapping")
    if(!tree.has("template")) throw new BadRequestException("Missing field: $.template")

    val mappingNode = getMappingNode(input)
    if(!mappingNode.hasNonNull("language")) throw new BadRequestException("Missing field: $.mapping.language")
    if(!mappingNode.hasNonNull("name")) throw new BadRequestException("Missing field: $.mapping.name")
    if(!mappingNode.hasNonNull("dump")) throw new BadRequestException("Missing field: $.mapping.dump")

    val templateNode = getTemplateNode(input)
    if(!templateNode.hasNonNull("name")) throw new BadRequestException("Missing field: $.template.name")
    if(!templateNode.hasNonNull("parameters")) throw new BadRequestException("Missing field: $.template.parameters")
  }

  /**
    * Checks the validity of a simple property request input
    *
    * @param input
    */
  private def checkSimplePropertyInput(input : String) = {

    checkBasicRequest(input)

    val parameterNode = getParameterNode(input)

    if(!parameterNode.has("property")) throw new BadRequestException("Missing field: $.template.parameters.property")
    if(!parameterNode.has("ontologyProperty")) throw new BadRequestException("Missing field: $.template.parameters.ontologyProperty")
    if(!parameterNode.has("select")) throw new BadRequestException("Missing field: $.template.parameters.select")
    if(!parameterNode.has("suffix")) throw new BadRequestException("Missing field: $.template.parameters.suffix")
    if(!parameterNode.has("prefix")) throw new BadRequestException("Missing field: $.template.parameters.prefix")
    if(!parameterNode.has("factor")) throw new BadRequestException("Missing field: $.template.parameters.factor")
    if(!parameterNode.has("unit")) throw new BadRequestException("Missing field: $.template.parameters.unit")
    if(!parameterNode.has("transform")) throw new BadRequestException("Missing field: $.template.parameters.transform")

    if(!parameterNode.hasNonNull("property")) throw new BadRequestException("Empty field: $.template.parameters.property")
    if(!parameterNode.hasNonNull("ontologyProperty")) throw new BadRequestException("Empty field: $.template.parameters.ontologyProperty")

  }

  /**
    * Checks the validity of a constant request input
    *
    * @param input
    */
  private def checkConstantInput(input : String) = {

    checkBasicRequest(input)

    val parameterNode = getParameterNode(input)

    // check if all necessary fields are there (can be null)
    if(!parameterNode.has("ontologyProperty")) throw new BadRequestException("Missing field: $.template.parameters.ontologyProperty")
    if(!parameterNode.has("value")) throw new BadRequestException("Missing field: $.template.parameters.value")
    if(!parameterNode.has("unit")) throw new BadRequestException("Missing field: $.template.parameters.unit")

    if(!parameterNode.hasNonNull("ontologyProperty")) throw new BadRequestException("Empty field: $.template.parameters.ontologyProperty")
    if(!parameterNode.hasNonNull("value")) throw new BadRequestException("Empty field: $.template.parameters.value")

  }

  private def checkGeocoordinateInput(input : String) = {

    checkBasicRequest(input)

    val parameterNode = getParameterNode(input)

    // check if all necessary fields are there (can be null)
    if(!parameterNode.has("coordinate")) throw new BadRequestException("Missing field: $.template.parameters.coordinate")
    if(!parameterNode.has("latitude")) throw new BadRequestException("Missing field: $.template.parameters.latitude")
    if(!parameterNode.has("longitude")) throw new BadRequestException("Missing field: $.template.parameters.longitude")
    if(!parameterNode.has("latitudeDegrees")) throw new BadRequestException("Missing field: $.template.parameters.latitudeDegrees")
    if(!parameterNode.has("latitudeMinutes")) throw new BadRequestException("Missing field: $.template.parameters.latitudeMinutes")
    if(!parameterNode.has("latitudeSeconds")) throw new BadRequestException("Missing field: $.template.parameters.latitudeSeconds")
    if(!parameterNode.has("latitudeDirection")) throw new BadRequestException("Missing field: $.template.parameters.latitudeDirection")
    if(!parameterNode.has("longitudeDegrees")) throw new BadRequestException("Missing field: $.template.parameters.longitudeDegrees")
    if(!parameterNode.has("longitudeMinutes")) throw new BadRequestException("Missing field: $.template.parameters.longitudeMinutes")
    if(!parameterNode.has("longitudeSeconds")) throw new BadRequestException("Missing field: $.template.parameters.longitudeSeconds")
    if(!parameterNode.has("longitudeDirection")) throw new BadRequestException("Missing field: $.template.parameters.longitudeDirection")

    if(
      !( // if all below does not hold, throw exception
      // coordinate is set and the rest is not
      (parameterNode.hasNonNull("coordinate") &&
        !parameterNode.hasNonNull("latitude") && !parameterNode.hasNonNull("longitude") && !parameterNode.hasNonNull("latitudeDegrees") &&
        !parameterNode.hasNonNull("latitudeMinutes") && !parameterNode.hasNonNull("latitudeSeconds") &&
        !parameterNode.hasNonNull("latitudeDirection") &&
        !parameterNode.hasNonNull("longitudeDegrees") && !parameterNode.hasNonNull("longitudeMinutes") &&
        !parameterNode.hasNonNull("longitudeSeconds") &&
        !parameterNode.hasNonNull("longitudeDirection"))

        ||

        // latitude and longitude is set and the rest is not
        (parameterNode.hasNonNull("latitude") && parameterNode.hasNonNull("longitude") &&
        !parameterNode.hasNonNull("coordinate") &&
        !parameterNode.hasNonNull("latitudeDegrees") && !parameterNode.hasNonNull("latitudeMinutes") &&
        !parameterNode.hasNonNull("latitudeSeconds") && !parameterNode.hasNonNull("latitudeDirection") &&
        !parameterNode.hasNonNull("longitudeDegrees") && !parameterNode.hasNonNull("longitudeMinutes") &&
        !parameterNode.hasNonNull("longitudeSeconds") &&
        !parameterNode.hasNonNull("longitudeDirection"))

        ||

        // all the degree parameters are set
        (!parameterNode.hasNonNull("latitude") && !parameterNode.hasNonNull("longitude") &&
          !parameterNode.hasNonNull("coordinate") &&
          parameterNode.hasNonNull("latitudeDegrees") && parameterNode.hasNonNull("latitudeMinutes") &&
          parameterNode.hasNonNull("latitudeSeconds") && parameterNode.hasNonNull("latitudeDirection") &&
          parameterNode.hasNonNull("longitudeDegrees") && parameterNode.hasNonNull("longitudeMinutes") &&
          parameterNode.hasNonNull("longitudeSeconds") &&
          parameterNode.hasNonNull("longitudeDirection")))
      )
      {
        throw new BadRequestException("Bad combination of fields.")
      }

  }

  private def checkStartDateInput(input : String)  = {

    checkBasicRequest(input)

    val parameterNode = getParameterNode(input)

    // check if all necessary fields are there (can be null)
    if(!parameterNode.has("ontologyProperty")) throw new BadRequestException("Missing field: $.template.parameters.ontologyProperty")
    if(!parameterNode.has("property")) throw new BadRequestException("Missing field: $.template.parameters.property")

    if(!parameterNode.hasNonNull("ontologyProperty")) throw new BadRequestException("Empty field: $.template.parameters.ontologyProperty")
    if(!parameterNode.hasNonNull("property")) throw new BadRequestException("Empty field: $.template.parameters.property")
  }



}
