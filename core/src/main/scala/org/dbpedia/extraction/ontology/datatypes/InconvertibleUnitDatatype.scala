package org.dbpedia.extraction.ontology.datatypes

/**
 * Represents a unit that cannot be converted to any other unit.
 */
//TODO find a better solution for dealing with inconvertible units without violating Liskov's substitution principle
class InconvertibleUnitDatatype(name : String, unitLabels : Set[String]) extends UnitDatatype(name, unitLabels)
{
    def toStandardUnit(value : Double) = throw new UnsupportedOperationException("Tried to convert inconvertible unit")

    def fromStandardUnit(value : Double) = throw new UnsupportedOperationException("Tried to convert inconvertible unit")
}
