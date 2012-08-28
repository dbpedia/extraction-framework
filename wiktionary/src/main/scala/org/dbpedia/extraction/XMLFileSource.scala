package org.dbpedia.extraction

import collection.JavaConversions._
import sources.{WikiPage, WikipediaDumpParser, Source}
import wikiparser.WikiTitle
import org.springframework.core.io.Resource
import java.io.InputStreamReader

/**
 * @author Sebastian Hellmann <hellmann@informatik.uni-leipzig.de>
 */

class XMLFileSource(resource: Resource, filter: WikiTitle => Boolean) extends Source
{
  def this(resource: Resource, namespaceNumbers: java.util.List[java.lang.Integer]) = {
    this (resource, title => {
      //val ret = false;
      //JListWrapper(namespaceNumbers).contains(new java.lang.Integer(title.namespace))
      true
    })
  }


  override def foreach[U](proc: WikiPage => U): Unit =
  {
    val stream = new InputStreamReader(resource.getInputStream, "UTF-8")
    try new WikipediaDumpParser(stream, null, filter.asInstanceOf[WikiTitle => java.lang.Boolean], proc).run()
    finally stream.close()
  }

  override def hasDefiniteSize = true
}