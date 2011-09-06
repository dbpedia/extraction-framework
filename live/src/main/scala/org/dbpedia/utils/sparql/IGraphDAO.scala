package org.dbpedia.utils.sparql

import com.hp.hpl.jena.query.QuerySolution
import com.hp.hpl.jena.rdf.model.Model

/**
 * Created by IntelliJ IDEA.
 * User: raven
 * Date: Sep 8, 2010
 * Time: 12:19:28 PM
 * To change this template use File | Settings | File Templates.
 */

trait IGraphDAO
{
  def executeSelect(query : String) : Seq[QuerySolution]
	def executeAsk(query : String) : Boolean
	def executeConstruct(query : String) : Model

	def defaultGraphName() : Option[String]
}