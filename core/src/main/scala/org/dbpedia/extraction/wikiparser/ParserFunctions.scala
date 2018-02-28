package org.dbpedia.extraction.wikiparser

import org.mariuszgromada.math.mxparser.Expression

import scala.util.{Failure, Success, Try}


object ParserFunctions {

  def ifFunc(test: Node, t: Node, f: Node = null): Option[Node] ={
    if(test == null)
      return Option(f)
    val b = test match{
      case pfp: ParserFunctionParameterNode => ! testForEmptyNodeSeq(pfp.children)
      case txt: TextNode => txt.text.trim.nonEmpty
      case _: Node => true
    }
    if(b)
      Option(t)
    else
      Option(f)
  }

  def exprFunc(test: String): Try[Double] ={
    Try{new Expression(test).calculate()}
  }

  private def isEqual(test1: String, test2: String): Boolean = {
    assert(test1 != null && test2 != null, "Test strings are null!")
    exprFunc(test1 + " == " + test2) match {
      case Success(s) => if (s == 0d)
        false
      else
        true
      case Failure(_) => if (test1.trim == test2.trim)
        true
      else
        false
    }
  }

  def ifEqFunc(test1: String, test2: String, t: Node, f: Node = null): Option[Node] = {
    if (test1 == null && test1 == null)
      return Option(t)
    if (test1 == null || test2 == null)
      return Option(f)

    if (isEqual(test1, test2))
      Option(t)
    else
      Option(f)
  }

  def ifErrorFunc(func: (String) => Try[_], test: String, t: Node, f: Node = null): Option[Node] = {
    func(test) match {
      case Failure(_) => Option(t)
      case Success(_) => Option(f)
    }
  }

  def ifExprFunc(test: String, t: Node, f: Node = null): Option[Node] = {
    if (test == null || test.trim.isEmpty)
      return Option(f)

    exprFunc(test) match {
      case Success(_) => Option(t)
      case Failure(_) => Option(f)
    }
  }

  def switchFunc(test: String, default: Option[String], cases: (String, Option[String])*): Option[String] = {
    assert(test != null, "Test string was null!")
    assert(cases.map(x => Option(x._1)).toSet.size == cases.size, "Switch cases are not unique!")

    val list = cases.toList
    var foundEqual = false
    for (i <- list.indices) {
      val (comp, res) = list(i)
      if (!foundEqual) {
        val t2 = Option(comp).getOrElse("")
        if (isEqual(test, t2)) {
          res match {
            case Some(s) => return Some(s)
            case None =>
          }
          foundEqual = true
        }
      }
      else {
        res match {
          case Some(s) => return Some(s)
          case None =>
        }
      }
    }
    default
  }

  def testForEmptyNodeSeq(nodes: List[Node]): Boolean ={
    val textNodes = nodes.filter(x => x.isInstanceOf[TextNode])
    if(nodes.size > textNodes.size)
      return false  //list contains non-TextNodes -> not empty
    textNodes.foreach(n => if(n.asInstanceOf[TextNode].text.trim.nonEmpty) return false)
    true
  }
}
