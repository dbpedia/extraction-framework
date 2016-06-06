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
class InfoboxMappingsExtractorTest extends FlatSpec with Matchers with PrivateMethodTester {

  "InfoboxMappingsExtractor" should """return correct property id's for YM infobox""" in {

    val lang = Language.English

    val parsed = parse(
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
            |""", "TestPage", lang, "#property")


      val answer = List(("YM", "ölkə", "P17"),(("YM"), "şəkil","P18"),(("YM"), "gerb", "P94"),
        (("YM"), "bayraq", "P41"),(("YM"), "telefon kodu", "P473"),(("YM"), "nəqliyyat kodu","P395"),(("YM"), "sayt","P856"))

    (parsed) should be (answer)
  }

  "InfoboxMappingsExtractor" should """return correct property id's for Infobox planet """ in {

    val lang = Language.English

    val parsed = parse(
      """{{Infobox planet
        | name = Uranus
        | symbol = [[File:{{#property:P367}}|25px]]
        | image = [[File:Uranus2.jpg|260px]]
        | caption = Uranus as a featureless disc, photographed&lt;br&gt;by ''[[Voyager&amp;nbsp;2]]'' in 1986
        | discovery = yes
        | image = [[File:Uranus2.jpg|260px]]
        | caption = Uranus as a featureless disc, photographed&lt;br&gt;by ''[[Voyager&amp;nbsp;2]]'' in 1986
        | discovery = yes
        | discoverer = [[{{#property:P61}}]]
        | discovered =  {{#time:F j, Y|{{#property:P575}}}}
        | epoch = [[J2000]]
        | aphelion = {{val|20.11|ul=AU}}&lt;br&gt;({{nowrap|{{val|fmt=commas|3008|u=Gm}}}})
        | perihelion = {{val|18.33|u=AU}}&lt;br&gt;({{nowrap|{{val|fmt=commas|2742|u=Gm}}}})
        | semimajor = {{val|19.2184|u=AU}}&lt;br&gt;({{nowrap|{{val|fmt=commas|2875.04|u=Gm}}}})
        | eccentricity = {{val|0.046381}}
        }}""", "TestPage", lang, "#property")

    val answer = List(("Infobox planet", "symbol", "P367"),("Infobox planet","discoverer","P61"))

    (parsed) should be (answer)

  }
  "InfoboxMappingsExtractor" should """return correct property id's for Commons Category """ in {

    val lang = Language.English

    val parsed = parse(
      """
        |* {{cite book|author = R. Prud'Homme Van Reine|title = Admiraal Zilvervloot – Biografie van Piet Hein|publisher = De Arbeiderspers|year = 2003|volume = }}
        |{{Commons category|{{#property:P373}}}}
        |{{Use dmy dates|date=September 2011}}
        |{{Commons category|{{#property:P373}}}}
        }}""", "TestPage", lang, "#property")

    val answer = List(( "Commons category","1","P373"), ( "Commons category","1","P373"))

    (parsed) should be (answer)

  }
  "InfoboxMappingsExtractor" should """return correct property id's for Infobox Politics """ in {

    val lang = Language.English

    val parsed = parse(
      """{{Infobox Politics
         | seat                    = [[{{#property:P36}}]]
         | leader_title            = Governor
         | leader_title            = Governor
         | leader_name             = [[{{#property:P6}}]]
         | area_total_km2          = 4,443
         | population_footnotes    = &lt;ref name=&quot;r-pop&quot;&gt;{{Metadata Population BE||QUELLE}}&lt;/ref&gt;
         | population_total        = {{Metadata Population BE|80000}}
         | population_as_of        = {{Metadata Population BE||STAND}}
         | population_density_km2  = auto
         | area_total_km2          = 4,443
         | population_footnotes    = &lt;ref name=&quot;r-pop&quot;&gt;{{Metadata Population BE||QUELLE}}&lt;/ref&gt;
         | population_total        = {{Metadata Population BE|80000}}
         | population_as_of        = {{Metadata Population BE||STAND}}
         | population_density_km2  = auto
         }}""", "TestPage", lang, "#property")

    val answer = List(("Infobox Politics","seat","P36"), ("Infobox Politics","leader_name","P6"))

    (parsed) should be (answer)
  }
  "InfoboxMappingsExtractor" should """return correct property id's for nest property functions """ in {

    val lang = Language.English

    val parsed = parse(
      """|title=Philippine ZIP Codes Directory
        ||area_code              = 0
        ||blank_name             =
        ||blank_info             =
        ||blank1_name            =
        ||blank1_info            =
        ||website                = {{nowrap|{{URL|{{#property:P856}}}}}}""", "TestPage", lang, "#property")

    val answer = List(("URL","1","P856"))

    (parsed) should be (answer)

  }
  "InfoboxMappingsExtractor" should """return correct property id's for Infobox test """ in {

    val lang = Language.English

    val parsed = parse(
      """
         {{Infobox Test
         | elevation_footnotes     =
         | elevation_m             =
         | population_total        = {{#property:P1082}}
         | population_as_of        = 2010
         | population_as_of        = 2010
         | population_density_km2  = {{#expr: {{formatnum: {{#property:P1082}}|R}} / 0.67 round 0}}
         | population_demonym      =
         | population_note         =
         }}""", "TestPage", lang, "#property")

    val answer = List(("Infobox Test", "population_total" , "P1082"))

   (parsed) should be (answer)
  }

  "InfoboxMappingsExtractor" should """return correct property id's for Infobox Tourism """ in {

    val lang = Language.English

    val parsed = parse(
      """{{Infobox Tourism
         | tourism_slogan         = Masaganang Maitum
         | image_map              = {{#property:P242}}
         | map_caption            = Map of {{#property:P131}} with Maitum highlighted
         | pushpin_map            = Philippines
         | pushpin_label_position = left
         }}""", "TestPage", lang, "#property")

    val answer = List(("Infobox Tourism", "image_map", "P242"),("Infobox Tourism", "map_caption", "P131"))

   (parsed) should be (answer)
  }

  "InfoboxMappingsExtractor" should """return correct property id's for Infobox Test """ in {

    val lang = Language.get("fr").getOrElse(Language.English)

    val parsed = parse(
      """
         {{Infobox Test
         | surnom            = {{#property:p742}}
         | date de naissance = {{Date|18|septembre|1943|au cinéma|âge=oui}}
         | lieu de naissance = [[Montréal]], {{Québec}} ([[Canada]])
         | nationalité       = {{drapeau|Canada}} [[Canada|Canadienne]]
         | date de naissance = {{Date|18|septembre|1943|au cinéma|âge=oui}}
         | lieu de naissance = [[Montréal]], {{Québec}} ([[Canada]])
         | nationalité       = {{drapeau|Canada}} [[Canada|Canadienne]]
         | date de décès     = {{#property:p570}}
         | lieu de décès     = {{#property:p20}}
         | profession        = [[Acteur|Actrice]]
         }}""", "TestPage", lang, "#property")

    val answer = List(("Infobox Test", "surnom","p742"), ("Infobox Test","date de décès" ,"p570"), ("Infobox Test","lieu de décès", "p20"))

    (parsed) should be (answer)
  }

  "InfoboxMappingsExtractor" should """return correct property id's for multiple #property in a line """ in {

    val lang = Language.get("fr").getOrElse(Language.English)
    val answer = List(("Infobox Test","nom","P735"), ("Infobox Test","nom","P734"))
    val parsed = parse(
      """
        {{Infobox Test
        | nom               = {{#property:P735}} {{#property:P734}}
        }}""", "TestPage", lang, "#property")

    (parsed) should be (answer)
  }

  "InfoboxMappingsExtractor" should """return correct property id's for multiple info boxes """ in {

    val lang = Language.get("fr").getOrElse(Language.English)
    val answer = List(("Infobox Test1","arg1","P1"), ("Infobox Test2","arg2","P2"))
    val parsed = parse(
      """
        {{Infobox Test1
        | arg1   = {{#property:P1}}
        }}

        {{Infobox Test2
        | arg2 = {{#property:P2}}
        }}
      """, "TestPage", lang, "#property")

    (parsed) should be (answer)
  }

  "InfoboxMappingsExtractor" should """return correct property id's #invoke type parser function  """ in {

    val lang = Language.get("fr").getOrElse(Language.English)
    val answer = List(("Infobox Test1","population_as_of","P1082/P585"))
    val parsed = parse(
      """
        {{Infobox Test1
        | area_total_km2         = 54.84
        | population_as_of       = {{#invoke:Wikidata|getQualifierDateValue|P1082|P585|FETCH_WIKIDATA|dmy}}
        | population_note        =
        | population_total       = {{#property:P1082}}
        }}
      """, "TestPage", lang, "#invoke")

    (parsed) should be (answer)
  }


  "InfoboxMappingsExtractor" should """return correct property id's #invoke type parser function nested """ in {

    val lang = Language.get("fr").getOrElse(Language.English)
    val answer = List(("Infobox Test1","data2","P137"))
    val parsed = parse(
      """
        {{Infobox Test1
        | label2 = Organisation
        || label2 = Organisation
        || data2  = {{#invoke:Wikidata|getValue|P137|{{{organization|{{{organisation|FETCH_WIKIDATA}}}}}}}}
        || label3 = Location(s)
        || class3 = label
        || label3 = Location(s)
        || class3 = label
        }}
      """, "TestPage", lang, "#invoke")

    (parsed) should be (answer)
  }
  "InfoboxMappingsExtractor" should """return correct property id's #invoke type parser function for multiple infoboxes """ in {

    val lang = Language.get("fr").getOrElse(Language.English)
    val answer = List(("Infobox Test1","arg1","P729"),("Infobox Test2","arg2","P87") )
    val parsed = parse(
      """
        {{Infobox Test1
        | arg1  = {{#invoke:Wikidata|getValue|P729|{{{first_light|FETCH_WIKIDATA}}}}}
        }}

        | {{Infobox Test2
        | arg2  = {{#invoke:Wikidata|getValue|P87|{{{first_light|FETCH_WIKIDATA}}}}}
        }
      """, "TestPage", lang, "#invoke")

    (parsed) should be (answer)
  }
  "InfoboxMappingsExtractor" should """return correct property id's #invoke type parser function for ProperyLink  """ in {

    val lang = Language.get("fr").getOrElse(Language.English)
    val answer = List(("Infobox Test1","operating system","p306"),("Infobox Test1","license","p275"), ("Infobox Test1","website","p856"))
    val parsed = parse(
      """
        {{Infobox Test1
        | operating system       = {{#invoke:PropertyLink|property|p306}}
        | license                = {{#invoke:PropertyLink|property|p275}}
        | website                = {{#invoke:Wikidata|property|p856}}
        | random                  = {{#invoke:Random|property|p456}}
        }}
      """, "TestPage", lang, "#invoke")

    (parsed) should be (answer)
  }

  "InfoboxMappingsExtractor" should """return correct property id's for P856  """ in {

    val lang = Language.English
    val answer = List(("Infobox Test1","website1","P856"), ("Infobox Test1","website2","P856"))
    val parsed = parse(
      """
        {{Infobox Test1
        | website1                = {{Official URL}}
        | website2                = {{Official website}}
        }}
      """, "TestPage", lang, "#P856")

    (parsed) should be (answer)
  }

  "InfoboxMappingsExtractor" should """return correct property id's for P856 for multiple infoboxes  """ in {

    val lang = Language.English
    val answer = List(("Infobox Test1","website1","P856"), ("Infobox Test2","website2","P856"))
    val parsed = parse(
      """
        {{Infobox Test1
        | website1                = {{Official website}}
        }}

        {{Infobox Test2
        | website2                = {{Official URL}}
        }}
      """, "TestPage", lang, "#P856")

    (parsed) should be (answer)
  }

  "InfoboxMappingsExtractor" should """return correct property id's for P856 for different Language  """ in {

    val lang = Language.getOrElse("no", Language.English)
    val answer = List(("Infoboks Test1","website1","P856"))
    val parsed = parse(
      """
        {{Infoboks Test1
        | website1                = {{BetingetURL}}
        }}

      """, "TestPage", lang, "#P856")

    (parsed) should be (answer)
  }

  private val parser = WikiParser.getInstance()

  private def parse(input : String, title: String = "TestPage", lang: Language = Language.English, test : String) : List[(String,String, String)] =
  {
    val page = new WikiPage(WikiTitle.parse(title, lang), input)
    val context = new {
      def ontology = InfoboxMappingsExtractorTest.context.ontology;
      def language = lang;
      def redirects = new Redirects(Map("Official" -> "Official website"))
    }

    val extractor = new InfoboxMappingsExtractor(context)
    var to_return : List[(String,String, String)]= List.empty
    if ( test == "#property") {
      to_return =  parser(page) match {
        case Some(pageNode) => extractor.getPropertyTuples(pageNode)
        case None => List.empty
      }
    } else if ( test == "#invoke"){
      to_return = parser(page) match {
        case Some(pageNode) => extractor.getInvokeTuples(pageNode)
        case None => List.empty
      }
    } else if ( test == "#P856") {
      to_return = parser(page) match {
        case Some(pageNode) => extractor.getP856Tuples(pageNode, lang)
        case None => List.empty
      }
    }
      to_return
  }
}

object InfoboxMappingsExtractorTest {

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

}