package org.dbpedia.extraction.mappings.rml.model.assembler

import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.model.assembler.TemplateAssembler.Counter
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLLiteral, RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template.GeocoordinateTemplate
import org.dbpedia.extraction.mappings.rml.translate.dbf.DbfFunction
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Created by wmaroy on 26.07.17.
  */
class GeocoordinateTemplateAssembler(rmlModel: RMLModel, baseUri : String, language: String, template : GeocoordinateTemplate,  counter : Counter) {

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Public methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def assemble() : List[RMLPredicateObjectMap] = {
    addGeoCoordinatesMapping()
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Private methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private def addGeoCoordinatesMapping() : List[RMLPredicateObjectMap] =
  {
    if(template.ontologyProperty != null) {
      /**
        * val pom = rmlModel.triplesMap.addPredicateObjectMap(uri)
        * pom.addDCTermsType(new RMLLiteral("intermediateGeoMapping"))
        * val triplesMap = addParentTriplesMapToPredicateObjectMap(pom)
        * triplesMap.addLogicalSource(rmlModel.logicalSource)
        * val parentSubjectMap = triplesMap.addSubjectMap(triplesMap.uri.extend("/SubjectMap"))
        * parentSubjectMap.addClass(RMLUri(RdfNamespace.GEO.namespace + "SpatialThing"))
        * parentSubjectMap.addIRITermType()
        * parentSubjectMap.addRMLReference(new RMLLiteral(mapping.ontologyProperty.name))
        * addGeoCoordinatesMappingToTriplesMap(triplesMap)
        * List(pom)
        **
        * TODO: enable these again
        *
        **/
      List()
    } else {
      addGeoCoordinatesMappingToTriplesMap(rmlModel.triplesMap)
    }
  }

  private def addGeoCoordinatesMappingToTriplesMap(triplesMap: RMLTriplesMap) : List[RMLPredicateObjectMap]  =
  {

    if(template.coordinate != null) {

      addCoordinatesToTriplesMap(triplesMap)

    } else if(template.latitude != null && template.longitude != null) {

      addLongitudeLatitudeToTriplesMap(triplesMap)

    } else {

      addDegreesToTriplesMap(triplesMap)

    }
  }

  private def addCoordinatesToTriplesMap(triplesMap: RMLTriplesMap) : List[RMLPredicateObjectMap] =
  {

    val latPom = triplesMap.addPredicateObjectMap(RMLUri(baseUri + "/" + RMLUri.LATITUDEMAPPING + "/" + counter.geoCoordinates))
    val lonPom = triplesMap.addPredicateObjectMap(RMLUri(baseUri + "/" + RMLUri.LONGITUDEMAPPING + "/" + counter.geoCoordinates))

    addCoordinatesToPredicateObjectMap(latPom, lonPom)

    List(latPom, lonPom)

  }

  private def addLongitudeLatitudeToTriplesMap(triplesMap: RMLTriplesMap) : List[RMLPredicateObjectMap] =
  {

    val latitudePom = triplesMap.addPredicateObjectMap(RMLUri(baseUri + "/" + RMLUri.LATITUDEMAPPING + "/" + counter.geoCoordinates))
    val longitudePom = triplesMap.addPredicateObjectMap(RMLUri(baseUri + "/" + RMLUri.LONGITUDEMAPPING + "/" + counter.geoCoordinates))

    addLongitudeLatitudeToPredicateObjectMap(latitudePom, longitudePom)

    List(latitudePom, longitudePom)

  }

  private def addDegreesToTriplesMap(triplesMap: RMLTriplesMap) : List[RMLPredicateObjectMap] =
  {

    val latitudePom = triplesMap.addPredicateObjectMap(RMLUri(baseUri +"/" + RMLUri.LATITUDEMAPPING + "/" + counter.geoCoordinates))
    val longitudePom = triplesMap.addPredicateObjectMap(RMLUri(baseUri +"/" + RMLUri.LONGITUDEMAPPING + "/" + counter.geoCoordinates))

    addDegreesToPredicateObjectMap(latitudePom, longitudePom)

    List(latitudePom, longitudePom)

  }

  private def addCoordinatesToPredicateObjectMap(latPom: RMLPredicateObjectMap, lonPom: RMLPredicateObjectMap) =
  {
    latPom.addPredicate(RMLUri(RdfNamespace.GEO.namespace + "lat"))
    val latOmUri = latPom.uri.extend("/FunctionTermMap")
    val latOm = latPom.addFunctionTermMap(latOmUri)
    val latFunctionValueUri = latOmUri
    val latFunctionValue = latOm.addFunctionValue(latFunctionValueUri)
    latFunctionValue.addLogicalSource(rmlModel.logicalSource)
    latFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val latExecutePom = latFunctionValue.addPredicateObjectMap(RMLUri(rmlModel.triplesMap.resource.getURI + "/Function/LatitudeFunction"))
    latExecutePom.addPredicate(RMLUri(RdfNamespace.FNO.namespace + "executes"))
    latExecutePom.addObject(RMLUri(RdfNamespace.DBF.namespace + "latFunction"))

    val latParameterPomUri = latFunctionValueUri.extend("/ParameterPOM")
    val latParameterPom = latFunctionValue.addPredicateObjectMap(latParameterPomUri)
    latParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + "coordParameter"))
    val latParameterOmUri = latParameterPomUri.extend("/ObjectMap")
    latParameterPom.addObjectMap(latParameterOmUri).addRMLReference(new RMLLiteral(template.coordinate))

    lonPom.addPredicate(RMLUri(RdfNamespace.GEO.namespace + "long"))
    val lonOmUri = lonPom.uri.extend("/FunctionTermMap")
    val lonOm = lonPom.addFunctionTermMap(lonOmUri)
    val lonFunctionValueUri = lonOmUri.extend("/FunctionValue")
    val lonFunctionValue = lonOm.addFunctionValue(lonFunctionValueUri)
    lonFunctionValue.addLogicalSource(rmlModel.logicalSource)
    lonFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val lonExecutePom = lonFunctionValue.addPredicateObjectMap(RMLUri(rmlModel.triplesMap.resource.getURI + "/Function/LongitudeFunction"))
    lonExecutePom.addPredicate(RMLUri(RdfNamespace.FNO.namespace + "executes"))
    lonExecutePom.addObject(RMLUri(RdfNamespace.DBF.namespace + "lonFunction"))

    val lonParameterPomUri = lonFunctionValueUri.extend("/ParameterPOM")
    val lonParameterPom = lonFunctionValue.addPredicateObjectMap(lonParameterPomUri)
    lonParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + "coordParameter"))
    val lonParameterOmUri = lonParameterPomUri.extend("/ObjectMap")
    lonParameterPom.addObjectMap(lonParameterOmUri).addRMLReference(new RMLLiteral(template.coordinate))

  }

  private def addLongitudeLatitudeToPredicateObjectMap(latPom: RMLPredicateObjectMap, lonPom: RMLPredicateObjectMap) =
  {
    latPom.addPredicate(RMLUri(RdfNamespace.GEO.namespace + "lat"))
    val latOmUri = latPom.uri.extend("/FunctionTermMap")
    val latOm = latPom.addFunctionTermMap(latOmUri)
    val latFunctionValueUri = latOmUri
    val latFunctionValue = latOm.addFunctionValue(latFunctionValueUri)
    latFunctionValue.addLogicalSource(rmlModel.logicalSource)
    latFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val latExecutePom = latFunctionValue.addPredicateObjectMap(RMLUri(rmlModel.triplesMap.resource.getURI + "/Function/LatitudeFunction"))
    latExecutePom.addPredicate(RMLUri(RdfNamespace.FNO.namespace + "executes"))
    latExecutePom.addObject(RMLUri(RdfNamespace.DBF.namespace + "latFunction"))

    val latParameterPomUri = latFunctionValueUri.extend("/ParameterPOM")
    val latParameterPom = latFunctionValue.addPredicateObjectMap(latParameterPomUri)
    latParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + "latParameter"))
    val latParameterOmUri = latParameterPomUri.extend("/ObjectMap")
    latParameterPom.addObjectMap(latParameterOmUri).addRMLReference(new RMLLiteral(template.latitude))

    lonPom.addPredicate(RMLUri(RdfNamespace.GEO.namespace + "long"))
    val lonOmUri = lonPom.uri.extend("/FunctionTermMap")
    val lonOm = lonPom.addFunctionTermMap(lonOmUri)
    val lonFunctionValueUri = lonOmUri.extend("/FunctionValue")
    val lonFunctionValue = lonOm.addFunctionValue(lonFunctionValueUri)
    lonFunctionValue.addLogicalSource(rmlModel.logicalSource)
    lonFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val lonExecutePom = lonFunctionValue.addPredicateObjectMap(RMLUri(rmlModel.triplesMap.resource.getURI + "/Function/LongitudeFunction"))
    lonExecutePom.addPredicate(RMLUri(RdfNamespace.FNO.namespace + "executes"))
    lonExecutePom.addObject(RMLUri(RdfNamespace.DBF.namespace + "lonFunction"))

    val lonParameterPomUri = lonFunctionValueUri.extend("/ParameterPOM")
    val lonParameterPom = lonFunctionValue.addPredicateObjectMap(lonParameterPomUri)
    lonParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + "lonParameter"))
    val lonParameterOmUri = lonParameterPomUri.extend("/ObjectMap")
    lonParameterPom.addObjectMap(lonParameterOmUri).addRMLReference(new RMLLiteral(template.longitude))
  }

  private def addDegreesToPredicateObjectMap(latPom: RMLPredicateObjectMap, lonPom: RMLPredicateObjectMap) =
  {
    latPom.addPredicate(RMLUri(RdfNamespace.GEO.namespace + "lat"))

    val latitudeOmUri = latPom.uri.extend("/ObjectMap")
    val latitudeOm = latPom.addFunctionTermMap(latitudeOmUri)

    val latitudeFunctionValueUri = latitudeOmUri.extend("/FunctionValue")
    val latitudeFunctionValue = latitudeOm.addFunctionValue(latitudeFunctionValueUri)

    latitudeFunctionValue.addLogicalSource(rmlModel.logicalSource)
    latitudeFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val latExecutePom = latitudeFunctionValue.addPredicateObjectMap(RMLUri(rmlModel.triplesMap.resource.getURI + "/Function/LatitudeFunction"))
    latExecutePom.addPredicate(RMLUri(RdfNamespace.FNO.namespace + "executes"))
    latExecutePom.addObject(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.latFunction.name))

    val latDegreesParameterPomUri = latitudeFunctionValueUri.extend("/LatDegreesParameterPOM")
    val latDegreesParameterPom = latitudeFunctionValue.addPredicateObjectMap(latDegreesParameterPomUri)
    latDegreesParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.latFunction.latDegreesParameter))
    val latDegreesParameterOmUri = latDegreesParameterPomUri.extend("/ObjectMap")
    if(template.latitudeDegrees != null) {
      latDegreesParameterPom.addObjectMap(latDegreesParameterOmUri).addRMLReference(new RMLLiteral(template.latitudeDegrees))
    } else {
      latDegreesParameterPom.addObjectMap(latDegreesParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }

    val latMinutesParameterPomUri = latitudeFunctionValueUri.extend("/LatMinutesParameterPOM")
    val latMinutesParameterPom = latitudeFunctionValue.addPredicateObjectMap(latMinutesParameterPomUri)
    latMinutesParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.latFunction.latMinutesParameter))
    val latMinutesParameterOmUri = latMinutesParameterPomUri.extend("/ObjectMap")
    if(template.latitudeMinutes != null) {
      latMinutesParameterPom.addObjectMap(latMinutesParameterOmUri).addRMLReference(new RMLLiteral(template.latitudeMinutes))
    } else {
      latMinutesParameterPom.addObjectMap(latMinutesParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }

    val latSecondsParameterPomUri = latitudeFunctionValueUri.extend("/LatSecondsParameterPOM")
    val latSecondsParameterPom = latitudeFunctionValue.addPredicateObjectMap(latSecondsParameterPomUri)
    latSecondsParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.latFunction.latSecondsParameter))
    val latSecondsParameterOmUri = latSecondsParameterPomUri.extend("/ObjectMap")
    if(template.latitudeSeconds != null) {
      latSecondsParameterPom.addObjectMap(latSecondsParameterOmUri).addRMLReference(new RMLLiteral(template.latitudeSeconds))
    } else {
      latSecondsParameterPom.addObjectMap(latSecondsParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }

    val latDirectionParameterPomUri = latitudeFunctionValueUri.extend("/latDirectionParameterPOM")
    val latDirectionParameterPom = latitudeFunctionValue.addPredicateObjectMap(latDirectionParameterPomUri)
    latDirectionParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.latFunction.latDirectionParameter))
    val latDirectionParameterOmUri = latDirectionParameterPomUri.extend("/ObjectMap")
    if(template.latitudeDirection != null) {
      latDirectionParameterPom.addObjectMap(latDirectionParameterOmUri).addRMLReference(new RMLLiteral(template.latitudeDirection))
    } else {
      latDirectionParameterPom.addObjectMap(latDirectionParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }

    lonPom.addPredicate(RMLUri(RdfNamespace.GEO.namespace + "long"))

    val longitudeOmUri = lonPom.uri.extend("/ObjectMap")
    val longitudeOm = lonPom.addFunctionTermMap(longitudeOmUri)

    val longitudeFunctionValueUri = longitudeOmUri.extend("/FunctionValue")
    val longitudeFunctionValue = longitudeOm.addFunctionValue(longitudeFunctionValueUri)

    longitudeFunctionValue.addLogicalSource(rmlModel.logicalSource)
    longitudeFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val lonExecutePom = longitudeFunctionValue.addPredicateObjectMap(RMLUri(rmlModel.triplesMap.resource.getURI + "/Function/LongitudeFunction"))
    lonExecutePom.addPredicate(RMLUri(RdfNamespace.FNO.namespace + "executes"))
    lonExecutePom.addObject(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.lonFunction.name))

    val lonDegreesParameterPomUri = longitudeFunctionValueUri.extend("/lonDegreesParameterPOM")
    val lonDegreesParameterPom = longitudeFunctionValue.addPredicateObjectMap(lonDegreesParameterPomUri)
    lonDegreesParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.lonFunction.lonDegreesParameter))
    val lonDegreesParameterOmUri = lonDegreesParameterPomUri.extend("/ObjectMap")

    if(template.longitudeDegrees != null) {
      lonDegreesParameterPom.addObjectMap(lonDegreesParameterOmUri).addRMLReference(new RMLLiteral(template.longitudeDegrees))
    } else {
      lonDegreesParameterPom.addObjectMap(lonDegreesParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }

    val lonMinutesParameterPomUri = longitudeFunctionValueUri.extend("/lonMinutesParameterPOM")
    val lonMinutesParameterPom = longitudeFunctionValue.addPredicateObjectMap(lonMinutesParameterPomUri)
    lonMinutesParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.lonFunction.lonMinutesParameter))
    val lonMinutesParameterOmUri = lonMinutesParameterPomUri.extend("/ObjectMap")
    if(template.longitudeMinutes != null) {
      lonMinutesParameterPom.addObjectMap(lonMinutesParameterOmUri).addRMLReference(new RMLLiteral(template.longitudeMinutes))
    } else {
      lonMinutesParameterPom.addObjectMap(lonMinutesParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }

    val lonSecondsParameterPomUri = longitudeFunctionValueUri.extend("/lonSecondsParameterPOM")
    val lonSecondsParameterPom = longitudeFunctionValue.addPredicateObjectMap(lonSecondsParameterPomUri)
    lonSecondsParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.lonFunction.lonSecondsParameter))
    val lonSecondsParameterOmUri = lonSecondsParameterPomUri.extend("/ObjectMap")
    if(template.longitudeSeconds != null) {
      lonSecondsParameterPom.addObjectMap(lonSecondsParameterOmUri).addRMLReference(new RMLLiteral(template.longitudeSeconds))
    } else {
      lonSecondsParameterPom.addObjectMap(lonSecondsParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }

    val lonDirectionParameterPomUri = longitudeFunctionValueUri.extend("/lonDirectionParameterPOM")
    val lonDirectionParameterPom = longitudeFunctionValue.addPredicateObjectMap(lonDirectionParameterPomUri)
    lonDirectionParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.lonFunction.lonDirectionParameter))
    val lonDirectionParameterOmUri = lonDirectionParameterPomUri.extend("/ObjectMap")
    if(template.longitudeDirection != null) {
      lonDirectionParameterPom.addObjectMap(lonDirectionParameterOmUri).addRMLReference(new RMLLiteral(template.longitudeDirection))
    } else {
      lonDirectionParameterPom.addObjectMap(lonDirectionParameterOmUri).addRMLReference(new RMLLiteral("null"))
    }


  }

  private def addParentTriplesMapToPredicateObjectMap(pom: RMLPredicateObjectMap) = {

    pom.addPredicate(RMLUri(template.ontologyProperty.uri))
    val objectMapUri = pom.uri.extend("/ObjectMap")
    val objectMap = pom.addObjectMap(objectMapUri)
    val parentTriplesMapUri = objectMapUri.extend("/ParentTriplesMap")
    objectMap.addParentTriplesMap(parentTriplesMapUri)

  }


}
