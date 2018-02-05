package org.dbpedia.extraction.destinations
import java.io.Writer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.dbpedia.extraction.config.provenance.QuadProvenanceRecord
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.iri.IRI

class ProvenanceDestination(factory: () => Writer) extends Destination{

  import com.fasterxml.jackson.databind.module.SimpleModule

  // Create the module
  val mod = new SimpleModule("Serializer Module")
  // Add the custom serializer to the module
  mod.addSerializer(ProvenanceDestination.iriSerializer)
  mod.addSerializer(ProvenanceDestination.langSerializer)
  // Create mapper
  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  // Configure mapper
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.registerModule(mod)
  private val prettyWriter = mapper.writerWithDefaultPrettyPrinter[ObjectWriter]()
  private var writer: Writer = _

  /**
    * Opens this destination. This method should only be called once during the lifetime
    * of a destination, and it should not be called concurrently with other methods of this class.
    */
  override def open(): Unit = {
    if(writer == null) { //to prevent errors when called twice
      writer = factory()
      writer.write("{\n\t\"placeholderHeader\":null ,\n\t\"records\":[\n\t\t")
    }
  }

  /**
    * Writes quads to this destination. Implementing classes should make sure that this method
    * can safely be executed concurrently by multiple threads.
    */
  override def write(graph: Traversable[Quad]): Unit = {
    graph.foreach(q =>{
      q.getProvenanceRecord match{
        case Some(prov) =>
          writer.write(prettyWriter.writeValueAsString(prov) + ",\n")
        case None =>
      }
    })
  }

  def writeProvenance(records: Traversable[QuadProvenanceRecord]): Unit ={
    records.foreach(prov => {
      writer.write(prettyWriter.writeValueAsString(prov) + ",\n")
    })
  }

  /**
    * Closes this destination. This method should only be called once during the lifetime
    * of a destination, and it should not be called concurrently with other methods of this class.
    */
  override def close(): Unit = {
    if(writer != null){
      writer.write("null\n\t]\n}")  //we need to add a dummy entry as last record, since we cant delete the last comma
      writer.close()
    }
  }
}

object ProvenanceDestination{

  /**
    * IRI serializer
    */
  val iriSerializer: StdSerializer[IRI] {
    def serialize(value: IRI, jgen: JsonGenerator, provider: SerializerProvider): Unit
  } = new StdSerializer[IRI](classOf[IRI]) {

    override def serialize(value: IRI, jgen: JsonGenerator, provider: SerializerProvider) = {
      jgen.writeString(value.toString)
    }
  }

  /**
    * Language Serializer
    */

  val langSerializer: StdSerializer[Language] {
    def serialize(value: Language, jgen: JsonGenerator, provider: SerializerProvider): Unit
  } = new StdSerializer[Language](classOf[Language]) {

    override def serialize(value: Language, jgen: JsonGenerator, provider: SerializerProvider) = {
      jgen.writeString(value.wikiCode)
    }
  }
}