package org.dbpedia.extraction.server.resources.ontology

import xml.Elem
import org.dbpedia.extraction.server.Server
import javax.ws.rs._
import org.dbpedia.extraction.ontology.{OntologyType, OntologyProperty, OntologyClass}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.server.resources.Base

@Path("/ontology/classes")
class Classes extends Base
{
    private val ontology = Server.extractor.ontology

    /**
     * Retrieves an overview page
     */
    @GET
    @Produces(Array("application/xhtml+xml"))
    def get : Elem =
    {
        //Map each class to a list of its sub classes
        val subClassesMap = ontology.classes   //Get all classes
                .filter(!_.name.contains(":")) //Filter non-DBpedia classes
                .sortWith(_.name < _.name)     //Sort by name
                .groupBy(_.subClassOf.head).toMap   //Group by super class

        val rootClass = ontology.getClass("owl:Thing").get

        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
          </head>
          <body>
            <h2>Ontology Classes</h2>
            {createClassHierarchy(rootClass, subClassesMap)}
          </body>
        </html>
    }

    /**
     * Retrieves a class page
     */
    @GET
    @Path("/{name}")
    @Produces(Array("application/xhtml+xml"))
    def getClass(@PathParam("name") name : String) : Elem =
    {
        ontology.getClass(name) match
        {
            case Some(ontClass) => createClassPage(ontClass)
            case None =>
            {
                <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
                  <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                  </head>
                  <body>
                    <strong>Class not found</strong>
                  </body>
                </html>
            }
        }
    }

    private def createClassHierarchy(baseClass : OntologyClass, subClassesMap : Map[OntologyClass, List[OntologyClass]]) : Elem =
    {
        <ul>
        {
            for(subClass <- subClassesMap.get(baseClass).getOrElse(List())) yield
            {
                <li>
                  <a name={subClass.name}/>
                  {createLink("classes/", subClass)} {createEditLink(subClass)}<br/>
                  {createClassHierarchy(subClass, subClassesMap)}
                </li>
            }
        }
        </ul>
    }

    private def createClassPage(ontClass : OntologyClass) =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
          </head>
          <body>
            <h2>{ontClass.name} <span style="font-size:10pt;">(<a href={"../classes#" + ontClass.name}>Show in class hierarchy</a>)</span></h2>
            <table border="0">
              {
                //Labels
                for((language, label) <- ontClass.labels) yield
                {
                  <tr>
                    <td><strong>{"Label (" + language + "): "}</strong></td>
                    <td>{label}</td>
                  </tr>
                }
              }
              {
                //Comments
                for((language, comment) <- ontClass.comments) yield
                {
                  <tr>
                    <td><strong>{"Comment (" + language + "): "}</strong></td>
                    <td>{comment}</td>
                  </tr>
                }
              }
              <tr>
                <td><strong>Super classes:</strong></td>
                {
                  for(superClass <- ontClass.subClassOf) yield
                  {
                    <td>{createLink("", superClass, ontClass.subClassOf.size == 1)}</td>
                  }
                }
              </tr>
            </table>
            <br/>
            <strong>Properties on <em>{ontClass.name}</em>:</strong>
            {createPropertiesTable(ontClass)}
          </body>
        </html>
    }

    private def createPropertiesTable(ontClass : OntologyClass) : Elem =
    {
        //Collect all properties
//        val properties = for(clazz <- Stream.iterate(ontClass)(_.subClassOf).takeWhile(_ != null);
//                             property <- ontology.properties.sortWith(_.name< _.name);
//                             if property.domain == clazz)
//                             yield property

        val classes = (ontClass :: ontClass.subClassOf)
        val properties = ontology.properties.sortBy(_.name).filter(classes contains _.domain)

        <table border="1" cellpadding="3" cellspacing="0">
          <tr style="font-weight: bold;" bgcolor="#CCCCFF">
             <td>Name</td>
             <td>Label</td>
             <td>Domain</td>
             <td>Range</td>
             <td>Comment</td>
          </tr>
          {properties.map(createPropertyRow)}
        </table>
    }
                  
    private def createPropertyRow(property : OntologyProperty) =
    {
        <tr>
          <td bgcolor="#EEEEFF">{property.name} {createEditLink(property)}</td>
          <td>{property.labels.get("en").getOrElse("undefined")}</td>
          <td>{createLink("", property.domain)}</td>
          <td>{createLink("", property.range)}</td>
          <td>{property.comments.get("en").getOrElse("")}</td>
        </tr>
    }

    private def createLink(prefix : String, t : OntologyType, addOwlThing : Boolean = true) = t match
    {
        case ontClass : OntologyClass if ontClass.name == "owl:Thing" =>
        {
            if(addOwlThing)
            {
                <em>owl:Thing</em>
            }
        }
        case ontClass : OntologyClass =>
        {
            <a href={prefix + ontClass.name} title={ontClass.labels.get("en").getOrElse(ontClass.name)}>{ontClass.name}</a>
        }
        case datatype : Datatype =>
        {
            <em>{datatype.name}</em>
        }
        case null =>
        {
            <em>undefined</em>
        }
    }

    private def createEditLink(t : OntologyType) = t match
    {
        case ontClass : OntologyClass if ontClass.name != "owl:Thing" =>
        {
            <small>(<a href={Server.config.wikiPagesUrl + "/OntologyClass:" + ontClass.name} title="Edit this class on the Wiki">edit</a>)</small>
        }
        case _ =>
        {
            //No edit link
            Elem
        }
    }

    private def createEditLink(p : OntologyProperty) =
    {
        <small>(<a href={Server.config.wikiPagesUrl + "/OntologyProperty:" + p.name} title="Edit this property on the Wiki">edit</a>)</small>
    }
}
