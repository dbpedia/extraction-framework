package org.dbpedia.extraction.nif

import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.WikiPage
import org.jsoup.nodes.{Document, Element, Node}
import scala.collection.convert.decorateAsScala._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.language.reflectiveCalls

/**
  * Created by Chile on 1/19/2017.
  */
class WikipediaNifExtractorRest(
    context : {
      def ontology : Ontology
      def language : Language
      def configFile : Config
    },
    wikiPage: WikiPage
  ) extends WikipediaNifExtractor(context ,wikiPage) {


  /**
    * subtracts the relevant text
    * @param html
    * @return list of sections
    */
  override def getRelevantParagraphs (html: String): mutable.ListBuffer[PageSection] = {

    val tocMap = new mutable.ListBuffer[PageSection]()
    val doc: Document = getJsoupDoc(html)

    var nodes = doc.select("body").first.childNodes.asScala

    val currentSection = new ListBuffer[Int]()                  //keeps track of section number
    currentSection.append(0)                                    //initialize on abstract section

    def getSection(currentNodes : scala.collection.mutable.Buffer[Node]) : Unit = {
      //look for the next <h> tag

      var subnodes = currentNodes.head.childNodes().asScala
      subnodes = subnodes.dropWhile(currentNodes => !currentNodes.nodeName().matches("h\\d") && !currentNodes.nodeName().matches("section"))
      var processEnd=false
      while (subnodes.nonEmpty && !processEnd) {
        if (subnodes.head.nodeName().matches("h\\d")) {
          val title = subnodes.headOption
          processEnd=super.isWikiPageEnd(subnodes.head)

          title match {

            case Some(t) if super.isWikiNextTitle(t) && !processEnd =>

              //calculate the section number by looking at the <h2> to <h4> tags
              val depth = Integer.parseInt(t.asInstanceOf[org.jsoup.nodes.Element].tagName().substring(1)) - 1
              if (currentSection.size < depth) //first subsection
                currentSection.append(1)
              else {
                //delete last entries depending on the depth difference to the last section
                val del = currentSection.size - depth + 1
                val zw = currentSection(currentSection.size - del)
                currentSection.remove(currentSection.size - del, del)
                //if its just another section of the same level -> add one
                if (currentSection.size == depth - 1)
                  currentSection.append(zw + 1)
              }
              subnodes = subnodes.drop(1)
              val section = new PageSection(
                //previous section (if on same depth level
                prev = currentSection.last match {
                  case x: Int if x > 1 => tocMap.lastOption
                  case _ => None
                },
                //super section
                top = tocMap.find(x => currentSection.size > 1 && x.ref == currentSection.slice(0, currentSection.size - 1).map(n => "." + n.toString).foldRight("")(_ + _).substring(1)),
                next = None,
                sub = None,
                id = t.attr("id"),
                title = t.asInstanceOf[Element].text(),
                //merge section numbers separated by a dot
                ref = currentSection.map(n => "." + n.toString).foldRight("")(_ + _).substring(1),
                tableCount = 0,
                equationCount = 0,
                //take all following tags until you hit another title or end of content
                content = Seq(t) ++ subnodes.takeWhile(node => !node.nodeName().matches("h\\d") && !node.nodeName().matches("section"))
              )
              tocMap.append(section)
            case None => processEnd=true
            case _ => processEnd=true
          }
        } else if (subnodes.head.nodeName().matches("section")) {
          getSection(subnodes)
          subnodes =  subnodes.drop(1)
        }
        subnodes = subnodes.dropWhile(node => !node.nodeName().matches("h\\d") && !node.nodeName().matches("section"))
      }
    }

    val abstractSect=doc.select("body").select("section").first.childNodes.asScala //get first section
    val ab = abstractSect.filter(node => node.nodeName() == "p") //move cursor to abstract

    nodes = nodes.drop(1)

    tocMap.append(new PageSection(                     //save abstract (abstract = section 0)
      prev = None,
      top = None,
      next = None,
      sub = None,
      id = "abstract",
      title = "abstract",
      ref = currentSection.map(n => "." + n.toString).foldRight("")(_+_).substring(1),
      tableCount=0,
      equationCount = 0,
      content = ab
    ))

    if(!abstractsOnly) {
      while (nodes.nonEmpty) {
        getSection(nodes)
        nodes =  nodes.drop(1)
      }
    }
    tocMap
  }
}
