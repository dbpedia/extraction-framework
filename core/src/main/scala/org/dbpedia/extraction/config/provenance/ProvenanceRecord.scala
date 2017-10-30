package org.dbpedia.extraction.config.provenance

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, ObjectWriter}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.dbpedia.extraction.config.Recordable
import org.dbpedia.extraction.transform.Quad

@JsonIgnoreProperties(Array("id"))
class ProvenanceRecord (
   val id: Long,						                          // the internal id hash
   val gfhIri: String,                                // the sha256 hash based IRI of the triple
   val triple: TripleRecord,                          // the triple in question
   val rootRev: Long,					                        // revision nr
   val timeStamp: Long,                               // the timestamp of this revision
   val namespace: Int,					                      // namespace nr (important to distinguish between Category and Main)
   val line: Int,						                          // line nr
   val language: String,                              // the associated wiki language
   val datasets: Seq[String],				                  // the uris of the datasets this triple ended up in
   val replacing: Seq[Long] = Seq(),		              // points out the prov-record of the triple which is replaced by this triple with mapping based info
   val extractor: Option[ExtractorRecord] = None,			// the extractor record
   val transformer: Option[TransformerRecord] = None	// the transformer record
 ) extends Recordable[Quad]{

  def this(
      id: Long,
      gfhIri: String,                                 // the sha256 hash based IRI of the triple
      triple: TripleRecord,                           // the triple in question
      nr: NodeRecord,
      datasets: Seq[String],				                  // the uris of the datasets this triple ended up in
      replacing: Seq[Long],		                        // points out the prov-record of the triple which is replaced by this triple with mapping based info
      extractor: Option[ExtractorRecord],			        // the extractor record
      transformer: Option[TransformerRecord]	        // the transformer record
    ) =
    this(id, gfhIri, triple, nr.rootRev, nr.timeStamp, nr.namespace, nr.line, nr.language, datasets, replacing, extractor, transformer)

  def copy(
    id: Long = this.id,
    gfhIri: String = this.gfhIri,
    triple: TripleRecord = this.triple,
    rootRev: Long = this.rootRev,
    timeStamp: Long = this.timeStamp,
    namespace: Int = this.namespace,
    line: Int = this.line,
    language: String,
    datasets: Seq[String] = this.datasets,
    replacing: Seq[Long] = this.replacing,
    extractor: Option[ExtractorRecord] = this.extractor,
    transformer: Option[TransformerRecord] = this.transformer
  ): ProvenanceRecord ={
    new ProvenanceRecord(id, gfhIri, triple, rootRev, timeStamp, namespace, line, language, datasets, replacing, extractor, transformer)
  }

  override def recordEntries = List()

  override def toString: String ={
    ProvenanceRecord.mapper.writerWithDefaultPrettyPrinter[ObjectWriter]().writeValueAsString(this)
  }
}

object ProvenanceRecord{
  private val mapper = new ObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def copyNodeRecord(rec: NodeRecord, line: Option[Int] = None, timeStamp: Option[Long] = None, namespace: Option[Int] = None, language: Option[String] = None): NodeRecord ={
    NodeRecord(
      rec.uri,
      rec.rootRev,
      timeStamp.getOrElse(rec.timeStamp),
      namespace.getOrElse(rec.namespace),
      line.getOrElse(rec.line),
      language.getOrElse(rec.language)
    )
  }
}

case class NodeRecord(
     uri: String,
     rootRev: Long,					                          // revision nr
     timeStamp: Long,                                 // the timestamp of this revision
     namespace: Int,					                        // namespace nr (important to distinguish between Category and Main)
     line: Int,						                            // line nr,
     language: String                                 // the associated wiki language
    )

case class TripleRecord(
     s: String,
     p: String,
     o: String
   )

case class ParserRecord(
      uri: String,
      wikiText: String,					                      // the string length of the origin wiki text
      resultValue: String					                    // the string length of the actual value
   )

case class ExtractorRecord(
      uri: String,
      parser: ParserRecord,                           // the parser record of the value extracted
      splits: Option[Int],                            // number of parse results
      template: Option[String] = None,			          // name of the infobox template
      property: Option[String] = None,			          // name of the infobox property
      mapping: Option[String] = None,			            // name/uri of the mapping used
      templatesEncountered: Seq[String] = Seq(),	    // names of nested templates encountered in this line
      templatesTransformed: Seq[String] = Seq(),	    // names of nested templated transformed (expanded) before extraction
      alternativeValues: Seq[Long] = Seq()		        // points out the prov-records for alternative triples for this property (extracted by other  parsers)
    )

case class TransformerRecord(
      uri: String
    )