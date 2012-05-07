package org.dbpedia.extraction.util

class StringPlusser {
  
  private val sb = new java.lang.StringBuilder // use Java StringBuilder directly, not through Scala wrapper
  
  def + (value: AnyRef): this.type = { sb append(value); this }
  def + (value: Boolean): this.type = { sb append value; this }
  def + (value: Char): this.type = { sb append value; this }
  def + (value: Int): this.type = { sb append value; this }
  def + (value: Long): this.type = { sb append value; this }
  def + (value: Float): this.type = { sb append value; this }
  def + (value: Double): this.type = { sb append value; this }
  
  override def toString = sb toString
}

