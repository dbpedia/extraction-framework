package org.dbpedia.extraction.util

import java.io.{IOException, File}

/**
 * Defines additional methods on Files, which are missing in the standard library.
 */
object FileUtils
{
    implicit def toFileUtils(file : File) = new FileUtils(file)
}

/**
 * Defines additional methods on Files, which are missing in the standard library.
 */
class FileUtils(file : File)
{
    /**
     * Retrieves the relative Path in respect to a given base directory.
     *
     * @param child
     * @return path from parent to child. uses forward slashes as separators. may be empty. does not end with a slash.
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
     *
     * @throws IOException if the directory or any of its sub directories could not be deleted
     */
    def deleteRecursive() : Unit =
    {
        if(file.isDirectory)
        {
            file.listFiles().foreach(child => new FileUtils(child).deleteRecursive())
        }

        if(!file.delete()) throw new IOException("Could not delete file " + file)
    }
}