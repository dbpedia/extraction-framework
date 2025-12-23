package org.dbpedia.extraction.util

import java.io.{File, IOException, InputStream, OutputStream}
import java.net.{HttpURLConnection, URL}

import org.apache.commons.io.FileUtils
import org.dbpedia.iri.{IRI, UriUtils}

import scala.util.{Failure, Success, Try}

/**
  * Provides the same flexibility as RichFile for web resources
  * No output stream available!
  *
  * Created by Chile on 1/30/2017.
  */
class RichWebResource(targetString: String)  extends FileLike[IRI] {

  //TODO - defferentiating between Directories and Files?

  val uri: IRI = UriUtils.createURI(targetString).get
  /**
    * @return file name, or null if file path has no parts
    */
  override def name: String = uri.getPath

  override def resolve(name: String): Try[IRI] = Try{IRI.create(uri.resolve(name)).getOrElse(throw new Exception("IRI validation failed for: " + uri.resolve(name)))}

  override def names: List[String] = List()

  override def list: List[IRI] = List()

  override def exists: Boolean = RichWebResource.testURL(uri.toURL)

  @throws[IOException]("if file does not exist or cannot be deleted")
  override def delete(recursive: Boolean): Unit = throw new IOException("URIs cannot be deleted")

  override def size(): Long = RichWebResource.getFileSize(uri.toURL)

  override def isFile: Boolean = false

  override def isDirectory: Boolean = false

  override def hasFiles: Boolean = false

  override def inputStream(): InputStream = uri.toURL.openStream()

  override def outputStream(append: Boolean): OutputStream = throw new IOException("URIs will provide no output stream")

  override def getFile: File = {
    val file = new File(uri.toURI)
    FileUtils.copyURLToFile(uri.toURL, file)
    file
  }

  override def toString: String = targetString
}

object RichWebResource{

  def testURL(url: URL): Boolean = {
    Try{
      val connection = getURLConnection(url)
      connection.connect()
      assert(HttpURLConnection.HTTP_OK == connection.getResponseCode)
    } match{
      case Failure(e) => false
      case Success(s) => true
    }
  }

  def getURLConnection(url: URL): HttpURLConnection = {
    url.openConnection().asInstanceOf[HttpURLConnection]
  }

  def getFileSize(url: URL): Int = {
    val connection = getURLConnection(url)
    Try {
      //connection.connect()
      connection.setRequestMethod("HEAD")
      connection.getInputStream
      connection.getContentLength
    } match{
      case Success(s) => s
      case Failure(e) => -1
    }
  }
}
