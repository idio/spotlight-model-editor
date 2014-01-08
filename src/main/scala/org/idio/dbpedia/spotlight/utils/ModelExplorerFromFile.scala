package org.idio.dbpedia.spotlight.utils

import org.idio.dbpedia.spotlight.IdioSpotlightModel

/**
 * Created by dav009 on 03/01/2014.
 */
class ModelExplorerFromFile(pathToModelFolder:String, pathToFile:String){

  /*
  * Parses an input line.
  * Returns the SurfaceForm, DbpediaID, Types, ContextWords, ContextCounts
  * */
  def parseLine(line:String):(Array[String], String) = {
    val splittedLine = line.trim.split("\t")
    var dbpediaUri = splittedLine(0)
    var surfaceForms = splittedLine(1).split('|')


    (surfaceForms, dbpediaUri)
  }

  /*
  * loads everything using the entries in the file.
  * and exports a new model.
  * if there is no context.mem it will load just the SF, and Dbpedia Resources.
  * */
  def checkEntitiesInFile(){
    var idioSpotlightModel:IdioSpotlightModel = new IdioSpotlightModel(this.pathToModelFolder)
    val source = scala.io.Source.fromFile(this.pathToFile)
    val lines = source.bufferedReader()
    var line = lines.readLine()

    var countsOfFoundTopics = 0
    var countsOfFoundSF = 0
    var countsOfLinkedSFTopic = 0
    var totalSF = 0
    var totalTopics = 0
    var totalSFAndTopics = 0



    while (line!=null){

      val (surfaceForms, dbpediaUri) = parseLine(line)

      var dbpediaId = -1
      var isDbpediaResourceinModel = idioSpotlightModel.searchForDBpediaResource(dbpediaUri)

      totalTopics = totalTopics + 1

      if(isDbpediaResourceinModel){
        dbpediaId = idioSpotlightModel.idioDbpediaResourceStore.resStore.idFromURI.get(dbpediaUri)
        countsOfFoundTopics = countsOfFoundTopics + 1
      }


      for(surfaceForm<-surfaceForms){
            totalSF  = totalSF + 1
            var surfaceId = -1
            val normalizedSF = idioSpotlightModel.idioSurfaceFormStore.sfStore.normalize(surfaceForm)
            var isSFinModel = idioSpotlightModel.idioSurfaceFormStore.sfStore.idForString.containsKey(surfaceForm) |  idioSpotlightModel.idioSurfaceFormStore.sfStore.idForString.containsKey(normalizedSF)

            var areSFandResourceLinked = false

            if (isSFinModel){
              countsOfFoundSF = countsOfFoundSF + 1
              try{
                surfaceId = idioSpotlightModel.idioSurfaceFormStore.sfStore.idForString.get(surfaceForm)
              }catch{

                case ex:Exception =>{
                  println("")
                  println("used normalized SF")
                  println("normalized:"+ normalizedSF)
                  println("")
                  surfaceId = idioSpotlightModel.idioSurfaceFormStore.sfStore.idForString.get(normalizedSF)
                }
              }

            }

            try{
              areSFandResourceLinked = idioSpotlightModel.idioCandidateMapStore.checkCandidateInSFCandidates(surfaceId, dbpediaId)
            } catch{
              case ex:Exception=>{}
            }

            if(areSFandResourceLinked){
              countsOfLinkedSFTopic = countsOfLinkedSFTopic + 1
            }

            println("----------------------------")
            println("SF: "+ surfaceForm)
            println("\t in model?\t\t"+ isSFinModel)
            println("Topic: "+ dbpediaUri)
            println("\t in model?\t\t"+isDbpediaResourceinModel)
            println("is SF connected to the Topic?")
            println("\t"+areSFandResourceLinked )
            println("----------------------------")
      }
      line = lines.readLine()
    }
    println("totals")
    println("TOPICS")
    println("\t# of topics: "+totalTopics)
    println("\t# of found topics: "+countsOfFoundTopics)
    println("SF")
    println("\t# of SF: "+totalSF)
    println("\t# of found SF: "+countsOfFoundSF)
    println("topics linked to SF: "+countsOfLinkedSFTopic)
    println("expected number of topics and SF links: "+totalSF)
    source.close()
  }


}
