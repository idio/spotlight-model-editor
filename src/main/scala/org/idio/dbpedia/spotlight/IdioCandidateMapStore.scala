package org.idio.dbpedia.spotlight

/**
 * Created by dav009 on 23/12/2013.
 */

import org.dbpedia.spotlight.db.memory.{MemoryResourceStore, MemoryStore, MemoryCandidateMapStore}
import java.io.{File, FileInputStream}

class IdioCandidateMapStore(val pathtoFolder:String, val resStore:MemoryResourceStore){


  var candidateMap:MemoryCandidateMapStore = MemoryStore.loadCandidateMapStore(new FileInputStream(new File(pathtoFolder,"candmap.mem")), resStore)

  /*
  * Tries to get the candidate array for the given surfaceForm.
  * In case such candidate array does not exist it will create it.
  *
  * It looks if the given candidateID is inside the candidate array.
  * if it is not it will add it
  * */
  def addOrCreate(surfaceFormID:Int, candidateID:Int){
    // try to get it, create the candidate array in case it doesnt exist
    try{
      this.candidateMap.candidates(surfaceFormID)
    }catch{
      case e:Exception =>
         println("\tcreating candidate map array for "+surfaceFormID)
         var candidates:Array[Int] =Array(candidateID)
         var counts:Array[Int] = Array(1)
         this.createCandidateMapForSurfaceForm(surfaceFormID, candidates, counts)
        println("\tcandidates")
        for(candidate <- this.candidateMap.candidates(surfaceFormID)){
          println("\t"+candidate)
        }
         return true
    }

    // if the candidate array exist, then check if the candidate Topic is inside
    if (!this.checkCandidateInSFCandidates(surfaceFormID, candidateID)){
      println("\tadding the candidate("+candidateID+") to candidates of "+surfaceFormID)
      this.addNewCandidateToSF(surfaceFormID, candidateID, 1)
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

}
