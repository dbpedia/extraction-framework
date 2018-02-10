package org.dbpedia.extraction.util

import java.io.File

import org.dbpedia.extraction.transform.Quad

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.{Codec, Source}

/**
  * Country representation with all useful iso codes
  * @param engName
  * @param iso3166a2
  * @param iso3166a3
  * @param primaryLang
  */
class Country(
   val engName: String,
   val iso3166a2: String,
   val iso3166a3: String,
   val primaryLang: Language
) {
  /**
    * picks out the lang string for a given language representing this country
    * @param lang
    * @return
    */
  def getLangName(lang: Language): Option[String] = Country.countryLangMap.get(this.iso3166a2) match{
    case Some(lmap) => lmap.get(lang)
    case None => None
  }
}


object Country{

  private val countryLangMap = new mutable.HashMap[String, mutable.HashMap[Language, String]]()
  val map: mutable.HashMap[String, Country] = new mutable.HashMap[String, Country]()

  locally {
    // loading the necessary data from the world fact files
    var reader = IOUtils.inputStream(new RichFile(new File(this.getClass.getClassLoader.getResource("worldfacts_dbpedia_data.ttl.bz2").toURI)))
    var source = Source.fromInputStream(reader)(Codec.UTF8)
    var sourceLines = (try source.getLines.toList finally source.close).map(l => Quad.unapply(l).orNull)

    var currentSubj: String = ""
    val group = new ListBuffer[Quad]()
    for (quad <- sourceLines.filter(l => l != null && l.subject.startsWith("http://worldfacts.dbpedia.org/regions")).sorted) {
      if (group.isEmpty || quad.subject == currentSubj) {
        group.append(quad)
        currentSubj = quad.subject
      }
      else {
        val engName = group.find(q => q.predicate == "http://www.w3.org/2004/02/skos/core#prefLabel").map(q => q.value).orNull
        val iso3166a2 = group.find(q => q.predicate == "http://worldfacts.dbpedia.org/ontology/iso3166-a2").map(q => q.value.toLowerCase).orNull
        val iso3166a3 = group.find(q => q.predicate == "http://worldfacts.dbpedia.org/ontology/iso3166-a3").map(q => q.value.toLowerCase).orNull
        val primaryLang = group.find(q => q.predicate == "http://worldfacts.dbpedia.org/ontology/primaryLanguage").map(q => q.value) match {
          case Some(v) => Language.map.values.find(l => l.iso639_3.toLowerCase == v.substring(v.lastIndexOf("/") + 1).toLowerCase).orNull
          case None => null
        }
        group.clear()
        if (iso3166a2 != null)
          map.put(iso3166a2, new Country(engName, iso3166a2, iso3166a3, primaryLang))
      }
    }

    reader = IOUtils.inputStream(new RichFile(new File(this.getClass.getClassLoader.getResource("worldfacts_dbpedia_labels.ttl.bz2").toURI)))
    source = Source.fromInputStream(reader)(Codec.UTF8)
    sourceLines = (try source.getLines.toList finally source.close).map(l => Quad.unapply(l).orNull)

    for (quad <- sourceLines.filter(l => l != null && l.subject.startsWith("http://worldfacts.dbpedia.org/regions"))) {
      readLabelQuad(quad)
    }

    def readLabelQuad(quad: Quad): Unit = {
      if (quad.predicate != "http://www.w3.org/2000/01/rdf-schema#label" || quad.language == null)
        return

      val a2 = quad.subject.substring(quad.subject.lastIndexOf("/") + 1).toLowerCase
      countryLangMap.get(a2) match {
        case Some(lmap) => {
          Language.get(quad.language) match {
            case Some(l) => lmap.get(l) match {
              case None => lmap.put(l, quad.value)
              case Some(_) =>
            }
            case None =>
          }
        }
        case None =>
          countryLangMap.put(a2, new mutable.HashMap[Language, String]())
          readLabelQuad(quad)
      }
    }

    reader = null
    source = null
    sourceLines = null
  }

  def apply(cCode: String): Country = map.get(cCode.toLowerCase) match{
    case Some(country) => country
    case None => map.values.find(x => x.iso3166a3 == cCode.toLowerCase).orNull
  }

  def get(cCode: String) = Option(apply(cCode))
}