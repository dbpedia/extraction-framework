package org.dbpedia.extraction.util

import org.wikidata.wdtk.datamodel.interfaces._
import org.wikidata.wdtk.datamodel.json.jackson.datavalues.JacksonValueMonolingualText

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

  def getItemId(value:Value) = value match {
    case v:ItemIdValue => replaceItemId(v.toString).replace(wikidataDBpNamespace,"")
    case _ => "V"+getHash(value)
  }

  def getStatementUri(subject:String, property:String,value:Value):String = {
    subject+"_"+ property.replace(WikidataUtil.wikidataDBpNamespace, "").trim+"_" + getItemId(value)
  }

  def getStatementUriWithHash(subject:String, property:String,value:Value,statementHash:String):String = {
    subject+"_"+ property.replace(WikidataUtil.wikidataDBpNamespace, "").trim+"_" + getItemId(value) + "_" + statementHash
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
    case value : JacksonValueMonolingualText => {
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
    case value:JacksonValueMonolingualText => {
      // Do we need value.getLanguageCode?
      value.getText
    }
    case value: PropertyIdValue => {
      getWikidataNamespace(value.getIri)
    }
    case _=> value.toString
  }

  def getWikidataNamespace(namespace: String): String = {
    namespace.replace(WikidataUtil.wikidataDBpNamespace, "http://wikidata.org/entity/")
  }

  val wikidataDBpNamespace = Language("wikidata").resourceUri.namespace
}
