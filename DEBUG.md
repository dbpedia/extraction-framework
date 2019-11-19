# DIEF Debugging


Mavens defualt behavior is to execute any found test during the `mvn install` goal.

```bash
git clone https://github.com/dbpedia/extraction-framework.git && cd extraction-framework
mvn clean install # add "-Dmaven.test.skip -DskipTests" to skip all tests during the install goal
```

## Minidump Test

To evaluate the development process of the DIEF, we introduce the minidump test.


The minidump tests implementation will run the following test

* Download the newest mappings and ontology files
* Extract [minidumps](`https://github.com/dbpedia/extraction-framework/tree/master/dump/src/test/resources/minidumps`) (generic and mappingbased approach)
* Validates extracted RDF with [RDFUnit](https://github.com/AKSW/RDFUnit)


If you want to perform only the minidump test

```bash
cd dumps/ # << $DIEF_DIR/dumps
mvn test
```

The latest test code can be found inside [MiniDumpTests.scala](https://github.com/dbpedia/extraction-framework/blob/master/dump/src/test/scala/org/dbpedia/extraction/dump/MinidumpTests.scala)

## See Also

* Implementing maven tests using [scalatest](http://www.scalatest.org/) 
* [How run a single test by name](https://stackoverflow.com/questions/24852484/how-to-run-a-single-test-in-scalatest-from-maven)
