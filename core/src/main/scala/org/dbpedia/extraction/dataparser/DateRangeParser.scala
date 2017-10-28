package org.dbpedia.extraction.dataparser


import org.dbpedia.extraction.config.{ExtractionRecorder, RecordCause, RecordEntry}
import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.{Date, Language}
import org.dbpedia.extraction.wikiparser.{Node, PageNode, TemplateNode}

import scala.util.{Failure, Success, Try}
import scala.language.reflectiveCalls
import scala.reflect.ClassTag

/**
  * Created by chile on 15.10.17.
  *
  * This extractor deals wth templates representing a date range
  */
class DateRangeParser ( context : {
  def language : Language
  def ontology: Ontology
  def redirects : Redirects
  def recorder[T: ClassTag] : ExtractionRecorder[T] },
                       datatype : Datatype,
                       strict : Boolean = false) extends DateTimeParser(context, datatype, strict)
{
  require(datatype != null, "datatype != null")
  private val recorder = context.recorder[PageNode]

  def parseRange(node: Node): Option[ParseResult[(Date, Date)]] = {
    try
    {
      for( child @ TemplateNode(_,_,_,_) <- node.children;
           date <- catchTemplate(child))
      {
        return Some(ParseResult(date))
      }
    }
    catch
      {
        case ex : IllegalArgumentException  =>
          recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Warning, Language.getOrElse(language, Language.None), "Error while parsing date", ex))
        case ex : NumberFormatException =>
          recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Warning, Language.getOrElse(language, Language.None), "Error while parsing date", ex))
      }

    None
  }

  private def catchTemplate(node: TemplateNode): Option[(Date, Date)] = {

    val templateName = context.redirects.resolve(node.title).decoded.toLowerCase

    for (currentTemplate <- templates.filter(x => x._2.keySet.contains("year2")).get(templateName)) {

      var yearNum = currentTemplate.getOrElse("year", "")
      var monthNum = currentTemplate.getOrElse("month", "")
      var dayNum = currentTemplate.getOrElse("day", "")

      val date1 = super.getDateByParameters(node, yearNum, monthNum, dayNum) match{
        case Some(s) => s
        case None => return None
      }

      yearNum = currentTemplate.getOrElse("year2", "")
      monthNum = currentTemplate.getOrElse("month2", "")
      dayNum = currentTemplate.getOrElse("day2", "")

      val date2 = super.getDateByParameters(node, yearNum, monthNum, dayNum) match{
        case Some(s) => s
        case None =>
          if(node.keySet.size == 2)
            new Date(Some(date1.year.get + 1), None, None, date1.datatype)
          else {
            node.property("range") match{
              case Some(t) => Try{t.toPlainText.toInt} match{
                case Success(s) => new Date(Some(date1.year.get + s), None, None, date1.datatype)
                case Failure(_) => return None
              }
              case None => if(!node.keySet.contains("start") || !node.keySet.contains("end"))
                new Date(Some(date1.year.get + 1), None, None, date1.datatype)
              else
                return None
            }
          }
      }

      return Some(date1, date2)
    }
    None
  }
}
