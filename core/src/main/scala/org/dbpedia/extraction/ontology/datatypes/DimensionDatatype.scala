package org.dbpedia.extraction.ontology.datatypes

/**
 * Represents a dimension.
 *
 * @param name The name of this dimension e.g. Mass
 * @param units The units of this dimension
 */
class DimensionDatatype(name : String, val units : List[UnitDatatype]) extends Datatype(name)
{
    for(unit <- units) unit._dimension = this

    private val labelMap = (for(unit <- units; label <- unit.unitLabels) yield (label -> unit)).toMap

    /**
     * Retrieves a unit of this dimension by its label.
     */
    def unit(label : String) = labelMap.get(label)

    /**
     * Set of all unit labels
     */
    def unitLabels = labelMap.keySet
}
