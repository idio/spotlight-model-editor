package org.idio.dbpedia.spotlight

/**
 * Created by dav009 on 23/12/2013.
 */
import org.dbpedia.spotlight.model.OntologyType
import org.dbpedia.spotlight.db.memory.{MemoryStore, MemoryResourceStore}
import java.io.File

class IdioSpotlightModel(val pathToFolder:String){

  var idioDbpediaResourceStore:IdioDbpediaResourceStore = new IdioDbpediaResourceStore(pathToFolder)
  var idioCandidateMapStore:IdioCandidateMapStore = new IdioCandidateMapStore(pathToFolder, idioDbpediaResourceStore.resStore)
  var idioSurfaceFormStore:IdioSurfaceFormStore = new IdioSurfaceFormStore(pathToFolder)


 /*
 * Serializes the current model in the given folder
 * */
 def exportModels(pathToFolder:String){
   println("exporting models to.." + pathToFolder)
   MemoryStore.dump(this.idioDbpediaResourceStore.resStore, new File(pathToFolder,"res.mem"))
   MemoryStore.dump(this.idioCandidateMapStore.candidateMap, new File(pathToFolder,"candmap.mem"))
   MemoryStore.dump(this.idioSurfaceFormStore.sfStore, new File(pathToFolder,"sf.mem"))
   println("finished exporting models to.." + pathToFolder)
 }

  /*
  * Attach a surfaceform to a candidateTopic
  * if SurfaceForm does not exist it is created
  * if candidateTopic does not exist it is created
  * */
 def addNew(surfaceFormText:String, candidateURI:String, support:Int, types:Array[String]){

   // create or get the surfaceForm
   val surfaceFormID:Int = this.idioSurfaceFormStore.getAddSurfaceForm(surfaceFormText)

   // create or get the dbpedia Resource
   val dbpediaResourceID:Int = this.idioDbpediaResourceStore.getAddDbpediaResource(candidateURI, support, types)

   //update the candidate Store
   this.idioCandidateMapStore.addOrCreate(surfaceFormID, dbpediaResourceID)


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
     val indexOfCandidate = this.idioCandidateMapStore.candidateMap.candidates(surfaceForm.id).indexWhere{ case(x) => x==candidate }
     println("\t\t"+this.idioCandidateMapStore.candidateMap.candidateCounts(surfaceForm.id)(indexOfCandidate))
     println("\tannotated_count")
     println("\t\t"+surfaceForm.annotatedCount)
     println("\t")
     println("\t\t"+dbpediaResource.prior)
   }

   println("using candidate map")
   val candidates2 = this.idioCandidateMapStore.candidateMap.getCandidates(surfaceForm)
   for(candidate <- candidates2){
     println(candidate.resource.getFullUri)
     println("\tsupport")
     println("\t\t"+candidate.support)
     println("\tprior(this prior is: candidateCount/surfaceFormCounts(I suspect this is the one used!)")
     println("\t\t"+candidate.prior)

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
