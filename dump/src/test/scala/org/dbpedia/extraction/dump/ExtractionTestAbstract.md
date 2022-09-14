# ExtractionTestAbstract

designed for testing abstracts extractors
## Before all

* Delete tag @DoNotDiscover of ExtractionTestAbstract
* add the tag @DoNotDiscover to other test class

## Procedure :
* first clean your target directory with ``` mvn clean ``` in the root directory of DIEF
* go to bash scripts via ``` cd /dump/src/test/bash  ```
* OPTIONAL :  creating a new Wikipedia minidump sample with ``` bash create_custom_sample.sh -n $numberOfPage -l $lang -d $optionalDate ```
* process sample of Wikipedia pages ``` bash Minidump_custom_sample.sh -f $filename/lst ```
* Update your extraction language parameter regarding of your minidump sample in [extraction.nif.abstracts.properties](https://github.com/datalogism/extraction-framework/blob/gsoc-celian/dump/src/test/resources/extraction-configs/extraction.nif.abstracts.properties) and in [extraction.plain.abstracts.properties](https://github.com/datalogism/extraction-framework/blob/gsoc-celian/dump/src/test/resources/extraction-configs/extraction.plain.abstracts.properties)
* Change the name of your log in the [**ExtractionTestAbstract.scala**](https://github.com/datalogism/extraction-framework/blob/gsoc-celian/dump/src/test/scala/org/dbpedia/extraction/dump/ExtractionTestAbstract.scala) file
* rebuild the app with ``` mvn install ``` or just test it with ```  mvn test -Dtest="ExtractionTestAbstract2" ```
