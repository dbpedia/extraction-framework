package org.dbpedia.extraction.server.providers

import javax.ws.rs.core.{MediaType, MultivaluedMap}
import javax.ws.rs.ext.{Provider, MessageBodyWriter}
import javax.ws.rs.{Produces, WebApplicationException}
import xml.{NodeBuffer, Utility, NodeSeq, Node}
import java.io.{OutputStreamWriter, IOException, OutputStream}
import java.util.Collections.singletonList

/**
 * Used by the server to write xml responses.
 */
@Provider
@Produces(Array("application/xml", "text/xml", "text/xsl", "application/xhtml+xml", "application/rdf+xml", "application/xslt+xml"))
class XMLMessageBodyWriter extends MessageBodyWriter[AnyRef]
{
    override def isWriteable(_type : java.lang.Class[_], genericType : java.lang.reflect.Type,
                             annotations : Array[java.lang.annotation.Annotation], mediaType : MediaType) : Boolean =
    {
        (classOf[NodeSeq] isAssignableFrom _type) || (classOf[NodeBuffer] isAssignableFrom _type)
    }

    override def getSize(xml : AnyRef, _type : java.lang.Class[_], genericType : java.lang.reflect.Type,
                         annotations : Array[java.lang.annotation.Annotation], mediaType : MediaType): Long =
    {
        -1
    }

    @throws(classOf[IOException])
    @throws(classOf[WebApplicationException])
    override def writeTo(xml : AnyRef, _type : java.lang.Class[_], genericType : java.lang.reflect.Type,
                         annotations : Array[java.lang.annotation.Annotation], mediaType : MediaType,
                         httpHeaders : MultivaluedMap[String, Object] , entityStream : OutputStream) : Unit =
    {
        val writer = new OutputStreamWriter(entityStream, "UTF-8")
        writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>")

        var main = mediaType.getType
        var sub = mediaType.getSubtype
        
        // IE cannot handle application/xhtml+xml directly
        if(main == "application" && sub == "xhtml+xml") { main = "text"; sub = "html" }
        
        httpHeaders.put("Content-Type", singletonList(main+"/"+sub+"; charset=UTF-8"))
            
        xml match
        {
            case node : NodeBuffer => writer.write(toString(node))
            case node : NodeSeq => writer.write(toString(node))
        }

        writer.flush()
    }
    
    private def toString( node : Seq[Node] ) : String =
    {
        val sb = new StringBuilder
        Utility.sequenceToXML(node, sb = sb, minimizeTags = true)
        sb.toString
    }
}
