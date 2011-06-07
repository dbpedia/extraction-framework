package org.dbpedia.utils.sparql

import java.lang.String
import java.security.MessageDigest
import com.hp.hpl.jena.query.QuerySolution
import java.net.URLEncoder
import org.dbpedia.utils.xml.SerializationUtils
import java.io.File


/**
 * Created by IntelliJ IDEA.
 * User: raven
 * Date: Sep 10, 2010
 * Time: 2:45:30 PM
 * To change this template use File | Settings | File Templates.
 */

class CachingGraphDAO(val decoratee : HTTPGraphDAO, val basePath : String)
  extends IGraphDAO
{
  def md5SumString(bytes : Array[Byte]) : String = {
    val md5 = MessageDigest.getInstance("MD5")
    md5.reset()
    md5.update(bytes)

    md5.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft(""){_ + _}
  }


  def defaultGraphName() = decoratee.defaultGraphName

  def executeConstruct(query: String) = decoratee.executeConstruct(query)

  def executeAsk(query: String) = decoratee.executeAsk(query)

  def executeSelect(query: String) : Seq[QuerySolution] = {
    println("Query is: " + query)

    val file = cacheFile(decoratee.serviceName, decoratee.defaultGraphName, query)

    cacheLookup(file, classOf[Seq[QuerySolution]]) match {
      case Some(obj) => return obj
      case None => {
        val tmp = decoratee.executeSelect(query)
        cacheWrite(file, tmp)
        return tmp
      }
    }
  }

  def cacheFile(serviceName : String, graphName : Option[String], query : String) : File = {
    // Create the directory
    var dir = basePath + "/" + URLEncoder.encode(serviceName, "UTF-8") + "/"

    graphName match {
      case Some(name) => dir += URLEncoder.encode(name, "UTF-8") + "/"
      case None => dir += "default/"
    }

    val file = new File("" + dir + md5SumString(query.getBytes))

    return file
  }

	def cacheLookup[T](file : File, resultType : Class[T]) : Option[T] = {
    if(file.exists) {
      try {
        println("Cache hit for: " + file);
        val tmp = SerializationUtils.deserializeXML(file)
        tmp match {
          case v : T => return Some(v)
          case _ => return None
        }
      }
      catch {
        case e => {
          println("Corrupted cache - deleting file")
          file.delete
        }
      }
    }

    return None
  }


	def cacheWrite(file : File, obj : Object) : Unit = {
		println("Creating cache file for: " + file)
		file.getParentFile.mkdirs

		SerializationUtils.serializeXML(obj, file)
	}
}