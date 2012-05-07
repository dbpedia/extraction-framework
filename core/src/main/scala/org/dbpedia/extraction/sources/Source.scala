package org.dbpedia.extraction.sources

import scala.collection.immutable.Traversable

/**
 * A source of wiki pages.
 * FIXME: get rid of this class. It has no purpose and makes APIs that use it less flexible. 
 * And: When you get rid of it, replace it by an Iterator, not a Traversable. We don't want
 * to keep its elements around any longer than we absolutely have to.
 */
trait Source extends Traversable[WikiPage]
{
    /**
     * True, if this source is guaranteed to have a finite size.
     */
    override def hasDefiniteSize = false
}
