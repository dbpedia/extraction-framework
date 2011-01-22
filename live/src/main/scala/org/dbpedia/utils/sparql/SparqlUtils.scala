package org.dbpedia.utils.sparql

import collection.mutable.{Set, HashMap, MultiMap}
import com.hp.hpl.jena.rdf.model.Literal

/**
 * Created by IntelliJ IDEA.
 * User: raven
 * Date: Sep 10, 2010
 * Time: 6:43:42 PM
 * To change this template use File | Settings | File Templates.
 */

object SparqlUtils
{
  /**
   * Retrieve all URIs and labels of instances of the given class
   */
  def getInstancesUriAndLabels(graphDAO : IGraphDAO, classURI : String, langTag : String) : MultiMap[String, String] = {

    val fromPart = graphDAO.defaultGraphName match {
      case Some(name) => "FROM <" + name + "> "
      case _ => ""
    }

    val query = "SELECT ?u ?l " + fromPart + "{ ?u a <" + classURI + "> . ?u rdfs:label ?l . Filter(langMatches(lang(?l), '" + langTag + "')) . }";

    return processURILabelsMap(graphDAO, query);
  }

  def processURILabelsMap(graphDAO : IGraphDAO, query : String) : MultiMap[String, String] = {
    val result = new HashMap[String, Set[String]]() with MultiMap[String, String];

    val resultCollection = new QueryCollection(graphDAO, query)

    resultCollection.foreach(qs => {result.addBinding(qs.get("l").asInstanceOf[Literal].getValue.toString, qs.get("u").toString)});

    return result;
  }
}