package org.dbpedia.extraction.mappings.wikitemplate

import org.openrdf.model.impl.ValueFactoryImpl
import org.dbpedia.extraction.destinations.Quad

trait PostProcessor {
    val vf = ValueFactoryImpl.getInstance
    def process(i : List[Quad], subject: String) : List[Quad]
}
