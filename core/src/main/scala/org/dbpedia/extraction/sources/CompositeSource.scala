package org.dbpedia.extraction.sources

import scala.collection.immutable.Traversable

/**
 * A source which is composed of multiple child sources.
 * Iterates through all pages of all child sources.
 *
 * @param sources The sources, this source is composed of.
 */
class CompositeSource(sources : Traversable[Source]) extends Source
{
    override def foreach[U](f : WikiPage => U) = sources.foreach(_.foreach(f))

    override def hasDefiniteSize = sources.forall(_.hasDefiniteSize)
}
