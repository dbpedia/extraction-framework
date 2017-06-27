package org.dbpedia.extraction.server.resources.rml.mappings

import org.dbpedia.extraction.mappings.GeoCoordinatesMapping
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.server.resources.rml.dbf.DbfFunction
import org.dbpedia.extraction.server.resources.rml.model.RMLModel
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.{RMLLiteral, RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import scala.language.reflectiveCalls

  /**
  * Creates RML Mapping from GeoCoordinatesMapping and adds the triples to the given model
  **/
class GeoCoordinatesRMLMapper(rmlModel: RMLModel, mapping: GeoCoordinatesMapping) {

  //TODO: refactor

  private val rmlFactory = rmlModel.rmlFactory
  private val uri = new RMLUri(rmlModel.wikiTitle.resourceIri + "/GeoCoordinatesMapping")

  def mapToModel() : List[RMLPredicateObjectMap] = {
    addGeoCoordinatesMapping()
  }

  def addGeoCoordinatesMapping() : List[RMLPredicateObjectMap] =
  {
    if(mapping.ontologyProperty != null) {
      val pom = rmlModel.triplesMap.addPredicateObjectMap(uri)
      pom.addDCTermsType(new RMLLiteral("intermediateGeoMapping"))
      val triplesMap = addParentTriplesMapToPredicateObjectMap(pom)
      triplesMap.addLogicalSource(rmlModel.logicalSource)
      val parentSubjectMap = triplesMap.addSubjectMap(triplesMap.uri.extend("/SubjectMap"))
      parentSubjectMap.addClass(new RMLUri(RdfNamespace.GEO.namespace + "SpatialThing"))
      parentSubjectMap.addIRITermType()
      parentSubjectMap.addRMLReference(new RMLLiteral(mapping.ontologyProperty.name))
      addGeoCoordinatesMappingToTriplesMap(triplesMap)
      List(pom)
    } else {
      addGeoCoordinatesMappingToTriplesMap(rmlModel.triplesMap)
    }
  }

  def addIndependentGeoCoordinatesMapping() : List[RMLPredicateObjectMap] =
  {
    if(mapping.ontologyProperty != null) {
      val pom = rmlFactory.createRMLPredicateObjectMap(uri.extend("/" + mapping.ontologyProperty.name))
      val triplesMap = addParentTriplesMapToPredicateObjectMap(pom)
      addGeoCoordinatesMappingToTriplesMap(triplesMap)
      List(pom)
    } else {
      addIndependentGeoCoordinatesMappingToPredicateObjectMap()
    }
  }

  def addGeoCoordinatesMappingToTriplesMap(triplesMap: RMLTriplesMap) : List[RMLPredicateObjectMap]  =
  {

    if(mapping.coordinates != null) {

      addCoordinatesToTriplesMap(triplesMap)

    } else if(mapping.latitude != null && mapping.longitude != null) {

      addLongitudeLatitudeToTriplesMap(triplesMap)

    } else {

      addDegreesToTriplesMap(triplesMap)

    }
  }



  def addCoordinatesToTriplesMap(triplesMap: RMLTriplesMap) : List[RMLPredicateObjectMap] =
  {

    val latPom = triplesMap.addPredicateObjectMap(triplesMap.uri.extend("/latitude"))
    val lonPom = triplesMap.addPredicateObjectMap(triplesMap.uri.extend("/longitude"))

    addCoordinatesToPredicateObjectMap(latPom, lonPom)

    List(latPom, lonPom)

  }

  def addLongitudeLatitudeToTriplesMap(triplesMap: RMLTriplesMap) : List[RMLPredicateObjectMap] =
  {

    val latitudePom = triplesMap.addPredicateObjectMap(triplesMap.uri.extend("/latitude"))
    val longitudePom = triplesMap.addPredicateObjectMap(triplesMap.uri.extend("/longitude"))

    addLongitudeLatitudeToPredicateObjectMap(latitudePom, longitudePom)

    List(latitudePom, longitudePom)

  }


  def addDegreesToTriplesMap(triplesMap: RMLTriplesMap) : List[RMLPredicateObjectMap] =
  {

    val latitudePom = triplesMap.addPredicateObjectMap(triplesMap.uri.extend("/latitude"))
    val longitudePom = triplesMap.addPredicateObjectMap(triplesMap.uri.extend("/longitude"))

    addDegreesToPredicateObjectMap(latitudePom, longitudePom)

    List(latitudePom, longitudePom)

  }

  private def addIndependentGeoCoordinatesMappingToPredicateObjectMap() : List[RMLPredicateObjectMap] =
  {


    if(mapping.coordinates != null) {
      val latPom = rmlFactory.createRMLPredicateObjectMap(uri.extend("/latitude"))
      val lonPom = rmlFactory.createRMLPredicateObjectMap(uri.extend("/longitude"))
      addCoordinatesToPredicateObjectMap(latPom, lonPom)
      List(latPom, lonPom)
    } else if(mapping.latitude != null && mapping.longitude != null) {
      val latPom = rmlFactory.createRMLPredicateObjectMap(uri.extend("/latitude"))
      val lonPom = rmlFactory.createRMLPredicateObjectMap(uri.extend("/longitude"))
      addLongitudeLatitudeToPredicateObjectMap(latPom, lonPom)
      List(latPom, lonPom)
    } else {
      val latPom = rmlFactory.createRMLPredicateObjectMap(uri.extend("/latitude"))
      val lonPom = rmlFactory.createRMLPredicateObjectMap(uri.extend("/longitude"))
      addDegreesToPredicateObjectMap(latPom, lonPom)
      List(latPom, lonPom)
    }
  }


  private def addCoordinatesToPredicateObjectMap(latPom: RMLPredicateObjectMap, lonPom: RMLPredicateObjectMap) =
  {
    latPom.addDCTermsType(new RMLLiteral("latitudeMapping"))
    latPom.addPredicate(new RMLUri(RdfNamespace.GEO.namespace + "lat"))
    val latOmUri = latPom.uri.extend("/FunctionTermMap")
    val latOm = latPom.addFunctionTermMap(latOmUri)
    val latFunctionValueUri = latOmUri
    val latFunctionValue = latOm.addFunctionValue(latFunctionValueUri)
    latFunctionValue.addLogicalSource(rmlModel.logicalSource)
    latFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val latExecutePomUri = latFunctionValueUri.extend("/ExecutePOM")
    val latExecutePom = latFunctionValue.addPredicateObjectMap(latExecutePomUri)
    latExecutePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
    val latExecuteOmUri = latExecutePomUri.extend("/ObjectMap")
    latExecutePom.addObjectMap(latExecuteOmUri).addConstant(new RMLUri(RdfNamespace.DBF.namespace + "latFunction"))

    val latParameterPomUri = latFunctionValueUri.extend("/ParameterPOM")
    val latParameterPom = latFunctionValue.addPredicateObjectMap(latParameterPomUri)
    latParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + "coordParameter"))
    val latParameterOmUri = latParameterPomUri.extend("/ObjectMap")
    latParameterPom.addObjectMap(latParameterOmUri).addRMLReference(new RMLLiteral(mapping.coordinates))

    lonPom.addDCTermsType(new RMLLiteral("longitudeMapping"))
    lonPom.addPredicate(new RMLUri(RdfNamespace.GEO.namespace + "long"))
    val lonOmUri = lonPom.uri.extend("/FunctionTermMap")
    val lonOm = lonPom.addFunctionTermMap(lonOmUri)
    val lonFunctionValueUri = lonOmUri.extend("/FunctionValue")
    val lonFunctionValue = lonOm.addFunctionValue(lonFunctionValueUri)
    lonFunctionValue.addLogicalSource(rmlModel.logicalSource)
    lonFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val lonExecutePomUri = lonFunctionValueUri.extend("/ExecutePOM")
    val lonExecutePom = lonFunctionValue.addPredicateObjectMap(lonExecutePomUri)
    lonExecutePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
    val lonExecuteOmUri = lonExecutePomUri.extend("/ObjectMap")
    lonExecutePom.addObjectMap(lonExecuteOmUri).addConstant(new RMLUri(RdfNamespace.DBF.namespace + "lonFunction"))

    val lonParameterPomUri = lonFunctionValueUri.extend("/ParameterPOM")
    val lonParameterPom = lonFunctionValue.addPredicateObjectMap(lonParameterPomUri)
    lonParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + "coordParameter"))
    val lonParameterOmUri = lonParameterPomUri.extend("/ObjectMap")
    lonParameterPom.addObjectMap(lonParameterOmUri).addRMLReference(new RMLLiteral(mapping.coordinates))

  }

  private def addLongitudeLatitudeToPredicateObjectMap(latPom: RMLPredicateObjectMap, lonPom: RMLPredicateObjectMap) =
  {
    latPom.addDCTermsType(new RMLLiteral("latitudeMapping"))
    latPom.addPredicate(new RMLUri(RdfNamespace.GEO.namespace + "lat"))
    val latOmUri = latPom.uri.extend("/FunctionTermMap")
    val latOm = latPom.addFunctionTermMap(latOmUri)
    val latFunctionValueUri = latOmUri
    val latFunctionValue = latOm.addFunctionValue(latFunctionValueUri)
    latFunctionValue.addLogicalSource(rmlModel.logicalSource)
    latFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val latExecutePomUri = latFunctionValueUri.extend("/ExecutePOM")
    val latExecutePom = latFunctionValue.addPredicateObjectMap(latExecutePomUri)
    latExecutePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
    val latExecuteOmUri = latExecutePomUri.extend("/ObjectMap")
    latExecutePom.addObjectMap(latExecuteOmUri).addConstant(new RMLUri(RdfNamespace.DBF.namespace + "latFunction"))

    val latParameterPomUri = latFunctionValueUri.extend("/ParameterPOM")
    val latParameterPom = latFunctionValue.addPredicateObjectMap(latParameterPomUri)
    latParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + "latParameter"))
    val latParameterOmUri = latParameterPomUri.extend("/ObjectMap")
    latParameterPom.addObjectMap(latParameterOmUri).addRMLReference(new RMLLiteral(mapping.latitude))

    lonPom.addDCTermsType(new RMLLiteral("longitudeMapping"))
    lonPom.addPredicate(new RMLUri(RdfNamespace.GEO.namespace + "long"))
    val lonOmUri = lonPom.uri.extend("/FunctionTermMap")
    val lonOm = lonPom.addFunctionTermMap(lonOmUri)
    val lonFunctionValueUri = lonOmUri.extend("/FunctionValue")
    val lonFunctionValue = lonOm.addFunctionValue(lonFunctionValueUri)
    lonFunctionValue.addLogicalSource(rmlModel.logicalSource)
    lonFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val lonExecutePomUri = lonFunctionValueUri.extend("/ExecutePOM")
    val lonExecutePom = lonFunctionValue.addPredicateObjectMap(lonExecutePomUri)
    lonExecutePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
    val lonExecuteOmUri = lonExecutePomUri.extend("/ObjectMap")
    lonExecutePom.addObjectMap(lonExecuteOmUri).addConstant(new RMLUri(RdfNamespace.DBF.namespace + "lonFunction"))

    val lonParameterPomUri = lonFunctionValueUri.extend("/ParameterPOM")
    val lonParameterPom = lonFunctionValue.addPredicateObjectMap(lonParameterPomUri)
    lonParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + "lonParameter"))
    val lonParameterOmUri = lonParameterPomUri.extend("/ObjectMap")
    lonParameterPom.addObjectMap(lonParameterOmUri).addRMLReference(new RMLLiteral(mapping.longitude))
  }

  private def addDegreesToPredicateObjectMap(latPom: RMLPredicateObjectMap, lonPom: RMLPredicateObjectMap) =
  {
    latPom.addDCTermsType(new RMLLiteral("latitudeMapping"))
    latPom.addPredicate(new RMLUri(RdfNamespace.GEO.namespace + "lat"))

    val latitudeOmUri = latPom.uri.extend("/ObjectMap")
    val latitudeOm = latPom.addFunctionTermMap(latitudeOmUri)

    val latitudeFunctionValueUri = latitudeOmUri.extend("/FunctionValue")
    val latitudeFunctionValue = latitudeOm.addFunctionValue(latitudeFunctionValueUri)

    latitudeFunctionValue.addLogicalSource(rmlModel.logicalSource)
    latitudeFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val latExecutePomUri = latitudeFunctionValueUri.extend("/ExecutePOM")
    val latExecutePom = latitudeFunctionValue.addPredicateObjectMap(latExecutePomUri)
    latExecutePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
    val latExecuteOmUri = latExecutePomUri.extend("/ObjectMap")
    latExecutePom.addObjectMap(latExecuteOmUri).addConstant(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.latFunction.name))

    val latDegreesParameterPomUri = latitudeFunctionValueUri.extend("/LatDegreesParameterPOM")
    val latDegreesParameterPom = latitudeFunctionValue.addPredicateObjectMap(latDegreesParameterPomUri)
    latDegreesParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.latFunction.latDegreesParameter))
    val latDegreesParameterOmUri = latDegreesParameterPomUri.extend("/ObjectMap")
    if(mapping.latitudeDegrees != null) {
      latDegreesParameterPom.addObjectMap(latDegreesParameterOmUri).addRMLReference(new RMLLiteral(mapping.latitudeDegrees))
    } else {
      latDegreesParameterPom.addObjectMap(latDegreesParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }

    val latMinutesParameterPomUri = latitudeFunctionValueUri.extend("/LatMinutesParameterPOM")
    val latMinutesParameterPom = latitudeFunctionValue.addPredicateObjectMap(latMinutesParameterPomUri)
    latMinutesParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.latFunction.latMinutesParameter))
    val latMinutesParameterOmUri = latMinutesParameterPomUri.extend("/ObjectMap")
    if(mapping.latitudeMinutes != null) {
      latMinutesParameterPom.addObjectMap(latMinutesParameterOmUri).addRMLReference(new RMLLiteral(mapping.latitudeMinutes))
    } else {
      latMinutesParameterPom.addObjectMap(latMinutesParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }

    val latSecondsParameterPomUri = latitudeFunctionValueUri.extend("/LatSecondsParameterPOM")
    val latSecondsParameterPom = latitudeFunctionValue.addPredicateObjectMap(latSecondsParameterPomUri)
    latSecondsParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.latFunction.latSecondsParameter))
    val latSecondsParameterOmUri = latSecondsParameterPomUri.extend("/ObjectMap")
    if(mapping.latitudeSeconds != null) {
      latSecondsParameterPom.addObjectMap(latSecondsParameterOmUri).addRMLReference(new RMLLiteral(mapping.latitudeSeconds))
    } else {
      latSecondsParameterPom.addObjectMap(latSecondsParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }

    val latDirectionParameterPomUri = latitudeFunctionValueUri.extend("/latDirectionParameterPOM")
    val latDirectionParameterPom = latitudeFunctionValue.addPredicateObjectMap(latDirectionParameterPomUri)
    latDirectionParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.latFunction.latDirectionParameter))
    val latDirectionParameterOmUri = latDirectionParameterPomUri.extend("/ObjectMap")
    if(mapping.latitudeDirection != null) {
      latDirectionParameterPom.addObjectMap(latDirectionParameterOmUri).addRMLReference(new RMLLiteral(mapping.latitudeDirection))
    } else {
      latDirectionParameterPom.addObjectMap(latDirectionParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }

    lonPom.addDCTermsType(new RMLLiteral("longitudeMapping"))
    lonPom.addPredicate(new RMLUri(RdfNamespace.GEO.namespace + "long"))

    val longitudeOmUri = lonPom.uri.extend("/ObjectMap")
    val longitudeOm = lonPom.addFunctionTermMap(longitudeOmUri)

    val longitudeFunctionValueUri = longitudeOmUri.extend("/FunctionValue")
    val longitudeFunctionValue = longitudeOm.addFunctionValue(longitudeFunctionValueUri)

    longitudeFunctionValue.addLogicalSource(rmlModel.logicalSource)
    longitudeFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val lonExecutePomUri = longitudeFunctionValueUri.extend("/ExecutePOM")
    val lonExecutePom = longitudeFunctionValue.addPredicateObjectMap(lonExecutePomUri)
    lonExecutePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
    val lonExecuteOmUri = lonExecutePomUri.extend("/ObjectMap")
    lonExecutePom.addObjectMap(lonExecuteOmUri).addConstant(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.lonFunction.name))

    val lonDegreesParameterPomUri = longitudeFunctionValueUri.extend("/lonDegreesParameterPOM")
    val lonDegreesParameterPom = longitudeFunctionValue.addPredicateObjectMap(lonDegreesParameterPomUri)
    lonDegreesParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.lonFunction.lonDegreesParameter))
    val lonDegreesParameterOmUri = lonDegreesParameterPomUri.extend("/ObjectMap")

    if(mapping.longitudeDegrees != null) {
      lonDegreesParameterPom.addObjectMap(lonDegreesParameterOmUri).addRMLReference(new RMLLiteral(mapping.longitudeDegrees))
    } else {
      lonDegreesParameterPom.addObjectMap(lonDegreesParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }

    val lonMinutesParameterPomUri = longitudeFunctionValueUri.extend("/lonMinutesParameterPOM")
    val lonMinutesParameterPom = longitudeFunctionValue.addPredicateObjectMap(lonMinutesParameterPomUri)
    lonMinutesParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.lonFunction.lonMinutesParameter))
    val lonMinutesParameterOmUri = lonMinutesParameterPomUri.extend("/ObjectMap")
    if(mapping.longitudeMinutes != null) {
      lonMinutesParameterPom.addObjectMap(lonMinutesParameterOmUri).addRMLReference(new RMLLiteral(mapping.longitudeMinutes))
    } else {
      lonMinutesParameterPom.addObjectMap(lonMinutesParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }

    val lonSecondsParameterPomUri = longitudeFunctionValueUri.extend("/lonSecondsParameterPOM")
    val lonSecondsParameterPom = longitudeFunctionValue.addPredicateObjectMap(lonSecondsParameterPomUri)
    lonSecondsParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.lonFunction.lonSecondsParameter))
    val lonSecondsParameterOmUri = lonSecondsParameterPomUri.extend("/ObjectMap")
    if(mapping.longitudeSeconds != null) {
      lonSecondsParameterPom.addObjectMap(lonSecondsParameterOmUri).addRMLReference(new RMLLiteral(mapping.longitudeSeconds))
    } else {
      lonSecondsParameterPom.addObjectMap(lonSecondsParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }

    val lonDirectionParameterPomUri = longitudeFunctionValueUri.extend("/lonDirectionParameterPOM")
    val lonDirectionParameterPom = longitudeFunctionValue.addPredicateObjectMap(lonDirectionParameterPomUri)
    lonDirectionParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.lonFunction.lonDirectionParameter))
    val lonDirectionParameterOmUri = lonDirectionParameterPomUri.extend("/ObjectMap")
    if(mapping.longitudeDirection != null) {
      lonDirectionParameterPom.addObjectMap(lonDirectionParameterOmUri).addRMLReference(new RMLLiteral(mapping.longitudeDirection))
    } else {
      lonDirectionParameterPom.addObjectMap(lonDirectionParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }


  }

  private def addParentTriplesMapToPredicateObjectMap(pom: RMLPredicateObjectMap) = {

    pom.addPredicate(new RMLUri(mapping.ontologyProperty.uri))
    val objectMapUri = pom.uri.extend("/ObjectMap")
    val objectMap = pom.addObjectMap(objectMapUri)
    val parentTriplesMapUri = objectMapUri.extend("/ParentTriplesMap")
    objectMap.addParentTriplesMap(parentTriplesMapUri)

  }

}
