package org.dbpedia.extraction.ontology.datatypes

/**
 * Represents a unit.
 *
 * @param name The name of this unit e.g. metre
 * @param unitLabels A set of common labels for this unit e.g. Set(m, metre...)
 */
abstract class UnitDatatype(name : String, val unitLabels : Set[String]) extends Datatype(name)
{
    /** Set by DimensionDatatype */
    @volatile private[datatypes] var _dimension : DimensionDatatype = null

    /** The dimension of this unit */
    def dimension = _dimension
    
    /**
     * Converts a value from this unit to the standard unit for the dimension.
     *
     * @param value value in this unit.
     * @return equivalent value in standard unit
     */
    def toStandardUnit(value : Double) : Double
    
    /**
     * Converts a value from the standard unit for the dimension to this unit.
     *
     * @param value value in standard unit
     * @return equivalent value in this unit
     */
    def fromStandardUnit(value : Double) : Double
}
