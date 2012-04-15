package org.dbpedia.extraction.util

import java.io.{IOException, File}
import RichFile.toRichFile

/**
 * Defines additional methods on Files, which are missing in the standard library.
 */
object RichFile
{
    implicit def toRichFile(file : File) = new RichFile(file)
}

/**
 * Defines additional methods on Files, which are missing in the standard library.
 */
class RichFile(file : File)
{
    /**
     * Retrieves the relative path in respect to a given base directory.
     * @param child
     * @return path from parent to child. uses forward slashes as separators. may be empty.
     * does not end with a slash.
     * @throws IllegalArgumentException if parent is not a parent directory of child.
     */
    def relativize(child : File) : String =
    {
        // Note: toURI encodes file paths, getPath decodes them
        var path = UriUtils.relativize(file.toURI, child.toURI).getPath
        if (path endsWith "/") path = path.substring(0, path.length() - 1)
        return path
    }

    /**
     * Deletes this directory and all sub directories.
     * @throws IOException if the directory or any of its sub directories could not be deleted
     */
    def deleteRecursive() : Unit =
    {
      if(file.isDirectory) file.listFiles.foreach(_.deleteRecursive)
      if(!file.delete) throw new IOException("Could not delete file "+file)
    }
}