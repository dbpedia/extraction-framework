package org.dbpedia.extraction.transform

import org.dbpedia.extraction.config.provenance._
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.iri.IRI

/**
 * Convenience methods that help to unclutter code. 
 */
object QuadBuilder {

  /**
    * QUAD BUILDERS
    */
  def apply(language: Language, dataset: Dataset, predicate: OntologyProperty, datatype: Datatype) =
    new QuadBuilder(None, Option(predicate), None, None, language, Option(datatype), Option(dataset), None)

  def withContext(language: Language, dataset: Dataset, predicate: OntologyProperty, context: String, datatype: Datatype) =
    new QuadBuilder(None, Option(predicate), None, Option(context), language, Option(datatype), Option(dataset), None)
  
  def dynamicType(language: Language, dataset: Dataset, predicate: OntologyProperty) =
    new QuadBuilder(None, Option(predicate), None, None, language, None, Option(dataset), None)

  def stringPredicate(language: Language, dataset: Dataset, predicate: String)  =
    new QuadBuilder(None, Option(predicate), None, None, Option(language), None, Option(dataset.canonicalUri), None)

  def stringPredicate(language: Language, dataset: Dataset, predicate: OntologyProperty, nr: NodeRecord, extractorRecord: ExtractorRecord = null)  = {
    val qb = new QuadBuilder(None, Option(predicate), None, None, language, None, Option(dataset), None)
    qb.setNodeRecord(nr)
    qb.setExtractor(extractorRecord)
    qb
  }

  def dynamicPredicate(language: Language, dataset: Dataset)=
    new QuadBuilder(None, None, None, None, language, None, Option(dataset), None)

  def dynamicPredicate(language: String, dataset: String) =
    new QuadBuilder(None, None, None, None, Option(Language(language)), None, Option(IRI.create(dataset).get), None)

  def dynamicPredicate(language: Language, dataset: Dataset, datatype: Datatype) =
    new QuadBuilder(None, None, None, None, language, Option(datatype), Option(dataset), None)

  def dynamicPredicate(language: Language, dataset: Dataset, datatype: Datatype, nr: NodeRecord, extractorRecord: ExtractorRecord = null) = {
    val qb = new QuadBuilder(None, None, None, None, language, Option(datatype), Option(dataset), None)
    qb.setNodeRecord(nr)
    qb.setExtractor(extractorRecord)
    qb
  }

  def staticSubject(language: Language, dataset: Dataset, subject: String, predicate: OntologyProperty) =
    new QuadBuilder(Option(subject), Option(predicate), None,  None, language, None, Option(dataset), None)

  def staticSubject(language: Language, dataset: Dataset, subject: String) =
    new QuadBuilder(Option(subject), None, None, None, language, None, Option(dataset), None)

  def staticSubject(language: Language, subject: String) =
    new QuadBuilder(Option(subject), None, None, None, language, None, None, None)

  def staticSubject(language: Language, dataset: Dataset, subject: String, nr: NodeRecord, extractorRecord: ExtractorRecord = null) = {
    val qb = new QuadBuilder(Option(subject), None, None, None, language, None, Option(dataset), None)
    qb.setNodeRecord(nr)
    qb.setExtractor(extractorRecord)
    qb
  }

  def staticSubject(language: Language, dataset: Dataset, subject: String, predicate: OntologyProperty, nr: NodeRecord, extractorRecord: ExtractorRecord) = {
    val qb = new QuadBuilder(Option(subject), Option(predicate), None, None, language, None, Option(dataset), None)
    qb.setNodeRecord(nr)
    qb.setExtractor(extractorRecord)
    qb
  }

  /**
    * SIMPLE BUILDERS (LEGACY)
    * Will create simple quads, no provenance
    */

  def applySimple(language: Language, dataset: Dataset, predicate: OntologyProperty, datatype: Datatype) (subject: String, value: String, context: String) =
    new Quad(language, dataset, subject, predicate, value, context, datatype)

  def dynamicTypeSimple(language: Language, dataset: Dataset, predicate: OntologyProperty) (subject: String, value: String, context: String, datatype: Datatype) =
    new Quad(language, dataset, subject, predicate, value, context, datatype)

  def stringPredicateSimple(language: Language, dataset: Dataset, predicate: String) (subject: String, value: String, context: String, datatype: Datatype) =
    new Quad(language, dataset, subject, predicate, value, context, datatype)

  def stringPredicateSimple(language: Language, dataset: Dataset, predicate: String, datatype: Datatype) (subject: String, value: String, context: String) =
    new Quad(language, dataset, subject, predicate, value, context, datatype)

  def dynamicPredicateSimple(language: Language, dataset: Dataset) (subject: String, predicate: String, value: String, context: String, datatype: Datatype) =
    new Quad(language, dataset, subject, predicate, value, context, datatype)

  def dynamicPredicateSimple(language: String, dataset: String) (subject: String, predicate: String, value: String, context: String, datatype: String) =
    new Quad(language, dataset, subject, predicate, value, context, datatype)

  def dynamicPredicateSimple(language: Language, dataset: Dataset, datatype: Datatype) (subject: String, predicate: OntologyProperty, value: String, context: String) =
    new Quad(language, dataset, subject, predicate, value, context, datatype)
}

/**
  * Companion builder class, for accumulating quad data and metadata
  */
class QuadBuilder(
   var subject: Option[String] = None,
   var predicate: Option[String] = None,
   var value: Option[String] = None,
   var sourceUri: Option[String] = None,
   var language: Option[Language] = None,
   var datatype: Option[String] = None,
   var dataset: Option[IRI] = None,
   metadata: Option[DBpediaMetadata] = None
 ){
  def this(quad: Quad) =
    this(Option(quad.subject), Option(quad.predicate), Option(quad.value), Option(quad.context), Option(Language(quad.language)), Option(quad.datatype), IRI.create(quad.dataset).toOption)

  def this(
    subject: String,
    predicate: String,
    value: String,
    sourceUri: String,
    language: String,
    datatype: String,
    dataset: String
    ) = this(Option(subject), Option(predicate), Option(value), Option(sourceUri), Option(Language(language)), Option(datatype), IRI.create(dataset).toOption)


  def this(
    subject: Option[String],
    predicate: Option[OntologyProperty],
    value: Option[String],
    context: Option[String],
    language: Language,
    datatype: Option[Datatype],
    dataset: Option[Dataset],
    metadata: Option[DBpediaMetadata]
  ) = this(subject, predicate.map(p => p.uri), value, context, Option(language), datatype.map(x => x.uri), dataset.map(d => d.canonicalUri), metadata)

  var nodeRecord: Option[NodeRecord] = metadata.flatMap(x => x.node)
  var extractor: Option[ExtractorRecord] = metadata.flatMap(x => x.extractor)			                // the extractor record
  var transformer: Option[TransformerRecord] = metadata.flatMap(x => x.transformer)	                // the transformer record
  var replacing: Seq[String] = metadata.map(x => x.replacing).getOrElse(Seq())

  override def clone: QuadBuilder = {
    val qb = new QuadBuilder(
      this.subject,
      this.predicate,
      this.value,
      this.sourceUri,
      this.language,
      this.datatype,
      this.dataset
    )
    nodeRecord.foreach(x => qb.setNodeRecord(x))
    extractor.foreach(x => qb.setExtractor(x))
    transformer.foreach(x => qb.setTransformer(x))
    replacing.foreach(x => qb.addReplacing(x))
    qb
  }

  def addReplacing(rep: String) = replacing ++= Seq(rep)
  def setTransformer(t: TransformerRecord) = transformer = Option(t)
  def setExtractor(t: ExtractorRecord): Unit = extractor = Option(t)
  def setExtractor(classIri: IRI): Unit = extractor = Option(classIri).map(ExtractorRecord(_))
  def setExtractor(classIri: String): Unit = setExtractor(IRI.create(classIri).getOrElse(null))
  def setLanguage(l: Language) = language = Option(l)
  def setDataset(ds: String) = dataset = Option(ds).map(d => IRI.create(d).get)
  def setDataset(ds: Dataset) = dataset = Option(ds).map(x => x.canonicalUri)
  def setSubject(s: String) = subject = Option(s)
  def setPredicate(t: String) = predicate = Option(t)
  def setPredicate(t: OntologyProperty) = predicate = Option(t).map(x => x.uri)
  def setValue(t: String) = value = Option(t)
  def setSourceUri(t: String) = sourceUri = Option(t)
  def setDatatype(t: String) = datatype = Option(t)
  def setDatatype(t: Datatype) = datatype = Option(t).map(x => x.uri)
  def setNodeRecord(nr: NodeRecord) = nodeRecord = Option(nr)

  def setTriple(s: String, p: String, v: String): Unit ={
    setSubject(s)
    setPredicate(p)
    setValue(v)
  }

  def setTriple(s: String, p: OntologyProperty, v: String, d: Datatype = null): Unit ={
    setSubject(s)
    setPredicate(p)
    setValue(v)
    if(datatype != null)
      setDatatype(d)
  }

  def getNodeRecord = nodeRecord

  def getQuad: Quad = if(subject.isDefined && predicate.isDefined && value.isDefined)
      new Quad(language.map(l => l.isoCode).orNull, dataset.map(d => d.toString).orNull, subject.get, predicate.get, value.get, sourceUri.orNull, datatype.orNull, getMetadataRecord)
    else
      throw new IllegalArgumentException("Inchoate information for building a new Quad!")

  def getMetadataRecord: Option[DBpediaMetadata] = if(dataset.isDefined && language.isDefined)
      Some(new DBpediaMetadata(dataset.get, nodeRecord, extractor, transformer, replacing))
    else
      None
}
