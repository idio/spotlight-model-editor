package org.idio.dbpedia.spotlight

/**
 * Created by dav009 on 23/12/2013.
 */
import org.dbpedia.spotlight.model.OntologyType
import org.dbpedia.spotlight.db.memory.{MemoryStore, MemoryResourceStore}
import java.io.File
import java.io.{FileReader, FileNotFoundException, IOException, FileInputStream}
import java.util.{Properties}
import collection.mutable.HashMap

class IdioSpotlightModel(val pathToFolder:String){

  // Load the properties file
  val properties:Properties = new Properties()
  val propertyFolder = new File(pathToFolder).getParent()
  properties.load(new FileInputStream(new File(propertyFolder, "model.properties")))

  // load the Stores
  var idioDbpediaResourceStore:IdioDbpediaResourceStore = new IdioDbpediaResourceStore(pathToFolder)
  var idioCandidateMapStore:IdioCandidateMapStore = new IdioCandidateMapStore(pathToFolder, idioDbpediaResourceStore.resStore)
  var idioSurfaceFormStore:IdioSurfaceFormStore = new IdioSurfaceFormStore(pathToFolder)
  var idioTokenTypeStore:IdioTokenResourceStore = new IdioTokenResourceStore(pathToFolder,  properties.getProperty("stemmer"))
  var idioContextStore:IdioContextStore = null
  try{
    this.idioContextStore =  new IdioContextStore(pathToFolder, idioTokenTypeStore.tokenStore)
  }catch{
    case ex: FileNotFoundException =>{
        println("...no context store found")
        this.idioContextStore = null
      }
   }


  /*
  * Serializes the current model in the given folder
  * */
  def exportModels(pathToFolder:String){
    println("exporting models to.." + pathToFolder)
    MemoryStore.dump(this.idioDbpediaResourceStore.resStore, new File(pathToFolder,"res.mem"))
    MemoryStore.dump(this.idioCandidateMapStore.candidateMap, new File(pathToFolder,"candmap.mem"))
    MemoryStore.dump(this.idioSurfaceFormStore.sfStore, new File(pathToFolder,"sf.mem"))
    MemoryStore.dump(this.idioTokenTypeStore.tokenStore, new File(pathToFolder,"tokens.mem") )

    try{
      MemoryStore.dump(this.idioContextStore.contextStore, new File(pathToFolder,"context.mem"))
    }catch{
      case ex: Exception =>{
        println("\t Context Store not exported")
      }
    }

    println("finished exporting models to.." + pathToFolder)

  }

   /*
  * Attach a surfaceform to a candidateTopic
  * if SurfaceForm does not exist it is created
  * if candidateTopic does not exist it is created
  * */
  def addNew(surfaceFormText:String, candidateURI:String, types:Array[String], contextWords:Array[String], contextCounts:Array[Int]){

    // create or get the surfaceForm
    val surfaceFormID:Int = this.idioSurfaceFormStore.getAddSurfaceForm(surfaceFormText)
    var defaultSupportForDbpediaResource:Int = 11

    // calculate the default support value based on the current support for the candidates for the given SF
    try{
      val candidates:Array[Int] = this.idioCandidateMapStore.candidateMap.candidates(surfaceFormID)
      var avgSupport:Double = 0.0
      for(candidate <- candidates){
        val dbpediaResource = this.idioDbpediaResourceStore.resStore.getResource(candidate)
        avgSupport = avgSupport + dbpediaResource.support
      }
      defaultSupportForDbpediaResource = (avgSupport/candidates.length).toInt
    }catch{
      case e:Exception => println("\tusing default support for.."+candidateURI)
    }


    // create or get the dbpedia Resource
    val dbpediaResourceID:Int = this.idioDbpediaResourceStore.getAddDbpediaResource(candidateURI, defaultSupportForDbpediaResource, types)

    //update the candidate Store
    this.idioCandidateMapStore.addOrCreate(surfaceFormID, dbpediaResourceID)

    //update the context Store
    println("\trying to update context for: "+ candidateURI)
    try{


      this.idioContextStore.createDefaultContextStore(dbpediaResourceID)
      val contextTokenCountMap:HashMap[String,Int] = this.idioTokenTypeStore.getContextTokens(contextWords, contextCounts)

      println("\tadding new context tokens to the context array")
      // Add the stems to the token Store and to the contextStore
      for(token<-contextTokenCountMap.keySet){

         val tokenID:Int = this.idioTokenTypeStore.getOrCreateToken(token)
         val tokenCount:Int = contextTokenCountMap.get(token).get
         this.idioContextStore.addContext(dbpediaResourceID,tokenID, tokenCount )

         println("\t\tadded token to context array - "+ token)
      }

    }catch{
      case ex: Exception =>{
        println("\tNot Context Store found....")
        println("\tSkipping Context Tokens....")
      }
    }


  }

  /**
   * Increments the counts of a surfaceForm and a candidate Topic.
   * This presupposes the existence of Both
   */
  def boostValue(surfaceFormText:String, candidateURI:String, boostValue:Int){

    val surfaceFormID:Int = this.idioSurfaceFormStore.sfStore.getSurfaceForm(surfaceFormText).id
    val candidateID:Int = this.idioDbpediaResourceStore.resStore.getResourceByName(candidateURI).id

    //update the annotated Count
    this.idioSurfaceFormStore.sfStore.annotatedCountForID(surfaceFormID) += boostValue

    // update the candidate count value
    println("updating candidate count value")
    this.idioCandidateMapStore.updateCountsOfCandidate(surfaceFormID, candidateID, boostValue)

    //updating the support in the resourceStore
    println("updating support in resource store for..."+this.idioDbpediaResourceStore.resStore.uriForID(candidateID))
    this.idioDbpediaResourceStore.resStore.totalSupport += boostValue
    this.idioDbpediaResourceStore.resStore.supportForID(candidateID) += boostValue

  }

  /*
  * Returns true if the dbpedia topic with the given URI exists in the resource Store
  * */
  def searchForDBpediaResource(candidateURI:String):Boolean ={
    try{
      this.idioDbpediaResourceStore.resStore.getResourceByName(candidateURI)
      return true
    }catch{
      case e:Exception => return false
    }
  }

  /*
  * Add a new Context Token for a dbpedia URI.
  * */
  def addContextToken(dbpediaResourceURI:String, token:String, count:Int){
    val dbpediaResourceId:Int = this.idioDbpediaResourceStore.resStore.getResourceByName(dbpediaResourceURI).id
    val tokenId:Int = this.idioTokenTypeStore.getOrCreateToken(token)
    this.idioContextStore.addContext(dbpediaResourceId, tokenId, count)
  }

  /*
  * Prints the context of a DbpediaResoruceURI
  * */
  def prettyPrintContext(dbpediaResourceURI:String){
    val dbpediaResourceID:Int =  this.idioDbpediaResourceStore.resStore.getResourceByName(dbpediaResourceURI).id
    var tokens:Array[Int] = this.idioContextStore.contextStore.tokens(dbpediaResourceID)
    var counts:Array[Int] = this.idioContextStore.contextStore.counts(dbpediaResourceID)
    println("Contexts for "+ dbpediaResourceURI+" Id:"+dbpediaResourceID)
    for( i <- 0 to tokens.size-1){
      this.idioTokenTypeStore.tokenStore.idFromToken
      println("\t"+this.idioTokenTypeStore.tokenStore.getTokenTypeByID(tokens(i))+"--"+ counts(i))
    }
  }

  /*
  * Prints the statistics for a surfaceForm and its candidates
  * */
  def getStatsForSurfaceForm(surfaceFormText:String){
    val surfaceForm = this.idioSurfaceFormStore.sfStore.getSurfaceForm(surfaceFormText)

    val candidates:Array[Int] = this.idioCandidateMapStore.candidateMap.candidates(surfaceForm.id)

    println("surface form id:"+surfaceForm.id)
    // for(candidate <- candidates){
    //   println("candidates:"+candidate)
    //}
    for(candidate <- candidates){

      println("---------------"+candidate+"---------------------")
      val dbpediaResource = this.idioDbpediaResourceStore.resStore.getResource(candidate)
      println(dbpediaResource.getFullUri+"_"+dbpediaResource.uri)
      println("\tid:"+candidate)
      println("\tsupport")
      println("\t\t"+dbpediaResource.support)
      println("\tannotated_count")
      println("\t\t"+surfaceForm.annotatedCount)
      println("\tprior")
      println("\t\t"+dbpediaResource.prior)
    }
  }

  /*
  * Prints the first 40 surface forms and their respective candidates
  */
  def showSomeSurfaceForms(){
    val someSurfaceForms = this.idioSurfaceFormStore.sfStore.iterateSurfaceForms.slice(0,40)
    for (surfaceForm <- someSurfaceForms){
      println(surfaceForm.name+"-"+surfaceForm.id)
      for(candidate <- this.idioCandidateMapStore.candidateMap.getCandidates(surfaceForm)){
        println("\t"+candidate.resource.getFullUri+"\t"+candidate.resource.uri)

        val dbpediaTypes:List[OntologyType] = candidate.resource.types

        for( dbpediaType:OntologyType<-dbpediaTypes){
          println("\t\t"+dbpediaType.typeID)
        }

      }

    }
  }



}