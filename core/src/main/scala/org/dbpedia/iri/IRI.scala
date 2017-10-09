package org.dbpedia.iri

import java.net.{URISyntaxException, URL}
import java.util
import java.util.logging.{Level, Logger}

import org.apache.jena.iri.impl.IRIFactoryImpl
import org.apache.jena.iri.{IRIFactory, Violation}

/**
  * Created by chile on 30.09.17.
  * Overrides all functions using AbsIRIImpl.getCooked which is not implemented and returns the raw results instead
  */
class IRI(iri: org.apache.jena.iri.IRI) extends org.apache.jena.iri.IRI{

  private val logger = Logger.getLogger(getClass.getName)

  def this(iriString: String) = this(IRI.iriFactory.construct(iriString))
  def this(uri: URI) = this(IRI.iriFactory.construct(uri))

  /**
    * Tells whether or not this URI is opaque.
    *
    * <p> A URI is opaque if, and only if, it is absolute and its
    * scheme-specific part does not begin with a slash character ('/').
    * An opaque URI has a scheme, a scheme-specific part, and possibly
    * a fragment; all other components are undefined. </p>
    *
    * @return  <tt>true</tt> if, and only if, this URI is opaque
    */
  def isOpaque: Boolean = this.isAbsolute && this.getRawPath == null

  override def getUserinfo: String = {
    if(iri.getRawUserinfo == null)
      return null
    if(!IRIBuilder.user.validate(iri.getRawUserinfo))
      logger.log(Level.WARNING, "User info is not valid: " + iri)
      iri.getRawUserinfo
  }

  override def getAuthority: String = {
    if(!IRIBuilder.authoritySection.validate(iri.getRawAuthority))
      logger.log(Level.WARNING, "Authority is not valid: " + iri)
      iri.getRawAuthority
  }

  override def getPath: String = {
    if(iri.getRawPath == null)
      return null
    if(!IRIBuilder.path.validate(iri.getRawPath))
      logger.log(Level.WARNING, "Path is not valid: " + iri)
    iri.getRawPath
  }

  override def getFragment: String = {
    if(iri.getRawFragment == null)
      return null
    if(!IRIBuilder.fragment.validate(iri.getRawFragment))
      logger.log(Level.WARNING, "Fragment is not valid: " + iri)
    iri.getRawFragment
  }

  override def getHost: String = {
    if(!IRIBuilder.host.validate(iri.getRawHost))
      logger.log(Level.WARNING, "Host is not valid: " + iri)
      iri.getRawHost
  }

  override def getQuery: String = {
    if(iri.getRawQuery == null)
      return null
    if(!IRIBuilder.query.validate(iri.getRawQuery))
      logger.log(Level.WARNING, "Query is not valid: " + iri)
      iri.getRawQuery
  }

  override def toString: String = {
    val sb = new StringBuilder()
    sb.append(getScheme)
    sb.append("://")
    sb.append(getAuthority)
    sb.append(getPath)
    if(getQuery != null){
      sb.append("?")
      sb.append(getQuery)
    }
    if(getFragment != null){
      sb.append("#")
      sb.append(getFragment)
    }
    sb.toString()
  }

  override def equals(o: scala.Any): Boolean = {
    if(o == null)
      return false
    val other = o match {
      case iri1: IRI => iri1
      case _ => throw new Exception("Expected an IRI for comparison, but was provided with: " + o.getClass.getName)
    }
    if(other.getScheme != this.getScheme)
      return false
    if(other.getAuthority != this.getAuthority)
      return false
    if(other.getPath != this.getPath)
      return false
    if(other.getQuery != this.getQuery)
      return false
    if(other.getFragment != this.getFragment)
      return false
    true
  }

  override def isRootless: Boolean = iri.isRootless

  override def getRawQuery: String = iri.getRawQuery

  override def violations(includeWarnings: Boolean): util.Iterator[Violation] = iri.violations(includeWarnings)

  override def toASCIIString: String = iri.toASCIIString

  override def getRawPath: String = iri.getRawPath

  override def isAbsolute: Boolean = iri.isAbsolute

  override def getASCIIHost: String = iri.getASCIIHost

  override def relativize(abs: org.apache.jena.iri.IRI, flags: Int): org.apache.jena.iri.IRI = iri.relativize(abs, flags)

  override def relativize(abs: org.apache.jena.iri.IRI): org.apache.jena.iri.IRI = iri.relativize(abs)

  override def relativize(abs: String): org.apache.jena.iri.IRI = iri.relativize(abs)

  override def relativize(abs: String, flags: Int): org.apache.jena.iri.IRI = iri.relativize(abs, flags)

  override def hasViolation(includeWarnings: Boolean): Boolean = iri.hasViolation(includeWarnings)

  override def getRawFragment: String = iri.getRawFragment

  override def getRawAuthority: String = iri.getRawAuthority

  override def toDisplayString: String = iri.toDisplayString

  override def getPort: Int = iri.getPort

  override def isRelative: Boolean = iri.isRelative

  override def normalize(useDns: Boolean): org.apache.jena.iri.IRI = iri.normalize(useDns)

  override def toURL: URL = iri.toURL

  override def getRawHost: String = iri.getRawHost

  override def ladderEquals(ir: org.apache.jena.iri.IRI, other: Int): Boolean = iri.ladderEquals(ir, other)

  override def ladderEquals(ir: org.apache.jena.iri.IRI): Int = iri.ladderEquals(ir)

  override def getScheme: String = iri.getScheme

  override def getRawUserinfo: String = iri.getRawUserinfo

  override def toURI: java.net.URI = iri.toURI

  override def create(i: org.apache.jena.iri.IRI): org.apache.jena.iri.IRI = iri.create(i)

  override def getFactory: IRIFactoryImpl = IRI.iriFactory
}

object IRI{
  protected val iriFactory: IRIFactory = new IRIFactory()
  IRIFactory.setIriImplementation(iriFactory)
  iriFactory.allowUnwiseCharacters()
}
