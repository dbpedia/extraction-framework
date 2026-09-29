package org.dbpedia.extraction.compat

import scala.collection.mutable
import scala.jdk.CollectionConverters._

/** Compatibility shims replacing the old scala.collection.JavaConversions implicits. */
object JavaConversions {
  implicit def iterableAsScalaIterable[A](i: java.lang.Iterable[A]): Iterable[A] = i.asScala
  implicit def asScalaIterator[A](i: java.util.Iterator[A]): Iterator[A] = i.asScala
  implicit def asJavaIterator[A](i: Iterator[A]): java.util.Iterator[A] = i.asJava

  implicit def asScalaSet[A](s: java.util.Set[A]): mutable.Set[A] = s.asScala
  implicit def asScalaBuffer[A](l: java.util.List[A]): mutable.Buffer[A] = l.asScala
  implicit def asScalaMap[K, V](m: java.util.Map[K, V]): mutable.Map[K, V] = m.asScala

  implicit def asJavaCollection[A](i: Iterable[A]): java.util.Collection[A] = i.asJavaCollection
  implicit def asJavaMap[K, V](m: scala.collection.Map[K, V]): java.util.Map[K, V] = m.asJava
}
