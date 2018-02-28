package org.dbpedia.extraction.mappings

import java.io.File

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}
import org.dbpedia.extraction.wikiparser._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.language.reflectiveCalls

/**
  * Created by aditya on 6/21/16.
  */
@RunWith(classOf[JUnitRunner])
class InfoboxMappingsTemplateExtractorTest  extends FlatSpec with Matchers with PrivateMethodTester{

  private val parser = WikiParser.getInstance("sweble")
/*
  "InfoboxMappingsTemplateExtractor" should """return correct property id's for conditional expressions """ in {

    val lang = Language.English
    val answer = List(("Infobox Test1","string1","P1082"), ("Infobox Test1","string2","P1082"), ("Infobox Test1","string4","P1082"))
    val parsed = parse(
      """
        {{Infobox Test1

        | data37    = {{#ifeq: temp_string1 | temp_string2 | temp_string3 | temp_string4 }}
        | data38    = {{#ifeq: string1 | string2 |{{#property:P1082}} | string4 }}
        | website   = {{#invoke:Wikidata|property|p856}}

        }}
      """, "TestPage", lang, "all")

    (parsed) should be (answer)
  }

  "InfoboxMappingsTemplateExtractor" should """return correct property id's for incorrect conditional expressions """ in {

    val lang = Language.English
    val answer = List()
    val parsed = parse(
      """
        {{Infobox Test1

        | data37    = {{#ifeq: temp_string1 | temp_string2 | temp_string3 | temp_string4 }}
        | data38    = {{#ifeq: string1 | string2 |{{#property:P1082}} |  {{#invoke:Wikidata|property|p456}} }}
        }}
      """, "TestPage", lang, "all")

    (parsed) should be (answer)
  }

  "InfoboxMappingsTemplateExtractor" should """return correct property id's for conditional expressions with one nested level """ in {

    val lang = Language.English
    val answer = List(("Infobox Test1","string2","p123"), ("Infobox Test1","string1","p123"), ("Infobox Test1","value if different","p123"), ("Infobox Test1","value if non-empty","p123"), ("Infobox Test1","value if empty","p123"))
    val parsed = parse(
      """
        {{Infobox Test1
        |data39   = {{#ifeq: string1 | string2 | {{#if: {{#property:p123}} | value if non-empty | value if empty }} | value if different }}
        }}
      """, "TestPage", lang, "all")

    (parsed) should be (answer)
  }

  "InfoboxMappingsTemplateExtractor" should """return correct property id's for conditional expressions with multiple nested level """ in {

    val lang = Language.English
    val answer = List(("Infobox Test1","test_string4","p1243"), ("Infobox Test1","string1","p1243"), ("Infobox Test1","test_string5","p1243"),
      ("Infobox Test1","test_string3","p1243"), ("Infobox Test1","string2","p1243"),("Infobox Test1","test_string1","p1243"), ("Infobox Test1","test_string2","p1243"))
    val parsed = parse(
      """
        {{Infobox Test1
        |data40   = {{#ifeq: string1 | string2 | {{#if: test_string1 |  {{#ifexist: {{#property:p1243}} | test_string2 | test_string3 }}| test_string4 }} | test_string5 }}        }}
      """, "TestPage", lang, "all")

    (parsed) should be (answer)
  }



  "InfoboxMappingsTemplateExtractor" should """return correct property id's for real complex expressions 1 """ in {

    val lang = Language.English
    val answer = List(("Infobox Test1","website","P856"), ("Infobox Test1","hide","P856"), ("Infobox Test1","established_date","P765"),
      ("Infobox Test1","URL","P856") )

    val parsed = parse(
      """
        {{Infobox Test1

        | data37 = {{#if:{{{website|}}}
                          |{{#ifeq:{{{website|}}}|hide|{{{website|}}} }}
                          |{{#if:{{#property:P856}}
                             |{{URL|{{#property:P856}}}}
                           }}
                       }}
        | established_date        = {{#if: {{{established_date|}}} | {{{established_date}}} | {{#invoke:Wikidata|property|P765}} }}
        }}
      """, "TestPage", lang, "all")

    (parsed) should be (answer)
  }

  "InfoboxMappingsTemplateExtractor" should """return correct property id's for real complex expressions 2  """ in {

    val lang = Language.English
    val answer = List(("Infobox Test1","ISBN","P212"), ("Infobox Test1","website","P856"), ("Infobox Test1","ISBN_note","P212"), ("Infobox Test1","pushpin_map","P625"),
      ("Infobox Test1","ISBNT","P212"), ("Infobox Test1","URL","P856"), ("Infobox Test1","homepage","P856"), ("Infobox Test1","coordinates_wikidata","P625"),
      ("Infobox Test1","link","P212"), ("Infobox Test1","Url","P856"), ("Infobox Test1","location map","P625"),
      ("Infobox Test1","longd","P625"), ("Infobox Test1","latd","P625"))
    val parsed = parse(
      """
        {{Infobox Test1
        |  data30 = {{#if:{{{ISBN|}}}
                     | {{#ifeq:{{{ISBN|}}}|FETCH_WIKIDATA
                       | {{#invoke:ISBNT|link|{{#property:P212}}}}
                       | {{ISBNT|1={{{ISBN|}}}}} {{{ISBN_note|}}}
                       }}
                     }}
        | data38    = {{{website|{{{homepage|{{{URL|{{#ifeq:{{{website|{{{homepage|{{{URL|}}}}}}}}}
            | FETCH_WIKIDATA
            | {{#if:{{#property:P856}}|{{Url|1={{#invoke:Wikidata|getValue|P856|FETCH_WIKIDATA}} }} }}
            |}}}}}}}}}}}

        | data40 = {{#if:{{both| {{{pushpin_map|}}} | {{both|{{{latd|}}}|{{{longd|}}}}} {{both|{{{coordinates_wikidata|{{{wikidata|}}}}}}|{{#property:P625}}}} }}| {{location map|{{{pushpin_map|}}} }}|{{#property:P625}} }}
        | data41 =
        }}
      """, "TestPage", lang, "all")

    (parsed) should be (answer)
  }
  "InfoboxMappingsTemplateExtractor" should """return correct property id's for real complex expressions 3  """ in {
    val input = """WikiPage(title=Disputed;ns=10/Template/Template;language:wiki=en,locale=en,392667,726972208,5075409,Plastikspork,{{
               {{{|safesubst:}}}#invoke:Unsubst||$N=Disputed |date=__DATE__ |$B=
                  <!--{{Disputed}} begin-->{{Ambox
                  | name  = Disputed
                  | image = [[File:Gnome-searchtool.svg|45px]]
                  | subst = <includeonly>{{subst:substcheck}}</includeonly>
                  | type  = content
                  | class = ambox-disputed
                  | issue = This {{{what|article}}}'s '''factual accuracy is [[Wikipedia:Accuracy dispute|disputed]]'''.
                  | fix   = Please help to ensure that disputed statements are [[Wikipedia:Identifying reliable sources|reliably sourced]].  {{#ifexist:{{TALKPAGENAME}}|See the relevant discussion on the [[{{{talkpage|{{{talk|{{TALKPAGENAME}}#{{{1|Disputed}}}}}}}}}|talk page]].}}
                  | small = {{{small}}}
                  | date  = {{{date|}}}
                  | cat   = Accuracy disputes
                  | all   = All accuracy disputes
                  | removalnotice = yes
                  }}<!--{{Disputed}} end-->
                  }}<noinclude>
                  {{Documentation}}
                  </noinclude>,text/x-wiki)"""



    val zw = parse(input, "Page", Language.English, "{{Disputed section|1=Explosion = detonation? Internal combustion engine: no explosion?|date=July 2016}}")
    ("") should be ("")
  }


    "InfoboxMappingsTemplateExtractor" should """return correct property id's for real complex expressions 4  """ in {

    val lang = Language.English
    val answer = List(("Infobox Test1","ISBN","P212"), ("Infobox Test1","website","P856"), ("Infobox Test1","ISBN_note","P212"), ("Infobox Test1","pushpin_map","P625"),
      ("Infobox Test1","ISBNT","P212"), ("Infobox Test1","URL","P856"), ("Infobox Test1","homepage","P856"), ("Infobox Test1","coordinates_wikidata","P625"),
      ("Infobox Test1","link","P212"), ("Infobox Test1","Url","P856"), ("Infobox Test1","location map","P625"),
      ("Infobox Test1","longd","P625"), ("Infobox Test1","latd","P625"))
    val input =
      """
        {{Infobox settlement
        | name                    = {{{name|}}}
        | official_name           = {{{official_name|Comune di {{{name}}}}}}
        | native_name             = {{{native_name|}}}
        | native_name_lang        = it
        | settlement_type         = {{{settlement_type|''{{lang|it|[[Comune]]}}''}}}
        | image_skyline           = {{{image_skyline|}}}
        | imagesize               = {{{imagesize|{{{image_size|}}}}}}
        | image_alt               = {{{image_alt|}}}
        | image_caption           = {{{image_caption|}}}
        | image_flag              = {{{image_flag|}}}
        | flag_size               = {{{flag_size|}}}
        | flag_alt                = {{{flag_alt|}}}
        | image_shield            = {{#ifexist:media:{{{image_shield}}}|{{{image_shield}}}|{{#ifexist:media:{{{img_coa}}}|{{{img_coa}}}}}}}
        | shield_size             = {{#if:{{{shield_size|}}}|{{{shield_size}}}|{{#if:{{{img_coa_small|}}}|px}}}}
        | shield_alt              = {{{shield_alt|}}}
        | nickname                = {{{nickname|}}}
        | motto                   = {{{motto|}}}
        | image_map               = {{{image_map|{{{map|}}}}}}
        | mapsize                 = {{{mapsize|}}}
        | map_alt                 = {{{map_alt|}}}
        | map_caption             = {{{map_caption|}}}
        | pushpin_map             = {{#if:{{{pushpin_map|}}}|{{{pushpin_map}}}|Italy}}
        | pushpin_label_position  = {{{pushpin_label_position|{{{locator_position|right}}}}}}
        | pushpin_map_alt         = {{{pushpin_map_alt|}}}
        | pushpin_mapsize         = {{{pushpin_mapsize|}}}
        | pushpin_map_caption     = Location of {{{name|{{{official_name}}}}}} in {{#if:{{{pushpin_map|}}}|{{#invoke:Location map|data|{{{pushpin_map}}}|name}}|Italy}}
        | coordinates             = {{#if:{{{coordinates|}}}|{{#invoke:Coordinates|coordinsert|{{{coordinates}}}|region:IT}}}}
        | coordinates_footnotes   = {{{coordinates_footnotes|}}}
        | subdivision_type        = Country
        | subdivision_name        = [[Italy]]
        | subdivision_type1       = [[Regions of Italy|Region]]
        | subdivision_name1       = {{{region|}}}
        | subdivision_type2       = {{#if:{{{metropolitan_city|}}}|[[Metropolitan cities of Italy|Metropolitan city]]|[[Provinces of Italy|Province]]}}
        | subdivision_name2       = {{#if:{{{metropolitan_city|}}}|{{{metropolitan_city}}}|{{{province|}}}}}
        | parts_type              = ''{{lang|it|[[Frazione|Frazioni]]}}''
        | parts_style             = para
        | parts                   =
        | p1                      = {{#if:{{{frazioni|}}}|<small>{{{frazioni}}}</small>}}
        | established_title       = {{#if:{{{established_date|}}}|{{{established_title|Founded}}}}}
        | established_date        = {{{established_date|}}}
        | leader_party            = {{#if:{{{mayor_party|}}}|{{{mayor_party}}}}}
        | leader_title            = {{#if:{{{mayor|}}}|Mayor}}
        | leader_name             = {{{mayor|}}}
        | area_footnotes          = {{{area_footnotes|}}}
        | area_total_km2          = {{{area_total_km2|}}}
        | population_footnotes    = {{{population_footnotes|}}}
        | population_total        = {{{population_total|}}}
        | population_as_of        = {{{population_as_of|}}}
        | population_density_km2  = {{#if:{{{area_total_km2|}}}|auto}}
        | population_demonym      = {{{population_demonym|{{{gentilic|}}}}}}
        | elevation_footnotes     = {{{elevation_footnotes|}}}
        | elevation_m             = {{{elevation_m|}}}
        | elevation_min_m         = {{{elevation_min_m|}}}
        | elevation_max_m         = {{{elevation_max_m|}}}
        | blank_name_sec1         = {{#if:{{{saint|}}}|Patron saint}}
        | blank_info_sec1         = {{{saint|}}}
        | blank1_name_sec1        = {{#if:{{{day|}}}|Saint day}}
        | blank1_info_sec1        = {{{day|}}}
        | timezone1               = [[Central European Time|CET]]
        | utc_offset1             = +1
        | timezone1_DST           = [[Central European Summer Time|CEST]]
        | utc_offset1_DST         = +2
        | postal_code_type        = Postal code
        | postal_code             = {{{postal_code|{{{postalcode|}}}}}}
        | area_code_type          = [[Area codes in Italy|Dialing&nbsp;code]]
        | area_code               = {{{area_code|{{{telephone|}}}}}}
        | website                 = {{{website|}}}
        | footnotes               = {{{footnotes|}}}
        }}
      """

    val testPage =
      """
        {{Infobox Italian comune
        | name                = Parma
        | official_name       = Comune di Parma
        | native_name         =
        | image_skyline       = Parma 01.jpg
        | imagesize           = 
        | image_alt           =
        | image_caption       = Palazzo del Governatore.
        | image_flag          = Flag of Parma.svg
        | image_shield        = Parma-Stemma.png
        | shield_alt          =
        | image_map           =
        | map_alt             =
        | map_caption         =
        | pushpin_label_position =
        | pushpin_map_alt     =
        | pushpin_map         = Italy#Emilia-Romagna#Europe
        | coordinates         = {{coord|44|48|N|10|20|E|display=inline,title}}
        | coordinates_footnotes =
        | region              = [[Emilia-Romagna]]
        | province            = [[Province of Parma|Parma]] (PR)
        | frazioni            = See [[#Frazioni|list]]
        | mayor_party         = [[Independent politician|Independent]]
        | mayor               = [[Federico Pizzarotti]]
        | area_footnotes      =
        | area_total_km2      = 260.77
        | population_footnotes ={{r|ISTAT_PR2016}}
        | population_total    = 195065
        | population_as_of    = 31 July 2017
        | pop_density_footnotes =
        | population_demonym  = (it) Parmigiani (Pram'zan) (Parmensi (Arijoz) are <br /> called the province's inhabitants)<br />(en) Parmesan/s
        | elevation_footnotes =
        | elevation_m         = 55
        | twin1               =
        | twin1_country       =
        | saint               = [[Hilary of Poitiers|Sant'Ilario di Poitiers]], [[Saint Honoratus|Sant'Onorato]], [[Saint Roch|San Rocco]]
        | day                 = January 13
        | postal_code         = 43121-43126
        | area_code           = 0521
        | website             = {{official website|http://www.comune.parma.it}}
        | footnotes           =
        }}
      """

    val zw = parse(input, "Page", lang, testPage)


    ("") should be (answer)
  }*/


  "InfoboxMappingsTemplateExtractor" should """return correct property id's for real complex expressions 5  """ in {
    val input = """<!--This template employs some extremely complicated and esoteric features of template syntax. Please do not attempt to alter it unless you are certain that you understand the setup and are prepared to repair any consequent collateral damage if the results are unexpected. Any experiments should be conducted in the template sandbox or your user space.
                  --><includeonly>{{Infobox
                  | bodyclass = [http://dbpedia.org/resource/{{{Name}}}]
                  | bodystyle=width: 22em; font-size: 88%
                  | aboveclass = fn org
                  | abovestyle=line-height:1.2em; font-size: 1.2em; padding: 0.4em 0.8em 0.4em
                  | above = {{{Fullname}}}

                  | imageclass = maptable
                  | image = <table style="align:center;text-align:center;background: none; width:100%;"><tr>
                  <td style="width: 58%; vertical-align: middle; text-align:center;">[[File:{{{Flag}}}|125px|{{#ifeq:{{{Name}}}|Ohio||border}}|{{#if: {{{FlagAlt|}}} |alt={{{FlagAlt}}}}}|Flag of {{{Name}}}]]</td>
                  <td style="width: 42%; vertical-align: middle; text-align:center;">[[File:{{{Seal}}}|85px|{{#if:{{{SealAlt|}}} |alt={{{SealAlt}}}}}|State seal of {{{Name}}}]]</td>
                  </tr><tr style="font-size: 83%;">
                  <td style="width:58%;text-align:center;">{{{Flaglink|[[Flag of {{{Name}}}|Flag]]}}}</td>
                  <td style="width:42%;text-align:center;">{{{Seallink|[[Seal of {{{Name}}}|Seal]]}}}</td>
                  </tr></table>

                  | data1 =  [[List of U.S. state nicknames|Nickname(s)]]: <span class="nickname">{{{Nickname}}}</span>

                  | data2 = {{#if: {{{Motto|}}} | [[List of U.S. state and territory mottos|Motto(s)]]: {{{Motto}}} }}

                  | data3 = {{#if: {{{StateAnthem|}}} | [[List of U.S. state songs|State song(s)]]: "<span class="State anthem">{{{StateAnthem}}}</span>" }}

                  | rowclass4 = maptable
                  | data4 = [[File:{{{Map}}}|center|270px|{{#if: {{{MapAlt|}}} |alt={{{MapAlt}}}}}|Map of the United States with {{{Name}}} highlighted]]

                  | label5 = [[Languages of the United States|Official language]]
                  | data5  = {{{OfficialLang|}}}

                  | rowclass6 = mergedrow
                  | label6 = [[Languages of the United States|Spoken languages]]
                  | data6 = {{{Languages|}}}

                  | rowclass7 = mergedrow
                  | label7 = [[Demonym]]
                  | data7 =  {{{Demonym|}}}

                  | rowclass8 = mergedtoprow
                  | label8 = {{#ifeq:{{{LargestCity}}}|capital
                    |[[List of capitals in the United States|Capital]]<br/>{{nobold|([[List of U.S. states' largest cities by population|and largest city]])}}
                    |[[List of capitals in the United States|Capital]]
                    }}
                  | data8 = {{{Capital}}}

                  | rowclass9 = mergedrow
                  | label9 = {{#ifeq:{{{LargestCity}}}|capital
                     |
                     |[[List of U.S. states' largest cities by population|Largest city]]
                     }}
                  | data9 = {{#ifeq:{{{LargestCity}}}|capital||{{{LargestCity}}}}}

                  | rowclass10 = mergedrow
                  | label10 = [[List of Metropolitan Statistical Areas|Largest metro]]
                  | data10 = {{{LargestMetro|}}}

                  | rowclass11 = mergedtoprow
                  | label11 = Area
                  | data11 = [[List of U.S. states and territories by area|Ranked {{{AreaRank}}}]] <!--
                    -->{{Infobox|child=yes
                    | labelstyle=font-weight:normal

                    | rowclass1 = mergedrow
                    | label1 = &nbsp;•&nbsp;Total
                    | data1 = {{{TotalAreaUS}}}&nbsp;sq&nbsp;mi <br/>({{{TotalArea}}} km<sup>2</sup>)

                    | rowclass2 = mergedrow
                    | label2 = &nbsp;•&nbsp;Width
                    | data2 = {{{WidthUS}}}&nbsp;miles&nbsp;({{{Width}}} km)

                    | rowclass3 = mergedrow
                    | label3 = &nbsp;•&nbsp;Length
                    | data3 = {{{LengthUS}}}&nbsp;miles&nbsp;({{{Length}}} km)

                    | rowclass4 = mergedrow
                    | label4 = &nbsp;•&nbsp;% water
                    | data4 = {{{PCWater}}}

                    | rowclass5 = mergedrow
                    | label5 = &nbsp;•&nbsp;Latitude
                    | data5 = {{{Latitude}}}

                    | rowclass6 = mergedrow
                    | label6 = &nbsp;•&nbsp;Longitude
                    | data6 = {{{Longitude}}}
                  }}

                  | rowclass15 = mergedtoprow
                  | label15 = Population
                  | data15 = [[List of U.S. states and territories by population|Ranked {{{PopRank}}}]] <!--
                    -->{{Infobox|child=yes
                    | labelstyle=font-weight:normal

                    | rowclass1 = mergedrow
                    | label1 = &nbsp;•&nbsp;Total
                    | data1 = {{{2010Pop|{{{2000Pop}}}}}}

                    | rowclass2 = {{#if:{{{MedianHouseholdIncome|}}}|mergedrow|mergedbottomrow}}
                    | label2 = &nbsp;•&nbsp;[[List of U.S. states by population density|Density]]
                    | data2 = {{{2010DensityUS|{{{2000DensityUS}}}}}}/sq&nbsp;mi&nbsp; ({{{2010Density|{{{2000Density}}}}}}/km<sup>2</sup>)<br />[[List of U.S. states by population density|Ranked {{{DensityRank}}}]]

                    | rowclass3 = mergedbottomrow
                    | label3 = &nbsp;•&nbsp;[[Household income in the United States#Income by state|Median household income]]
                    | data3 = {{#if:{{{MedianHouseholdIncome|}}}|{{{MedianHouseholdIncome}}} ({{{IncomeRank}}}) }}
                    }}

                  | rowclass20 = mergedtoprow
                  | label20 = [[List of U.S. states by elevation|Elevation]]
                  | data20 = <!--
                   -->{{Infobox|child=yes
                    | labelstyle=font-weight:normal

                    | rowclass1 = mergedrow
                    | label1 = &nbsp;•&nbsp;Highest point
                    | data1 = {{{HighestPoint}}}<br/>{{{HighestElevUS}}}&nbsp;ft&#32;({{{HighestElev}}} m)

                    | rowclass2 = mergedrow
                    | label2 = &nbsp;•&nbsp;Mean
                    | data2 = {{{MeanElevUS}}}&nbsp;ft&nbsp; ({{{MeanElev}}} m)

                    | rowclass3 = mergedbottomrow
                    | label3 = &nbsp;•&nbsp;Lowest point
                    | data3 = {{#if: {{{LowestPoint|}}} |{{{LowestPoint}}}<br/>}} {{#ifeq:{{{LowestElev}}}|0|sea level|{{{LowestElevUS}}}&nbsp;ft&#32;({{{LowestElev}}} m)}}
                  }}
                  | rowclass25 = mergedtoprow
                  | label25 = Before statehood
                  | data25 = {{#ifexist:{{{Former|}}}|[[{{{Former|}}}]]|{{{Former|}}}}}

                  | rowclass26 = {{#if:{{{Former|}}}|mergedbottomrow}}
                  | label26 = [[Admission to the Union|Admission to Union]]
                  | data26 = {{{AdmittanceDate}}} ({{{AdmittanceOrder}}})

                  | rowclass27 = mergedtoprow
                  | label27 = [[Governor of {{{Name}}}|Governor]]
                  | data27 = {{{Governor}}}

                  | rowclass28 = mergedrow
                  | label28 = [[Lieutenant Governor of {{{Name}}}|{{{Lieutenant Governor_alt|Lieutenant Governor}}}]]
                  | data28 = {{{Lieutenant Governor}}}

                  | rowclass29 = mergedtoprow
                  | label29 = [[Legislature]]
                  | data29 = {{{Legislature}}} <!--
                   -->{{infobox|child=yes
                    | labelstyle=font-weight:normal

                    | rowclass1 = mergedrow
                    | label1 = &nbsp;•&nbsp;[[Upper house]]
                    | data1 = {{{Upperhouse}}}

                    | rowclass2 = mergedrow
                    | label2 = &nbsp;•&nbsp;[[Lower house]]
                    | data2 = {{{Lowerhouse}}}
                    }}

                  | rowclass35 = mergedtoprow
                  | label35 = [[List of United States Senators from {{{Name}}}|U.S. Senators]]
                  | data35 = {{{Senators}}}

                  | rowclass36 = mergedbottomrow
                  | label36 = [[United States House of Representatives|U.S. House delegation]]
                  | data36 = {{#if:{{{Representative|}}}
                    | {{{Representative}}} ([[United States congressional delegations from {{{Name}}}|list]])
                    | [[United States congressional delegations from {{{Name}}}|List]]
                    }}

                  | rowclass37 = mergedtoprow
                  | label37 = [[List of time offsets by U.S. state|Time zone{{#if: {{{TimeZone2|}}}|s}}]]
                  | data37 = {{#if: {{{TimeZone2|}}}
                    | {{{TZDesc|&nbsp;}}} <!--
                    -->{{infobox|child = yes
                      | labelstyle=font-weight:normal

                      | rowclass1 = mergedrow
                      | label1 = &nbsp;• {{{TZ1Where}}}
                      | data1 = {{{TimeZone}}}

                      | rowclass2 = mergedrow
                      | label2 = &nbsp;• {{{TZ2Where}}}
                      | data2 = {{{TimeZone2}}}
                      }}
                    | {{{TimeZone}}}
                    }}

                  | label38 = [[ISO 3166]]
                  | data38 = {{#if:{{{ISOCode| }}}| [[ISO 3166-2:US|{{{ISOCode}}}]] }}

                  | label39 = Abbreviations
                  | data39 = [[List of U.S. state abbreviations#Postal codes|{{{PostalAbbreviation| }}}]]{{#if: {{{TradAbbreviation|}}} | , [[List of U.S. state abbreviations#Current use of traditional abbreviations|{{{TradAbbreviation| }}}]]}}

                  | rowclass40 = mergedtoprow
                  | label40 = Website
                  | data40 = {{#if:{{{Website|}}}| {{URL|1={{{Website}}}}} }}

                  | rowclass41 = mergedtoprow
                  | data41 = {{#if: {{{Footnotes|}}}| '''Footnotes:''' {{{Footnotes}}} }}

                  }}</includeonly><noinclude>{{Documentation}}</noinclude>"""


    val testTemplate = """{{US state
      |Name            = Kansas
      |Fullname        = State of Kansas
      |Flag            = Flag of Kansas.svg
      |Flaglink        = [[Flag of Kansas|Flag]]
      |Seal            = Seal of Kansas.svg
      |Map             = Kansas in United States.svg
      |BorderingStates = [[Nebraska]], [[Missouri]],<br />[[Oklahoma]], [[Colorado]]
      |OfficialLang    = English<ref>{{cite web|url=http://www.us-english.org/inc/news/preleases/viewRelease.asp?ID=252 |title=Governor's Signature Makes English the Official Language of Kansas |publisher=US English |date=May 11, 2007 |accessdate=August 6, 2008 |deadurl=yes |archiveurl=https://web.archive.org/web/20070710015939/http://www.us-english.org/inc/news/preleases/viewRelease.asp?ID=252 |archivedate=July 10, 2007}}</ref>
        |Nickname        = The Sunflower State (official);<br />The Wheat State;<br />The Free State<ref>{{cite web |url=http://www.legendsofkansas.com/freestate.html|title=Free-Staters of Kansas|publisher=legendsofkansas.com}}</ref>
        |Former          = Kansas Territory
        |Demonym         = Kansan
      |Motto           = [[Per aspera ad astra|Ad astra per aspera]] ([[Latin language|Latin]] for ''To the stars through difficulties'')
      |StateAnthem     = [[Home on the Range]]
      |Capital         = [[Topeka, Kansas|Topeka]]
      |LargestCity     = [[Wichita, Kansas|Wichita]]
      |Governor        = [[Jeff Colyer]] ([[Republican Party (United States)|R]]) <!--Taking office January 31, 2018: [[Jeff Colyer]] (R)-->
      |Lieutenant Governor = TBD<!-- Taking office 2018: TBD-->
        |Legislature     = [[Kansas Legislature]]
      |Upperhouse      = [[Kansas Senate|Senate]]
      |Lowerhouse      = [[Kansas House of Representatives|House of Representatives]]
      |Senators        = [[Pat Roberts]] (R)<br />[[Jerry Moran]] (R)
      |Representative  = [[Roger Marshall (politician)|Roger Marshall]] (R)<br />[[Lynn Jenkins]] (R)<br />[[Kevin Yoder]] (R)<br />[[Ron Estes]] (R)
      |PostalAbbreviation = KS
      |TradAbbreviation = Kan., Kans.
        |AreaRank        = 15th
        |TotalAreaUS     = 82,278<ref name="auto">{{cite web|url=https://www.census.gov/geo/reference/state-area.html|title=State Area Measurements and Internal Point Coordinates|first=US Census Bureau|last=Geography|publisher=}}</ref>
        |TotalArea       = 213,100
      |LandAreaUS      = 81,759<ref name="auto"/>
        |LandArea        = 211,754
      |WaterAreaUS     = 520<ref name="auto"/>
        |WaterArea       = 1,346
      |PCWater         = 0.6<ref>{{cite web|url=http://water.usgs.gov/edu/wetstates.html|title=Area of each state that is water|first=Howard Perlman,|last=USGS|publisher=}}</ref>
        |PopRank = 35th
        |2010Pop = 2,907,289 (2016 est.)<ref name=PopHousingEst>{{cite web|url=https://www.census.gov/programs-surveys/popest.html |title=Population and Housing Unit Estimates |date=June 21, 2017 |accessdate=June 21, 2017|publisher=[[U.S. Census Bureau]]}}</ref>
        |DensityRank     = 40th
        |2000DensityUS   = 35.1
      |2000Density     = 13.5
      |MedianHouseholdIncome = $54,865<ref>{{cite web|url=http://kff.org/other/state-indicator/median-annual-income/?currentTimeframe=0|work=The Henry J. Kaiser Family Foundation|title=Median Annual Household Income|accessdate=December 9, 2016}}</ref>
        |IncomeRank      = 30th
        |AdmittanceOrder = 34th
        |AdmittanceDate  = January 29, 1861<div>
      [[Kansas Day]]
      |TimeZone        = [[Central Time Zone (North America)|Central]]: [[Coordinated Universal Time|UTC]] [[Central Standard Time|−6]]/[[Central Daylight Time|−5]]
      |TZ1Where        = Primary
      |TimeZone2       = [[Mountain Time Zone|Mountain]]: [[Coordinated Universal Time|UTC]] [[Mountain Standard Time|−7]]/[[Mountain Daylight Time|−6]]
      |TZ2Where        = [[Hamilton County, Kansas|Hamilton]], [[Greeley County, Kansas|Greeley]], [[Wallace County, Kansas|Wallace]], and [[Sherman County, Kansas|Sherman]] counties
      |Latitude        = [[37th parallel north|37° N]] to [[40th parallel north|40° N]]
      |Longitude       = 94° 35′ W to 102° 3′ W
      |WidthUS         = 410<ref name="netstate.com">{{cite web|url=http://www.netstate.com/states/geography/ks_geography.htm|title=Kansas Geography from NETSTATE|publisher=}}</ref>
        |Width           = 660
      |LengthUS        = 213<ref name="netstate.com"/>
        |Length          = 343
      |HighestPoint = [[Mount Sunflower]]<ref name=USGS>{{cite web|url=http://egsc.usgs.gov/isb/pubs/booklets/elvadist/elvadist.html |title=Elevations and Distances in the United States |publisher=[[United States Geological Survey]] |year=2001 |accessdate=October 21, 2011 |deadurl=yes |archiveurl=https://web.archive.org/web/20111015012701/http://egsc.usgs.gov/isb/pubs/booklets/elvadist/elvadist.html |archivedate=October 15, 2011 }}</ref><ref name= NAVD88>Elevation adjusted to [[North American Vertical Datum of 1988]].</ref>
      |HighestElevUS   = 4,041
      |HighestElev     = 1232
      |MeanElevUS      = 2,000
      |MeanElev        = 610
      |LowestPoint     = [[Verdigris River]] at {{nobreak|[[Oklahoma]] border}}<ref name=USGS/><ref name=NAVD88/>
        |LowestElevUS    = 679
      |LowestElev      = 207
      |ISOCode         = US-KS
      |Website         = www.kansas.gov
      |LargestMetro=Kansas portion of [[Kansas City Metropolitan Area|Kansas City, Metropolitan Area]]}}"""


    val zw = parse(input, "US state", Language.English, testTemplate)
    ("") should be ("")
  }

  private def parse(input : String, title: String = "TestPage", lang: Language = Language.English, test : String) : Option[TemplateNode] = {
    val wikipage = new WikiPage(WikiTitle.parse(title, lang), input)
    val testPage = parser(new WikiPage(WikiTitle.parse("test", lang), test))

    InfoboxMappingsTemplateExtractorTest.getTuplesFromConditionalExpressions(wikipage, Language.English).flatMap(t => {
      val red = new TemplatePropertyRedirects(lang)
      red.enterTemplate(title, t)
      val resolved = testPage.get.children.find(p => p.isInstanceOf[TemplateNode] && p.asInstanceOf[TemplateNode].title.decoded.contains("US state")) match {
        case Some(te) =>
          val temp = te.asInstanceOf[TemplateNode]
          Option(red.resolveTemplate(temp))
        case None => None
      }
      resolved.flatten
    })
  }
}


object InfoboxMappingsTemplateExtractorTest {

  val context = new {
    def ontology =
    {
      val ontoFilePath = "../ontology.xml"
      val ontoFile = new File(ontoFilePath)
      val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
      new OntologyReader().read(ontologySource)
    }
    def language = "en"
    def redirects = new Redirects()
  }



  def getTuplesFromConditionalExpressions(page : WikiPage, lang : Language) : Option[TemplateNode] = {

    val simpleParser = WikiParser.getInstance("simple")
    //val swebleParser = WikiParser.getInstance("sweble")
    //val tempPageSweble = new WikiPage(page.title, page.source)
    val tempPageSimple = new WikiPage(page.title, page.source)
    //val templateNodesSweble = ExtractorUtils.collectTemplatesFromNodeTransitive(swebleParser.apply(tempPageSweble, context.redirects).orNull)
    val infoboxPage = simpleParser.apply(tempPageSimple, context.redirects).orNull
    val templateNodesSimple = ExtractorUtils.collectTemplatesFromNodeTransitive(infoboxPage)

    //val infoboxesSweble = templateNodesSweble.filter(p => p.title.toString().contains(infoboxNameMap.getOrElse(lang.wikiCode, "Infobox")))
    val infoboxesSimple = templateNodesSimple.filter(p => p.title.toString().contains("box"))

    infoboxesSimple.headOption
  }


}