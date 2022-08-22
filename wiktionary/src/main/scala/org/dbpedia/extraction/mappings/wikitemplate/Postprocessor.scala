package org.dbpedia.extraction.mappings.wikitemplate

import org.dbpedia.extraction.transform.Quad
import org.openrdf.model.impl.ValueFactoryImpl
import org.openrdf.model.Statement

trait PostProcessor {
    val vf = new ValueFactoryImpl()
    def process(i : List[Statement], subject: String) : List[Statement]
}
