package org.dbpedia.iri

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

/**
  * Created by chile on 01.10.17.
  *
  * The IRIBuilder contains definitions of valid segments of an IRI, defined as instances of IRISection (see below).
  * All IRI sections are defined as specified in RFC3987 (https://www.ietf.org/rfc/rfc3987.txt).
  * TODO The only exceptions to this rule are IP literals as host(e.g. for IPV6).
  * While all but one atomic sections are implemented as AtomicSection, the pct-encoded (percent encoded characters) section
  * is implemented as a simple heuristic, since it has to consider multiple characters in sequence.
  * Some variations of atomic sections as simple complex sections concerned with their receptiveness are subsumed (e.g. isegment & isegment-nz).
  */

object IRIBuilder{

  /**
    * DELINEATORS: defining available delineators of an IRI (e.g.: /,?,#,@...)
    */
  val slash = new SectionDelineator("slash", "/")
  val at = new SectionDelineator("at", "@")
  val colon = new SectionDelineator("colon", ":")
  val doubleSlash = new SectionDelineator("doubleSlash", "//")
  val hash = new SectionDelineator("hash", "#")
  val questionMark = new SectionDelineator("questionMark", "?")

  /**
    * ATOMIC IRI SECTIONS: These IRI sections are the base building blocks of an IRI
    */
  // port           = *DIGIT
  val port = new AtomicIRISection("port", IriCharacters.L_DIGIT, IriCharacters.H_DIGIT, "", "", 0, 1)
  // scheme         = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
  val scheme = new AtomicIRISection("scheme", IriCharacters.L_SCHEME, IriCharacters.H_SCHEME, "", ":", 1, 1)
  // iquery         = *( ipchar / iprivate / "/" / "?" )
  val query = new AtomicIRISection("query", IriCharacters.L_IQUERY, IriCharacters.H_IQUERY, "&", "#", 0, Integer.MAX_VALUE, percentEncodingAllowed = true)  //TODO occs and check if & is the only one
  // ifragment      = *( ipchar / "/" / "?" )
  val fragment = new AtomicIRISection("fragment", IriCharacters.L_IFRAGMENT, IriCharacters.H_IFRAGMENT, "&", "", 0, Integer.MAX_VALUE, percentEncodingAllowed = true)  //TODO occs and check if & is the only one
  // iuserinfo      = *( iunreserved / pct-encoded / sub-delims / ":" )
  val user = new AtomicIRISection("user", IriCharacters.L_IUSER, IriCharacters.H_IUSER, "", "@", 0, 1, percentEncodingAllowed = true)
  // ireg-name      = *( iunreserved / pct-encoded / sub-delims )
  val regName = new AtomicIRISection("regName", IriCharacters.L_IREGNAME, IriCharacters.H_IREGNAME, "", "", 0, Integer.MAX_VALUE, percentEncodingAllowed = true)
  // ipchar (isegment) = iunreserved / pct-encoded / sub-delims / ":" / "@"
  val segment = new AtomicIRISection("segment", IriCharacters.L_ISEG, IriCharacters.H_ISEG, "", "", 0, Integer.MAX_VALUE, percentEncodingAllowed = true)
  // isegment-nz    = 1*ipchar
  val segmentNz = new AtomicIRISection("segmentNz", IriCharacters.L_ISEG, IriCharacters.H_ISEG, "", "", 1, 1, percentEncodingAllowed = true)
  // isegment-nz-nc = 1*( iunreserved / pct-encoded / sub-delims / "@" )
  val segmentNzNc = new AtomicIRISection("segmentNzNc", IriCharacters.L_ISEGNC, IriCharacters.L_ISEGNC, "", "", 1, 1, percentEncodingAllowed = true)
  // IPv4address    = dec-octet "." dec-octet "." dec-octet "." dec-octet
  // (note: dec-octet is subsumed and implemented as postprocessing function here since used only with IP4)
  //TODO postprocessing validation function of IP4 needs some work
  val ip4 = new AtomicIRISection("ip4", IriCharacters.L_DIGIT, IriCharacters.H_DIGIT, ".", "", 4, 4, false, { digits =>
    if(digits.count(x => x.length > 0 && x.length <= 3 && (if (x.length == 3 && x.head.toInt > 2) false else true)) != 4)
      throw new IRISyntaxException("Not a valid IP4 host address!")
    digits
  })

  /**
    * COMPLEX IRI SECTIONS: combining multiple atomic and complex sections separated by delineators to more sophisticated sections.
    */
  // ipath-abempty  = *( "/" isegment )
  val pathAbEmpty = new ComplexSeqIRISection("pathAbEmpty", List(
    slash,
    segment
  ), 0, Integer.MAX_VALUE)
  // ipath-absolute = "/" [ isegment-nz *( "/" isegment ) ]
  val pathAbsOptions = new ComplexSeqIRISection("pathAbsOptions", List(
    slash,
    segmentNz,
    pathAbEmpty
  ), 0, Integer.MAX_VALUE)
  // ipath-absolute = "/" isegment-nz *( "/" isegment )  -> notice: the different min occurrences!
  val pathAbsolute = new ComplexSeqIRISection("pathAbsolute", List(
    slash,
    pathAbsOptions
  ))
  // ipath-noscheme = isegment-nz-nc *( "/" isegment )
  val pathNoScheme = new ComplexSeqIRISection("pathNoScheme", List(
    segmentNzNc,
    pathAbEmpty
  ))
  // ipath-rootless = isegment-nz *( "/" isegment )
  val pathNoRoot = new ComplexSeqIRISection("pathNoRoot", List(
    segmentNz,
    pathAbEmpty
  ))
  // ihost          = IP-literal / IPv4address / ireg-name
  val host = new ComplexAltIRISection("host", List(
    regName,
    ip4
    //TODO no IP Literals yet!!!
  ))
  // iauthority     = [ iuserinfo "@" ] ihost [ ":" port ]
  val authoritySection = new ComplexSeqIRISection("authoritySection", List[IRISection](
    new ComplexSeqIRISection("userWithAt", List(
      user,
      at
    ), 0, 1),
    host,
    new ComplexSeqIRISection("portWithColon", List(
      colon,
      port
    ), 0, 1)
  ))

  /**
    * ihier-part (first line) = "//" -> iauthority -> ipath-abempty
    */
  val authPathOrEmpty = new ComplexSeqIRISection("authPathOrEmpty", List(
    doubleSlash,
    authoritySection,
    pathAbEmpty
  ))

  /**
    * note: using complex alternatives section
    * ihier-part     = "//" iauthority ipath-abempty
    *                   / ipath-absolute
    *                   / ipath-rootless
    *                   / ipath-empty
    */
  val authHostPath = new ComplexAltIRISection("authHostPath", List(
    authPathOrEmpty,
    pathAbsolute,
    pathNoRoot,
    new ComplexSeqIRISection("empty", List(), 0, 0)              //empty!
  ))

  /**
    *    note: using complex alternatives section
    *    ipath          = ipath-abempty   ; begins with "/" or is empty
                        / ipath-absolute  ; begins with "/" but not "//"
                        / ipath-noscheme  ; begins with a non-colon segment
                        / ipath-rootless  ; begins with a segment
                        / ipath-empty     ; zero characters
    */
  val path = new ComplexAltIRISection("path", List(
    pathAbEmpty,
    pathAbsolute,
    pathNoRoot,
    pathNoScheme,
    new ComplexSeqIRISection("empty", List(), 0, 0)              //empty!
  ))

  /**
    * The final iri sections combining all other sections
    * IRI = scheme ":" ihier-part [ "?" iquery ] [ "#" ifragment ]
    */
  val iri = new ComplexSeqIRISection("iri", List(
    scheme,                                             //scheme
    new SectionDelineator("colon", ":"),                   //":"
    authHostPath,                                       //ihier-part
    new ComplexSeqIRISection("queryWithMark", List(
      questionMark,
      query                                             // query
    ), 0, 1),
    new ComplexSeqIRISection("fragmentWithMark", List(
      hash,
      fragment                                          // fragment
    ), 0, 1)
  ))
}

/**
  * IRI section interface
  */
trait IRISection{
  /**
    * traverse the provided string (starting at position x) as long as the section specification
    * recognises the sequential characters as part of this section
    * @param iri - the iri (or part of the iri) provided
    * @param position - start position of iri
    * @return - returns all consecutive occurrences of this section (e.g. multiple query segments divided by '&')
    */
  def traverse(iri: String, position: Int): List[String]

  /**
    * validates the given iri (part) against the specification of this section
    * @param iriPart
    * @return
    */
  def validate(iriPart: String): Boolean

  /**
    * @return the specified minimum number of successfully traversed section parts expected by traverse
    */
  def getMinOcc: Int

  /**
    * @return the specified maximum number of successfully traversed section parts expected by traverse
    */
  def getMaxOcc: Int

  /**
    * @return the name of this section
    */
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
      throw new IRISyntaxException("Too many occurrences of section found: " + res.size)
    if(res.size < minOcc)
      throw new IRISyntaxException("Too few occurrences of section found: " + res.size)
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
      case Success(_) => true
      case Failure(_) => false
    }
  }

  override def getMinOcc: Int = minOcc

  override def getMaxOcc: Int = maxOcc

  override def getName: String = this.name
}

class SectionDelineator(val name: String, val del: String) extends IRISection{

  override def traverse(iri: String, position: Int): List[String] = {
    if(iri == null)
      throw new IRISyntaxException("Deliminator was not found: " + del)
    for(i <- 0 until del.length){
      val chr = iri.charAt(position + i)
      if(del.charAt(i) != chr)
        throw new IRISyntaxException("Deliminator was not found: " + del)
    }
    List(del)
  }

  override def validate(iriPart: String): Boolean = {
    Try{traverse(iriPart, 0)} match{
      case Success(_) => true
      case Failure(_) => false
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
      throw new IRISyntaxException("Too many occurrences of section found: " + res.size)
    if(res.size < minOcc)
      throw new IRISyntaxException("Too few occurrences of section found: " + res.size)
    res
  }

  override def validate(iriPart: String): Boolean = {
    Try{traverse(iriPart, 0)} match{
      case Success(_) => true
      case Failure(_) => false
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
        case Failure(_) =>
      }
    }
    List()
  }

  override def traverse(iri: String, position: Int): List[String] = {
    val res = internalTraverse(iri, position)
    if(res.size > maxOcc)
      throw new IRISyntaxException("Too many occurrences of section found: " + res.size)
    if(res.size < minOcc)
      throw new IRISyntaxException("Too few occurrences of section found: " + res.size)
    res
  }

  override def validate(iriPart: String): Boolean = {
    Try{traverse(iriPart, 0)} match{
      case Success(_) => true
      case Failure(_) => false
    }
  }

  override def getMinOcc: Int = minOcc

  override def getMaxOcc: Int = maxOcc

  override def getName: String = this.name
}