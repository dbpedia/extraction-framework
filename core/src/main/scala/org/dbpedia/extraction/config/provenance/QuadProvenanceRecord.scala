package org.dbpedia.extraction.config.provenance

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.dbpedia.extraction.config.Recordable
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.iri.IRI

@JsonIgnoreProperties(Array("id"))
class QuadProvenanceRecord(
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
  ): QuadProvenanceRecord ={
    new QuadProvenanceRecord(id, gfhIri, triple, timeStamp, metadata)
  }

  override def recordEntries = List()

  override def toString: String = gfhIri
}

trait ProvenanceMetadata{
  def datasetIri: IRI
  def language: Language
}

case class NodeRecord(
       sourceUri: IRI, // the wikipedia source uri
       nodeType: IRI, // uri id of the node
       revision: Long, // revision nr
       namespace: Int, // namespace nr (important to distinguish between Category and Main)
       internalId: Long, // if page node -> wiki page id, else EF internal id (which is a negative Long to distinguish them from wiki page id)
       language: Language, // the associated wiki language
       absoluteLine: Option[Int] = Some(0), // section name if any
       name: Option[String] = None, // property name (if property node)
       section: Option[String] = None // absolute line nr
    ) extends ProvenanceMetadata{

  def copy(
        sourceUri: Option[IRI] = Option(this.sourceUri),
        nodeType: Option[IRI] = Option(this.nodeType),
        revision: Option[Long] = Option(this.revision),
        namespace: Option[Int] = Option(this.namespace),
        internalId: Option[Long] = Option(this.internalId),
        language: Option[Language] = Option(this.language),
        absoluteLine: Option[Int] = this.absoluteLine,
        name: Option[String] = this.section,
        section: Option[String] = this.section): NodeRecord ={
    NodeRecord(
      sourceUri.getOrElse(this.sourceUri),
      nodeType.getOrElse(this.nodeType),
      revision.getOrElse(this.revision),
      namespace.getOrElse(this.namespace),
      internalId.getOrElse(this.internalId),
      language.getOrElse(this.language),
      absoluteLine.orElse(this.absoluteLine),
      name.orElse(this.name),
      section.orElse(this.section)
    )
  }

  override def datasetIri: IRI = DBpediaDatasets.ParserResults.canonicalUri
}

class DBpediaMetadata(
   override val datasetIri: IRI,
   val node: Option[NodeRecord],
   val extractor: Option[ExtractorRecord] = None, // the extractor record
   val transformer: Option[TransformerRecord] = None, // the transformer record
   val replacing: Seq[String] = Seq() // points out the prov-record of the triple which is replaced by this triple with mapping based info
)extends ProvenanceMetadata {
  override def language: Language = node.map(x => x.language).getOrElse(Language.None)
}


case class TripleRecord(
     s: String,                                       // subject - IRI
     p: String,                                       // predicate - IRI
     o: String,                                       // object - ANY
     l: Option[String] = None,                        // language - String
     d: Option[String] = None                         // datatype - IRI
   )

case class ParserRecord(
      uri: IRI,
      wikiText: String,					                      // the string length of the origin wiki text
      transformed: String,					                  // the transformed wiki text after expanding the nested templates
      resultValue: String					                    // the string length of the actual value
   )

@JsonIgnoreProperties(Array("mappingTemplate"))
case class ExtractorRecord(
  uri: IRI,
  var parser: Seq[ParserRecord] = Seq(),                   // the parser record of the value extracted
  splits: Option[Int] = None,                                // number of parse results
  property: Option[String] = None,                    // name of the infobox property
  mappingTemplate: Option[WikiTitle] = None,          // name/uri of the mapping used
  templatesEncountered: Seq[String] = Seq(),          // names of nested templates encountered in this line
  templatesTransformed: Seq[String] = Seq(),          // names of nested templates transformed (expanded) before extraction
  alternativeValues: Seq[String] = Seq()                // points out the prov-records for alternative triples for this property (extracted by other  parsers)
    ){
  lazy val mappingTemplateUri: Option[String] = mappingTemplate.map(t => t.resourceIri.toString)
  lazy val mappingTemplateName: Option[String] = mappingTemplate.map(t => t.decoded)

  /**
    * Will add a new parse record on top of others
    * @param parserRecord
    */
  def addParserRecord(parserRecord: Option[ParserRecord]): Unit ={
    parserRecord match{
      case Some(p) => parser ++= Seq(p)
      case None =>
    }
  }

  /**
    * Will remove any existing ParseRecord, replacing it with the new
    * @param parserRecord
    */
  def setParseRecord(parserRecord: Option[ParserRecord]): Unit ={
    parser = parserRecord.toList
  }
}

//TODO
case class TransformerRecord(
      uri: String
    )