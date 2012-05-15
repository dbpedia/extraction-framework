package org.dbpedia.extraction.sources

import scala.collection.immutable.Traversable

/**
 * A source of wiki pages.
 * FIXME: get rid of this class. It has no purpose except hasDefiniteSize = false and makes APIs 
 * that use it less flexible. When we get rid of it, we should replace it by an Iterator or
 * a Stream, not a Traversable. We don't want to keep its elements around any longer than we 
 * absolutely have to.
 */
trait Source extends Traversable[WikiPage]
{
    /**
     * True, if this source is guaranteed to have a finite size.
     */
    override def hasDefiniteSize = false
}
