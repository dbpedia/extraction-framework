package org.dbpedia.extraction.dump

import org.dbpedia.extraction.util.Language
import java.io.File
import org.dbpedia.extraction.wikiparser.WikiTitle
import java.net.URL
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.{Source, XMLSource, MemorySource, WikiSource}

/**
 * Holds methods required by the extractors. This object is to be injected into the extractors as constructor argument.
 */
class DumpExtractionContext(lang : Language, dumpDir : File)
{
    def language : Language = lang

    def ontology : Ontology = _ontology

    def mappingsSource : Source = _mappingSource

    def articlesSource : Source = _articlesSource

    def commonsSource : Source = _commonsSource

    def redirects : Redirects = _redirects

    private lazy val _ontology =
    {
        val ontologySource = WikiSource.fromNamespaces(namespaces = Set(WikiTitle.Namespace.OntologyClass, WikiTitle.Namespace.OntologyProperty),
            url = new URL("http://mappings.dbpedia.org/api.php"),
            language = Language.Default )
        new OntologyReader().read(ontologySource)
    }

    private lazy val _mappingSource =
    {
        WikiTitle.Namespace.mappingNamespace(language) match
        {
            case Some(namespace) => WikiSource.fromNamespaces(namespaces = Set(namespace),
                url = new URL("http://mappings.dbpedia.org/api.php"),
                language = Language.Default)
            case None => new MemorySource()
        }
    }

    private lazy val _articlesSource =
    {
        XMLSource.fromFile(DumpExtractionContext.getDumpFile(dumpDir, language.wikiCode),
            title => title.namespace == WikiTitle.Namespace.Main || title.namespace == WikiTitle.Namespace.File ||
                title.namespace == WikiTitle.Namespace.Category || title.namespace == WikiTitle.Namespace.Template)
    }

    private lazy val _commonsSource =
    {
        XMLSource.fromFile(DumpExtractionContext.getDumpFile(dumpDir, "commons"), _.namespace == WikiTitle.Namespace.File)
    }

    private lazy val _redirects =
    {
        Redirects.load(articlesSource, language)
    }
}

private object DumpExtractionContext
{
    /**
     * Retrieves the dump stream for a specific language edition.
     */
    private def getDumpFile(dumpDir : File, wikiPrefix : String) : File =    //wikiPrefix is language prefix (and can be 'commons')
    {
        val wikiDir = new File(dumpDir + "/" + wikiPrefix)
        if(!wikiDir.isDirectory) throw new Exception("Dump directory not found: " + wikiDir)

        //Find most recent dump date
        val date = wikiDir.list()
                   .filter(_.matches("\\d{8}"))
                   .sortWith(_.toInt > _.toInt)
                   .headOption.getOrElse(throw new Exception("No dump found for Wiki: " + wikiPrefix))

        val articlesDump = new File(wikiDir + "/" + date + "/" + wikiPrefix.replace('-', '_') + "wiki-" + date + "-pages-articles.xml")
        if(!articlesDump.isFile) throw new Exception("Dump not found: " + articlesDump)

        articlesDump
    }
}
