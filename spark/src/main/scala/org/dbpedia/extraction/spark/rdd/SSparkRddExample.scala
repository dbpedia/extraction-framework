package org.dbpedia.extraction.spark.rdd

import org.apache.spark.{SparkConf, SparkContext}
import org.dbpedia.extraction.destinations.Quad

/**
  * Created by Chile on 2/7/2017.
  */
class SSparkRddExample{

}

object SSparkRddExample{

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf(true)
    conf.setMaster("local[*]")
    conf.setAppName("test")

    //create spark context
    val sc = SparkContext.getOrCreate(conf)

    //Input: create quad file handle (RDD)
    val rdd = sc.textFile("C:\\Users\\Chile\\Desktop\\Dbpedia\\core-i18n\\dewiki\\20160305\\dewiki-20160305-mappingbased-objects-uncleaned.tql.bz2")

    //DPU: parse to quad objects (String -> Option[Quad])
    var quads = rdd.map[Option[Quad]] {
      case Quad(quad) => Some(quad)
      case _ => None
    }

    //DPU: filter 'deathPlace' predicates
    quads = quads.filter{
      case Some(quad) if quad.predicate == "http://dbpedia.org/ontology/deathPlace" => true
      case _ => false
    }

    //DPU:
    val recoder = new SSparkRecodeUris()
    var out = quads.map[Quad](quad => recoder.transform(quad.get))

    //DPU: sort resulting quads by subject
    out = out.sortBy({quad => quad.subject}, true)

    //Output: save resulting quad collection (Quad is serializable!)
    quads.saveAsTextFile( "C:\\Users\\Chile\\Desktop\\Dbpedia\\core-i18n\\dewiki\\20160305\\dewiki-20160305-mappingbased-objects-uncleaned_sorted.tql.bz2")
  }
}
