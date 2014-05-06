package org.idio.dbpedia.spotlight.stores

/**
 * Created by dav009 on 23/12/2013.
 */

import org.dbpedia.spotlight.db.memory.{MemoryResourceStore, MemoryStore, MemoryCandidateMapStore}
import java.io.{File, FileInputStream}
import Array.concat
import org.dbpedia.spotlight.model.SurfaceForm

class IdioCandidateMapStore(var candidateMap:MemoryCandidateMapStore, val pathtoFolder:String, val resStore:MemoryResourceStore){



  def this(pathtoFolder:String, resStore:MemoryResourceStore){
    this(MemoryStore.loadCandidateMapStore(new FileInputStream(new File(pathtoFolder,"candmap.mem")), resStore), pathtoFolder, resStore)
  }

  def this(candidateMap:MemoryCandidateMapStore, resStore:MemoryResourceStore){
    this(candidateMap, "", resStore)
  }

  /*
  * Tries to get the candidate array for the given surfaceForm.
  * In case such candidate array does not exist it will create it.
  *
  * It looks if the given candidateID is inside the candidate array.
  * if it is not it will add it
  * */
  def addOrCreate(surfaceFormID:Int, candidateID:Int, candidateCounts:Int){
    // try to get it, create the candidate array in case it doesnt exist
    try{
      this.candidateMap.candidates(surfaceFormID)
    }catch{
      case e:Exception =>{
         println("\tcreating candidate map array for "+surfaceFormID)

         val candidates:Array[Int] = Array(candidateID)
         val counts:Array[Int] = Array(candidateCounts)
         this.createCandidateMapForSurfaceForm(surfaceFormID, candidates, counts)

         println("\tcandidates")

        this.candidateMap.candidates(surfaceFormID).foreach{ candidate =>
          println("\t"+candidate)
        }

         return true
      }
    }

    try{
      this.candidateMap.candidates(surfaceFormID).size
    }catch{
      case e:Exception =>{

        println("\tcreating candidate map array for "+surfaceFormID)

        this.candidateMap.candidates(surfaceFormID) = Array[Int](candidateID)
        this.candidateMap.candidateCounts(surfaceFormID) = Array[Int](candidateCounts)

        println("\tcandidates")

        this.candidateMap.candidates(surfaceFormID).foreach{ candidate =>
          println("\t"+candidate)
        }

      }
    }


    // if the candidate array exist, then check if the candidate Topic is inside
    if (!this.checkCandidateInSFCandidates(surfaceFormID, candidateID)){
      println("\tadding the candidate("+candidateID+") to candidates of "+surfaceFormID)
      this.addNewCandidateToSF(surfaceFormID, candidateID, candidateCounts)
    }

  }

  /*
  * appends listOfCandidates to the end of the candidate
  * appends listOfCounts to the end of candidateCounts
  *
  * This is used when a surfaceForm is introduced to the model.
  * */
  def createCandidateMapForSurfaceForm(surfaceFormID:Int, listOfCandidates:Array[Int], listOfCounts:Array[Int]){
    this.candidateMap.candidates = Array concat(this.candidateMap.candidates, Array(listOfCandidates))
    this.candidateMap.candidateCounts =  Array concat(this.candidateMap.candidateCounts, Array(listOfCounts))
  }

  /*
* returns the AVG candidate counts for a given SF
* This value is used when creating a new association between a SF and a Topic
* */
  def getAVGSupportForSF(surfaceFormID:Int):Int = {
    val candidateCounts = this.candidateMap.candidateCounts(surfaceFormID)
    if  (candidateCounts.isInstanceOf[Array[Int]]){
      return (candidateCounts.sum  / candidateCounts.size.toDouble).toInt
    }
    return 0
  }

  /*
  * Checks if a candidateId is already in the candidate array of a surfaceForm.
  * */
  def checkCandidateInSFCandidates(surfaceFormID:Int,candidateID:Int):Boolean = {
    for (candidate:Int<-candidateMap.candidates(surfaceFormID)){
        if (candidate == candidateID)
            return true
    }
    return false
  }

  /*
  * increments the candidates Counts for a given surfaceForm and candidate
  * */
  def updateCountsOfCandidate(surfaceFormID:Int, candidateID:Int, boostValue:Int){
    // update the candidate count value
    println("updating candidate count value")
    val indexOfCandidateInArray = this.candidateMap.candidates(surfaceFormID).indexWhere{ case(x) => x==candidateID }
    this.candidateMap.candidateCounts(surfaceFormID)(indexOfCandidateInArray) += boostValue
  }

  /*
  * Add a new topic candidate to the list of candidates of a SurfaceForm
  * */
  def addNewCandidateToSF(surfaceFormID:Int, candidateID:Int, candidateCounts:Int){
      if (!this.checkCandidateInSFCandidates(surfaceFormID, candidateID)){
        this.candidateMap.candidates(surfaceFormID) = this.candidateMap.candidates(surfaceFormID) :+ candidateID
        this.candidateMap.candidateCounts(surfaceFormID) = this.candidateMap.candidateCounts(surfaceFormID) :+ candidateCounts
        return 1
      }
      return 0
  }

  /*
  * Remove association between a SF and a DbpediaURI
  * */
  def removeAssociation(surfaceFormID:Int, candidateID:Int){
      val indexOfCandidateInArray = this.candidateMap.candidates(surfaceFormID).indexWhere{ case(x) => x==candidateID }
      this.candidateMap.candidates(surfaceFormID) = this.dropIndex(this.candidateMap.candidates(surfaceFormID), indexOfCandidateInArray)
      this.candidateMap.candidateCounts(surfaceFormID) =this.dropIndex(this.candidateMap.candidateCounts(surfaceFormID), indexOfCandidateInArray)
  }

  /*
  * get all candidates associated to sourceSurfaceForm
  * and associates them also to destinationSurfaceForm
  * */
  def copyCandidates(sourceSurfaceForm:SurfaceForm, destinationSurfaceForm:SurfaceForm){

    // get the candidates associated to the sourceSF
    var newDestinationCandidates = this.candidateMap.candidates(sourceSurfaceForm.id).clone()
    var newDestinationCandidatesCounts = this.candidateMap.candidateCounts(sourceSurfaceForm.id).clone()

    // add the candidates associated to the destinationSF but not to the sourceSF
    val setOfCandidatesTopics:collection.immutable.Set[Int] = collection.immutable.Set[Int](newDestinationCandidates:_*)
    val currentDestinationCandidates =  this.candidateMap.candidates(destinationSurfaceForm.id).zip(this.candidateMap.candidateCounts(destinationSurfaceForm.id))


    currentDestinationCandidates.foreach{ case (topicId, count)  =>

      // if candidate is not already in the new candidate list then add it
      if (!setOfCandidatesTopics.contains(topicId)){
        newDestinationCandidates = newDestinationCandidates :+ topicId
        newDestinationCandidatesCounts = newDestinationCandidatesCounts :+ count
      }

    }

    // update the destinationSF candidate arrays
    this.candidateMap.candidates(destinationSurfaceForm.id) = newDestinationCandidates
    this.candidateMap.candidateCounts(destinationSurfaceForm.id) = newDestinationCandidatesCounts

  }

  /**
   *  Drops the 'i'th element of a list
  */
  def dropIndex(xs:Array[Int], n:Int):Array[Int]={
    return concat(xs.slice(0, n), xs.slice(n+1, xs.length))
  }
}
