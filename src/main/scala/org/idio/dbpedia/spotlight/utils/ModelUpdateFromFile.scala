package org.idio.dbpedia.spotlight.utils

import org.idio.dbpedia.spotlight.IdioSpotlightModel

/**
 * Created by dav009 on 03/01/2014.
 */
class ModelUpdateFromFile(pathToModelFolder:String, pathToFile:String){

  var idioSpotlightModel:IdioSpotlightModel = new IdioSpotlightModel(pathToModelFolder)


  /*
  * Parses an input line.
  * Returns the SurfaceForm, DbpediaID, Types, ContextWords, ContextCounts
  * */
  def parseLine(line:String):(String, String, Array[String], Array[String], Array[Int]) = {
    val splittedLine = line.trim.split("\t")
    var surfaceForm = splittedLine(0)
    var dbpediaId = splittedLine(1)
    var types = splittedLine(2).split('|')
    var contextWords = splittedLine(3)
    var contextStringCounts = splittedLine(4).split('|')
    var contextWordsArray = contextWords.split('|')

    var contextCounts = new Array[Int](contextStringCounts.length)

    // Cast Context Counts to Integers
    for (counts<-contextStringCounts.zipWithIndex){
      val index = counts._2
      val countValue = counts._2
      contextCounts(index) = countValue.toInt
    }

    (surfaceForm, dbpediaId, types, contextWordsArray, contextCounts)
  }

  /*
  * Reads
  * */
  def loadNewEntriesFromFile(){
    val source = scala.io.Source.fromFile(this.pathToFile)
    val lines = source.bufferedReader()
    var line = lines.readLine()
    while (line!=null){

      val (surfaceForm, dbpediaId, types, contextWordsArray, contextCounts) = parseLine(line)

      println("SF: "+ surfaceForm)
      println("Topic: "+ dbpediaId)
      println("Types: "+ types.mkString(" "))
      println("Context: "+ contextWordsArray.mkString(" "))
      idioSpotlightModel.addNew(surfaceForm,dbpediaId, types, contextWordsArray, contextCounts )
      println("----------------------------")
      line = lines.readLine()
    }
    source.close()
  }


}
