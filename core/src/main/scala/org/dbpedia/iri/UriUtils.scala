package org.dbpedia.iri

import java.net.{URLDecoder, URLEncoder}

import org.apache.commons.lang3.StringEscapeUtils
import org.dbpedia.extraction.util.{StringUtils, WikiUtil}

import scala.util.{Failure, Success, Try}

object UriUtils
{
  private val knownSchemes = Set("http", "https", "ftp")

  private val knownPrefixes = knownSchemes.map(_ + "://")

  def hasKnownScheme(uri: String) : Boolean = hasKnownScheme(IRI.create(uri).getOrElse(return false))
  def hasKnownScheme(uri: IRI) : Boolean = knownPrefixes.exists(uri.toString.startsWith(_))

  /**
   * TODO: comment
   */
  def cleanLink( uri : IRI ) : Option[String] =
  {
    if (knownSchemes.contains(uri.getScheme))
      Try{uri.toURI.normalize.toString}.toOption //IRI.normalize not yet implemented!
    else None
  }

  def relativize( parent : URI, child : URI ) : IRI = relativize(uriToIri(parent), uriToIri(child))
  /**
   * Relativizes the given parent URI against a child URI.
   *
   * @param parent
   * @param child
   * @return path from parent to child
   * @throws IllegalArgumentException if parent is not a parent directory of child.
   */
  def relativize( parent : IRI, child : IRI ) : IRI =
  {
      val path = parent.relativize(child)
      if (path eq child) throw new IllegalArgumentException("["+parent+"] is not a parent directory of ["+child+"]")
      IRI.create(path).get
  }

  def createURI(uri: String): Try[URI] ={
      URI.create(uri)
  }

  /**

    */
    def toDbpediaUri(uri: String): URI = {
      val sb = new java.lang.StringBuilder()
      val input = StringUtils.replaceChars(sb, StringEscapeUtils.unescapeJava(uri), " \u00A0\u200E\u200F\u2028\u202A\u202B\u202C\u3000", "_").toString
      val respos = input.indexOf("dbpedia.org/resource/") + 21
      var pos = 0
      if(respos > 20)
      {
        val query = input.indexOf('?')
        val fragment = input.indexOf('#')
        val prelude = input.substring(0, respos)
        val resource = encodeAndClean(
          if(query > respos)
          input.substring(respos, query)
        else if (fragment > respos)
          input.substring(respos, fragment)
        else
          input.substring(respos)
        )

        val qu = if(query > respos){
          if(fragment > query)
            "?" + encodeAndClean(input.substring(query+1, fragment))
          else
            "?" + encodeAndClean(input.substring(query+1))
        } else ""

        val frag = if(fragment > respos)
            "#" + encodeAndClean(input.substring(fragment+1))
          else ""

        URI.create(prelude + resource + qu + frag) match{
          case Failure(f) => throw f
          case Success(u) => u
        }
      }
      else
        URI.create(input) match{
          case Success(s) => s
          case Failure(f) => null
        }
    }

  /**
    * decodes (ASCII) uris and transforms them into iris with the DBpedia naming rules
    *
    * @param uri
    * @return
    */
  def uriToDbpediaIri(uri: String): IRI = {
    val urii = toDbpediaUri(uri)
    uriToIri(urii)
  }


  def uriToIri(uri: String) : IRI = URI.create(uri) match{
    case Success(s) => uriToIri(s)
    case Failure(f) => null
  }
  /**
    * see uriToIri(uri: String)
    *
    * @param uri
    * @return
    */
  def uriToIri(uri: URI): IRI = {
    val scheme = uri.getScheme + "://"
    val authority = uri.getAuthority
    val path = WikiUtil.wikiEncode(iriDecode(uri.getPath)).replaceAll("%25", "%")    // we only want to wiki-encode the path!
    val query = if (uri.getQuery != null) "?" + iriDecode(uri.getRawQuery).replaceAll("%25", "%") else ""
    val fragment = if(uri.getFragment != null) "#" + iriDecode(uri.getRawFragment).replaceAll("%25", "%") else ""
    IRI.create(
      scheme +
      authority +
      path +
      query +
      fragment
    ).get
  }

  private def encodeAndClean(uriPart: String): String={
    var decoded = uriPart
    while(UriDecoder.decode(decoded) != decoded)
      decoded = UriDecoder.decode(decoded)
    StringUtils.escape(decoded, WikiUtil.iriReplacements)
  }

  def decode(uriPart: String): String={
    var decoded = uriPart
    while(UriDecoder.decode(decoded) != decoded)
      decoded = UriDecoder.decode(decoded)

    decoded.replaceAll("[<>#%\\?\\[\\\\\\]]", "_")
  }

  def iriDecode(uriPart: String): String={
    var decoded = uriPart
    val decoder = new UriToIriDecoder
    while(decoder.decode(decoded) != decoded)
      decoded = decoder.decode(decoded)
    decoded
  }

  def encodeUriComponent(comp: String): String={
    URLEncoder.encode(comp, "UTF-8")
      .replaceAll("\\+", "%20")
      .replaceAll("\\%21", "!")
      .replaceAll("\\%27", "'")
      .replaceAll("\\%28", "(")
      .replaceAll("\\%29", ")")
      .replaceAll("\\%7E", "~")
  }
}
