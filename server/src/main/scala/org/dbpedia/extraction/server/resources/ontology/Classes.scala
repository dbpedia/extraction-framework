package org.dbpedia.extraction.server.resources.ontology

import scala.xml.Elem
import org.dbpedia.extraction.server.Server
import javax.ws.rs._
import org.dbpedia.extraction.ontology.{OntologyType, OntologyProperty, OntologyClass}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.Language

@Path("/ontology/classes/")
class Classes
{
    private val ontology = Server.instance.extractor.ontology

    /**
     * Retrieves an overview page
     */
    @GET
    @Produces(Array("application/xhtml+xml"))
    def get : Elem =
    {
        //Map each class to a list of its sub classes
        val subClassesMap = ontology.classes.values.toList   //Get all classes
                // Don't filter non-DBpedia classes - it's useful to see foaf:Document etc
                // .filter(! _.name.contains(":")) //Filter non-DBpedia classes
                .sortWith(_.name < _.name)     //Sort by name
                .groupBy(_.baseClasses.head).toMap   //Group by super class

        val rootClass = ontology.classes("owl:Thing")

        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
          </head>
          <body>
            <h2>Ontology Classes</h2>
            <ul>
            {createClassHierarchy(rootClass, subClassesMap)}
            </ul>
          </body>
        </html>
    }

    /**
     * Retrieves a class page
     */
    @GET
    @Path("/{name}")
    @Produces(Array("application/xhtml+xml"))
    def getClass(@PathParam("name") name : String) : Elem = ontology.classes.get(name) match {
      case Some(cls) => createClassPage(cls)
      case None => createUnknownClass
    }

    private def createUnknownClass : Elem = {
      <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
      </head>
      <body>
        <strong>Class not found</strong>
      </body>
      </html>
    }

    private def createClassHierarchy(baseClass : OntologyClass, subClassesMap : Map[OntologyClass, List[OntologyClass]]) : Elem =
    {
        <li>
        <a name={baseClass.name}/>
        {createLink(baseClass)} {createEditLink(baseClass)}
        <ul>
        {
            for(subClass <- subClassesMap.get(baseClass).getOrElse(List.empty)) yield
            {
                {createClassHierarchy(subClass, subClassesMap)}
            }
        }
        </ul>
        </li>
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
                    <td><strong>{"Label (" + language.wikiCode + "): "}</strong></td>
                    <td>{label}</td>
                  </tr>
                }
              }
              {
                //Comments
                for((language, comment) <- ontClass.comments) yield
                {
                  <tr>
                    <td><strong>{"Comment (" + language.wikiCode + "): "}</strong></td>
                    <td>{comment}</td>
                  </tr>
                }
              }
              <tr>
                <td><strong>Super classes:</strong></td>
                {
                  for(baseClass <- ontClass.baseClasses) yield
                  {
                    <td>{createLink(baseClass)}</td>
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

        // TODO: why show only this class and its direct base classes?
        val classes = (ontClass :: ontClass.baseClasses)
        val properties = ontology.properties.values.toList.sortBy(_.name).filter(classes contains _.domain)

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
          <td>{property.labels.get(Language.English).getOrElse("undefined")}</td>
          <td>{createLink(property.domain)}</td>
          <td>{createLink(property.range)}</td>
          <td>{property.comments.get(Language.English).getOrElse("")}</td>
        </tr>
    }

    private def createLink(t : OntologyType) = t match
    {
        case ontClass : OntologyClass =>
        {
            // escape colon in names like owl:Thing - otherwise browser thinks the namespace as a protocol.
            <a href={ontClass.name.replace(":", "%3A")} title={ontClass.labels.get(Language.English).getOrElse(ontClass.name)}>{ontClass.name}</a>
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
            <small>(<a href={Server.instance.paths.pagesUrl + "/OntologyClass:" + ontClass.name} title="Edit this class on the Wiki">edit</a>)</small>
        }
        case _ =>
        {
            // No edit link
        }
    }

    private def createEditLink(p : OntologyProperty) =
    {
        <small>(<a href={Server.instance.paths.pagesUrl + "/OntologyProperty:" + p.name} title="Edit this property on the Wiki">edit</a>)</small>
    }
}
