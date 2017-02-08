package org.dbpedia.extraction.spark.rdd

/**
  * Created by Chile on 2/8/2017.
  */
trait Transformer[I, O] {

  def transform(in:I): O
}
