# How to create a new Extractor


## Setting everything up
Detailed information of how to start working with the extraction framework you can find here https://github.com/dbpedia/extraction-framework/blob/master/documentation/quickstart.md#1-setting-everything-up . There is also a tutorial in wikis how to setup IntelliJ IDEA: https://github.com/dbpedia/extraction-framework/wiki/Setting-up-intellij-idea .
## Try to download and extract
Try to download a part of Wikipedia/Wikidata dump (the instructions you can find here: https://github.com/dbpedia/extraction-framework/blob/master/documentation/quickstart.md#2-resource-downloads ). Tutorial how to extract the data: https://github.com/dbpedia/extraction-framework/blob/master/documentation/quickstart.md#3b-running-the-dump-extraction . 
### Possible problems with the download
If you have problems with the download of the dump using the tutorials above, you can try to download it manually. Go to the website https://ftp.acc.umu.se/mirror/wikimedia.org/dumps/ , choose the language of Wikipedia articles (it also contains Wikimedia commons and Wikidata articles), then choose the date of dump, and choose the file to download. For example, you can download the part of dump `enwiki-20200820-pages-articles-multistream27.xml-p63663462p65010436.bz2` from https://ftp.acc.umu.se/mirror/wikimedia.org/dumps/enwiki/20200820/?C=N;O=D . Then you need to create in your basedir directory - directory with language+wiki directory and then create date directory in this languagewiki directory. After that, copy the file to date directory. so the path to the file will look like `../basedir/enwiki/20200820/enwiki-20200820-pages-articles-multistream27.xml-p63663462p65010436.bz2`. But as the universal properties use the source file `pages-articles-multistream.xml.bz2` it is necessary to rename the file to `enwiki-20200820-pages-articles-multistream.xml.bz2` . Then you need to set the `require-download-complete` property to `false` in your extraction properties file (e.g. in the `extraction.default properties`). Then you can run the extraction process with the command `../run extraction extraction.default.properties` or you can use your IDE and run the extraction in it.

#### wikipedias.csv file error
This file will be created if the download fails. So if you will try to download the dump in other ways and try to extract the data from it, you will get an errors until you will have not deleted this file. So, delete this file if it was created after failed download.

## How to implement Wikidata extractor
### Wikidata extractors overview
The Wikidata pages are represented in xml+json format. The Extraction Framework uses Wikidata Toolkit Library for parsing all the data from Wikidata pages. Wikidata Toolkit library is the main library in Wikidata extractors because it contains all necessary methods for getting data from each entity. 
### Implementation
Firstly, you need to create a class that extends the `JsonNodeExtractor`. Then you need to implement `extract` method. E.g.  `override def extract(page: JsonNode, subjectUri: String): Seq[Quad]` . The `extract` method is the method that is responsible for the data extraction. As you can see it contains a parameter with JsonNode class type. JsonNode class has two variables: `wikiPage` and `wikiDataDocument`.
Json Node class:
```
class JsonNode  (
                  val wikiPage : WikiPage,
                  val wikiDataDocument : JsonDeserializer
                  )
  extends Node(List.empty, 0) {
  def toPlainText: String = ""
  def toWikiText: String = ""
}
```
The `WikiPage` class ussually represents the Wikipedia/Wikidata/Wikimedia Commons page. It consists of many fields that contain many different info: `id, title, redirect, revision, timestamp, contributorID, contributorName, source, format`. You can find *Wwkitext* of the page in the `source` field. 

So, if you want to get data from *wikitext*, you need to deserialize the source. But before this, you need to check what type of entity (page) you have, because if the input page is Wikidata Item (the namesapce of it in wikidata is `Namespace.Main`) but you deserialize it as Wikidata Lexeme, you will get the error. To prevent this error in each wikidata extractor (other extractors also have simillar checks) we check the type of entity by namespace. Let's look at the [Wikidata Property Extractor](https://github.com/dbpedia/extraction-framework/blob/5f437a90d647b50235f1ee64a85e9aa9d794b469/core/src/main/scala/org/dbpedia/extraction/mappings/wikidata/WikidataPropertyExtractor.scala#L33):
```
  override def extract(page: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    val subject = WikidataUtil.getWikidataNamespace(subjectUri).replace("Property:", "")

    quads ++= getAliases(page, subject)
    quads ++= getDescriptions(page, subject)
    quads ++= getLabels(page, subject)
    quads ++= getStatements(page, subject)


    quads
  }

  private def getAliases(document: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    if (document.wikiPage.title.namespace == Namespace.WikidataProperty) {
      val page = document.wikiDataDocument.deserializePropertyDocument(document.wikiPage.source)
      for ((lang, value) <- page.getAliases) {
        val alias = WikidataUtil.replacePunctuation(value.toString, lang)
        Language.get(lang) match {
          case Some(dbpedia_lang) => {
            quads += new Quad(dbpedia_lang, DBpediaDatasets.WikidataProperty, subjectUri, aliasProperty, alias,
              document.wikiPage.sourceIri, context.ontology.datatypes("rdf:langString"))
          }
          case _ =>
        }
      }
    }
    quads
  }
```
The `if (document.wikiPage.title.namespace == Namespace.WikidataProperty)` checks by namespace if the type of our input page is Wikidata Property.
 
Here `val page = document.wikiDataDocument.deserializePropertyDocument(document.wikiPage.source)` we deserialize our input wikitext and now we can get all the data by different methods from our `PropertyDocument` object.  

In this line `for ((lang, value) <- page.getAliases)` you can see that from our deserialized page we can traverse the `aliases`.
 
To save the triple you need to create a quad object where the 1st parameter is a language, the 2nd parameter is the dataset where you want to save the triple, the 3rd will be the subject, the 4th - predicate, the 5th - value, the 6th - context, the 7th - datatype.
Let's see an example:
   `quads += new Quad(dbpedia_lang, DBpediaDatasets.WikidataProperty, subjectUri, aliasProperty, alias, document.wikiPage.sourceIri, context.ontology.datatypes("rdf:langString"))`

Here we have:
```
language - dbpedia_lang
dataset - DBpediaDatasets.WikidataProperty, 
subject - subjectUri, 
predicate - aliasProperty,
object - alias
context - document.wikiPage.sourceIri
datatype - context.ontology.datatypes("rdf:langString")
```

## How to implement Wikipedia Extractor
The Extractors for Wikipedia pages are usually extended with `WikiPageExtractor` or `PageNodeExtractor`. `PageNodeExtractor` is usually used when you want to work with the Wikitext AST (most common case); `WikiPageExtractor` is used when you want to work only with the page metadata. e.g.`RedirectExtractor`, `LabelExtractor` .

So let's see an example. Label Extractor extracts page title.  
```
override def extract(page: WikiPage, subjectUri: String) : Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title)) return Seq.empty

    val label = page.title.decoded
    
    if(label.isEmpty) Seq.empty
    else Seq(new Quad(context.language, DBpediaDatasets.Labels, subjectUri, labelProperty, label, page.sourceIri, context.ontology.datatypes("rdf:langString")))
  }
```
In this extractor we implement method `extract`. `page.title.namespace != Namespace.Main` checks the namespace of our page and if it is not page with namespace code that has Namespace.Main - it will skip this page (in Wikidata example you could see the simillar if statement). Then if it is page with the same code as Namespace.Main we get the label here: `val label = page.title.decoded` and then create a triple in this part of code `new Quad(context.language, DBpediaDatasets.Labels, subjectUri, labelProperty, label, page.sourceIri, context.ontology.datatypes("rdf:langString")`. 
Here we have:
```
language - context.language
dataset - DBpediaDatasets.Labels, 
subject - subjectUri, 
predicate - labelProperty,
object - label
context - page.sourceIri
datatype - context.ontology.datatypes("rdf:langString")
```
The datatype `context.ontology.datatypes("rdf:langString")` shows that our object is the *string with language tag*. Below you can see the example of a triple produced by Label Extractor:
`<http://dbpedia.org/resource/Samsung> <http://www.w3.org/2000/01/rdf-schema#label> "Samsung"@en .`
 In other extractors, you may see that datatype can also be `null`, it means that object is *url*.

## How to implement new Wikimedia Commons Files extractor

Firstly, check the type of your page in `extract` method, you can do it in this way: 
```
if (page.title.namespace == Namespace.File){
// your code
}
```
Then you need to implement the algorithm of specific data extraction (e.g. Licenses) from it. For Wikimedia Commons it will be also good to extend [commons minidump](https://github.com/dbpedia/extraction-framework/tree/master/dump/src/test/resources/minidumps/commons) with some new pages. Information of how to work with minidump, you can find here: http://dev.dbpedia.org/Testing_on_Minidumps .

## Output Dataset
If you create a new extractor, it is better to create a new dataset for output data. In the `DBpediaDatasets` you can see the examples of the datasets. https://github.com/dbpedia/extraction-framework/tree/65d7549dd8d750ee55c995db14c61aa0f0e86e26/core/src/main/scala/org/dbpedia/extraction/config/provenance 

## Write SHACL tests
http://dev.dbpedia.org/Integrating_SHACL_Tests 

## Run Minidump Tests
After you have implemented some new Extractor, you need to run minidump tests. Firstly you need to open `core` directory in the command line and execute `mvn install`, this command will recompile code in the `core` module. Then open `dump` directory in the command line and run `mvn test` command, it will run minidump tests. You can also extend the minidump with your 
Some additional information about it you can find here: http://dev.dbpedia.org/Testing_on_Minidumps .



## 

## Useful links
1. WikidataToolkit JsonDeserializer: https://github.com/Wikidata/Wikidata-Toolkit/blob/9d17dd7e1b199189145088e5220ed6f05d9e31e5/wdtk-datamodel/src/main/java/org/wikidata/wdtk/datamodel/helpers/JsonDeserializer.java 
2. Wikidata entities data model: https://www.mediawiki.org/wiki/Wikibase/DataModel




## TODO: integrate the content below, leftover from the Wiki

According to https://github.com/dbpedia/extraction-framework/pull/35/#issuecomment-16187074 the current design is the following

![](https://camo.githubusercontent.com/c274b58393dc2aafc799f023d84de5d670d232e3/68747470733a2f2f662e636c6f75642e6769746875622e636f6d2f6173736574732f3630373436382f3336333238362f31663864613632632d613166662d313165322d393963332d6262353133366163636330372e706e67)

In order to create a new Extractor you need to extend the `Extractor[T]` trait and in particular:

* `WikiPageExtractor` when you want to use only the page metadata. e.g. `RedirectExtractor`, `LabelExtractor', ... 
* `PageNodeExtractor` when you want to work with the Wikitext AST (most common case)
* `JsonNodeExtractor` when you want to work with Wikidata pages

Examples of Extractors can be found in the `org/dbpedia/extraction/mappings` package in `core` module.

If you want to test your new extractor you can do it in two ways:

* for a full dump extraction you can add `.MyNewExtractor` in the extraction property files in `dump` module
* add your extractor in the `server.default.properties` in the `server` module and start the mapping server with  `../run server`. Open `http://localhost:{PORT}/server/extraction/{LANG}/` and try your extractor on a specific page. (You can also run this from your IDE and debug)
