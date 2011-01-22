package org.dbpedia.extraction.server.providers

import javax.ws.rs.core.{MediaType, MultivaluedMap}
import javax.ws.rs.ext.{Provider, MessageBodyWriter}
import javax.ws.rs.{Produces, WebApplicationException}
import xml.{NodeBuffer, PrettyPrinter, NodeSeq}
import java.io.{OutputStreamWriter, IOException, OutputStream}

/**
 * Used by the server to write xml responses.
 */
@Provider
@Produces(Array("application/xml", "text/xml", "application/xhtml+xml", "application/rdf+xml", "application/xslt+xml"))
class XMLMessageBodyWriter extends MessageBodyWriter[AnyRef]
{
    override def isWriteable(_type : java.lang.Class[_], genericType : java.lang.reflect.Type,
                             annotations : Array[java.lang.annotation.Annotation], mediaType : MediaType) : Boolean =
    {
        _type.getName == classOf[scala.xml.Elem].getName || _type.getName == classOf[scala.xml.NodeBuffer].getName
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

        if(mediaType.getSubtype == "xhtml+xml")
        {
            //TODO IE cannot handle application/xhtml+xml directly
            val contentType : java.util.List[java.lang.Object] = new java.util.ArrayList[java.lang.Object]()
            contentType.add("text/html")
            httpHeaders.put("Content-Type", contentType)

            xml match
            {
                case buffer : NodeBuffer => for(node <- buffer) writer.write(node.toString)
                case node : NodeSeq => writer.write(node.toString)
            }

            writer.flush()
        }
        else
        {
//TODO pretty printing disabled, since PrettyPrinter hangs for some cases (notably when formatting the ontology)
//            val formatter = new PrettyPrinter(width = 300, step = 2)
//
//            val text = xml match
//            {
//                case buffer : NodeBuffer => formatter.formatNodes(buffer)
//                case node : NodeSeq =>
//                {
//                    formatter.format(node)
//                }
//            }
//
//            writer.write(text)

            xml match
            {
                case buffer : NodeBuffer => for(node <- buffer) writer.write(node.toString)
                case node : NodeSeq => writer.write(node.toString)
            }

            writer.flush()
        }
    }
}
