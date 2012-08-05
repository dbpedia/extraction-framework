package org.dbpedia.extraction.sources

import scala.collection.immutable.Traversable

/**
 * A source of wiki pages.
 * TODO: do we need this class? Methods that have a paramater with this type are not as flexible
 * as they could be, for example they cannot be called with List(page). The only advantage of
 * this class seems to be the definition hasDefiniteSize = false, which can easily be added to
 * any Traversable implementations that actually need it. We should clearly document in which
 * cases hasDefiniteSize = false is useful or necessary. The Scala documentation doesn't help
 * much here. Traversable also defines all kinds of methods - for example size() - that call
 * foreach() and iterate over the whole collection, which for most of our sources is a huge
 * waste. Even calling head() causes a big overhead, for example a file or a network connection
 * is opened. Is there a more appropriate Scala type?
 */
trait Source extends Traversable[WikiPage]
{
    /**
     * True, if this source is guaranteed to have a finite size.
     */
    override def hasDefiniteSize = false
}
