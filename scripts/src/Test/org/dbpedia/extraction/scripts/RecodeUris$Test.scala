package org.dbpedia.extraction.scripts

import org.dbpedia.iri.UriUtils
import org.scalatest._

/**
  * Created by Chile on 10/4/2016.
  */
class RecodeUris$Test extends FunSuite {

  test("uriToIri") {
    info(UriUtils.uriToDbpediaIri("http://dbpedia.org/resource/Robert_Sch\\u00F6ller"))
    info(UriUtils.uriToDbpediaIri("http://dbpedia.org/resource/Robert_Schöller"))
    info(UriUtils.uriToDbpediaIri("http://dbpedia.org/resource/Mírzá_`Abbás_Núrí"))
    info(UriUtils.uriToDbpediaIri("http://dbpedia.org/resource/%22H%22_Is_for_Homicide"))
    info(UriUtils.uriToDbpediaIri("http://dbpedia.org/resource/\"H\"_Is_for_Homicide"))
    info(UriUtils.uriToDbpediaIri("http://pt.dbpedia.org/resource/%25C3%2581rea_de_Re…"))
    info(UriUtils.uriToDbpediaIri("http://dbpedia.org/resource/Rosman_Sulaiman__4"))
    info(UriUtils.uriToDbpediaIri("http://nl.dbpedia.org/resource/Tord_Tamerlan_Teodor_Thorell%3EThorell"))
    info(UriUtils.uriToDbpediaIri("http://dbpedia.org/class/yago/WikicatNationalRainbowCoalition– KenyaPoliticians"))
    info(UriUtils.uriToDbpediaIri("http://dbpedia.org/resource/Robert_Sch\\u00F6ller?oldid=702093022#absolute-??line=13&template=Infobox_scientist&property=field&mapped="))
    info(UriUtils.uriToDbpediaIri("http://de.dbpedia.org/resource/Robert_Sch%C3%B6ller?oldid=702093022#absolute-[]line=13&template=Infobox_scienti%C3%B6st&property=field&mapped="))
    info(UriUtils.uriToDbpediaIri("http://dbpedia.org/resource/Robert_Sch%C3%B6ller?oldid=702093022#absolute-line=13&template=Infobox_scienti%C3%B6st&property=field&mapped="))
  }
}
