# DIEF Debugging

Debugging a large codebase, for example the DIEF repository, is quite hard.
One of the common methodologies is to write Unit tests (e.g. by using JUnit in JAVA).
Therefore, we started to cover the DIEF code with Unit tests as well. 
This will lead to a better debugging experience and enables the evaluation of quality improvement between older and newer code. 

In the case of our implementation, we chose to use mainly [scala-test](http://www.scalatest.org/) plugin for [Apache Maven](https://maven.apache.org/), but if you want to use JAVA it is also possible to write tests using [JUnit-4](https://junit.org/junit4/).

Mavens default behavior is to execute any found test in the code base during the "install" goal. \
Thus, to install the DIEF you just simply clone the repository, enter the directory and execute `mvn install`

```bash
git clone https://github.com/dbpedia/extraction-framework.git && cd extraction-framework
mvn clean install # add "-Dmaven.test.skip -DskipTests" to skip all tests during the install goal
```

For troubleshooting, check if you fulfill the needed [requirements](#requirements).

## Contribution

If you want to contribute to this debugging process feel free to, add a Unit test for a given part of the DIEF (e.g. one of the implemented [data parsers](https://github.com/dbpedia/extraction-framework/tree/master/core/src/main/scala/org/dbpedia/extraction/dataparser)) and create a pull request.

## Ad hoc extraction
You can [deploy](http://dev.dbpedia.org/Extraction_QuickStart#3a-running-per-entity-ad-hoc-extraction--deploying-ad-hoc-extraction-server) your own instance of an ad hoc extraction server on your local machine in order to see the extraction results for a single entity/resource/article (see e.g. http://dbpedia.informatik.uni-leipzig.de:9999/server/extraction/en/) 
## Minidump Tests

For evaluating the quality of the DIEF development process, we introduce the minidump tests.
The main goal of this test collection is to retrieve a global overview of the extraction quality. This is needed because sometimes the code improvement of one part in the code can lead to a decress or failure of other parts.

#### Workflow

The minidump test uses subsets of single official Wikipedia dumps as extraction import.
For now, its implementation will run the following test

* Download the newest DBpedia [mappings](http://mappings.dbpedia.org/index.php/Main_Page) and [ontology](https://github.com/dbpedia/ontology-tracker/tree/master/databus/dbpedia/ontology/dbo-snapshots) files
* Extract RDF from the [minidumps](https://github.com/dbpedia/extraction-framework/tree/master/dump/src/test/resources/minidumps) (generic and mapping-based approach)
* Evaluate the RDF [syntax quality](#dief-syntax-evaluation) of the extracted dumps
* Validate the extracted RDF dumps with [RDFUnit](https://github.com/AKSW/RDFUnit)

#### Testing

To perform only the minidump tests change to the `dumps` directory and execute `mvn test`.

```bash
cd dumps/ # << $DIEF_DIR/dumps
mvn test
```

#### Code

The latest test code can be found inside [MiniDumpTests.scala](https://github.com/dbpedia/extraction-framework/blob/master/dump/src/test/scala/org/dbpedia/extraction/dump/MinidumpTests.scala)

## DIEF Syntax Evaluation

Further, we designed a quality assessment approach which can be used to evaluate given RDF data (more formats will be accessible in the future).

In short, the evaluation is using a various number of defined IRI namespace and literal pattern. These models are stored using the RDF turtle serialization, for example [dbpedia-specific-ci-tests.ttl](https://github.com/dbpedia/extraction-framework/blob/master/dump/src/test/resources/dbpedia-specific-ci-tests.ttl).

#### EvalMod

> **TODO** [ValidationLauncher.scala](https://github.com/dbpedia/extraction-framework/blob/master/core/src/main/scala/org/dbpedia/validation/ValidationLauncher.scala), maybe rename class 

## Requirements 

> **TODO** move to README.md

* A compatible shell (e.g. bash) to follow the instructions
* The version control system Git
* Java JDK 1.8 ( does not compile with JDK 1.11 )
* Apache  Maven 3.3 or higher
* Scala 2.11.4 (should only matter in an IDEA e.g. Intellij)

## FYI

* [How run a single scala-test by name](https://stackoverflow.com/questions/24852484/how-to-run-a-single-test-in-scalatest-from-maven)




