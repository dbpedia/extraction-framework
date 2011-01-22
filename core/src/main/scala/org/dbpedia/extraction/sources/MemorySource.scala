package org.dbpedia.extraction.sources

import scala.collection.immutable.Traversable

/**
 * A source which yields pages from a user-defined container
 *
 * @param The pages this source will iterate through
 */
class MemorySource(pages : Traversable[WikiPage]) extends Source
{
    def this(pages : WikiPage*) = this(pages.toList)

    override def foreach[U](f : WikiPage => U) = pages.foreach(f)

    override def hasDefiniteSize = pages.hasDefiniteSize
}
