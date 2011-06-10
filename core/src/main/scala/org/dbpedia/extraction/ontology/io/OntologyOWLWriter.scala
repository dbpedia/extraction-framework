package org.dbpedia.extraction.ontology.io

import org.dbpedia.extraction.ontology._
import datatypes.{DimensionDatatype, UnitDatatype}

class OntologyOWLWriter(writeSpecificProperties : Boolean = true)
{

	private val Version = "3.6";
	
	def write(ontology : Ontology) : scala.xml.Elem =
    {
        <rdf:RDF
        	xmlns = "http://dbpedia.org/ontology/"
        	xml:base="http://dbpedia.org/ontology/"
        	xmlns:owl="http://www.w3.org/2002/07/owl#"
        	xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
        	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
        	xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">

        <owl:Ontology rdf:about="">
          <owl:versionInfo xml:lang="en">{"Version " + Version}</owl:versionInfo>
        </owl:Ontology>
        {
	        //Write classes from the default namespace (Don't write owl, rdf and rdfs built-in classes etc.)
	        val classes = for(ontologyClass <- ontology.classes if !ontologyClass.name.contains(':'))
	        	yield writeClass(ontologyClass)

	        //Write properties from the default namespace
	        val properties = for(ontologyProperty <- ontology.properties if !ontologyProperty.name.contains(':'))
	        	yield writeProperty(ontologyProperty)

            if(writeSpecificProperties)
            {
                //Write specific properties
                val specificProperties = for(((clazz, property), datatype) <- ontology.specializations)
                    yield writeSpecificProperty(clazz, property, datatype)

                classes ++ properties ++ specificProperties
            }
            else
            {
                classes ++ properties
            }
        }
        </rdf:RDF>
    }

    private def writeClass(ontologyClass : OntologyClass) : scala.xml.Elem =
    {
    	val xml = new scala.xml.NodeBuffer()

        //Labels
	    for((language, label) <- ontologyClass.labels)
	    {
	        xml += <rdfs:label xml:lang={language}>{label}</rdfs:label>
	    }

        //Comments
	    for((language, comment) <- ontologyClass.comments)
	    {
	        xml += <rdfs:comment xml:lang={language}>{comment}</rdfs:comment>
	    }

        //Super classes
	    if (ontologyClass.subClassOf != null)
	    {
	        xml += <rdfs:subClassOf rdf:resource={ontologyClass.subClassOf.uri}/>
	    }

        //Equivalent classes
	    for(equivalentClass <- ontologyClass.equivalentClasses)
	    {
	        xml += <owl:equivalentClass rdf:resource={equivalentClass.uri}/>
	    }

    	<owl:Class rdf:about={ontologyClass.uri}>
    	{xml}
    	</owl:Class>
    }

    private def writeProperty(property : OntologyProperty) : scala.xml.Elem =
    {
    	val xml = new scala.xml.NodeBuffer()

        //Type
        if (property.isFunctional)
        {
             xml += <rdf:type rdf:resource="http://www.w3.org/2002/07/owl#FunctionalProperty" />
        }

        //Labels
        val labelPostfix = property.range match
        {
            case unit : UnitDatatype =>
            {
                //Append the unit to the label
                val unitLabel = unit.unitLabels.toList.sortWith(_.size < _.size).headOption.getOrElse("")
                " (" + unitLabel + ")"
            }

            case dimension: DimensionDatatype =>
            {
                //Append the unit to the label
                val unitLabel = dimension.units.head.unitLabels.toList.sortWith(_.size < _.size).headOption.getOrElse("")
                " (" + unitLabel + ")"
            }
            case _ => ""
        }

	    for((language, label) <- property.labels)
	    {
	        xml += <rdfs:label xml:lang={language}>{label + labelPostfix}</rdfs:label>
	    }

        //Comments
	    for((language, comment) <- property.comments)
	    {
	        xml += <rdfs:comment xml:lang={language}>{comment}</rdfs:comment>
	    }

        //Domain
        if (property.domain.name != "owl:Thing")
        {
            xml += <rdfs:domain rdf:resource={property.domain.uri} />
        }

        //Range
        property match
        {
            case objectProperty : OntologyObjectProperty if objectProperty.range.name != "owl:Thing" =>
            {
                xml += <rdfs:range rdf:resource={objectProperty.range.uri} />
            }
            case datatypeProperty : OntologyDatatypeProperty =>
            {
               datatypeProperty.range match
               {
                   case dimension: DimensionDatatype => xml += <rdfs:range rdf:resource="http://www.w3.org/2001/XMLSchema#double" />
                   case _ => xml += <rdfs:range rdf:resource={datatypeProperty.range.uri} />
               }
            }
            case _ =>
        }

        //Return xml
        property match
        {
            case objectProperty : OntologyObjectProperty =>
            {
                <owl:ObjectProperty rdf:about={property.uri}>
                { xml }
                </owl:ObjectProperty>
            }
            case datatypeProperty : OntologyDatatypeProperty =>
            {
                <owl:DatatypeProperty rdf:about={property.uri}>
                { xml }
                </owl:DatatypeProperty>
            }
        }
    }

    private def writeSpecificProperty(clazz : OntologyClass, property : OntologyProperty, unit : UnitDatatype) : scala.xml.Elem =
    {
        val propertyUri = OntologyNamespaces.DBPEDIA_SPECIFICPROPERTY_NAMESPACE + clazz.name + "/" + property.name

        //Append the unit to the label
        val labelPostfix = " (" + unit.unitLabels.toList.sortWith(_.size < _.size).headOption.getOrElse("") + ")"

        <owl:DatatypeProperty rdf:about={propertyUri}>
            { for((language, label) <- property.labels) yield <rdfs:label xml:lang={language}>{label + labelPostfix}</rdfs:label> }
            { for((language, comment) <- property.comments) yield <rdfs:comment xml:lang={language}>{comment}</rdfs:comment> }
            <rdfs:domain rdf:resource={clazz.uri} />
            <rdfs:range rdf:resource={unit.uri} />
        </owl:DatatypeProperty>
    }
}
