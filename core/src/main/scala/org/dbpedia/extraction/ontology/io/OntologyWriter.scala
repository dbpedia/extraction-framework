package org.dbpedia.extraction.ontology.io

import java.io.{OutputStreamWriter, FileOutputStream, File}
import collection.mutable.{MultiMap, HashMap, Set}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology._

/**
 * Writes an ontology to configuration files using the DBpedia mapping language.
 */
//TODO write rdfs:comment
class OntologyWriter
{
    def write(ontology : Ontology, dir : File)
    {
        //Convert specialisations from Map[(OntologyClass, OntologyProperty), Datatype)] to Map[OntologyClass, Set(OntologyProperty, Datatype)]
        val specializations = new HashMap[OntologyClass, Set[(OntologyProperty, Datatype)]] with MultiMap[OntologyClass, (OntologyProperty, Datatype)]
        for(((clazz, property), dt) <- ontology.specializations)
        {
            specializations.addBinding(clazz, (property, dt))
        }

        //Write classes
        for(clazz <- ontology.classes if clazz.name.indexOf(':') == -1)
        {
            writeClass(clazz, dir, specializations)
        }

        //Write properties
        for(property <- ontology.properties if property.name.indexOf(':') == -1)
        {
            writeProperty(property, dir)
        }
    }

    def writeClass(clazz : OntologyClass, dir : File, specializations : HashMap[OntologyClass, Set[(OntologyProperty, Datatype)]])
    {
        import OntologyWriter._

        val file = new File(dir + "/" + getFileName(clazz.name, "classes/"))
        file.getParentFile.mkdirs()
        val stream = new FileOutputStream(file)
        val writer = new OutputStreamWriter(stream, "UTF-8")

        try
        {
            writer.write("{{" + CLASSTEMPLATE_NAME)

            for((language, label) <- clazz.labels)
            {
                writer.write("\n| rdfs:label@ " + language + " = " + label)
            }

            for((language, comment) <- clazz.comments)
            {
                writer.write("\n| rdfs:comment@ " + language + " = " + comment)
            }

            for(superClass <- clazz.subClassOf if superClass.name != "owl:Thing")
            {
                writer.write("\n| rdfs:subClassOf = " + superClass.name)
            }

            writer.write("\n| owl:equivalentClass = " + clazz.equivalentClasses.map(_.name).mkString(", "))

            specializations.get(clazz) match
            {
                case Some(s) => writer.write("\n| specificProperties = " + s.map{case (property, dt) =>
                    "{{SpecificProperty | ontologyProperty = " + property.name + " | unit = " + dt.name + " }}"}.mkString("\n" + " " * 23))
                case None =>
            }

            writer.write("\n}}")

        }
        finally
        {
            writer.close()
        }
    }

    def writeProperty(property : OntologyProperty, dir : File)
    {
        import OntologyWriter._

        val file = new File(dir + "/" + getFileName(property.name, "properties/"))
        file.getParentFile.mkdirs()
        val stream = new FileOutputStream(file)
        val writer = new OutputStreamWriter(stream, "UTF-8")

        try
        {
            writer.write("{{")
            property match
            {
                case _ : OntologyObjectProperty => writer.write(OBJECTPROPERTY_NAME)
                case _ : OntologyDatatypeProperty => writer.write(DATATYPEPROPERTY_NAME)
            }

            for((language, label) <- property.labels)
            {
                writer.write("\n| rdfs:label@ " + language + " = " + label)
            }

            for((language, comment) <- property.comments)
            {
                writer.write("\n| rdfs:comment@ " + language + " = " + comment)
            }

            if(property.domain.name != "owl:Thing") writer.write("\n| rdfs:domain = " + property.domain.name)
            if(property.range.name != "owl:Thing") writer.write("\n| rdfs:range = " + property.range.name)
            if(property.isFunctional) writer.write("\n| rdf:type = owl:FunctionalProperty")

            writer.write("\n}}")

        }
        finally
        {
            writer.close()
        }
    }
}

private object OntologyWriter
{
    val CLASSTEMPLATE_NAME = "DBpediaClass"
    val OBJECTPROPERTY_NAME = "DBpediaObjectProperty"
    val DATATYPEPROPERTY_NAME = "DBpediaDatatypeProperty"

    private def getFileName(name : String, prefix : String) : String =
    {
        if (name.indexOf(':') == -1)
        {
            "dbpedia/" + prefix + name
        }
        else
        {
            name.replaceFirst(":", "/");
        }
    }
}
