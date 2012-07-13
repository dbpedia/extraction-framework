package org.dbpedia.extraction.wiktionary

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import java.io.{FileInputStream, FileOutputStream, File}
import org.dbpedia.extraction.util.RichFile.wrapFile
import java.util.concurrent.{Executor, Executors}

/**
 *  Compresses all files inside a directory
 */
object Compress
{
    def main(args : Array[String]) : Unit =
    {
        val inputDir = System.getProperty("inputDir")
        val outputDir = System.getProperty("outputDir")

        if(inputDir == null || outputDir == null)
        {
            println("Please set the properties 'inputDir' and 'outputDir'")
            return
        }

        compress(new File(inputDir), new File(outputDir))
    }

    def compress(inputDir : File, outputDir : File)
    {
        println("Compressing directory " + inputDir + " to " + outputDir)

        val executor = Executors.newFixedThreadPool(4)
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
            executor.execute(new CompressTask(currentFile, new File(outputDir + "/" + baseDir.relativize(currentFile) + ".bz2")))
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
                    if(bytesRead == -1) return;
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
