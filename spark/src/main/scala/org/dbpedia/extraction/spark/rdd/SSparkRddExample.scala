package org.dbpedia.extraction.spark.rdd

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.compress.CompressionCodec
import org.apache.hadoop.util.ReflectionUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.dbpedia.extraction.destinations.Quad

/**
  * Created by Chile on 2/7/2017.
  */
object SSparkRddExample{
  def main(args: Array[String]): Unit = {

    System.setProperty("hadoop.home.dir","C:\\hadoop" );
    val conf = new SparkConf(true)
    conf.setMaster("local[*]")
    conf.setAppName("test")

    //create spark context
    val sc = SparkContext.getOrCreate(conf)

    //Input: create quad file handle (RDD)
    val rdd: RDD[String] = sc.textFile("C:\\Users\\Chile\\Desktop\\Dbpedia\\core-i18n\\dewiki\\20160305\\dewiki-20160305-mappingbased-literals.tql.bz2")

    //DPU: parse to quad objects (String -> Option[Quad])
    var quads = rdd.map[Option[Quad]] {
      case Quad(quad) => Some(quad)
      case _ => None
    }

    //DPU: filter 'deathPlace' predicates
    quads = quads.filter{
      case Some(quad) if quad.predicate == "http://dbpedia.org/ontology/deathDate" => true
      case _ => false
    }

    //DPU:
    val recoder = new SSparkRecodeUris()
    var out = quads.map[Quad](quad => recoder.transform(quad.get))

    //DPU: sort resulting quads by subject
    out = out.sortBy({quad => quad.subject}, true)

    val codecClass = Class.forName("org.apache.hadoop.io.compress.BZip2Codec")
    val c = new Configuration()
    val codec = ReflectionUtils.newInstance(codecClass, c).asInstanceOf[CompressionCodec]

    //Output: save resulting quad collection (Quad is serializable!)
    out.map(q => Quad.serialize(q)).coalesce(1).saveAsTextFile( "C:\\Users\\Chile\\Desktop\\Dbpedia\\core-i18n\\dewiki\\20160305\\dewiki-20160305-mappingbased-literals-filtered.tql.bz2", codec.getClass)
  }
}
