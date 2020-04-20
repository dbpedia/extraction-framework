//package org.dbpedia.databus.mod
//
//import java.io.{File, FileWriter}
//import java.net.URL
//import java.text.SimpleDateFormat
//import java.time.{Instant, ZoneId, ZonedDateTime}
//import java.util.Calendar
//
//import org.apache.commons.io.FileUtils
//import org.apache.spark.sql.{SQLContext, SparkSession}
//import scopt.OptionParser
//
//import scala.language.postfixOps
//import scala.sys.process.Process
//
///**
// * A DBpedia Databus Mod
// *   Evaluation of dataset construct correctness.
// */
//object EvalMod {
//
//  val modVocab: String =
//    s"""
//       |# no base
//       |@prefix mod: <http://dataid.dbpedia.org/ns/mod.ttl#> .
//       |@prefix owl: <http://www.w3.org/2002/07/owl#>.
//       |@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
//       |@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.
//       |
//       |<#NtriplesEvalMod> a owl:Class ;
//       |  rdfs:subClassOf mod:DatabusMod ;
//       |  rdfs:label "Evaluation of Ntirple Datasets" ;
//       |  rdfs:comment "Downloads the files and analyses IRI correctness and xsd:datatypes" .
//       |
//       |
//       |<errorRate> a owl:DatatypeProperty ;
//       |  rdfs:subPropertyOf mod:statSummary ;
//       |  rdfs:label "error rate" ;
//       |  rdfs:comment "Average error rate of failed test cases (prevalence of test case divided by its fails)" ;
//       |  rdfs:range xsd:float .
//       """.stripMargin
//
//  case class EvalModConf(
//                          repo: String = null,
//                          serviceRepoURL: String = null,
//                          updateTSVs: Seq[String] = null,
//                          testModels: Seq[String] = null
//                        )
//
//  def main(args: Array[String]): Unit = {
//
//    val optionParser = new OptionParser[EvalModConf]("eval mod") {
//
//      head("eval mod", "0.1")
//
//      // @param repo
//      opt[String]('r', "repo").required().maxOccurs(1).action((s, p) => p.copy(repo = s))
//        .text("TOOD")
//
//      // @param serviceURL
//      opt[String]('s', "service-url").required().maxOccurs(1).action((s, p) => p.copy(serviceRepoURL = s))
//        .text("TODO")
//
//      // @param update-tsv
//      opt[Seq[String]]('u', "update-tsv").required().maxOccurs(1).action((s, p) => p.copy(updateTSVs = s))
//        .text("TODO")
//
//      // @param test-model
//      opt[Seq[String]]('t', "test-model").required().maxOccurs(1).action((s, p) => p.copy(testModels = s))
//        .text("TODO")
//    }
//
//    optionParser.parse(args, EvalModConf()) match {
//
//      case Some(evalModConf) =>
//
//        val hadoopHomeDir = new File("./.haoop/")
//        hadoopHomeDir.mkdirs()
//        System.setProperty("hadoop.home.dir", hadoopHomeDir.getAbsolutePath)
//
//        val sparkSession = SparkSession.builder()
//          .config("hadoop.home.dir", "./.hadoop")
//          .config("spark.local.dir", "./.spark")
//          .appName("Test Iris").master("local[*]").getOrCreate()
//        sparkSession.sparkContext.setLogLevel("WARN")
//
//        val sqlContext: SQLContext = sparkSession.sqlContext
//
//        eval_uploaded_data(evalModConf)(sqlContext)
//
//      case _ => optionParser.showTryHelp()
//    }
//  }
//
//  def eval_uploaded_data(evalModConf: EvalModConf)(implicit sqlContext: SQLContext): Unit = {
//
//    val repo = evalModConf.repo
//    val serviceURL = evalModConf.serviceRepoURL
//    val updateTSVs = evalModConf.updateTSVs
//    val testModels = evalModConf.testModels
//
//    //reset aggregate
//    //writeFile(s"$repo/aggregate.nt", "")
//
//    //write vocab
//    writeFile(s"$repo/modvocab.ttl", modVocab)
//
//    new File(s"$repo/tmp/").mkdirs()
//
//    val testSuite = TestSuiteFactory.loadTestSuite(testModels.toArray)
//
//    updateTSVs.foreach(
//
//      update => {
//
//        scala.io.Source.fromFile(update).getLines().drop(1).foreach(
//
//          line => {
//
//            val split = line.split("\t")
//            val file = split(0).trim.drop(1).dropRight(1)
//            val sha = split(1).trim.drop(1).dropRight(1)
//            val downloadURL = split(2).trim.drop(1).dropRight(1)
//
//            // path
//            val tmp = file.replace("https://databus.dbpedia.org/", "")
//            val pos = tmp.lastIndexOf("/")
//            val path = tmp.substring(0, pos)
//            val filename = tmp.substring(pos + 1)
//
//            new File(s"$repo/$path/").mkdirs()
//
//
//            if (!new File(s"$repo/$path/$sha.ttl").exists()) {
//
//              downloadFile(downloadURL, new File(s"$repo/tmp/${sha}_${downloadURL.split("/").last}"))
//
//              val encodedReport = {
//                ValidationExecutor.testIris(
//                  s"$repo/tmp/${sha}_${downloadURL.split("/").last}",
//                  testSuite
//                )
//              }
//
//              val modReport = {
//                org.dbpedia.validation.buildModReport(
//                  s"Aggregated Test Case Report of: $sha ( $filename )",
//                  encodedReport.reduce(_ + _),
//                  testSuite.triggerCollection,
//                  testSuite.testApproachCollection
//                )
//              }
//              val html = modReport._1
//              val errorRate = modReport._2
//
//              writeFile(s"$repo/$path/$sha.html", html.mkString)
//
//              writeSVG(s"$repo/$path/$sha.svg", errorRate)
//
//              writeActivityTTL(s"$repo/$path/$sha.ttl", file, errorRate, 0.0f, serviceURL, path, sha, repo)
//
//              FileUtils.deleteQuietly(new File(s"$repo/tmp/${sha}_${downloadURL.split("/").last}"))
//            }
//            else if (sha == "d3dda84eb03b9738d118eb2be78e246106900493c0ae07819ad60815134a8058") {
//
//              val cmd = Seq("bash", "-c", s"grep $file $repo/$path/$sha.ttl >/dev/null")
//
//              val exitCode = Process(cmd).!
//
//              if (exitCode != 0) {
//                writeActivityTTL(s"$repo/$path/$sha.ttl", file,
//                  0, 0.0f, serviceURL, path, sha, repo, true)
//              }
//            }
//          }
//        )
//      }
//    )
//  }
//
//  def downloadFile(downloadURL: String, sinkFile: File): Unit = {
//
//    //TODO change method of downloading
//
//    import sys.process._
//
//    new URL(downloadURL) #> sinkFile !!
//  }
//
//  def writeFile(file: String, contents: String, append: Boolean = false): Unit = {
//    val fw = new FileWriter(file, append)
//    try {
//      fw.write(contents)
//      System.err.println(
//        s"${new SimpleDateFormat("yyyy-dd-MM hh:mm:ss").format(Calendar.getInstance().getTime)} " +
//          s"| INFO " +
//          s"| writeFile " +
//          s"| written (append: $append) " + file
//      )
//    }
//    finally fw.close()
//  }
//
//  def getRecursiveListOfFiles(dir: File): Array[File] = {
//    val these = dir.listFiles
//    these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
//  }
//
//  def writeActivityTTL(activityFile: String, databusFile: String,
//                       errorRate: Float, coverage: Float, serviceRepoURL: String,
//                       path: String, sha: String, repo: String, append: Boolean = false) {
//
//    val invocationTime: ZonedDateTime = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
//
//    val ntriples = {
//
//      if (append) {
//        s"""
//           |<$serviceRepoURL/$path/$sha.svg> <http://dataid.dbpedia.org/ns/mod.ttl#svgDerivedFrom> <$databusFile> .
//           |<$serviceRepoURL/$path/$sha.html> <http://dataid.dbpedia.org/ns/mod.ttl#htmlDerivedFrom> <$databusFile> .
//           |<$serviceRepoURL/$path/$sha.ttl#this> <http://www.w3.org/ns/prov#used> <$databusFile> .
//           |""".stripMargin
//      }
//      else {
//        s"""
//           |<$serviceRepoURL/$path/$sha.svg> <http://dataid.dbpedia.org/ns/mod.ttl#svgDerivedFrom> <$databusFile> .
//           |<$serviceRepoURL/$path/$sha.html> <http://dataid.dbpedia.org/ns/mod.ttl#htmlDerivedFrom> <$databusFile> .
//           |<$serviceRepoURL/$path/$sha.ttl#this> <http://www.w3.org/ns/prov#generated> <$serviceRepoURL/$path/$sha.svg> .
//           |<$serviceRepoURL/$path/$sha.ttl#this> <http://www.w3.org/ns/prov#generated> <$serviceRepoURL/$path/$sha.html> .
//           |<$serviceRepoURL/$path/$sha.ttl#this> <http://www.w3.org/ns/prov#used> <$databusFile> .
//           |<$serviceRepoURL/$path/$sha.ttl#this> <http://www.w3.org/ns/prov#endedAtTime> "$invocationTime"^^<http://www.w3.org/2001/XMLSchema#dateTime> .
//           |<$serviceRepoURL/$path/$sha.ttl#this> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <$serviceRepoURL/modvocab.ttl#EvalMod> .
//           |<$serviceRepoURL/$path/$sha.ttl#this> <$serviceRepoURL/modvocab.ttl#errorRate> "$errorRate"^^<http://www.w3.org/2001/XMLSchema#float> .
//           |""".stripMargin
//      }
//    }
//
//    //<$serviceRepoURL/$path/$sha.ttl#this> <$serviceRepoURL/modvocab.ttl#coverage> "$coverage"^^<http://www.w3.org/2001/XMLSchema#float> .
//
//    writeFile(activityFile, ntriples, append)
//    //writeFile(s"$repo/aggregate.nt", ntriples, true)
//  }
//
//  def writeSVG(svgFile: String, errorRate: Float): Unit = {
//
//    var color = errorRate match {
//      case x if (1 - x) >= 1.0f => "#4c1"
//      case x if (1 - x) > 0.80f => "#fc0"
//      case _ => "#cc0000"
//    }
//
//    val percentage = BigDecimal((errorRate * 100).toDouble).setScale(2, BigDecimal.RoundingMode.HALF_UP) + "%"
//
//    val svg =
//      s"""<?xml version="1.0" encoding="UTF-8" standalone="no"?>
//         |<svg
//         |   xmlns:dc="http://purl.org/dc/elements/1.1/"
//         |   xmlns:cc="http://creativecommons.org/ns#"
//         |   xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
//         |   xmlns:svg="http://www.w3.org/2000/svg"
//         |   xmlns="http://www.w3.org/2000/svg"
//         |   xmlns:sodipodi="http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd"
//         |   xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape"
//         |   width="105.5"
//         |   height="20"
//         |   id="svg2"
//         |   version="1.1"
//         |   inkscape:version="0.91 r13725"
//         |   sodipodi:docname="test.svg">
//         |  <metadata
//         |     id="metadata31">
//         |    <rdf:RDF>
//         |      <cc:Work
//         |         rdf:about="">
//         |        <dc:format>image/svg+xml</dc:format>
//         |        <dc:type
//         |           rdf:resource="http://purl.org/dc/dcmitype/StillImage" />
//         |      </cc:Work>
//         |    </rdf:RDF>
//         |  </metadata>
//         |  <defs
//         |     id="defs29" />
//         |  <sodipodi:namedview
//         |     pagecolor="#ffffff"
//         |     bordercolor="#666666"
//         |     borderopacity="1"
//         |     objecttolerance="10"
//         |     gridtolerance="10"
//         |     guidetolerance="10"
//         |     inkscape:pageopacity="0"
//         |     inkscape:pageshadow="2"
//         |     inkscape:window-width="1920"
//         |     inkscape:window-height="1031"
//         |     id="namedview27"
//         |     showgrid="false"
//         |     inkscape:zoom="7.8888889"
//         |     inkscape:cx="45"
//         |     inkscape:cy="10"
//         |     inkscape:window-x="0"
//         |     inkscape:window-y="27"
//         |     inkscape:window-maximized="1"
//         |     inkscape:current-layer="g17" />
//         |  <linearGradient
//         |     id="a"
//         |     x2="0"
//         |     y2="100%">
//         |    <stop
//         |       offset="0"
//         |       stop-color="#bbb"
//         |       stop-opacity=".1"
//         |       id="stop5" />
//         |    <stop
//         |       offset="1"
//         |       stop-opacity=".1"
//         |       id="stop7" />
//         |  </linearGradient>
//         |  <rect
//         |     rx="3"
//         |     width="88"
//         |     height="20"
//         |     fill="#555"
//         |     id="rect9" />
//         |  <rect
//         |     rx="3"
//         |     x="58"
//         |     width="47.5"
//         |     height="20"
//         |     fill="${color}"
//         |     id="rect11" />
//         |  <path
//         |     fill="${color}"
//         |     d="M58 0h4v20h-4z"
//         |     id="path13" />
//         |  <rect
//         |     rx="3"
//         |     width="105.5"
//         |     height="20"
//         |     fill="url(#a)"
//         |     id="rect15" />
//         |  <g
//         |     fill="#fff"
//         |     text-anchor="middle"
//         |     font-family="DejaVu Sans,Verdana,Geneva,sans-serif"
//         |     font-size="11"
//         |     id="g17">
//         |    <text
//         |       x="28.5"
//         |       y="14"
//         |       id="text19">errorRate</text>
//         |    <text
//         |       x="60.5"
//         |       y="15"
//         |       id="text23"
//         |       style="fill:#010101;fill-opacity:0.3" />
//         |    <text
//         |       x="81.0"
//         |       y="14"
//         |       id="text25">${percentage}</text>
//         |  </g>
//         |</svg>
//       """.stripMargin
//
//    writeFile(svgFile, svg)
//  }
//}
