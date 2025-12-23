package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.destinations.DestinationUtils
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{DateFinder, Language}
import org.dbpedia.extraction.util.RichFile.wrapFile

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by chile on 26.06.17.
  * Repaces Wikidata -uris with languages-uris if available and tries to select only labels with the language tag in question (be default it reverts to en else)
  * The origin Persondata file of Wikidata needs to be sorted by subject!
  * FIXME parallel me!!!  -> needs consideration of how many interlanguage link maps fit into memory
  */
object MapWikidataToLanguageUri {
  def main(args: Array[String]): Unit = {
    require(args != null && args.length == 1, "One arguments required, extraction config file")

    val config = new Config(args(0))

    val baseDir = config.dumpDir
    require(baseDir.isDirectory && baseDir.canRead && baseDir.canWrite, "Please specify a valid local base extraction directory - invalid path: " + baseDir)

    val suffix = config.inputSuffix match{
      case Some(x) if x.startsWith(".") => x
      case _ => throw new IllegalArgumentException("Please specify a valid file extension starting with a '.'!")
    }
    require("\\.[a-zA-Z0-9]{2,3}\\.(gz|bz2)".r.replaceFirstIn(suffix, "") == "", "provide a valid serialization extension starting with a dot (e.g. .ttl.bz2)")

    val wikidataFinder = new DateFinder(baseDir, Language.Wikidata)
    val wikidataPersonData = wikidataFinder.byName(DBpediaDatasets.Persondata + "-sorted" + suffix, auto = true) match{
      case Some(f) => f
      case None => throw new IllegalArgumentException("Persondata dataset for Wikidata was not found was not found: " + DBpediaDatasets.Persondata.encoded + suffix)
    }

    val descriptionUri = RdfNamespace.resolvePrefix("dct:description")
    val givenNameUri = RdfNamespace.resolvePrefix("foaf:givenName")
    val surNameuri = RdfNamespace.resolvePrefix("foaf:surname")
    val nameUri = RdfNamespace.resolvePrefix("foaf:name")


    for(language <- config.languages){

      val finder = new DateFinder(baseDir, language)
      val interlanguageLinks = finder.byName(DBpediaDatasets.InterLanguageLinks.filenameEncoded + suffix, auto = true) match{
        case Some(f) => f
        case None => throw new IllegalArgumentException("Interlanguage link file for language " + language.name + " was not found: " + DBpediaDatasets.InterLanguageLinks.filenameEncoded + suffix)
      }

      val destination = DestinationUtils.createDatasetDestination(finder, Array(DBpediaDatasets.Persondata.getLanguageVersion(language, config.dbPediaVersion).get), config.formats)

      val map = new mutable.HashMap[String, String]()

      val cutWikidataAt = Language.Wikidata.resourceUri.toString.length
      val langResourcUri = if(language == Language.English) language.resourceUri.toString.replace("en.", "") else language.resourceUri.toString
      val cutLanguageAt = langResourcUri.length
      new QuadReader().readQuads(language, interlanguageLinks){ quad =>
        if(quad.value.startsWith(Language.Wikidata.resourceUri.toString))
          map.put(quad.value.substring(cutWikidataAt), quad.subject.substring(cutLanguageAt))
      }

      def selectPredicateGroupRepresentative(newSubject: String, group: (String, Traversable[Quad])): Option[Quad] = {
        val quad = group._2.head
        if (quad.language == null) {
          if(quad.datatype == null && quad.value.startsWith(Language.Wikidata.resourceUri.toString))
            map.get(quad.value.substring(cutWikidataAt)) match {
              case Some(v) => Option(quad.copy(subject = newSubject, value = langResourcUri + v, dataset = DBpediaDatasets.Persondata.encoded))
              case None => Option(quad.copy(subject = newSubject, dataset = DBpediaDatasets.Persondata.encoded))
            }
          else
            Option(quad.copy(subject = newSubject, dataset = DBpediaDatasets.Persondata.encoded))
        }
        else {
          group._2.find(x => x.language == language.wikiCode) match {
            case Some(q) => Option(q.copy(subject = newSubject, dataset = DBpediaDatasets.Persondata.encoded))
            case None => if(group._1 != descriptionUri)   //do not try to find alternative language labels for dct:description! +++ TODO make these exceptions configurable!!!
              group._2.find(x => x.language == Language.English.wikiCode) match {
                case Some(q) => Option(q.copy(subject = newSubject, language = language.wikiCode, dataset = DBpediaDatasets.Persondata.encoded))          //try English as alternative language first
                case None => Option(group._2.head.copy(subject = newSubject, language = language.wikiCode, dataset = DBpediaDatasets.Persondata.encoded)) //else just pick the first one +++ TODO this is quiet arbitrary -> reevaluate this!
              }
              else
                None
          }
        }
      }

      def clacSurOrGivenName(groups: Map[String, Traversable[Quad]], ret: ArrayBuffer[Quad]): Option[Quad] = {
        groups.get(surNameuri) match{
            //calc surname
          case None => groups.get(givenNameUri) match {
            case Some(gn) => groups.get(nameUri) match {
              case Some(n) => {
                val name = ret.find(q => q.predicate == nameUri).get
                val givenName = ret.find(q => q.predicate == givenNameUri).get
                if(name.value.length > givenName.value.length && name.value.substring(0, givenName.value.length) == givenName.value){
                  val v = name.value.substring(givenName.value.length).trim.split(" ")  // subtract the given name from name / trim / split by spaces (we do this to make sure no additional given names or titles are in there)
                  if(v.length == 1)  //only if we can be sure about the surname...
                    Some(givenName.copy(value = v.head, predicate = surNameuri))
                  else None
                }
                else None
              }
              case None => None
            }
            case None => None
          }
          case Some(sn) => groups.get(givenNameUri) match {
              //calc given name
            case None => groups.get(nameUri) match {
              case Some(n) => {
                val name = ret.find(q => q.predicate == nameUri).get
                val surName = ret.find(q => q.predicate == surNameuri).get
                val givenNameLength = name.value.length - surName.value.length
                if(name.value.length > givenNameLength && givenNameLength > 0 && name.value.substring(givenNameLength).trim == surName.value){
                  val v = name.value.substring(0, givenNameLength).trim.split(" ")
                  if(v.length == 1)
                    Some(surName.copy(value = v.head, predicate = givenNameUri))
                  else None
                }
                else None
              }
              case None => None
            }
            case Some(x) => None
          }
        }
      }

      new QuadMapper().mapSortedQuads(language, wikidataPersonData, destination, required = true) { quads =>
        if (quads.nonEmpty) {
          map.get(quads.head.subject.substring(cutWikidataAt)) match {
            case Some(u) => {
              val groups = quads.groupBy(g => g.predicate)
              val ret = new ArrayBuffer[Quad]()
              ret ++= (for (group <- groups) yield selectPredicateGroupRepresentative(langResourcUri + u, group)).flatten
              clacSurOrGivenName(groups, ret) match{
                case Some(q) => ret += q
                case None =>
              }
              ret.toList
            }
            case None => Seq()
          }
        }
        else
          Seq()
      }
    }
  }
}
