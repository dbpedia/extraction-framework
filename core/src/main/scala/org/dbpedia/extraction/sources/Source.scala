package org.dbpedia.extraction.sources

import scala.collection.immutable.Traversable

/**
 * A source of wiki pages.
 */
trait Source extends Traversable[WikiPage]
{
    /**
     * True, if this source is guaranteed to have a finite size.
     */
    override def hasDefiniteSize = false
}
