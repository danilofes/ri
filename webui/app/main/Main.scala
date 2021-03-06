package main

import java.io.File

import scala.Array.canBuildFrom
import scala.collection.mutable.ListBuffer
import scala.io.Source

import models.DocumentDb
import models.QueryEngine
import models.Vocabulary

object Main extends App {

  val inputDir = new File(args(0))
  val queryFiles = inputDir.listFiles.filter(_.isFile())

  val vocabulary = Vocabulary.fromFile(new File("data/cri.vocabulary.dat"), new File("data/cri.index.dat"), new File("data/cri.avocabulary.dat"), new File("data/cri.aindex.dat"))
  val documentDb = DocumentDb.fromFile(new File("data/cri.docs.txt"), new File("data/cri.pr.txt"))
  //val vocabulary = Vocabulary.fromFile(new File("data/out.vocabulary.dat"), new File("data/out.index.dat"))
  //val documentDb = DocumentDb.fromFile(new File("data/out.docs.txt"), new File("data/out.pr.txt"))
  Console.println("#initialized")

  //val vector = QueryEngine.vector(vocabulary, documentDb)
  val bm25_075 = QueryEngine.bm25(vocabulary, documentDb, 1.0, 0.8)
  val bm25Pr_075 = QueryEngine.bm25Pr(vocabulary, documentDb, 1.0, 0.8)
  val bm25LogPr_075 = QueryEngine.bm25LogPr(vocabulary, documentDb, 1.0, 0.8)
  val bm25aonly = QueryEngine.bm25AOnly(vocabulary, documentDb, 1.0, 0.8)
  val bm25a = QueryEngine.bm25a(vocabulary, documentDb, 1.0, 0.8, 2.0)
  val bm25pra2 = QueryEngine.bm25pra(vocabulary, documentDb, 1.0, 0.8, 2.0)
  //val vbm25_05 = QueryEngine.vectorBm25(vocabulary, documentDb, 0.5, 1.0, 0.8)
  //val vbm25_up = QueryEngine.vectorBm25(vocabulary, documentDb, 0.5, 1.0, 0.8)
  //val vbm25_08 = QueryEngine.vectorBm25(vocabulary, documentDb, 0.8, 2.0, 0.8)
  //val vbm1 = QueryEngine.vectorBm25(vocabulary, documentDb, 0.5, 1.0, 0.8)
  //val vbm2 = QueryEngine.vectorBm25(vocabulary, documentDb, 0.9, 1.0, 0.8)

  val engines = List(bm25_075, bm25aonly, bm25a, bm25pra2)

  val r = 1000000
  val accumulators = 1000000

  var count = 1;
  var plotsOnPage = new StringBuffer
  var precisionAt10Mean = engines.toArray.map( _ => 0.0 )
  
  for (queryFile <- queryFiles) {
    val query = queryFile.getName()
    val relevants = relevantSet(queryFile)
    //Console.println(query)

    val values = new ListBuffer[Array[Tuple2[Double, Double]]]
    val names = ListBuffer[String]()
    
    var i = 0;
    for (engine <- engines) {
      
      val result = engine.findDocuments(query, r, accumulators)
      val actual = result.map(_.url)
      //Console.println(actual.size + " documentos recuperados")

      val series = precisionAtRecall(actual, relevants)
      val precisionAt10 = precisionAtRank(20, actual, relevants)
      precisionAt10Mean(i) += precisionAt10 / queryFiles.size

      names += engine.name
      values += series
      //for (v <- series) {
        //Console.printf("%.2f; %.2f\n", v._1, v._2)
      //}
      i += 1;
    }

    plotsOnPage.append(ChartHelper.rPlot(query, names.toList, values.toArray))
    if (count % 6 == 0) {
      val rpage = ChartHelper.rPage(("page" + (count / 6)), plotsOnPage.toString)
      Console.println(rpage)
      plotsOnPage = new StringBuffer
    }
    count += 1
    
  }

  if (plotsOnPage.length() > 0) {
    val rpage = ChartHelper.rPage(("page" + ((count / 6) + 1)), plotsOnPage.toString)
    Console.println(rpage)
  }

  Console.print("\n# Mean PR@10\n")
  for (precAt10 <- precisionAt10Mean) {
    Console.print("# " + precAt10 + "\n")
  }

  def precisionAtRecall(actual: Seq[String], relevants: Set[String]): Array[Tuple2[Double, Double]] = {
    var values = new ListBuffer[Tuple2[Double, Double]]

    val relevantsSeen = collection.mutable.Set[String]()

    var relevantsFound = 0
    var totalFound = 0
    var totalRelevants = relevants.size
    for (doc <- actual) {
      // Ignora duplicatas
      if (!relevantsSeen.contains(doc)) {
        totalFound += 1
        if (relevants.contains(doc)) {
          relevantsSeen += doc
          relevantsFound += 1
          val recall = 100.0 * (relevantsFound.toDouble / totalRelevants)
          val precision = 100.0 * (relevantsFound.toDouble / totalFound)
          values += Tuple2(recall, precision)
          //Console.printf("%.2f; %.2f\n", recall, precision)
        }

      }
    }
    if (relevantsFound < totalRelevants) {
      values += Tuple2(100.0, 0.0)
    }
    values.insert(0, Tuple2(0.0, values(0)._2))  
    
    values.toArray
  }

  def relevantSet(file: File): Set[String] = {
    Source.fromFile(file).getLines().filterNot(_.isEmpty()).toSet
  }

  def precisionAtRank(rank: Int, actual: Seq[String], relevants: Set[String]): Double = {

    val relevantsSeen = collection.mutable.Set[String]()

    var relevantsFound = 0
    var totalFound = 0
    for (doc <- actual) {
      // Ignora duplicatas
      if (!relevantsSeen.contains(doc)) {
        totalFound += 1
        if (relevants.contains(doc)) {
          relevantsSeen += doc
          relevantsFound += 1
        }
		if (totalFound == rank) {
			return 100.0 * (relevantsFound.toDouble / totalFound)
		}
      }
    }
    
    return 0.0
  }
}