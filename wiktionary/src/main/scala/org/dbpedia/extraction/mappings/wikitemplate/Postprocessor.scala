package org.dbpedia.extraction.mappings.wikitemplate

import org.openrdf.model.impl.ValueFactoryImpl
import org.dbpedia.extraction.destinations.Quad
import org.openrdf.model.Statement

trait PostProcessor {
    val vf = ValueFactoryImpl.getInstance
    def process(i : List[Statement], subject: String) : List[Statement]
}
