package org.dbpedia.extraction.server.resources.rml.mappings

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.GeoCoordinatesMapping
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.server.resources.rml.model.RMLModel
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.{RMLLiteral, RMLTriplesMap, RMLUri}

  /**
  * Creates RML Mapping from GeoCoordinatesMapping and adds the triples to the given model
  **/
class GeoCoordinatesRMLMapper(rmlModel: RMLModel, mapping: GeoCoordinatesMapping) {

  //TODO: refactor

  def mapToModel() = {
    addGeoCoordinatesMapping()
  }

  def addGeoCoordinatesMapping() =
  {
    val uri = rmlModel.wikiTitle.resourceIri + "/GeoCoordinatesMapping"
    if(mapping.ontologyProperty != null) {
      val triplesMap = addParentTriplesMapToTriplesMap(uri, rmlModel.triplesMap)
      addGeoCoordinatesMappingToTriplesMap(uri, triplesMap)
    } else {
      addGeoCoordinatesMappingToTriplesMap(uri, rmlModel.triplesMap)
    }

  }

  def addGeoCoordinatesMappingToTriplesMap(uri: String, triplesMap: RMLTriplesMap) =
  {

    if(mapping.coordinates != null) {

      addCoordinatesToTriplesMap(uri + "/" + mapping.coordinates, triplesMap)

    } else if(mapping.latitude != null && mapping.longitude != null) {

      addLongituteLatitudeToTriplesMap(uri + "/" + mapping.latitude + "/" + mapping.latitude, triplesMap)

    } else {

      addDegreesToTriplesMap(uri + "/Degrees", triplesMap)

    }
  }

  def addCoordinatesToTriplesMap(uri: String, triplesMap: RMLTriplesMap) =
  {
    val rmlUri = new RMLUri(uri)
    val latPomUri = rmlUri.extend("/LatitudePom")
    val latPom = triplesMap.addPredicateObjectMap(latPomUri)
    latPom.addPredicate(new RMLUri(RdfNamespace.GEO.namespace + "lat"))
    val latOmUri = latPomUri.extend("/FunctionTermMap")
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
    latParameterPom.addObjectMap(latParameterOmUri).addRMLReference(new RMLLiteral(mapping.coordinates))

    val lonPomUri = rmlUri.extend("/LongitudePom")
    val lonPom = triplesMap.addPredicateObjectMap(lonPomUri)
    lonPom.addPredicate(new RMLUri(RdfNamespace.GEO.namespace + "lon"))
    val lonOmUri = lonPomUri.extend("/FunctionTermMap")
    val lonOm = lonPom.addFunctionTermMap(lonOmUri)
    val lonFunctionValueUri = lonOmUri
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
    lonParameterPom.addObjectMap(lonParameterOmUri).addRMLReference(new RMLLiteral(mapping.coordinates))

  }

  def addLongituteLatitudeToTriplesMap(uri: String, triplesMap: RMLTriplesMap) =
  {
    val latitudePomUri = new RMLUri(uri + "/LatitudePOM")
    val latitudePom = triplesMap.addPredicateObjectMap(latitudePomUri)
    latitudePom.addDCTermsType(new RMLLiteral("latitudeMapping"))
    latitudePom.addPredicate(new RMLUri(RdfNamespace.GEO.namespace + "lat"))

    val latitudeOmUri = latitudePomUri.extend("/ObjectMap")
    latitudePom.addObjectMap(latitudeOmUri).addRMLReference(new RMLLiteral("latitude"))


    val longitudePomUri = new RMLUri(uri + "/LongitudePOM")
    val longitudePom = triplesMap.addPredicateObjectMap(longitudePomUri)
    longitudePom.addDCTermsType(new RMLLiteral("longitudeMapping"))
    longitudePom.addPredicate(new RMLUri(RdfNamespace.GEO.namespace + "lon"))

    val longitudeOmUri = latitudePomUri.extend("/ObjectMap")
    latitudePom.addObjectMap(latitudeOmUri).addRMLReference(new RMLLiteral("longitude"))
  }

  def addDegreesToTriplesMap(uri: String, triplesMap: RMLTriplesMap) =
  {

    val latitudePomUri = new RMLUri(uri + "/LatitudePOM")
    val latitudePom = triplesMap.addPredicateObjectMap(latitudePomUri)
    latitudePom.addDCTermsType(new RMLLiteral("latitudeMapping"))
    latitudePom.addPredicate(new RMLUri(RdfNamespace.GEO.namespace + "lat"))

    val latitudeOmUri = latitudePomUri.extend("/ObjectMap")
    val latitudeOm = latitudePom.addFunctionTermMap(latitudeOmUri)

    val latitudeFunctionValueUri = latitudeOmUri.extend("/FunctionValue")
    val latitudeFunctionValue = latitudeOm.addFunctionValue(latitudeFunctionValueUri)

    latitudeFunctionValue.addLogicalSource(rmlModel.logicalSource)
    latitudeFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val latExecutePomUri = latitudeFunctionValueUri.extend("/ExecutePOM")
    val latExecutePom = latitudeFunctionValue.addPredicateObjectMap(latExecutePomUri)
    latExecutePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
    val latExecuteOmUri = latExecutePomUri.extend("/ObjectMap")
    latExecutePom.addObjectMap(latExecuteOmUri).addConstant(new RMLUri(RdfNamespace.DBF.namespace + "latFunction"))

    val latDegreesParameterPomUri = latitudeFunctionValueUri.extend("/LatDegreesParameterPOM")
    val latDegreesParameterPom = latitudeFunctionValue.addPredicateObjectMap(latDegreesParameterPomUri)
    latDegreesParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + "latDegreesParameter"))
    val latDegreesParameterOmUri = latDegreesParameterPomUri.extend("/ObjectMap")
    latDegreesParameterPom.addObjectMap(latDegreesParameterOmUri).addRMLReference(new RMLLiteral(mapping.latitudeDegrees))

    val latMinutesParameterPomUri = latitudeFunctionValueUri.extend("/LatMinutesParameterPOM")
    val latMinutesParameterPom = latitudeFunctionValue.addPredicateObjectMap(latMinutesParameterPomUri)
    latMinutesParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + "latMinutesParameter"))
    val latMinutesParameterOmUri = latMinutesParameterPomUri.extend("/ObjectMap")
    latMinutesParameterPom.addObjectMap(latMinutesParameterOmUri).addRMLReference(new RMLLiteral(mapping.latitudeMinutes))

    val latDirectionParameterPomUri = latitudeFunctionValueUri.extend("/latDirectionParameterPOM")
    val latDirectionParameterPom = latitudeFunctionValue.addPredicateObjectMap(latDirectionParameterPomUri)
    latDirectionParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + "latDirectionParameter"))
    val latDirectionParameterOmUri = latDirectionParameterPomUri.extend("/ObjectMap")
    latDirectionParameterPom.addObjectMap(latDirectionParameterOmUri).addRMLReference(new RMLLiteral(mapping.latitudeDirection))
    
    val longitudePomUri = new RMLUri(uri + "/LongitudePOM")
    val longitudePom = triplesMap.addPredicateObjectMap(longitudePomUri)
    longitudePom.addDCTermsType(new RMLLiteral("longitudeMapping"))
    longitudePom.addPredicate(new RMLUri(RdfNamespace.GEO.namespace + "lon"))

    val longitudeOmUri = longitudePomUri.extend("/ObjectMap")
    val longitudeOm = longitudePom.addFunctionTermMap(longitudeOmUri)

    val longitudeFunctionValueUri = longitudeOmUri.extend("/FunctionValue")
    val longitudeFunctionValue = longitudeOm.addFunctionValue(longitudeFunctionValueUri)

    longitudeFunctionValue.addLogicalSource(rmlModel.logicalSource)
    longitudeFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val lonExecutePomUri = longitudeFunctionValueUri.extend("/ExecutePOM")
    val lonExecutePom = longitudeFunctionValue.addPredicateObjectMap(lonExecutePomUri)
    lonExecutePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
    val lonExecuteOmUri = lonExecutePomUri.extend("/ObjectMap")
    lonExecutePom.addObjectMap(lonExecuteOmUri).addConstant(new RMLUri(RdfNamespace.DBF.namespace + "lonFunction"))

    val lonDegreesParameterPomUri = longitudeFunctionValueUri.extend("/lonDegreesParameterPOM")
    val lonDegreesParameterPom = longitudeFunctionValue.addPredicateObjectMap(lonDegreesParameterPomUri)
    lonDegreesParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + "lonDegreesParameter"))
    val lonDegreesParameterOmUri = lonDegreesParameterPomUri.extend("/ObjectMap")
    lonDegreesParameterPom.addObjectMap(lonDegreesParameterOmUri).addRMLReference(new RMLLiteral(mapping.longitudeDegrees))

    val lonMinutesParameterPomUri = longitudeFunctionValueUri.extend("/lonMinutesParameterPOM")
    val lonMinutesParameterPom = longitudeFunctionValue.addPredicateObjectMap(lonMinutesParameterPomUri)
    lonMinutesParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + "lonMinutesParameter"))
    val lonMinutesParameterOmUri = lonMinutesParameterPomUri.extend("/ObjectMap")
    lonMinutesParameterPom.addObjectMap(lonMinutesParameterOmUri).addRMLReference(new RMLLiteral(mapping.longitudeMinutes))

    val lonDirectionParameterPomUri = longitudeFunctionValueUri.extend("/lonDirectionParameterPOM")
    val lonDirectionParameterPom = longitudeFunctionValue.addPredicateObjectMap(lonDirectionParameterPomUri)
    lonDirectionParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + "lonDirectionParameter"))
    val lonDirectionParameterOmUri = lonDirectionParameterPomUri.extend("/ObjectMap")
    lonDirectionParameterPom.addObjectMap(lonDirectionParameterOmUri).addRMLReference(new RMLLiteral(mapping.longitudeDirection))

  }

  private def addParentTriplesMapToTriplesMap(uri: String, triplesMap : RMLTriplesMap) = {
    val pomUri = new RMLUri(uri).extend("/" + mapping.ontologyProperty.name)
    val pom = rmlModel.triplesMap.addPredicateObjectMap(pomUri)
    pom.addPredicate(new RMLUri(mapping.ontologyProperty.uri))
    val objectMapUri = pomUri.extend("/ObjectMap")
    val objectMap = pom.addObjectMap(objectMapUri)
    val parentTriplesMapUri = objectMapUri.extend("/ParentTriplesMap")
    objectMap.addParentTriplesMap(parentTriplesMapUri)
  }

}
