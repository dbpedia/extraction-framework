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
   val timeStamp: Long,                               // the timestamp of this revision
   val metadata: ProvenanceMetadata                   // the actual metadata (domain specific)
 ) extends Recordable[Quad]{

  def copy(
    id: Long = this.id,
    gfhIri: String = this.gfhIri,
    triple: TripleRecord = this.triple,
    timeStamp: Long = this.timeStamp,
    metadata: ProvenanceMetadata
  ): ProvenanceRecord ={
    new ProvenanceRecord(id, gfhIri, triple, timeStamp, metadata)
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
}

trait ProvenanceMetadata{

}

case class DBpediaMetadata(
    revision: Long,					                        // revision nr
    namespace: Int,					                        // namespace nr (important to distinguish between Category and Main)
    line: Int,						                          // line nr
    language: String,                               // the associated wiki language
    datasets: Seq[String],				                  // the uris of the datasets this triple ended up in
    extractor: Option[ExtractorRecord] = None,			// the extractor record
    transformer: Option[TransformerRecord] = None,	// the transformer record
    replacing: Seq[Long] = Seq()		                // points out the prov-record of the triple which is replaced by this triple with mapping based info
  )extends ProvenanceMetadata{
  def this(
    nr: NodeRecord,
    datasets: Seq[String],				                  // the uris of the datasets this triple ended up in
    extractor: Option[ExtractorRecord],			        // the extractor record
    transformer: Option[TransformerRecord],	        // the transformer record
    replacing: Seq[Long]		                        // points out the prov-record of the triple which is replaced by this triple with mapping based info
  ) =
  this(nr.revision, nr.namespace, nr.line, nr.language, datasets, extractor, transformer, replacing)
}

case class NodeRecord(
     uri: String,
     revision: Long,					                          // revision nr
     namespace: Int,					                        // namespace nr (important to distinguish between Category and Main)
     line: Int,						                            // line nr,
     language: String                                 // the associated wiki language
    ){

  def copy(line: Option[Int] = None, timeStamp: Option[Long] = None, namespace: Option[Int] = None, language: Option[String] = None): NodeRecord ={
    NodeRecord(
      this.uri,
      this.revision,
      namespace.getOrElse(this.namespace),
      line.getOrElse(this.line),
      language.getOrElse(this.language)
    )
  }
}

case class TripleRecord(
     s: String,                                       // subject - IRI
     p: String,                                       // predicate - IRI
     o: String,                                       // object - ANY
     l: Option[String] = None,                        // language - String
     d: Option[String] = None                        // datatype - IRI
   )

case class ParserRecord(
      uri: String,
      wikiText: String,					                      // the string length of the origin wiki text
      transformed: String,					                  // the transformed wiki text after expanding the nested templates
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

//TODO
case class TransformerRecord(
      uri: String
    )