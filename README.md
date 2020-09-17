# DBpedia Information Extraction Framework
![Build Status](https://github.com/dbpedia/extraction-framework/workflows/Java%20CI%20with%20Maven/badge.svg)

**Homepage**: http://dbpedia.org <br/>
**Documentation**: http://dev.dbpedia.org/Extraction  <br/>
**Get in touch with DBpedia**: https://wiki.dbpedia.org/join/get-in-touch <br/>
**Slack**: https://dbpedia-slack.herokuapp.com/ - join the **#dev-team** slack channel where [updates are posted](https://github.com/dbpedia/extraction-framework/blob/master/.github/workflows/maven.yml) and discussion is happening <br/>

## More documentation + better browsing
Need more documentation about the extraction? Check the DBpedia development bible at http://dev.dbpedia.org/Extraction

## About DBpedia
DBpedia is a crowd-sourced community effort to extract structured information from Wikipedia and make this information available on the Web. DBpedia allows you to ask sophisticated queries against Wikipedia, and to link the different data sets on the Web to Wikipedia data. We hope that this work will make it easier for the huge amount of information in Wikipedia to be used in some new interesting ways. Furthermore, it might inspire new mechanisms for navigating, linking, and improving the encyclopedia itself. <br>
To check out the projects of DBpedia, visit the [official DBpedia website](http://dbpedia.org).

## QuickStart

Running the extraction framework is a relatively complex task which is in details documented in the [advanced QuickStart guide](https://github.com/dbpedia/extraction-framework/blob/master/documentation/quickstart.md). To run the extraction process same as the DBpedia core team does, you can do using the [MARVIN release bot](https://git.informatik.uni-leipzig.de/dbpedia-assoc/marvin-config). The MARVIN bot automates the overall extraction process, from downloading the ontology, mappings and Wikipedia dumps, to extraction and post-processing the data.
```
git clone https://git.informatik.uni-leipzig.de/dbpedia-assoc/marvin-config
cd marvin-config
./setup-or-reset-dief.sh
# test run Romanian extraction, very small
./marvin_extraction_run.sh test
# around 4-7 days
./marvin_extraction_run.sh generic
```
... if you plan to work on improving the codebase of the framework you would need to run the extraction framework alone as described in the [QuickStart guide](https://github.com/dbpedia/extraction-framework/blob/master/documentation/quickstart.md). This is highly recommended, since during this process you will learn a lot about the extraction framework.

* Extractors represent the core of the extraction framework. So far, many extractors have been developed for extraction of particular information from different Wikimedia projects. To learn more, check the [New Extractors](https://github.com/dbpedia/extraction-framework/blob/master/documentation/new-extractor.md) guide, which explains the process of writing new extractor.

* Check the [Debugging Guide](https://github.com/dbpedia/extraction-framework/blob/master/documentation/debug.md) and learn how to debug the extraction framework.

### Extraction using Apache Spark

In order to speed up the extration process, the extraction framework has been adopted to run on Apache Spark.
Currently, more than half of the extractors can be executed using Spark. The extraction process using Spark is a slightly different process and requires different Execution. Check the [QuickStart](https://github.com/dbpedia/extraction-framework/blob/master/documentation/extraction-process.md#2-generic-spark-extraction) guide on how to run the extraction using Apache Spark.

Note: if possible, new extractors should be implemented using Apache Spark. To learn more, check the [New Extractors](https://github.com/dbpedia/extraction-framework/blob/master/documentation/new-extractor.md) guide, which explains the process of writing new extractor.


## The DBpedia Extraction Framework

The DBpedia community uses a flexible and extensible framework to extract different kinds of structured information from Wikipedia. The DBpedia extraction framework is written using Scala 2.8. The framework is available from the DBpedia Github repository (GNU GPL License). The change log may reveal more recent developments. More recent configuration options can be found here: https://github.com/dbpedia/extraction-framework/wiki

The DBpedia extraction framework is structured into different modules

* **Core Module** : Contains the core components of the framework.
* **Dump extraction Module** : Contains the DBpedia dump extraction application.

### Core Module

![http://www4.wiwiss.fu-berlin.de/dbpedia/wiki/DataFlow.png](http://www4.wiwiss.fu-berlin.de/dbpedia/wiki/DataFlow.png "http://www4.wiwiss.fu-berlin.de/dbpedia/wiki/DataFlow.png")

<a name="p27582-10"></a>

**Components**

* [Source](http://wiki.dbpedia.org/DeveloperDocumentation/Source?v=bms "Developer Documentation / Source") : The Source package provides an abstraction over a source of Media Wiki pages.
* [WikiParser](http://wiki.dbpedia.org/DeveloperDocumentation/WikiParser?v=hdy "Developer Documentation / Wiki Parser") : The Wiki Parser package specifies a parser, which transforms an Media Wiki page source into an Abstract Syntax Tree (AST).
* [Extractor](http://wiki.dbpedia.org/DeveloperDocumentation/Extractor?v=vqu "Developer Documentation / Extractor") : An Extractor is a mapping from a page node to a graph of statements about it.
* [Destination](http://wiki.dbpedia.org/DeveloperDocumentation/Destination?v=l9g "Developer Documentation / Destination") : The Destination package provides an abstraction over a destination of RDF statements.

<a name="p27582-11"></a>

In addition to the core components, a number of utility packages offers essential functionality to be used by the extraction code:

* **Ontology** Classes used to represent an ontology. Methods for both, reading and writing ontologies are provided. All classes are located in the namespace [org.dbpedia.extraction.ontology](tree/master/core/src/main/scala/org/dbpedia/extraction/ontology)
* **DataParser** Parsers to extract data from nodes in the abstract syntax tree. All classes are located in the namespace [org.dbpedia.extraction.dataparser](tree/master/core/src/main/scala/org/dbpedia/extraction/dataparser)
* **Util** Various utility classes. All classes are located in the namespace [org.dbpedia.extraction.util](tree/master/core/src/main/scala/org/dbpedia/extraction/util)

### Dump extraction Module
More recent configuration options can be found here: [https://github.com/dbpedia/extraction-framework/wiki/Extraction-Instructions](https://github.com/dbpedia/extraction-framework/wiki/Extraction-Instructions "Outgoing link (in new window)").

To know more about the extraction framework, click [here](https://github.com/dbpedia/extraction-framework/wiki/Documentation#h25-3)


## Contribution Guidelines

If you want to work on one of the [issues](https://github.com/dbpedia/extraction-framework/issues), assign yourself to it or at least leave a comment that you are working on it and how.  
If you have an idea for a new feature, make an issue first, assign yourself to it, then start working.  
Please make sure you have read the Developer's Certificate of Origin, further down on this page!

1. Fork the [main extraction-framework repository](https://github.com/dbpedia/extraction-framework) on GitHub.
2. Clone this fork onto your machine (`git clone <your_repo_url_on_github>`).
3. Switch to the `dev` branch (`git checkout dev`).
4. From the latest revision of the dev branch, make a new development branch from the latest revision. Name the branch something meaningful, for example _fixRestApiParams_ (`git checkout dev -b fixRestApiParams`).
5. Make changes and commit them to this branch.
  * Please commit regularly in small batches of things "that go together" (for example, changing a constructor and all the instance creating calls). Putting a huge batch of changes in one commit is bad for code reviews.
  * In the commit messages, summarize the commit in the first line using not more than 70 characters. Leave one line blank and describe the details in the following lines, preferably in bullet points, like in [7776e31...](https://github.com/dbpedia-spotlight/dbpedia-spotlight/commit/7776e314d4363c4254e921998b0165a43782589c).
6. When you are done with a bugfix or feature, `rebase` your branch onto `extraction-framework/dev` (`git pull --rebase git://github.com/dbpedia/extraction-framework.git`). Resolve possible conflicts and commit.
7. Push your branch to GitHub (`git push origin fixRestApiParams`).
8. Send a pull request from your branch into `extraction-framework/dev` via GitHub.
  * In the description, reference the associated commit (for example, _"Fixes #123 by ..."_ for issue number 123).
  * Your changes will be reviewed and discussed on GitHub.
  * In addition, [Travis-CI](http://about.travis-ci.org/) will test if the merged version passes the build.
  * If there are further changes you need to make, because Travis said the build fails or because somebody caught something you overlooked, go back to item 4. Stay on the same branch (if it is still related to the same issue). GitHub will add the new commits to the same pull request.
  * When everything is fine, your changes will be merged into `extraction-framework/dev`, finally the `dev` together with your improvements will be merged with the `master` branch.

Please keep in mind:
- Try *not* to modify the indentation. If you want to re-format, use a separate "formatting" commit in which no functionality changes are made.
- **Never** rebase the master onto a development branch (i.e. _never_ call `rebase` from `extraction-framework/master`). Only rebase your branch onto the dev branch, *if and only if* nobody already pulled from the development branch!
- If you already pushed a branch to GitHub, later rebased the master onto this branch and then tried to push again, GitHub won't let you saying _"To prevent you from losing history, non-fast-forward updates were rejected"_. If _(and only if)_ you are sure that nobody already pulled from this branch, add `--force` to the push command.  
[_"Don’t rebase branches you have shared with another developer."_](http://www.jarrodspillers.com/2009/08/19/git-merge-vs-git-rebase-avoiding-rebase-hell/)  
[_"Rebase is awesome, I use rebase exclusively for everything local. Never for anything that I've already pushed."_](http://jeffkreeftmeijer.com/2010/the-magical-and-not-harmful-rebase/#comment-87479247)  
[_"Never ever rebase a branch that you pushed, or that you pulled from another person_"](http://blog.experimentalworks.net/2009/03/merge-vs-rebase-a-deep-dive-into-the-mysteries-of-revision-control/)
- In general, we prefer Scala over Java.

More tips:
- Guides to setup your development environment for [Intellij](Setting up IntelliJ IDEA) or [Eclipse](Setting up eclipse).
- Get help with the [Maven build](Build-from-Source-with-Maven) or another form of [installation](Installation).
- [Download](Downloads) some data to work with.
- How to run [from Scala/Java](Run-from-Java-or-Scala) or [from a JAR](Run-from-a-JAR).
- Having different troubles? Check the [troubleshooting page](Troubleshooting) or post on https://forum.dbpedia.org.

### Important: Developer's Certificate of Origin
By sending a pull request to the [extraction-framework repository](https://github.com/dbpedia/extraction-framework) on GitHub, you implicitly accept the [Developer's Certificate of Origin 1.1](https://github.com/dbpedia/extraction-framework/blob/master/documentation/DeveloperCertificateOfOrigin.md)

## License
The source code is under the terms of the [GNU General Public License, version 2](http://www.gnu.org/licenses/gpl-2.0.html).


