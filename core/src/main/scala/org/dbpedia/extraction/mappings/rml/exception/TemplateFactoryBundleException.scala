package org.dbpedia.extraction.mappings.rml.exception

/**
  * Created by wmaroy on 26.07.17.
  */
class TemplateFactoryBundleException(msg : String) extends Exception(msg) {

}

object TemplateFactoryBundleException {

  val WRONG_BUNDLE_MSG = "Wrong instance of TemplateFactoryBundle is used."

}
