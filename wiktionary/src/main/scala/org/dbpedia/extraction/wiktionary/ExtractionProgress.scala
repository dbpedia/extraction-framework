package org.dbpedia.extraction.wiktionary

/**
 * Takes track of the extraction progress.
 *
 * @param startTime The time when the page extraction has been started. Milliseconds since midnight, January 1, 1970 UTC.
 * @param pageCount The number of pages which have been extracted successfully
 * @param failedPages The number of pages for which the extraction failed
 */
class ExtractionProgress(@volatile var startTime : Long = 0, @volatile var extractedPages : Int = 0, @volatile var failedPages : Int = 0)
{
    override def clone = synchronized
    {
        new ExtractionProgress(startTime, extractedPages, failedPages)
    }
}
