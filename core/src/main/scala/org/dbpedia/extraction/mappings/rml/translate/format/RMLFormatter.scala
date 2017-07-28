package org.dbpedia.extraction.mappings.rml.translate.format
import java.net.URLDecoder
import org.apache.jena.rdf.model.{Resource, Statement, StmtIterator}
import collection.JavaConverters._
import org.dbpedia.extraction.mappings.rml.model.{ModelWrapper, RMLModel}
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Created by wmaroy on 03.07.17.
  * //TODO: clean up this code, this was scripted
  */
object RMLFormatter extends Formatter {

  private val offset = "\n\n"

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Public methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  override def format(model: RMLModel, base : String): String = {

    try {
      val prefixes = getPrefixes(model.writeAsTurtle(base))
      val triplesMapPart = getTriplesMapPart(model, base)
      val subjectMapPart = getSubjectMapPart(model, base)
      val mappingsPart = getAllMappings(model, base)
      val conditionalsPart = getAllConditionalMappings(model, base)
      val functionsPart = getFunctions(model, base)
      val logicalSourcePart = getLogicalSource(model, base)
      val subjectMapFunctionPart = if(!functionsPart.equals("")) getSubjectMapFunction(model, base) else ""
      val formatted = Seq(prefixes,
                          triplesMapPart,
                          subjectMapPart,
                          logicalSourcePart,
                          mappingsPart,
                          conditionalsPart,
                          functionsPart,
                          subjectMapFunctionPart)
                          .reduce((first, second) => first.concat('\n' + second))
      postProcess(formatted)
    } catch {
      case x : Exception => x.printStackTrace(); ""
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Private methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
    * Post process formatting for a complete preformatted mapping string
    * @param mapping
    * @return
    */
  private def postProcess(mapping: String) : String = {
    mapping.split("\n")
           .map(line => line.replaceAll("/FunctionTermMap", "/FTM")
                            .replaceAll("/FunctionValue", "/FV"))
          .reduce((first, second) => first.concat("\n" + second))
  }


  /**
    * Gets the main triples map of a mapping
    *
    * @param model
    * @param base
    * @return
    */
  private def getTriplesMapPart(model : RMLModel, base : String) : String = {

    val freshModel = new ModelWrapper
    val triplesMapResource = model.triplesMap.resource
    freshModel.insertRDFNamespacePrefixes()
    freshModel.model.add(triplesMapResource.listProperties())
    val turtle = freshModel.writeAsTurtle(base)
    val decoded = URLDecoder.decode(turtle, "UTF-8") // Jena uses URL encoding, no IRI encoding, for now this is the solution //TODO
    val formatted = decoded.replaceAll(",", ",\n\t\t\t      ")
    val heading = hashtags(3) + " Main TriplesMap\n" + hashtags(20)

    heading + removePrefixes(formatted) + offset

  }

  /**
    * Gets the subjectMap of a mapping
    *
    * @param model
    * @param base
    * @return
    */
  private def getSubjectMapPart(model : RMLModel, base : String) : String = {

    val freshModel = new ModelWrapper
    val subjectMapResource = model.subjectMap.resource
    freshModel.insertRDFNamespacePrefixes()
    freshModel.model.add(subjectMapResource.listProperties())
    val turtle = freshModel.writeAsTurtle(base)
    val formatted = turtle.replaceAll(",", ",\n\t\t    ")
    val heading = hashtags(3) + " Main SubjectMap\n" + hashtags(20)

    heading + removePrefixes(formatted) + offset
  }

  /**
    * Gets all the property mappings from a mapping
    *
    * @param model
    * @param base
    * @return
    */
  private def getAllMappings(model: RMLModel, base : String) : String = {

    // Get the normal (non-conditional) mappings first
    val triplesMapResource = model.triplesMap.resource
    val statements = triplesMapResource.listProperties(model.model.createProperty(RdfNamespace.RR.namespace + "predicateObjectMap")).toList

    val mappings = statements.asScala.map(statement => {

      val predicateObjectMap = statement.getObject
      if(!hasConditions(predicateObjectMap.asResource())) {
        val properties = predicateObjectMap.asResource().listProperties()
        getMapping(properties, base)
      } else {
        "" // skip conditionals here
      }

    })

    val mappingsString = if(mappings.nonEmpty) {
      mappings.reduce((first, second) => first.concat("\n" + second)) // so return all concatenated mappings
    } else ""

    val heading = hashtags(11)+ "\n# Mappings\n" + hashtags(11) + offset

    heading + mappingsString
  }

  private def getAllConditionalMappings(model: RMLModel, base : String) : String = {

    val triplesMapResource = model.triplesMap.resource
    val statements = triplesMapResource.listProperties(model.model.createProperty(RdfNamespace.RR.namespace + "predicateObjectMap")).toList
    val heading = hashtags(22) + "\n# Conditional Mappings\n" + hashtags(22) + "\n"


    val conditionals = statements.asScala.map(statement => {

      val predicateObjectMap = statement.getObject
      if(hasConditions(predicateObjectMap.asResource())) {
        val tuple = getConditionalMapping(predicateObjectMap.asResource(), base)
        tuple._1.concat(tuple._2)
      } else {
        "" // here
      }

    }).filter(conditionString => !conditionString.equals(""))

    val conditionalString = if(conditionals.nonEmpty) {
      conditionals.reduce((first, second) => first.concat("\n" + second))
    } else ""

    URLDecoder.decode(heading + offset + conditionalString, "UTF-8") // Jena uses URL encoding, no IRI encoding, for now this is the solution //TODO

  }

  /**
    * Retrieve all necessary constructs of a single mapping (rr:PredicateObjectMap)
    * This functions checks the properties of a rr:PredicateObjectMap
    *
    * @param properties
    * @param base
    * @return
    */
  private def getMapping(properties : StmtIterator, base : String) : String = {
    val propertiesArray: Array[Statement] = properties.toList.asScala.toArray
    val freshModel = new ModelWrapper
    freshModel.insertRDFNamespacePrefixes()
    freshModel.model.add(propertiesArray)

    val heading = hashtags(3) + " Predicate Object Map\n" + hashtags(25)
    val predicateObjectMapString = removePrefixes(freshModel.writeAsTurtle(base : String))

    /** The case if a function is used:
      * Checks if the rr:PredicateObjectMap contains a FunctionTermMap
      */
    val functionTermMapString = if(hasFunctionTermMap(propertiesArray.head.getSubject)) {
      val functionTermMap = getObjectMap(propertiesArray.head.getSubject)
      getFunctionTermMap(functionTermMap.listProperties(), base)
    } else ""

    /**
      * The case if the rr:PredicateObjectMap contains a rr:object --> this is a constant mapping, already covered
      * by adding all properties to the new model ()
      */

    heading + predicateObjectMapString + offset + functionTermMapString
  }

  /**
    * Retrieves all necessary constructs for a conditional mapping
    *
    * Returns a tuple: ._1 = PredicateObjectMaps itself, ._2 = FunctionTermMaps from the conditional function
    *
    * @param resource
    * @param base
    * @return
    */
  private def getConditionalMapping(resource : Resource, base : String) : (String, String) = {
    val heading = hashtags(3) + " Conditional PredicateObjectMap" + "\n" + hashtags(35)
    if(hasConditions(resource)) {
      val current_condition = getCondition(resource, base)
      if(hasFallback(resource)) {
        val fallbackMap = getFallbackMap(resource)
        val other_conditions = getConditionalMapping(fallbackMap, base)
        (heading + current_condition._1 + offset + current_condition._2 , other_conditions._1  + other_conditions._2)
      } else {
        (heading + getResourceString(resource,base) + offset, "")
      }
    } else {
      (heading + getResourceString(resource,base) + offset, "")
    }
  }

  /**
    * Returns crml:equalCondition and all other necessary constructs
    *
    * @param resource
    * @param base
    * @return
    */
  private def getCondition(resource : Resource, base : String) : (String, String) = {
    val property = resource.getModel.getProperty(RdfNamespace.CRML.namespace + "equalCondition")
    val functionPOM = resource.getPropertyResourceValue(property).asResource()
    val functionPOMString = getResourceString(functionPOM, base)
    val heading = "### Function Term Map"


    val propertyFunctionValue = resource.getModel.getProperty(RdfNamespace.FNML.namespace + "functionValue")
    val functionValue = functionPOM.getPropertyResourceValue(propertyFunctionValue)
    val functionValueString = getFunctionValue(functionValue.listProperties(), base)

    val conditionPOM = getResourceString(resource, base)
    (conditionPOM, heading + functionPOMString.concat(offset + functionValueString))
  }

  /**
    * Returns a formatted logical source
    *
    * @param model
    * @param base
    * @return
    */
  private def getLogicalSource(model: RMLModel, base : String) : String = {
    val logicalSource = model.logicalSource.resource
    val logicalSourceString = getResourceString(logicalSource, base)
    val heading = "### LogicalSource\n" + hashtags(18) + "\n"

    heading + logicalSourceString + offset
  }

  /**
    * Returns a string with all the properties of a resource in Turtle, prefixed, without prefixes
    *
    * @param resource
    * @param base
    * @return
    */
  private def getResourceString(resource : Resource, base : String) : String = {
    val statements = resource.listProperties().toList.asScala.toArray
    val modelWrapper = createModelWrapper(statements)
    val turtle = modelWrapper.writeAsTurtle(base)
    removePrefixes(turtle)
  }

  /**
    * Retrieves a crml:FallbackMap
    *
    * @param resource
    * @return
    */
  private def getFallbackMap(resource: Resource) : Resource = {
    val property = resource.getModel.getProperty(RdfNamespace.CRML.namespace + "fallbackMap")
    resource.getPropertyResourceValue(property)
  }



  /**
    * Retrieves all function rr:PredicateObjectMaps
    *
    * @return
    */
  private def getFunctions(modelWrapper : ModelWrapper, base : String): String = {
    val predicate =  modelWrapper.model.getProperty(RdfNamespace.RR.namespace, "predicate")
    val _object = modelWrapper.model.getResource(RdfNamespace.FNO.namespace + "executes")
    val statement = modelWrapper.model.listResourcesWithProperty(predicate, _object)
      .toList.asScala
    val properties = modelWrapper.model.listStatements(null,null, _object)
      .toList.asScala
      .flatMap(statement => statement.getSubject.listProperties().toList.asScala)

    if(properties.nonEmpty) {
      val freshWrapper = new ModelWrapper
      freshWrapper.insertRDFNamespacePrefixes()
      freshWrapper.model.add(properties.toArray)

      val heading = hashtags(12) + "\n# Functions\n" + hashtags(12) + offset
      val functionsString = removePrefixes(freshWrapper.writeAsTurtle(base))

      heading + functionsString + offset
    } else ""
  }

  /**
    * Retrieves all necessary constructs of a function term map
    *
    * @param properties
    * @param base
    * @return
    */
  private def getFunctionTermMap(properties : StmtIterator, base : String) : String = {
    val propertiesArray: Array[Statement] = properties.toList.asScala.toArray
    val freshModel = new ModelWrapper
    freshModel.insertRDFNamespacePrefixes()
    freshModel.model.add(propertiesArray)

    val heading = hashtags(3) + " Function Term Map"
    val functionTermMapString = removePrefixes(freshModel.writeAsTurtle(base : String))

    val functionValueProperty = freshModel.model.getProperty(RdfNamespace.FNML.namespace + "functionValue")
    val functionValue = propertiesArray.head.getSubject.getProperty(functionValueProperty).getObject.asResource()
    val functionValueString = getFunctionValue(functionValue.listProperties(), base)

    heading + functionTermMapString + offset + functionValueString

  }

  /**
    * Retrieves all necessary constructs for a function value
    *
    * @param properties
    * @param base
    * @return
    */
  private def getFunctionValue(properties : StmtIterator, base : String) : String = {
    val propertiesArray: Array[Statement] = properties.toList.asScala.toArray
    val freshModel = new ModelWrapper
    freshModel.insertRDFNamespacePrefixes()
    freshModel.model.add(propertiesArray)

    val heading = hashtags(3) + " Function Execution Mapping"
    val functionValueString = removePrefixes(freshModel.writeAsTurtle(base : String).replaceAll(",", ",\n\t\t\t      "))

    val predicateObjectMapProperty = freshModel.model.getProperty(RdfNamespace.RR.namespace + "predicateObjectMap")
    val parameters = propertiesArray.head.getSubject.listProperties(predicateObjectMapProperty).toList.asScala.toArray

    val parameterString = if(hasParameters(propertiesArray.head.getSubject)) {
      getParameters(parameters, base)
    } else ""


    heading + functionValueString + offset + parameterString

  }

  /**
    * Retrieves all necessary constructs for parameters of a function value
    *
    * @param parameterStatements
    * @return
    */
  private def getParameters(parameterStatements : Array[Statement], base : String) : String = {
    val paramStmtnsWithoutFunctions = parameterStatements
                                        .filter(statement => !hasFunction(statement.getObject.asResource()))

    val properties = paramStmtnsWithoutFunctions
                      .flatMap(statement => statement.getObject.asResource().listProperties().toList.asScala)

    val freshModel = new ModelWrapper
    freshModel.insertRDFNamespacePrefixes()
    freshModel.model.add(properties)

    val heading = hashtags(3) + " Parameters"
    val parameterString = removePrefixes(freshModel.writeAsTurtle(base))

    val referencesString = getReferences(paramStmtnsWithoutFunctions, base)


    heading + parameterString + offset + referencesString

  }

  /**
    * Retrieve referencing Object Maps from a list of statements
    *
    * @param statements
    * @param base
    * @return
    */
  private def getReferences(statements : Array[Statement], base : String) : String = {

    val properties = statements.filter(statement => hasObjectMap(statement.getObject.asResource()))
      .flatMap(statement => getObjectMap(statement.getObject.asResource()).listProperties().toList.asScala)

    val freshModel = new ModelWrapper
    freshModel.insertRDFNamespacePrefixes()
    freshModel.model.add(properties)

    val heading = hashtags(3) + " References"
    val referencesString = removePrefixes(freshModel.writeAsTurtle(base))

    heading + referencesString + offset


  }

  /**
    * Checks if a rr:PredicateObjectMap has parameters
    *
    * @param resource
    * @return
    */
  private def hasParameters(resource : Resource) = {
    val property = resource.getModel.getProperty(RdfNamespace.RR.namespace + "predicateObjectMap")
    resource.listProperties(property).toList.asScala.size > 1
  }

  /**
    * Checks if given resource has a rr:predicate fno:exucutes
    *
    * @param resource
    * @return
    */
  private def hasFunction(resource : Resource) : Boolean = {
    resource.listProperties().toList.asScala.exists(statement => {
      statement.getObject.toString.equals(RdfNamespace.FNO.namespace + "executes")
    })
  }

  /**
    * Checks if given resource is rr:predicate rr:objectMap
    *
    * @param resource
    * @return
    */
  private def hasObjectMap(resource : Resource) : Boolean = {
    resource.listProperties().toList.asScala.exists(statement => {
      statement.getPredicate.getURI.equals(RdfNamespace.RR.namespace + "objectMap")
    })
  }

  /**
    * Checks if given resource contains an rr:ObjectMap that is a rr:FunctionTermMap
    *
    * @param resource
    * @return
    */
  private def hasFunctionTermMap(resource : Resource) : Boolean = {
    if(hasObjectMap(resource)) {
      getObjectMap(resource).listProperties().toList.asScala.exists(statement => {
        statement.getObject.toString.equals(RdfNamespace.FNML.namespace + "FunctionTermMap")
      })
    } else false
  }

  /**
    * Checks if given resource contains a crml:fallbackMap
    *
    * @param resource
    * @return
    */
  private def hasFallback(resource: Resource) : Boolean = {
    val property = resource.getModel.createProperty(RdfNamespace.CRML.namespace + "fallbackMap")
    resource.hasProperty(property)
  }

  /**
    * Retrievs the object map resource of a resource
    *
    * @param resource
    * @return
    */
  private def getObjectMap(resource: Resource) : Resource = {
    val objectMapProperty = resource.getModel.getProperty(RdfNamespace.RR.namespace + "objectMap")
    resource.getProperty(objectMapProperty).getObject.asResource()
  }

  /**
    * Checks if given resource contains crml:equalCondition
    *
    * @param resource
    * @return
    */
  private def hasConditions(resource : Resource) : Boolean = {
    val property = resource.getModel.createProperty(RdfNamespace.CRML.namespace + "equalCondition")
    resource.hasProperty(property)
  }



  /**
    * Gets the prefixes from a turtle string
    *
    * @param turtle
    */
  private def getPrefixes(turtle : String) = {
    val result = turtle.split("\n")
                       .filter(line => line.contains("@"))
                       .map(line => {
                         val replace = if(line.indexOf(":") < 14) "\t\t<" else "\t<"
                         line.replaceFirst("<", replace).replace("@base", "@base\t")
                       })
                       .reduce((first, second) => first.concat("\n" + second))
    result + offset
  }

  /**
    * Removes prefixes from a turtle string
    *
    * @param turtle
    * @return
    */
  private def removePrefixes(turtle : String) = {

    val result = turtle.split("\n")
        .filter(line => !line.contains("@prefix"))
        .filter(line => !line.contains("@base"))
        .reduce((first, second) => first.concat("\n" + second))

    result

  }

  /**
    * Creates a new Model Wrapper with prefixes and statements
    *
    * @param statements
    * @return
    */
  private def createModelWrapper(statements : Array[Statement]) = {
    val freshModel = new ModelWrapper
    freshModel.insertRDFNamespacePrefixes()
    freshModel.model.add(statements)
    freshModel
  }

  /**
    * Returns a string of hashtags
    *
    * @param amount
    * @return
    */
  private def hashtags(amount : Integer) : String = {
    var _hashtags = ""
    for(x <- 1 to amount) {
      _hashtags = _hashtags + "#"
    }
    _hashtags
  }

  private def getSubjectMapFunction(rmlModel: RMLModel, base : String) : String = {
    val resource = rmlModel.functionSubjectMap.resource
    val subjectMapFunctionString = getResourceString(resource, base)
    val heading = "### Functions SubjectMap\n" + hashtags(25) + "\n"

    heading + subjectMapFunctionString + offset
  }
}
