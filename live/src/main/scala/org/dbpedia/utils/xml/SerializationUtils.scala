package org.dbpedia.utils.xml;


import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.DomDriver
import java.io._

/**
 * Utility methods for de-/serializing objects to streams and files.
 * 
 * @author Claus Stadler
 *
 */
object SerializationUtils
{
	def serializeXML(obj : Object, file : File) : Unit = {
    val out = new FileOutputStream(file)
		serializeXML(obj, out);
		out.close();
	}


	def serializeXML(obj : Object, out : OutputStream) : Unit = {
		val xstream = new XStream(new DomDriver());
		xstream.toXML(obj, out);
	}
	
	def deserializeXML(in : InputStream) : Object = {
		val xstream = new XStream(new DomDriver());
		val result = xstream.fromXML(in);
		return result;
  }

	def deserializeXML(file : File) : Object = {
    val in = new FileInputStream(file)
		val result = deserializeXML(in)
		in.close()
		return result
	}
}
