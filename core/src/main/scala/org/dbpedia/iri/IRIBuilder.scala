package org.dbpedia.iri

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

/**
  * Created by chile on 01.10.17.
  */
class IRIBuilder {

}

object IRIBuilder{

  val slash = new SectionDeliniator("slash", "/")
  val segment = new AtomicIRISection("segment", IriCharacters.L_ISEG, IriCharacters.H_ISEG, "", "", 0, Integer.MAX_VALUE, percentEncodingAllowed = true)
  val segmentNz = new AtomicIRISection("segmentNz", IriCharacters.L_ISEG, IriCharacters.H_ISEG, "", "", 1, 1, percentEncodingAllowed = true)
  val segmentNzNc = new AtomicIRISection("segmentNzNc", IriCharacters.L_ISEGNC, IriCharacters.L_ISEGNC, "", "", 1, 1, percentEncodingAllowed = true)
  val pathAbEmpty = new ComplexSeqIRISection("pathAbEmpty", List(
    slash,
    segment
  ), 0, Integer.MAX_VALUE)
  val pathAbsOptions = new ComplexSeqIRISection("pathAbsOptions", List(
    segmentNz,
    pathAbEmpty
  ), 0, Integer.MAX_VALUE)
  val pathAbsolute = new ComplexSeqIRISection("pathAbsolute", List(
    slash,
    pathAbsOptions
  ))
  val pathNoScheme = new ComplexSeqIRISection("pathNoScheme", List(
    segmentNzNc,
    pathAbEmpty
  ))
  val pathNoRoot = new ComplexSeqIRISection("pathNoRoot", List(
    segmentNz,
    pathAbEmpty
  ))
  val ip4 = new AtomicIRISection("ip4", IriCharacters.L_DIGIT, IriCharacters.H_DIGIT, ".", "", 4, 4, false, { digits =>
    if(digits.count(x => x.length > 0 && x.length <= 3 && (if (x.length == 3 && x.head.toInt > 2) false else true)) != 4)
      throw new Exception("")
    digits
  })
  val port = new AtomicIRISection("port", IriCharacters.L_DIGIT, IriCharacters.H_DIGIT, "", "", 0, 1)
  val scheme = new AtomicIRISection("scheme", IriCharacters.L_SCHEME, IriCharacters.H_SCHEME, "", ":", 1, 1)
  val query = new AtomicIRISection("query", IriCharacters.L_IQUERY, IriCharacters.H_IQUERY, "&", "#", 0, Integer.MAX_VALUE, percentEncodingAllowed = true)  //TODO occs and check if & is the only one
  val fragment = new AtomicIRISection("fragment", IriCharacters.L_IFRAGMENT, IriCharacters.H_IFRAGMENT, "&", "", 0, Integer.MAX_VALUE, percentEncodingAllowed = true)  //TODO occs and check if & is the only one

  val user = new AtomicIRISection("user", IriCharacters.L_IUSER, IriCharacters.H_IUSER, "", "@", 0, 1, percentEncodingAllowed = true)
  val regName = new AtomicIRISection("regName", IriCharacters.L_IREGNAME, IriCharacters.H_IREGNAME, "", "", 0, Integer.MAX_VALUE, percentEncodingAllowed = true)
  val host = new ComplexAltIRISection("host", List(
    regName,
    ip4
    //TODO no IP Literals yet!!!
  ))

  val authoritySection = new ComplexSeqIRISection("authoritySection", List[IRISection](
    new ComplexSeqIRISection("userWithAt", List(
      user,
      new SectionDeliniator("at", "@")
    ), 0, 1),
    host,
    new ComplexSeqIRISection("portWithColon", List(
      new SectionDeliniator("colon", ":"),
      port
    ), 0, 1)
  ))

  val authPathOrEmpty = new ComplexSeqIRISection("authPathOrEmpty", List(
    new SectionDeliniator("doubleSlash", "//"),
    authoritySection,
    pathAbEmpty
  ))

  val authHostPath = new ComplexAltIRISection("authHostPath", List(
    authPathOrEmpty,
    pathAbsolute,
    pathNoRoot,
    new ComplexSeqIRISection("empty", List(), 0, 0)              //empty!
  ))

  val path = new ComplexAltIRISection("path", List(
    pathAbEmpty,
    pathAbsolute,
    pathNoRoot,
    new ComplexSeqIRISection("empty", List(), 0, 0)              //empty!
  ))

  val iri = new ComplexSeqIRISection("iri", List(
    scheme,                                             //scheme
    new SectionDeliniator("colon", ":"),                   //":"
    authHostPath,                                       //ihier-part
    new ComplexSeqIRISection("queryWithMark", List(
      new SectionDeliniator("questionmark", "?"),                 // ?
      query                                             // query
    ), 0, 1),
    new ComplexSeqIRISection("fragmentWithMark", List(
      new SectionDeliniator("hash", "#"),                 // #
      fragment                                          // fragment
    ), 0, 1)
  ))
}

trait IRISection{
  def traverse(iri: String, position: Int): List[String]

  def validate(iriPart: String): Boolean

  def getMinOcc: Int

  def getMaxOcc: Int

  def getName: String
}

class AtomicIRISection (
   val name: String,
   val lowMask: Long,
   val highMask: Long,
   val deliniators: String,
   val finishers: String,
   val minOcc: Int = 0,
   val maxOcc: Int = Integer.MAX_VALUE,
   val percentEncodingAllowed: Boolean = false,
   val postCondition: List[String] => List[String] = {x => x})
  extends IRISection{
  private val deliChars = deliniators.toCharArray
  private val finChars = deliniators.toCharArray

  override def traverse(iri: String, position: Int): List[String] ={
    val res = if(iri != null)
      internalTraverse(iri, position, List())
    else
      List()
    if(res.size > maxOcc)
      throw new Exception("Too many occurrences of section found: " + res.size)
    if(res.size < minOcc)
      throw new Exception("Too few occurrences of section found: " + res.size)
    postCondition(res)
  }

  def internalTraverse(iri: String, position: Int, partRes: List[String] = List()): List[String] ={
    if(partRes.size == maxOcc)
      return partRes

    val res = new StringBuilder()
    var pos = position
    var current = iri.charAt(pos)
    var percentCharsLeft = 0
    var escaped = "%"
    while(!deliChars.contains(current) && !finChars.contains(current)) {
      if (IriCharacters.`match`(current, lowMask, highMask)) {
        if (percentCharsLeft > 0) {
          escaped = escaped + current
          percentCharsLeft = percentCharsLeft - 1
          if(percentCharsLeft == 0) {
            res.append(IriCharacters.decode(escaped))
          }
        }
        else
          res.append(current)
      }
      else if(percentEncodingAllowed && current == '%'){
        percentCharsLeft = 2
        escaped = "%"
      }
      else
        return partRes ::: List(res.toString())

      if(pos == iri.length -1)
        return partRes ::: List(res.toString())
      pos = pos+1
      current = iri.charAt(pos)
    }

    if(deliChars.contains(current)) {
      res.append(current)
      pos = pos + 1
      internalTraverse(iri, pos, partRes ::: List(res.toString()))
    }
    else
      partRes ::: List(res.toString())
  }

  override def validate(iriPart: String): Boolean = {
    Try{traverse(iriPart, 0)} match{
      case Success(s) => true
      case Failure(f) => false
    }
  }

  override def getMinOcc: Int = minOcc

  override def getMaxOcc: Int = maxOcc

  override def getName: String = this.name
}

class SectionDeliniator(val name: String, val del: String) extends IRISection{

  override def traverse(iri: String, position: Int): List[String] = {
    if(iri == null)
      throw new Exception("Deliminator was not found: " + del)
    for(i <- 0 until del.length){
      val chr = iri.charAt(position + i)
      if(del.charAt(i) != chr)
        throw new Exception("Deliminator was not found: " + del)
    }
    List(del)
  }

  override def validate(iriPart: String): Boolean = {
    Try{traverse(iriPart, 0)} match{
      case Success(s) => true
      case Failure(f) => false
    }
  }

  override def getMinOcc: Int = 1

  override def getMaxOcc: Int = 1

  override def getName: String = this.name
}

class ComplexSeqIRISection(val name: String, val sections: List[IRISection], val minOcc: Int = 0, val maxOcc: Int = Integer.MAX_VALUE) extends IRISection{
  def internalTraverse(iri: String, position: Int): List[String] = {
    val res = new ListBuffer[String]()
    var pos = position
    for (section <- sections) {
      Try {
        section.traverse(iri, pos)
      } match{
        case Success(s) =>
          res.appendAll(s)
          pos = pos + s.map(_.length).sum
        case Failure(f) => if(section.getMinOcc > 0)
          throw f
      }
    }
    res.toList
  }

  override def traverse(iri: String, position: Int): List[String] = {
    val res = if(iri != null)
      internalTraverse(iri, position)
    else
      List()
    if(res.size > maxOcc)
      throw new Exception("Too many occurrences of section found: " + res.size)
    if(res.size < minOcc)
      throw new Exception("Too few occurrences of section found: " + res.size)
    res
  }

  override def validate(iriPart: String): Boolean = {
    Try{traverse(iriPart, 0)} match{
      case Success(s) => true
      case Failure(f) => false
    }
  }

  override def getMinOcc: Int = minOcc

  override def getMaxOcc: Int = maxOcc

  override def getName: String = this.name
}

class ComplexAltIRISection(val name: String, val sections: List[IRISection], val minOcc: Int = 0, val maxOcc: Int = Integer.MAX_VALUE) extends IRISection{
  def internalTraverse(iri: String, position: Int): List[String] = {
    for(section <- sections){
      Try{
        section.traverse(iri, position)
      } match{
        case Success(r) => return r
        case Failure(f) =>
      }
    }
    List()
  }

  override def traverse(iri: String, position: Int): List[String] = {
    val res = internalTraverse(iri, position)
    if(res.size > maxOcc)
      throw new Exception("Too many occurrences of section found: " + res.size)
    if(res.size < minOcc)
      throw new Exception("Too few occurrences of section found: " + res.size)
    res
  }

  override def validate(iriPart: String): Boolean = {
    Try{traverse(iriPart, 0)} match{
      case Success(s) => true
      case Failure(f) => false
    }
  }

  override def getMinOcc: Int = minOcc

  override def getMaxOcc: Int = maxOcc

  override def getName: String = this.name
}