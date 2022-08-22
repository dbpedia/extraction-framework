package org.dbpedia.extraction.util

import org.wikidata.wdtk.datamodel.implementation.MonolingualTextValueImpl
import org.wikidata.wdtk.datamodel.interfaces._

/**
 * Created by ali on 2/1/15.
 */
object WikidataUtil {
  def replacePunctuation(value:String,lang:String=""): String = {
    value.replace("(" + lang + ")", "").replace("[","").replace("]","").replace("\"","").trim()
  }

  def replaceItemId(item:String):String= {
    item.replace("(item)","").toString().trim()
  }

  def replaceString(str:String):String = {
    str.replace("(String)","").trim()
  }
  def replaceSpaceWithUnderscore(str: String): String = {
    str.replace(" ", "_")
  }
  def getItemId(value:Value) = value match {
    case v:ItemIdValue => replaceItemId(v.toString).replace(wikidataDBpNamespace,"")
    case _ => "V"+getHash(value)
  }

  def getUrl(value: Value): String = {
    value.toString.split(" ")(0)
  }
  def getId(value:Value): String = {
    value.toString.split(" ")(0).replace(WikidataUtil.wikidataDBpNamespace, "")
  }

  def getStatementUri(subject:String, property:String,value:Value):String = {
    subject+"_"+ property.replace(WikidataUtil.wikidataDBpNamespace, "").trim+"_" + getItemId(value)
  }

  def getStatementUriWithHash(subject:String, property:String,value:Value,statementId:String):String = {
    subject+"_"+ property.replace(WikidataUtil.wikidataDBpNamespace, "").trim+"_" + getItemId(value) + "_" + getStatementHash(statementId)
  }

  def getStatementHash(statementId:String): String ={
    statementId.substring(statementId.indexOf("$")+1,statementId.indexOf("$")+6)
  }

  def getHash(value:Value):String={
    val hash_string = value.toString
    StringUtils.md5sum(hash_string).substring(0,5)
  }

  def getDatatype(value: Value) = value match {
    case value: ItemIdValue => {
      null
    }
    case value: StringValue => {
      if (value.toString.contains("http://") || value.toString.contains("https://"))
        null
      else
      "xsd:string"
    }

    case value: TimeValue => {
      "xsd:date"
    }
    case value: GlobeCoordinatesValue => {
      "xsd:string"
    }
    case value: QuantityValue => {
      "xsd:float"
    }
    case value : MonolingualTextValue => {
      "xsd:string"
    }
    case _=> null
  }

  def getValue(value: Value) = value match {
    case value: ItemIdValue => {
      WikidataUtil.replaceItemId(value.toString)
    }
    case value: StringValue => {
      WikidataUtil.replacePunctuation(replaceString(value.toString))
    }

    case value: TimeValue => {
      value.getYear + "-" + value.getMonth + "-" + value.getDay

    }
    case value: GlobeCoordinatesValue => {
      value.getLatitude + " " + value.getLongitude
    }

    case value: QuantityValue => {
      value.getNumericValue.toString
    }
    case value: MonolingualTextValue => {
      // Do we need value.getLanguageCode?
      value.getText
    }
    case value: PropertyIdValue => {
      getWikidataNamespace(value.getIri)
    }
    case value: FormIdValue => {
      getWikidataNamespace(value.getIri)
    }
    case value: LexemeIdValue => {
      getWikidataNamespace(value.getIri)
    }
    case value: SenseIdValue => {
      getWikidataNamespace(value.getIri)
    }
    case _=> value.toString
  }
  def getWikiCommonsUrl(file: String): String = {
    val url = "http://commons.wikimedia.org/wiki/File:"+WikidataUtil.replaceSpaceWithUnderscore(file)
    url
  }
  def getWikidataNamespace(namespace: String): String = {
    namespace.replace(WikidataUtil.wikidataDBpNamespace, "http://www.wikidata.org/entity/")
  }

  val wikidataDBpNamespace = Language("wikidata").resourceUri.namespace
}
