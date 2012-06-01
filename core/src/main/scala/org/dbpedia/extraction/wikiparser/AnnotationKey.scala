package org.dbpedia.extraction.wikiparser

/**
 * A marker class whose purpose is to add compile-time type safety to node annotations.
 * Replaces annotation key names. Usage is similar to annotation key names, but annotation key 
 * names are distinguished by equality, while AnnotationKey objects are distinguished by identity.
 * Thus, this class does not override hashCode and equals.
 * 
 * Note: we could add a name to objects of this class, which would probably be nice for debugging,
 * but could lead to confusion: one might think that the name is the actual key, but is isn't,
 * and cannot be, because we cannot associate the name with the type of the annotation - at run-time,
 * the type is gone because of erasure. Two annotation key objects with the same name would still
 * be completely unrelated keys.
 * 
 * TODO: as a dedugging aid, call Thread.currentThread.getStackTrace in the constructor and
 * save class name, method name and line number of the place where the annotation key was created.
 * Probably the second element in the stack trace array, but that's not guaranteed.
 * 
 */
final class AnnotationKey[T]