package org.dbpedia.extraction.ontology.datatypes

/**
 * Represents a unit that can be converted to the standard unit
 * for its dimension by adding an offset and multiplication by a certain factor.
 */
class FactorUnitDatatype(name : String, unitLabels : Set[String], val factor : Double = 1.0, val offset : Double = 0.0) extends UnitDatatype(name, unitLabels)
{
    // convert all doubles to BigDecimals to avoid ugly rounding errors
    private val factorBigDecimal = BigDecimal(factor)
    private val offsetBigDecimal = BigDecimal(offset)

    /**
     * Converts a value in this unit to the corresponding value in the standard unit of the dimension.
     */
    def toStandardUnit(value : Double) = ((BigDecimal(value) + offsetBigDecimal) * factorBigDecimal).toDouble

    /**
     * Converts a value in the standard unit of the dimension to the corresponding value in this unit.
     */
    def fromStandardUnit(value : Double) = (BigDecimal(value) / factorBigDecimal - offsetBigDecimal).toDouble
}
