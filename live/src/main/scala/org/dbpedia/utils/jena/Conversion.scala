package org.dbpedia.utils.jena

import org.dbpedia.extraction.destinations.Quad
import com.hp.hpl.jena.rdf.model.{ModelFactory, Model}
import com.hp.hpl.jena.datatypes.TypeMapper

/**
 * Created by IntelliJ IDEA.
 * User: raven
 * Date: Sep 21, 2010
 * Time: 3:08:24 PM
 * To change this template use File | Settings | File Templates.
 */

object Conversion
{
  /**
   * Converts an iterable of Quads to a Jena Model
   */
  def quadsToModel(quads : List[Quad]) : Model = {
    val result = ModelFactory.createDefaultModel
    val tm = TypeMapper.getInstance();

    quads.foreach(q => {

      val subject = result.createResource(q.subject)
      val predicate = result.createProperty(q.predicate)
      val value = q.value

      if(q.datatype != null) {
        if(q.datatype.uri == "http://www.w3.org/2001/XMLSchema#string") {
          val langTag = q.extractionContext.language.locale.getLanguage

          result.add(subject, predicate, value, langTag)
        } else {
          val rdfDataType = tm.getSafeTypeByName(q.datatype.toString)
          result.add(subject, predicate, value, rdfDataType)
        }

      } else {
          val obj = result.createResource(value)
        result.add(subject, predicate, obj)
      }

    })

    return result
  }
}