package org.idio.dbpedia.spotlight.utils

import org.idio.dbpedia.spotlight.IdioSpotlightModel
import scala.collection.mutable.HashSet
/**
 * Allows to update a Model (Sf, DbpediaResources, ContextWords) from a file
 * The format of each line of the file is:
 * dbpediaURI tab surfaceForm1|surfaceForm2... tab contextW1|contextW2.. tab contextW1Count|contextW2Count..
 * Created by dav009 on 03/01/2014.
 */
class ModelUpdateFromFile(pathToModelFolder:String, pathToFile:String){

  /*
  * Parses an input line.
  * Returns the SurfaceForm, DbpediaID, Types, ContextWords, ContextCounts
  * */
  def parseLine(line:String):(Array[String], String, Array[String], Array[String], Array[Int]) = {
    println(line)
    val splittedLine = line.trim.split("\t")
    var dbpediaURI = splittedLine(0)
    var surfaceForms = splittedLine(1).split('|')
    //var types = splittedLine(2).split('|')
    var types = new Array[String](0)

    var contextWordsArray = new Array[String](0)
    var contextCounts = new Array[Int](0)

    if (splittedLine.size>2){
        var contextWords = splittedLine(2)
        var contextStringCounts = splittedLine(3).split('|')
        contextWordsArray = contextWords.split('|')
        contextCounts = new Array[Int](contextStringCounts.length)

        // Cast Context Counts to Integers
        for (counts<-contextStringCounts.zipWithIndex){
          val index = counts._2
          val countValue = counts._1
          contextCounts(index) = countValue.toInt
        }
    }

    (surfaceForms, dbpediaURI, types, contextWordsArray, contextCounts)
  }

  /*
  * Parses the input File and outputs a set of SF and dbpedia URIs
  * */
  def parseFile(pathToFile:String):(HashSet[String], HashSet[String])={
    val setOfSurfaceForms:HashSet[String] = new HashSet[String]()
    val setOfDbpediaURIS:HashSet[String] = new HashSet[String]()

    val source = scala.io.Source.fromFile(this.pathToFile)
    val lines = source.getLines()

    for (line<-lines){
      val (surfaceForms, dbpediaURI, types, contextWordsArray, contextCounts) = this.parseLine(line)
      //get all SurfaceForms into a Set
      for(surfaceForm<-surfaceForms){
        setOfSurfaceForms.add(surfaceForm)
      }
      //get all DbpediaUris into a Set
      setOfDbpediaURIS.add(dbpediaURI)
    }

    return (setOfSurfaceForms, setOfDbpediaURIS)
  }

  /*
  * loads everything using the entries in the file.
  * and exports a new model.
  * if there is no context.mem it will load just the SF, and Dbpedia Resources.
  * */
  def loadNewEntriesFromFile(){
    val (setOfSurfaceForms, setOfDbpediaURIS) = this.parseFile(this.pathToFile)

    var idioSpotlightModel:IdioSpotlightModel = new IdioSpotlightModel(this.pathToModelFolder)

    println("adding all SF..."+ setOfSurfaceForms.size)
    idioSpotlightModel.addListOfSurfaceForms(setOfSurfaceForms)

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
