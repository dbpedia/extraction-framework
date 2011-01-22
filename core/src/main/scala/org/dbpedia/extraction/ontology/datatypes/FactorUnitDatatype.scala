package org.dbpedia.extraction.ontology.datatypes

/**
 * Represents a unit that can be converted to the standard unit
 * for its dimension by adding an offset and multiplication by a certain factor.
 */
class FactorUnitDatatype(name : String, unitLabels : Set[String], val factor : Double = 1.0, val offset : Double = 0.0) extends UnitDatatype(name, unitLabels)
{
    /**
     * Converts a value in this unit to the corresponding value in the standard unit of the dimension.
     */
    def toStandardUnit(value : Double) = (value + offset) * factor

    /**
     * Converts a value in the standard unit of the dimension to the corresponding value in this unit.
     */
    def fromStandardUnit(value : Double) = value / factor - offset
}
