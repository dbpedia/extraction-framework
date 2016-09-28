import java.io.File

import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.util.ConfigUtils
import org.scalatest.{ConfigMap, FunSuite}
import org.dbpedia.extraction.scripts.SdTypeCreation
/**
  * Created by Chile on 9/26/2016.
  */
class SdTypeCreation$BoosterTest extends FunSuite {

  test("TestDomainRangeBooster"){
    val baseDir = new File("C:\\Users\\Chile\\Desktop\\Dbpedia\\core-i18n")
    val language = ConfigUtils.parseLanguages(baseDir, Seq("de"))(0)
    val ontology = {
      val ontologySource = XMLSource.fromFile(new File("C:\\Users\\Chile\\Desktop\\Dbpedia\\ontology.xml"), language)
      new OntologyReader().read(ontologySource)
    }

    System.out.println(SdTypeCreation.calculateDomainRangeBooster("http://dbpedia.org/ontology/University", "http://dbpedia.org/ontology/numberOfStudents", SdTypeCreation.PredicateDirection.Out, ontology ))
    System.out.println(SdTypeCreation.calculateDomainRangeBooster("http://dbpedia.org/ontology/EducationalInstitution", "http://dbpedia.org/ontology/numberOfStudents", SdTypeCreation.PredicateDirection.Out, ontology ))
    System.out.println(SdTypeCreation.calculateDomainRangeBooster("http://dbpedia.org/ontology/University", "http://dbpedia.org/ontology/editing", SdTypeCreation.PredicateDirection.Out, ontology ))
  }

}
