package org.dbpedia.extraction.dump

/**
 * Helper class for CompletionWriter.
 * 
 * CompletionWriter.write() receives lines with ids. The lines may not be quite in the correct
 * order (because they are written by multiple threads), but must be written in the correct order
 * (otherwise, CompletionReader does not work). This queue helps: it stores elements by their id.
 * CompletionWriter simply inserts elements into this queue and calls foreach(). foreach() iterates 
 * only over complete ranges and stops at the first gap. At a later point, CompletionWriter will 
 * fill that gap.
 * 
 * Not thread-safe! Use external synchronization.
 * 
 * @param initSize initial size of queue. will grow as necessary.
 * @param startId initial base id. will grow as necessary.
 * @param maxOffset maximum offset between lowest and highest id in queue. used to limit queue size.
 * if an id with a larger offset is encountered, update() throws an exception.
 */
class CompletionQueue[V](initSize : Int = 16, initId : Int = 0, maxOffset : Int = 100000) {
  
  /** current base id. element with this id is at base index. */
  private var baseId = initId
  
  /** current base index. element at this index has base id. */
  private var baseIndex = 0
  
  /** rolling array. base index walks through the array and wraps to the start when it reaches the end. */
  private var queue = new Array[Any](initSize)
  
  /**
   * Add the element with the given id to the queue.
   * @param must not be less than the current base id
   * @param value must not be null
   */
  def update(id : Int, value : V) : Unit = {
    if (value == null) throw new NullPointerException("value")
    if (id < baseId) throw new IllegalArgumentException("id ["+id+"] is less than base id ["+baseId+"]")
    
    val offset = id - baseId
    if (offset > maxOffset) throw new IllegalArgumentException("id ["+id+"] is greater than base id ["+baseId+"] plus max offset ["+maxOffset+"]")
    
    var size = queue.length
    if (offset >= size) size = expand(offset)
    
    queue((baseIndex + offset) % size) = value
  }
  
  /**
   * Get the element for the given id from the queue.
   * @return may be null
   */
  def apply(id : Int) : V = {
    val offset = id - baseId
    var size = queue.length
    val value = if (offset > size) null else queue((baseIndex + offset) % size)
    value.asInstanceOf[V]
  }
  
  /**
   * Iterate over all existing elements in order. Starts at the current start of the queue,
   * stops at the first gap.
   */
  def foreach( f : V => Unit ) : Unit = {
    val size = queue.length
    while(true) {
      val value = queue(baseIndex)
      if (value == null) return
      
      // change state before each step. f() may throw an exception.
      queue(baseIndex) = null
      baseId += 1
      baseIndex += 1
      if (baseIndex == size) baseIndex = 0
      
      f(value.asInstanceOf[V])
    }
  }
  
  /**
   * double array size until there's enough space 
   */
  private def expand(offset : Int) : Int = {
    
    var size, expand = queue.length
    do {
      // bail before the next doubling would overflow
      if (expand >= (Int.MaxValue >> 1)) throw new IllegalStateException("cannot expand queue size further than ["+expand+"]")
      expand <<= 1
    } 
    while(offset >= expand)
      
    val copy = new Array[Any](expand)
    
    // elements from base index to end of array are copied to same position
    // elements below base index are moved to new places above previous end
    System.arraycopy(queue, baseIndex, copy, baseIndex, size - baseIndex)
    System.arraycopy(queue, 0, copy, size, baseIndex)
    
    queue = copy
    
    expand
  }
  
}
