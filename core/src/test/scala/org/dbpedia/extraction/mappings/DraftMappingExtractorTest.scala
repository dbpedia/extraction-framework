package org.dbpedia.extraction.mappings

import java.io.File

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.ontology.{OntologyProperty, Ontology}
import org.scalatest.{PrivateMethodTester, FlatSpec}
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.collection.mutable.{Set => MutableSet, HashSet}
import org.dbpedia.extraction.sources.{XMLSource, WikiPage, MemorySource}
import org.dbpedia.extraction.wikiparser.{WikiParser, Namespace, WikiTitle}
import org.dbpedia.extraction.util.Language

/**
  *
  */
@RunWith(classOf[JUnitRunner])
class DraftMappingExtractorTest extends FlatSpec with Matchers with PrivateMethodTester {

  "DraftMappingExtractor" should """return correct property id's P17, P18, P94, P41""" in {

    val lang = Language.English

    val quads = parse(
      """{{YM
            | |status                                 =
            | |azərbaycan dilində adı       = Telqte
            | |orijinal adı                          = Telgte
            | |ölkə                                    = {{#property:P17}}
            | |şəkil                                    ={{#property:P18}}
            | |gerb                                    = {{#property:P94}}
            | |bayraq                                = {{#property:P41}}
            | |bayraq yazısı                      =
            |  |lat_dir =N |lat_deg =51 |lat_min =58 |lat_sec =55
            |  |lon_dir =E |lon_deg =7 |lon_min =47 |lon_sec = 8
            | |ölkə xəritəsi                        = <!-- alternativ, eyni koordinatlı diyarlar -->
            | |statuslu                             =
            | |sahəsi                               = 90.6
            | |əhalisi                              = 19522
            | |saat qurşağı                    = +1
            | |telefon kodu                    = {{#property:P473}}
            | |nəqliyyat kodu                 = {{#property:P395}}
            | |sayt                                 = {{#property:P856}}
            | |saytın dili                         = de
            |}}
            |""", "TestPage", lang)


      val answer = Set("{{#property:P17}}","{{#property:P18}}","{{#property:P94}}", "{{#property:P41}}","{{#property:P473}}","{{#property:P395}}","{{#property:P856}}")
      var parsed = scala.collection.mutable.Set[String]()
       for ( quad <- quads){
          parsed.add(quad.value)
      }
    (parsed) should be (answer)
  }

  "DraftMappingExtractor" should """return correct property id's P367, P61, P575 """ in {

    val lang = Language.English

    val quads = parse(
      """{{Use British English|date=January 2015}}
        || name = Uranus
        || symbol = [[File:{{#property:P367}}|25px]]
        || image = [[File:Uranus2.jpg|260px]]
        || caption = Uranus as a featureless disc, photographed&lt;br&gt;by ''[[Voyager&amp;nbsp;2]]'' in 1986
        || discovery = yes
        || image = [[File:Uranus2.jpg|260px]]
        || caption = Uranus as a featureless disc, photographed&lt;br&gt;by ''[[Voyager&amp;nbsp;2]]'' in 1986
        || discovery = yes
        || discoverer = [[{{#property:P61}}]]
        || discovered =  {{#time:F j, Y|{{#property:P575}}}}
        || orbit_ref =
        |&lt;ref name=&quot;VSOP87&quot; /&gt;{{efn | These are the mean elements from VSOP87, together with derived quantities.}}
        || epoch = [[J2000]]
        || aphelion = {{val|20.11|ul=AU}}&lt;br&gt;({{nowrap|{{val|fmt=commas|3008|u=Gm}}}})
        || perihelion = {{val|18.33|u=AU}}&lt;br&gt;({{nowrap|{{val|fmt=commas|2742|u=Gm}}}})
        || semimajor = {{val|19.2184|u=AU}}&lt;br&gt;({{nowrap|{{val|fmt=commas|2875.04|u=Gm}}}})
        || eccentricity = {{val|0.046381}}""", "TestPage", lang)

    val answer = Set("{{#property:P367}}","{{#property:P61}}","{{#property:P575}}")
    var parsed = scala.collection.mutable.Set[String]()
    for ( quad <- quads){
      parsed.add(quad.value)
    }
    (parsed) should be (answer)

  }
  "DraftMappingExtractor" should """return correct property id's P373 """ in {

    val lang = Language.English

    val quads = parse(
      """* {{cite book|author = R. Prud'Homme Van Reine|title = Admiraal Zilvervloot – Biografie van Piet Hein|publisher = De Arbeiderspers|year = 2003|volume = }}
        |{{Commons category|{{#property:P373}}}}
        |{{Use dmy dates|date=September 2011}}
        |{{Commons category|{{#property:P373}}}}""", "TestPage", lang)

    val answer = Set("{{#property:P373}}","{{#property:P373}}")
    var parsed = scala.collection.mutable.Set[String]()
    for ( quad <- quads){
      parsed.add(quad.value)
    }
    (parsed) should be (answer)

  }
  "DraftMappingExtractor" should """return correct property id's P36, P6, P856 """ in {

    val lang = Language.English

    val quads = parse(
      """| seat                    = [[{{#property:P36}}]]
        || leader_title            = Governor
        || leader_title            = Governor
        || leader_name             = [[{{#property:P6}}]]
        || area_total_km2          = 4,443
        || population_footnotes    = &lt;ref name=&quot;r-pop&quot;&gt;{{Metadata Population BE||QUELLE}}&lt;/ref&gt;
        || population_total        = {{Metadata Population BE|80000}}
        || population_as_of        = {{Metadata Population BE||STAND}}
        || population_density_km2  = auto
        || area_total_km2          = 4,443
        || population_footnotes    = &lt;ref name=&quot;r-pop&quot;&gt;{{Metadata Population BE||QUELLE}}&lt;/ref&gt;
        || population_total        = {{Metadata Population BE|80000}}
        || population_as_of        = {{Metadata Population BE||STAND}}
        || population_density_km2  = auto
        || website                 = {{URL|{{#property:P856}}}}""", "TestPage", lang)

    val answer = Set("{{#property:P36}}","{{#property:P6}}","{{#property:P856}}")
    var parsed = scala.collection.mutable.Set[String]()
    for ( quad <- quads){
      parsed.add(quad.value)
    }
    (parsed) should be (answer)
  }
  "DraftMappingExtractor" should """return correct property id's P856""" in {

    val lang = Language.English

    val quads = parse(
      """|title=Philippine ZIP Codes Directory
        ||area_code              = 0
        ||blank_name             =
        ||blank_info             =
        ||blank1_name            =
        ||blank1_info            =
        ||website                = {{nowrap|{{URL|{{#property:P856}}}}}}""", "TestPage", lang)

    val answer = Set("{{#property:P856}}")
    var parsed = scala.collection.mutable.Set[String]()
    for ( quad <- quads){
      parsed.add(quad.value)
    }
    (parsed) should be (answer)

  }
  "DraftMappingExtractor" should """return correct property id's P1082 """ in {

    val lang = Language.English

    val quads = parse(
      """| elevation_footnotes     =
        || elevation_m             =
        || population_total        = {{#property:P1082}}
        || population_as_of        = 2010
        || population_as_of        = 2010
        || population_density_km2  = {{#expr: {{formatnum: {{#property:P1082}}|R}} / 0.67 round 0}}
        || population_demonym      =
        || population_note         = """, "TestPage", lang)

    val answer = Set("{{#property:P1082}}","{{#property:P1082}}")
    var parsed = scala.collection.mutable.Set[String]()
    for ( quad <- quads){
      parsed.add(quad.value)
    }
    (parsed) should be (answer)
  }
  "DraftMappingExtractor" should """return correct property id's P242, P131 """ in {

    val lang = Language.English

    val quads = parse(
      """| tourism_slogan         = Masaganang Maitum
        || image_map              = {{#property:P242}}
        || map_caption            = Map of {{#property:P131}} with Maitum highlighted
        || pushpin_map            = Philippines
        || pushpin_label_position = left """, "TestPage", lang)

    val answer = Set("{{#property:P242}}","{{#property:P131}}")
    var parsed = scala.collection.mutable.Set[String]()
    for ( quad <- quads){
      parsed.add(quad.value)
    }
    (parsed) should be (answer)
  }
  "DraftMappingExtractor" should """return correct property id's P625 """ in {

    val lang = Language.English

    val quads = parse(
      """| rowclass9 = mergedtoprow
        ||  data9 = {{#if:{{{pushpin_map_narrow|}}}||{{#if:{{both| {{{pushpin_map|}}} | {{both|{{{latd|}}}|{{{longd|}}}}} {{both|{{{coordinates_wikidata|{{{wikidata|}}}}}}|{{#property:P625}}}} }}|""", "TestPage", lang)

    val answer = Set("{{#property:P625}}")
    var parsed = scala.collection.mutable.Set[String]()
    for ( quad <- quads){
      parsed.add(quad.value)
    }
    (parsed) should be (answer)
  }

  "DraftMappingExtractor" should """return correct property id's p742, p570, p20 """ in {

    val lang = Language.English

    val quads = parse(
      """| surnom            = {{#property:p742}}
         | date de naissance = {{Date|18|septembre|1943|au cinéma|âge=oui}}
         | lieu de naissance = [[Montréal]], {{Québec}} ([[Canada]])
         | nationalité       = {{drapeau|Canada}} [[Canada|Canadienne]]
         | date de naissance = {{Date|18|septembre|1943|au cinéma|âge=oui}}
         | lieu de naissance = [[Montréal]], {{Québec}} ([[Canada]])
         | nationalité       = {{drapeau|Canada}} [[Canada|Canadienne]]
         | date de décès     = {{#property:p570}}
         | lieu de décès     = {{#property:p20}}
         | profession        = [[Acteur|Actrice]]""", "TestPage", lang)

    val answer = Set("{{#property:p742}}", "{{#property:p570}}", "{{#property:p20}}")
    var parsed = scala.collection.mutable.Set[String]()
    for ( quad <- quads){
      parsed.add(quad.value)
    }
    (parsed) should be (answer)
  }

  "DraftMappingExtractor" should """return correct property id's P735, P734 """ in {

    val lang = Language.English

    val quads = parse(
      """ | nom               = {{#property:P735}} {{#property:P734}}
        """, "TestPage", lang)

    val answer = Set("{{#property:P735}}", "{{#property:P734}}")
    var parsed = scala.collection.mutable.Set[String]()
    for ( quad <- quads){
      parsed.add(quad.value)
    }
    (parsed) should be (answer)
  }

  "DraftMappingExtractor" should """return correct property id's p1563 """ in {

    val lang = Language.English

    val quads = parse(
      """lien auteur2=Edmund Robertson
        |jour={{{jour|}}}
        |mois={{{mois|}}}
        |année={{{année|}}}
        |lire en ligne=http://www-history.mcs.st-andrews.ac.uk/Biographies/{{{id|{{#property:p1563}}}}}.html
        |titre chapitre={{{title|{{PAGENAME}}}}}
        |titre ouvrage=[[MacTutor History of Mathematics archive]]
        |lien éditeur=université de St Andrews
        |éditeur=université de {{lang|en|St Andrews}}
        |consulté le={{{accessdate|}}}""", "TestPage", lang)

    val answer = Set("{{#property:p1563}}")
    var parsed = scala.collection.mutable.Set[String]()
    for ( quad <- quads){
      parsed.add(quad.value)
    }
    (parsed) should be (answer)
  }

  "DraftMappingExtractor" should """return correct property id's P31, P570 """ in {

    val lang = Language.English

    val quads = parse(
      """{{Infobox/Ligne mixte optionnelle|Date de décès|{{{date de décès|}}}|{{#invoke:Date|dateInfobox|mort|{{{date de naissance|}}}|{{{date de décès|}}}|qualificatif={{{qualificatif date|}}} }}{{#ifeq:{{#property:P31}}|être humain|{{#ifeq:{{#property:P570}}{{NAMESPACE}}||[[Category:P570 absent de Wikidata]]}}}} }}""", "TestPage", lang)

    val answer = Set("{{#property:P31}}", "{{#property:P570}}")
    var parsed = scala.collection.mutable.Set[String]()
    for ( quad <- quads){
      parsed.add(quad.value)
    }
    (parsed) should be (answer)
  }


  private val parser = WikiParser.getInstance()

  private def parse(input : String, title: String = "TestPage", lang: Language = Language.English) : Seq[Quad] =
  {
    val page = new WikiPage(WikiTitle.parse(title, lang), input)
    val context = new {
      def ontology = DraftMappingExtractorTest.context.ontology;
      def language = lang;
      def redirects = new Redirects(Map("Official" -> "Official website"))
    }

    val extractor = new DraftMappingExtractor(context)
    parser(page) match {
      case Some(pageNode) => extractor.getPropertyParserFunctions(pageNode, "TestPage")
      case None => Seq.empty
    }

  }



}


object DraftMappingExtractorTest {

  val context = new {
    def ontology = {
      val ontoFilePath = "../ontology.xml"
      val ontoFile = new File(ontoFilePath)
      val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
      new OntologyReader().read(ontologySource)
    }
    def language = "en"
    def redirects = new Redirects(Map())
  }


//  private val datasets = new DraftMappingExtractor(new {
//    def ontology = DraftMappingExtractorTest.ontology;
//    def language = Language.English;
//    def redirects = null
//  }).datasets
}