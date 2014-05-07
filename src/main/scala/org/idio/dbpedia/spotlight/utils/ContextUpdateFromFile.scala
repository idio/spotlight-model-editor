package org.idio.dbpedia.spotlight.utils

import org.idio.dbpedia.spotlight.stores.{ IdioTokenResourceStore, IdioContextStore }
import org.idio.dbpedia.spotlight.IdioSpotlightModel
import java.util.Properties
import java.io.{ FileInputStream, File }

/**
 * Adds ContextWords to DbpediaResources
 * The format of each life of the file is:
 * dbpediaURI tab contextW1|contextW2.. tab contextW1Count|contextW2Count..
 * Created by dav009 on 03/01/2014.
 */
class ContextUpdateFromFile(pathToModelFolder: String, pathToFile: String) {

  /*
  * Parses an input line.
  * Returns the SurfaceForm, dbpediaURI, Types, ContextWords, ContextCounts
  * */
  def parseLine(line: String): (Int, Array[String], Array[Int]) = {
    val splittedLine = line.trim.split("\t")
    var dbpediaId = splittedLine(0).toInt

    var contextWordsArray = new Array[String](0)
    var contextCounts = new Array[Int](0)

    if (splittedLine.size > 2) {
      var contextWords = splittedLine(1)
      var contextStringCounts = splittedLine(2).split('|')
      contextWordsArray = contextWords.split('|')
      contextCounts = new Array[Int](contextStringCounts.length)

      // Cast Context Counts to Integers
      for (counts <- contextStringCounts.zipWithIndex) {
        val index = counts._2
        val countValue = counts._1
        contextCounts(index) = countValue.toInt
      }
    }

    (dbpediaId, contextWordsArray, contextCounts)
  }

  /*
  * Links DbpediaURIS with Context words and exports the model.
  * */
  def loadContextWords() {
    var idioSpotlightModel: IdioSpotlightModel = new IdioSpotlightModel(this.pathToModelFolder)
    val source = scala.io.Source.fromFile(this.pathToFile)
    val lines = source.bufferedReader()
    var line = lines.readLine()
    while (line != null) {

      val (dbpediaId, contextWordsArray, contextCounts) = parseLine(line)

      println("dbpediaURI: " + dbpediaId)
      println("Context: " + contextWordsArray.mkString(" "))
      println("context Counts: " + contextCounts.mkString(" "))

      idioSpotlightModel.addNewContextWords(dbpediaId, contextWordsArray, contextCounts)

      line = lines.readLine()
    }
    source.close()
    println("serializing the new model.....")
    idioSpotlightModel.exportModels(this.pathToModelFolder)
    println("finished serializing the new model.....")
  }

}
