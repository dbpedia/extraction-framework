package org.dbpedia.utils.sparql

import java.lang.String
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP
import com.hp.hpl.jena.query.{ResultSetFormatter, QuerySolution}
/**
 * Created by Claus Stadler
 * User: raven
 * Date: Sep 8, 2010
 * Time: 12:26:13 PM
 *
 * A shallow convenience wrapper for Jena's QueryEngineHTTP
 */
class HTTPGraphDAO(val serviceName : String, override val defaultGraphName : Option[String])
  extends IGraphDAO
{

  private def queryExecution(query : String) : QueryEngineHTTP = {

    //println("Query is: " + query)

    val result = new QueryEngineHTTP(serviceName, query)

    defaultGraphName match {
      case Some(graphName) => result.addDefaultGraph(graphName)
      case None =>
    }

    return result
  }

  def executeConstruct(query : String) : Model = {
    val result = queryExecution(query).execConstruct
    return result
  }

  def executeAsk(query: String) : Boolean = {
    val result = queryExecution(query).execAsk
    return result
  }

  def executeSelect(query: String) : Seq[QuerySolution] = {
    val rs = queryExecution(query).execSelect()
    val tmp = ResultSetFormatter.toList(rs)
    val result = new collection.JavaConversions.JListWrapper(tmp)
    //val result : List[QuerySolution] = tmp

    return result
  }

}