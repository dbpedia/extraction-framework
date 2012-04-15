package org.dbpedia.extraction.dump

import _root_.org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import java.io.{FileInputStream, FileOutputStream, File}
import _root_.org.dbpedia.extraction.util.RichFile.toRichFile
import java.util.concurrent.{Executor, Executors}

/**
 * Compresses all files inside a directory and its sub-directories.
 */
object Compress
{
    val overwriteExistingZipFiles = false

    val nThreads = 4

    def main(args : Array[String]) : Unit =
    {
        val inputDir = args(0)
        val outputDir = args(1)

        if(args.length != 2 || !(new File(inputDir).isDirectory))
        {
            println("Please set the properties 'inputDir' and 'outputDir'")
            return
        }

        compress(new File(inputDir), new File(outputDir))
    }

    def compress(inputDir : File, outputDir : File)
    {
        println("Compressing directory " + inputDir + " to " + outputDir)

        val executor = Executors.newFixedThreadPool(nThreads)
        compressFiles(inputDir, inputDir, outputDir, executor)
        executor.shutdown()

        println("Finished compressing directory " + inputDir + " to " + outputDir)
    }

    /**
     * Recursively compresses all files in a directory.
     */
    private def compressFiles(baseDir : File, currentFile : File, outputDir : File, executor : Executor) : Unit =
    {
        if(currentFile.isHidden) return

        if(currentFile.isFile)
        {
            val targetFile = new File(outputDir + "/" + baseDir.relativize(currentFile) + ".bz2")
            if(!targetFile.isFile || overwriteExistingZipFiles)
            {
                executor.execute(new CompressTask(currentFile, targetFile))
            }
            else
            {
                System.err.println(targetFile + " already exists; overwrite=false")
            }
        }
        else
        {
            for(childFile <- currentFile.listFiles())
            {
                compressFiles(baseDir, childFile, outputDir, executor)
            }
        }
    }

    /**
     * Task which compresses a single file.
     */
    private class CompressTask(inputFile : File, outputFile : File) extends Runnable
    {
        override def run : Unit =
        {
            println("Compressing file " + inputFile + " to " + outputFile)
            outputFile.getParentFile.mkdirs

            val inputStream = new FileInputStream(inputFile)
            val outputStream = new BZip2CompressorOutputStream(new FileOutputStream(outputFile))
            val buffer = new Array[Byte](65536)

            var totalBytes = 0L
            val startTime = System.nanoTime
            var lastLogTime = 0L

            try
            {
                while(true)
                {
                    val bytesRead = inputStream.read(buffer)
                    if(bytesRead == -1) return
                    outputStream.write(buffer, 0, bytesRead)

                    //Count read bytes
                    totalBytes += bytesRead
                    val time = System.nanoTime
                    if(time - lastLogTime > 1000000000L)
                    {
                        val kb = totalBytes / 1024L
                        lastLogTime = time
                        println(inputFile + ": " + kb + " KB (" + (kb.toDouble / (time - startTime) * 1000000000.0).toLong + "kb/s)")
                    }
                }
            }
            finally
            {
                inputStream.close()
                outputStream.close()
            }
        }
    }
}
