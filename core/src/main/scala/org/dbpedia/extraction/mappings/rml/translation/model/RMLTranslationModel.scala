package org.dbpedia.extraction.mappings.rml.translation.model

import org.dbpedia.extraction.mappings.rml.translation.model.factories.RMLResourceFactory
import org.dbpedia.extraction.mappings.rml.translation.model.rmlresources._
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.wikiparser.WikiTitle

/**
  * Created by wmaroy on 21.07.17.
  * RMLModel that retrieves its triplesMap and etc from the WikiMappings
  */
class RMLTranslationModel(val wikiTitle: WikiTitle, val sourceUri : String) extends RMLModel {

  protected val _triplesMap: RMLTriplesMap = rmlFactory.createRMLTriplesMap(new RMLUri(wikiTitle.resourceIri))
  protected val _subjectMap: RMLSubjectMap = _triplesMap.addSubjectMap(new RMLUri(convertToSubjectMapUri(wikiTitle)))
  protected val _logicalSource: RMLLogicalSource = _triplesMap.addLogicalSource(new RMLUri(convertToLogicalSourceUri(wikiTitle)))
  protected val _functionSubjectMap: RMLSubjectMap = rmlFactory.createRMLSubjectMap(new RMLUri(convertToSubjectMapUri(wikiTitle) + "/Function"))
    .addClass(new RMLUri(RdfNamespace.FNO.namespace + "Execution"))
    .addBlankNodeTermType()

  _logicalSource.addIterator(new RMLLiteral("Infobox:" + wikiTitle.encoded))
  _logicalSource.addReferenceFormulation(new RMLUri(RdfNamespace.QL.namespace + "wikitext"))

}
