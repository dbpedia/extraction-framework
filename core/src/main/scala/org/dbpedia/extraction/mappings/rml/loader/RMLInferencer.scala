package org.dbpedia.extraction.mappings.rml.loader

import be.ugent.mmlab.rml.model.RMLMapping
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.rulesys.{GenericRuleReasoner, Rule}
import org.apache.jena.util.FileManager
import org.dbpedia.extraction.util.Language

/**
  * Created by wmaroy on 10.07.17.
  */
class RMLInferencer {

  def load(language :Language, pathToRMLMappingsDir : String) : RMLMapping = {

    // loading rules
    val file = this.getClass.getClassLoader.getResource("rules.rule")
    val rules = Rule.rulesFromURL(getClass.getClassLoader.getResource("rules.rule").getPath)

    // create an empty model
    val model = ModelFactory.createDefaultModel()

    // use the FileManager to find the input file
    val in = FileManager.get().open("/home/wmaroy/github/leipzig/test_output.ttl")
    if (in == null) {
      throw new IllegalArgumentException(
        "File not found.")
    }

    // read the RDF/XML file
    model.read(in, "http://en.dbpedia.org/resource/Mapping_en:Infobox_martial_artist", "TURTLE")

    // create the reasoner
    val reasoner = new GenericRuleReasoner(rules)
    val infModel = ModelFactory.createInfModel(reasoner, model)

    infModel.write(System.out, "TURTLE", "http://en.dbpedia.org/resource/Mapping_en:Infobox_martial_artist/")

    // return nothing atm
    null
  }

}
