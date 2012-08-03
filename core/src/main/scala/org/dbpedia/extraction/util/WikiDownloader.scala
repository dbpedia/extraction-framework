package org.dbpedia.extraction.util

import java.io.{File,FileOutputStream,IOException,InputStreamReader,OutputStreamWriter}
import java.net.URL
import java.nio.charset.Charset

import org.dbpedia.extraction.wikiparser.Namespace

import javax.xml.namespace.QName
import javax.xml.stream.{XMLEventFactory,XMLEventReader,XMLInputFactory,XMLOutputFactory}

import org.dbpedia.extraction.util.RichStartElement.richStartElement

/**
 * Downloads all pages for a given list of namespaces from api.php and transforms them
 * into the format of the dump files (because XMLSource understands that format).
 * 
 * TODO: extend this class a bit and replace the XML-handling code in WikiApi.
 */
class WikiDownloader(val apiUrl : String) {
  
  // TODO: get the charset from the URL? Difficult - writing starts before reading.
  val charset = Charset.forName("UTF-8")
  
  // newInstance() is deprecated, but the newer methods don't exist in many JDK 6 versions
  val inFactory = XMLInputFactory.newInstance
  val outFactory = XMLOutputFactory.newInstance
  val events = XMLEventFactory.newInstance
  
  def buildURL(namespace : Namespace, gapfrom : String) : URL = 
  {
    var sb = new StringBuilder
    sb append apiUrl
    sb append "?action=query&generator=allpages&prop=revisions&rvprop=ids|content|timestamp&format=xml"
    sb append "&gapnamespace=" append namespace.code append "&gaplimit=50"
    if (gapfrom != null) sb append "&gapfrom=" append gapfrom.replace(' ', '_')
    // I'm not sure what kind of escaping URL is doing. Seems ok if we just replace spaces.
    new URL(sb.toString)
  }

  def download(file : File, namespaces : Namespace *) : Unit =
  {
    val stream = new FileOutputStream(file)
    try
    {
      val writer = new OutputStreamWriter(stream, charset)
      val xmlOut = outFactory.createXMLEventWriter(writer)
      val out = new XMLEventBuilder(xmlOut, events)
      out.document(charset.name){
        // TODO: the generated file is probably not valid according to this schema. 
        out.elementNS("mediawiki", "http://www.mediawiki.org/xml/export-0.6/") {
          for (namespace <- namespaces) addPages(namespace, out)
        }
      }
      xmlOut.close
    }
    finally stream.close
  }
    
  def addPages(namespace : Namespace, out : XMLEventBuilder) : Unit =
  {
    var gapfrom = ""
    do
    {
      val url = buildURL(namespace, gapfrom)
      gapfrom = null
      
      val stream = url.openStream
      try
      {
        val reader = new InputStreamReader(stream, charset)
        val xmlIn = inFactory.createXMLEventReader(reader)
        val in = new XMLEventAnalyzer(xmlIn)
        in.document { _ =>
          in.element("api") { _ =>
            in.ifElement("error") { error => throw new IOException(error attr "info") }
            in.ifElement("query-continue") { _ =>
              in.element("allpages") { allpages => gapfrom = allpages attr "gapfrom" } 
            }
            in.ifElement("query") { _ => // note: there's no <query> element if the namespace contains no pages
              in.element("pages") { _ =>
                in.elements("page") { page =>
                  in.element("revisions") { _ =>
                    in.element("rev") { rev =>
                      in.text { text =>
                        out.element("page") {
                          out.element("title") { out.text( page attr "title" ) }
                          out.element("ns") { out.text( page attr "ns" ) }
                          out.element("id") { out.text( page attr "pageid" ) }
                          out.element ("revision") {
                            out.element ("id") { out.text( rev attr "revid" ) }
                            out.element ("timestamp") { out.text( rev attr "timestamp" ) }
                            out.element ("text") { out.text(text) }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        xmlIn.close
      }
      finally stream.close
    } while (gapfrom != null)
  }
  
}
