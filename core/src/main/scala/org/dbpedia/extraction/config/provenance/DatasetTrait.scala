package org.dbpedia.extraction.config.provenance

/**
  * Created by Chile on 11/15/2016.
  *
  * TODO extend further
  */
object DatasetTrait extends Enumeration {
  val Ordered = Value
  val Validated = Value
  val Published = Value
  val Temporary = Value
  val Virtual = Value
  val Deprecated = Value
  val Testeset = Value
  val Provenance = Value
  val LinkedData = Value
  val Unredirected = Value
  val EnglishUris = Value
  val WikidataUris = Value
}
