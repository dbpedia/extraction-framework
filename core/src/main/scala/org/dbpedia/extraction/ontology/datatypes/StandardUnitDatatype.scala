package org.dbpedia.extraction.ontology.datatypes

/**
 * Defines a standard unit of a specific dimension.
 *
 * @param name The name of this unit e.g. metre
 * @param label A set of common labels for unit e.g. Set(m, metre...)
 */
class StandardUnitDatatype(name : String, unitLabels : Set[String]) extends UnitDatatype(name, unitLabels)
{
    def toStandardUnit(value : Double) = value

    def fromStandardUnit(value : Double) = value
}
