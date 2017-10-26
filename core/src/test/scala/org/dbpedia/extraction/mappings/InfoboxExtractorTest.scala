package org.dbpedia.extraction.mappings

import java.io.{File, FilenameFilter}

import org.dbpedia.extraction.config.ExtractionRecorder
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._
import org.junit.Test

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.language.reflectiveCalls
import scala.reflect.ClassTag

/**
 * Compares outpur from InfoboxExtractor to gold standard.
 * Files are expected to be found in core/src/test/resources/org/dbpedia/extraction/mappings/xx where xx is the iso code for language chapters.
 * By default files in core/src/test/resources/org/dbpedia/extraction/mappings/ will be considered to be taken from DBpedia en.
 * 
 * Input files are named [title].xml, gold standard are given in files [title]-gold.tql
 */
class InfoboxExtractorTest
{
	private val testDataRootDir = new File("src/test/resources/org/dbpedia/extraction/mappings/InfoboxExtractor_samples")
	
	private val filter = new FilenameFilter
	{
		def accept(dir: File, name: String) = name endsWith ".xml"
	}
	
	private val formater = new TerseFormatter(true,true)
	private val parser = WikiParser.getInstance()

	/**
	 * Assumes that gold standard files end in "-gold.txt" and input files end in ".xml"
	 */
	@Test
	def testAll()
	{
	  /*
	   * Files in testDataRootDir are assumed to come from DBpedia en
	   */
	  testForLanguage(testDataRootDir, Language.English)
	  
	  /*
	   * For linguistic chapters, files are assumed to be in subfolders given by their iso code
	   */
		for(langFolder <- testDataRootDir.listFiles.filter(a => a.isDirectory())){
			Language.get(langFolder.getName) match {
			case Some(l) => testForLanguage(langFolder, l)
			case None =>
			}
		}
		
		//val lang = Language.getOrElse("fr", Language.English)
	}

	def testForLanguage(folder : File, _language : Language){
		println("exploration du dossier " + folder.getAbsolutePath())

		val context = new {
			def ontology = {
					val ontoFilePath = "../ontology.xml"
							val ontoFile = new File(ontoFilePath)
					val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
					new OntologyReader().read(ontologySource)
			}
			def language = _language
			def redirects = new Redirects(Map())
			def recorder[T: ClassTag] : ExtractionRecorder[T] = new ExtractionRecorder[T]()
		}

		for(f <- folder.listFiles(filter) )
		{
		  println("test file " + f.getName())
			test(f.getName, context, folder)
		}
	}

	def test(fileNameWiki : String, context : AnyRef{def ontology: Ontology; def language : Language; def redirects : Redirects; def recorder[T: ClassTag] : ExtractionRecorder[T]}, folder:File)
	{
		val goldFile = fileNameWiki.replace(".xml","-gold.tql")
		
		println("testing wiki "+fileNameWiki+" and golds "+goldFile)
		val d = render(fileNameWiki, context, folder).map(formater.render(_).trim()).toSet
		val g = gold(goldFile, context.language, folder).map(formater.render(_).trim()).toSet

		// comparing the result with gold standard
		val diffInD = d.diff(g)
		val diffInG = g.diff(d)

		if (!diffInD.isEmpty){
			println("-- triples not expected: --")
			diffInD.foreach(println)
			println("-- end triples not expected --")
		}
		if (!diffInG.isEmpty){
			println("-- triples expected, not found: --")
			diffInG.foreach(println)
			println("-- end triples expected, not found --")
		}
				
		assert(d == g, "difference for "+ fileNameWiki +", nb triples-diff: " + (diffInD.size+diffInG.size))


	}





	private def render(file : String, context : AnyRef{def ontology: Ontology; def language : Language; def redirects : Redirects; def recorder[T: ClassTag] : ExtractionRecorder[T]}, folder : File) : Seq[Quad] =
	{
		val extractor = new InfoboxExtractor(context)

		println("input file : " + folder + "/" + file)
		val page = //new FileSource(folder, context.language, _ endsWith file).head
		  XMLSource.fromFile(new File(folder.getPath() + "/" + file),context.language).head
		println("resourceIri : " + page.title.resourceIri)

    parser(page) match {
      case Some(n) => extractor.extract(n,page.title.resourceIri)
      case None => Seq.empty

    }

	}

	private def gold(fileName : String, language : Language, folder : File) : Seq[Quad] =
	{
		var quads = new ArrayBuffer[Quad]()
		println("gold standard file : " + folder + "/" + fileName)
		val lines = Source.fromFile(folder + "/" + fileName, "UTF-8").getLines() //.mkString("").replaceAll("\\s+", " ")
		for(line <- lines){
			Quad.unapply(line) match {
				case Some(s) => {
					quads += new Quad(language.isoCode,"",s.subject,s.predicate,s.value,s.context,s.datatype)
				}
				case None =>
			}
		}

		quads
	}
}