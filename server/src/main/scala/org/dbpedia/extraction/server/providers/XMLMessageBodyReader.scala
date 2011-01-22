package org.dbpedia.extraction.server.providers

import util.control.ControlThrowable
import xml.{Elem, XML}
import javax.ws.rs.ext.{Provider, MessageBodyReader}
import javax.ws.rs.{Consumes, WebApplicationException}
import java.io.{IOException, InputStream}
import javax.ws.rs.core.{MediaType, MultivaluedMap}

/**
 * Used by the server to read incoming xml requests.
 */
@Provider
@Consumes(Array("application/xml", "text/xml"))
class XMLMessageBodyReader extends MessageBodyReader[Elem]
{
     override def isReadable(_type : java.lang.Class[_], genericType : java.lang.reflect.Type,
                             annotations : Array[java.lang.annotation.Annotation], mediaType : MediaType) : Boolean =
     {
         mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE) || mediaType.isCompatible(MediaType.TEXT_XML_TYPE)
     }

    @throws(classOf[IOException])
    @throws(classOf[WebApplicationException])
    override def readFrom(_type : java.lang.Class[Elem], genericType : java.lang.reflect.Type,
                          annotations : Array[java.lang.annotation.Annotation], mediaType : MediaType,
                          httpHeaders : MultivaluedMap[String, String], inputStream : InputStream) : Elem =
    {
        try
        {
            XML.load(inputStream)
        }
        catch
        {
            case ex : RuntimeException => throw ex
            case ex : IOException => throw ex
            case ex : Exception => throw new WebApplicationException(ex)
        }
    }
}
