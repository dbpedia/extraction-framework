package org.dbpedia.extraction.util

import java.io.StringWriter

import org.openrdf.model.{Resource, Value, Model}
import org.openrdf.model.impl.{StatementImpl, LiteralImpl, URIImpl, TreeModel}
import org.openrdf.rio.{RioSetting, Rio, RDFFormat, WriterConfig}
import org.openrdf.rio.helpers.{JSONLDMode, JSONLDSettings, BasicWriterSettings}
import scala.collection.JavaConversions

/**
  * Created by Chile on 2/16/2016.
  */
object OpenRdfUtils {
  def convertToOpenRdfModel(jena: com.hp.hpl.jena.rdf.model.Model): org.openrdf.model.Model =
  {
    val retModel = new TreeModel()

    for(zw <- JavaConversions.asScalaSet(jena.getNsPrefixMap.entrySet()))
    {
      retModel.setNamespace(zw.getKey, zw.getValue)
    }

    val miter = jena.listStatements()
    while(miter.hasNext)
    {
      val stmt = miter.next()
      val zw = new StatementImpl(convertResource(stmt.getSubject), new URIImpl(stmt.getPredicate.getURI), convertObject(stmt.getObject))
      retModel.add(zw)
    }
    retModel
  }

  private def convertResource(res: com.hp.hpl.jena.rdf.model.Resource): Resource =
  {
    if(res.isURIResource)
      new URIImpl(res.getURI)
    if(res.isAnon)
      new org.openrdf.model.impl.BNodeImpl(res.asNode().getBlankNodeId.getLabelString)
    new URIImpl(res.getURI)
  }

  private def convertObject(obj: com.hp.hpl.jena.rdf.model.RDFNode): Value =
  {
    if(obj.isResource)
      return convertResource(obj.asInstanceOf[com.hp.hpl.jena.rdf.model.Resource])
    if(obj.isLiteral) {
      val lit = obj.asInstanceOf[com.hp.hpl.jena.rdf.model.Literal]
      if (lit.getDatatype != null)
        return new LiteralImpl(lit.getString, new URIImpl(lit.getDatatypeURI))
      if(lit.getLanguage.trim.nonEmpty)
        return new LiteralImpl(lit.getString, lit.getLanguage)
      return new LiteralImpl(lit.getString)
    }
    null
  }

  def writeSerialization(m: Model, serialization: RDFFormat): String = {
      val sw: StringWriter = new StringWriter
      val conf: WriterConfig = new WriterConfig
      conf.set(BasicWriterSettings.PRETTY_PRINT.asInstanceOf[RioSetting[Boolean]], true)
      if (serialization eq RDFFormat.JSONLD)
        conf.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT)
      Rio.write(m, sw, serialization, conf)
      sw.close
      sw.toString
  }
}
