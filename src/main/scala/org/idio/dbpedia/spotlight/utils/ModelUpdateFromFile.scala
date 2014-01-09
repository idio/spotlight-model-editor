package org.idio.dbpedia.spotlight.utils

import org.idio.dbpedia.spotlight.IdioSpotlightModel

/**
 * Created by dav009 on 03/01/2014.
 */
class ModelUpdateFromFile(pathToModelFolder:String, pathToFile:String){

  /*
  * Parses an input line.
  * Returns the SurfaceForm, DbpediaID, Types, ContextWords, ContextCounts
  * */
  def parseLine(line:String):(Array[String], String, Array[String], Array[String], Array[Int]) = {
    val splittedLine = line.trim.split("\t")
    var dbpediaURI = splittedLine(0)
    var surfaceForms = splittedLine(1).split('|')
    //var types = splittedLine(2).split('|')
    var types = new Array[String](0)
    var contextWords = splittedLine(2)
    var contextStringCounts = splittedLine(3).split('|')
    var contextWordsArray = contextWords.split('|')

    var contextCounts = new Array[Int](contextStringCounts.length)

    // Cast Context Counts to Integers
    for (counts<-contextStringCounts.zipWithIndex){
      val index = counts._2
      val countValue = counts._1
      contextCounts(index) = countValue.toInt
    }

    (surfaceForms, dbpediaURI, types, contextWordsArray, contextCounts)
  }

  /*
  * loads everything using the entries in the file.
  * and exports a new model.
  * if there is no context.mem it will load just the SF, and Dbpedia Resources.
  * */
  def loadNewEntriesFromFile(){
    var idioSpotlightModel:IdioSpotlightModel = new IdioSpotlightModel(this.pathToModelFolder)
    val source = scala.io.Source.fromFile(this.pathToFile)
    val lines = source.bufferedReader()
    var line = lines.readLine()

    val contextFileWriter = new java.io.PrintWriter(this.pathToFile+"_just_context")

    while (line!=null){

      val (surfaceForms, dbpediaURI, types, contextWordsArray, contextCounts) = parseLine(line)

      for(surfaceForm<-surfaceForms){
        println("SF: "+ surfaceForm)
        println("Topic: "+ dbpediaURI)
        println("Types: "+ types.mkString(" "))
        println("Context: "+ contextWordsArray.mkString(" "))


        val (surfaceFormId, dbpediaResourceId) = idioSpotlightModel.addNew(surfaceForm,dbpediaURI, types, contextWordsArray, contextCounts )

        contextFileWriter.println(dbpediaResourceId+"\t"+contextWordsArray.mkString("|")+"\t"+contextCounts.mkString("|"))

        println("----------------------------")
      }
      line = lines.readLine()
    }
    source.close()
    contextFileWriter.close()
    println("serializing the new model.....")
    idioSpotlightModel.exportModels(this.pathToModelFolder)
    println("finished serializing the new model.....")
  }


}
