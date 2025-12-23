package org.dbpedia.extraction.util

import java.util.logging.{Logger, Level}
import scala.util.control.Breaks._

class GeezNumberUtils {

  private val geezNumberMap: Map[Char, Int] = Map(
    '፩' -> 1,
    '፪' -> 2,
    '፫' -> 3,
    '፬' -> 4,
    '፭' -> 5,
    '፮' -> 6,
    '፯' -> 7,
    '፰' -> 8,
    '፱' -> 9,
    '፲' -> 10,
    '፳' -> 20,
    '፴' -> 30,
    '፵' -> 40,
    '፶' -> 50,
    '፷' -> 60,
    '፸' -> 70,
    '፹' -> 80,
    '፺' -> 90,
    '፻' -> 100,
    '፼' -> 10000
  )

  // Preprocesses and converts geez scripts to Arabic numerals.
  def preprocess(geezStr: String): List[Int] = {
    val strippedGeezStr = geezStr.trim
    val stack = scala.collection.mutable.ListBuffer[Int]()

    for (char <- strippedGeezStr) {
      geezNumberMap.get(char) match {
        case Some(value) => stack.append(value)
        case None =>
          throw new Exception(s"Unknown Geez number character: $char")
      }
    }

    stack.toList
  }

  // Recursively calculates and performs the calculation to convert geez numbers to Arabic numerals.
  def calculate(start: Int, end: Int, arr: List[Int]): Option[Int] = {
    if (start > end) return None
    if (start == end) {
      return Some(arr(start))
    }
    var idxs: List[Int] = List()

    breakable {
      for (multiplier <- List(10000, 100)) {
        if (arr.slice(start, end + 1).contains(multiplier)) {
          idxs = (end to start by -1).filter(i => arr(i) == multiplier).toList
          if (idxs.nonEmpty) {
            break
          }
        }
      }
    }

    for (node <- idxs) {
      val leftSubTreeValue = calculate(start, node - 1, arr).getOrElse(1)
      val rightSubTreeValue = calculate(node + 1, end, arr).getOrElse(0)
      return Some((arr(node) * leftSubTreeValue) + rightSubTreeValue)
    }

    if (idxs.isEmpty) {
      Some(arr.slice(start, end + 1).sum)
    } else {
      None
    }

  }

  def convertGeezToArabicNumeral(geezStr: String): Option[Int] = {
    val stack = preprocess(geezStr)
    val convertedNumber = calculate(0, stack.length - 1, stack)
    convertedNumber match {
      case Some(number) => return Some(number)
      case None         => return None
    }
  }

}
