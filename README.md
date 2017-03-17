# DBpedia Information Extraction Framework

[![Build Status](https://travis-ci.org/dbpedia/extraction-framework.svg?branch=master)](https://travis-ci.org/dbpedia/extraction-framework)

### Contents
* [About DBpedia](#about-dbpedia)  
* [The DBpedia Extraction Framework](#the-dbpedia-extraction-framework) <br>
     1. [Core Module](#core-module)
     2. [Dump Extraction Module](#dump-extraction-module)
* [Quickstart](#quickstart)
* [Contribution Guidelines](#contribution-guidelines)
* [Wiki](#wiki)
* [License](#license)<br><br>


## About DBpedia
DBpedia is a crowd-sourced community effort to extract structured information from Wikipedia and make this information available on the Web. DBpedia allows you to ask sophisticated queries against Wikipedia, and to link the different data sets on the Web to Wikipedia data. We hope that this work will make it easier for the huge amount of information in Wikipedia to be used in some new interesting ways. Furthermore, it might inspire new mechanisms for navigating, linking, and improving the encyclopedia itself. <br>
To check out the projects of DBpedia, visit the [official DBpedia website](http://dbpedia.org).

## The DBpedia Extraction Framework

The DBpedia community uses a flexible and extensible framework to extract different kinds of structured information from Wikipedia. The DBpedia extraction framework is written using Scala 2.8. The framework is available from the DBpedia Github repository (GNU GPL License). The change log may reveal more recent developments. More recent configuration options can be found here: https://github.com/dbpedia/extraction-framework/wiki

The DBpedia extraction framework is structured into different modules

* **Core Module** : Contains the core components of the framework.
* **Dump extraction Module** : Contains the DBpedia dump extraction application.

<a name="p27582-8"></a>


<a name="h25-4"></a>

### Core Module

<a name="p27582-9"></a>

![http://www4.wiwiss.fu-berlin.de/dbpedia/wiki/DataFlow.png](http://www4.wiwiss.fu-berlin.de/dbpedia/wiki/DataFlow.png "http://www4.wiwiss.fu-berlin.de/dbpedia/wiki/DataFlow.png")

<a name="p27582-10"></a>

**Components**

* [Source](http://wiki.dbpedia.org/DeveloperDocumentation/Source?v=bms "Developer Documentation / Source") : The Source package provides an abstraction over a source of Media Wiki pages.
* [WikiParser](http://wiki.dbpedia.org/DeveloperDocumentation/WikiParser?v=hdy "Developer Documentation / Wiki Parser") : The Wiki Parser package specifies a parser, which transforms an Media Wiki page source into an Abstract Syntax Tree (AST).
* [Extractor](http://wiki.dbpedia.org/DeveloperDocumentation/Extractor?v=vqu "Developer Documentation / Extractor") : An Extractor is a mapping from a page node to a graph of statements about it.
* [Destination](http://wiki.dbpedia.org/DeveloperDocumentation/Destination?v=l9g "Developer Documentation / Destination") : The Destination package provides an abstraction over a destination of RDF statements.

<a name="p27582-11"></a>

In addition to the core components, a number of utility packages offers essential functionality to be used by the extraction code:

* **Ontology** Classes used to represent an ontology. Methods for both, reading and writing ontologies are provided. All classes are located in the namespace [org.dbpedia.extraction.ontology](http://www4.wiwiss.fu-berlin.de/dbpedia/scaladoc/org/dbpedia/extraction/ontology/package.html "Outgoing link (in new window)")
* **DataParser** Parsers to extract data from nodes in the abstract syntax tree. All classes are located in the namespace [org.dbpedia.extraction.dataparser](http://www4.wiwiss.fu-berlin.de/dbpedia/scaladoc/org/dbpedia/extraction/dataparser/package.html "Outgoing link (in new window)")
* **Util** Various utility classes. All classes are located in the namespace [org.dbpedia.extraction.util](http://www4.wiwiss.fu-berlin.de/dbpedia/scaladoc/org/dbpedia/extraction/util/package.html "Outgoing link (in new window)")

<a name="p27582-12"></a>

For details about a package follow the links.

You can find the complete scaladoc [here](http://www4.wiwiss.fu-berlin.de/dbpedia/scaladoc/index.html "Outgoing link (in new window)")

<a name="h25-5"></a>

### Dump extraction Module
More recent configuration options can be found here: [https://github.com/dbpedia/extraction-framework/wiki/Extraction-Instructions](https://github.com/dbpedia/extraction-framework/wiki/Extraction-Instructions "Outgoing link (in new window)").

To know more about the extraction framework, click [here](https://github.com/dbpedia/extraction-framework/wiki/Documentation#h25-3)

## Quickstart 

Before you can start developing you need to take care of some prerequisites:

* **DBpedia Extraction Framework** Get the most recent revision from the [Github repository](https://github.com/dbpedia/extraction-framework).

     `$ git clone git://github.com/dbpedia/extraction-framework.git`
* **Java Development Kit** The DBpedia extraction framework uses Java. Get the most recent JDK from [http://java.sun.com/](http://java.sun.com/). DBpedia requires at least Java 7 (v1.7.0). To compile and run it with an earlier version, delete or blank the following two files.(The launchers purge-download and purge-extract in the dump module won't work, but they are not vitally necessary.)  

    `core/src/main/scala/org/dbpedia/extraction/util/RichPath.scala`

    `dump/src/main/scala/org/dbpedia/extraction/dump/clean/Clean.scala`

* **Maven** is used for project management and build automation. Get it from: [http://maven.apache.org/](http://maven.apache.org/). Please download Maven 3.

This is enough to compile and run the DBpedia extraction framework. The required input files, the wikimedia dumps, will be downloaded by extractor code if configured to do so (see [here](https://github.com/dbpedia/extraction-framework/wiki/Extraction-Instructions)). Check [this](https://github.com/dbpedia/extraction-framework/wiki/Development-Environment-Setup) out to know more about Development Environment Setup. 

## Contribution Guidelines

If you want to work on one of the [issues](https://github.com/dbpedia/extraction-framework/issues), assign yourself to it or at least leave a comment that you are working on it and how.  
If you have an idea for a new feature, make an issue first, assign yourself to it, then start working.  
Please make sure you have read the Developer's Certificate of Origin, further down on this page!

1. Fork the [main extraction-framework repository](https://github.com/dbpedia/extraction-framework) on GitHub.
2. Clone this fork onto your machine (`git clone <your_repo_url_on_github>`).
3. From the latest revision of the master branch, make a new development branch from the latest revision. Name the branch something meaningful, for example _fixRestApiParams_ (`git checkout master -b fixRestApiParams`).
4. Make changes and commit them to this branch.
  * Please commit regularly in small batches of things "that go together" (for example, changing a constructor and all the instance creating calls). Putting a huge batch of changes in one commit is bad for code reviews.
  * In the commit messages, summarize the commit in the first line using not more than 70 characters. Leave one line blank and describe the details in the following lines, preferably in bullet points, like in [7776e31...](https://github.com/dbpedia-spotlight/dbpedia-spotlight/commit/7776e314d4363c4254e921998b0165a43782589c).
5. When you are done with a bugfix or feature, `rebase` your branch onto `extraction-framework/master` (`git pull --rebase git://github.com/dbpedia/extraction-framework.git`). Resolve possible conflicts and commit.
6. Push your branch to GitHub (`git push origin fixRestApiParams`).
7. Send a pull request from your development branch into `extraction-framework/master` via GitHub.
  * In the description, reference the associated commit (for example, _"Fixes #123 by ..."_ for issue number 123).
  * Your changes will be reviewed and discussed on GitHub.
  * In addition, [Travis-CI](http://about.travis-ci.org/) will test if the merged version passes the build.
  * If there are further changes you need to make, because Travis said the build fails or because somebody caught something you overlooked, go back to item 4. Stay on the same branch (if it is still related to the same issue). GitHub will add the new commits to the same pull request.
  * Finally, when everything is fine, your changes will be merged into `extraction-framework/master`.
  
Read the complete contribution guidelines [here](https://github.com/dbpedia/extraction-framework/wiki/Contributing) 

## Wiki 
For more information about DBpedia, check out the [wiki](https://github.com/dbpedia/extraction-framework/wiki) page.   

## License

The source code is under the terms of the [GNU General Public License, version 2](http://www.gnu.org/licenses/gpl-2.0.html).

