package org.dbpedia.validation.construct.report

import org.dbpedia.validation.construct.model.TriggerType.Value

package object formats {

  object ReportFormat extends Enumeration {

    val HTML, RDF, GENERIC: Value = Value
  }
}
