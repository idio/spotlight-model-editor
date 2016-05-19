/**
 * Copyright 2014 Idio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author David Przybilla david.przybilla@idioplatform.com
 **/

package org.idio.dbpedia.spotlight.stores


import org.dbpedia.spotlight.db.memory.{ MemoryResourceStore, MemoryStore, MemoryCandidateMapStore }
import java.io.{ File, FileInputStream }
import Array.concat
import org.dbpedia.spotlight.model.SurfaceForm
import org.idio.dbpedia.spotlight.utils.ArrayUtils
import org.dbpedia.spotlight.db.similarity.PercentageOfContextVector

class CustomCandidateMapStore(var candidateMap: MemoryCandidateMapStore,
                              val pathtoFolder: String,
                              val resStore:MemoryResourceStore,
                              val countStore: CustomQuantiziedCountStore) extends QuantiziedMemoryStore {

  quantizedCountStore = countStore

  def this(pathtoFolder: String, resStore: MemoryResourceStore, countStore: CustomQuantiziedCountStore) {
    this(MemoryStore.loadCandidateMapStore(new FileInputStream(new File(pathtoFolder, "candmap.mem")), resStore, countStore.quantizedStore),
                                           pathtoFolder, resStore, countStore)
  }

  def this(candidateMap: MemoryCandidateMapStore, resStore: MemoryResourceStore, countStore: CustomQuantiziedCountStore) {
    this(candidateMap, "", resStore, countStore)
  }

  /*
  * Tries to get the candidate array for the given surfaceForm.
  * In case such candidate array does not exist it will create it.
  *
  * It looks if the given candidateID is inside the candidate array.
  * if it is not it will add it
  * */
  def addOrCreate(surfaceFormID: Int, candidateID: Int, candidateCounts: Int) {

    val quantiziedCandidateCounts:Short = getQuantiziedCounts(candidateCounts)

    // try to get it, create the candidate array in case it doesnt exist
    try {
      this.candidateMap.candidates(surfaceFormID)
    } catch {
      case e: Exception => {
        println("\tcreating candidate map array for " + surfaceFormID)

        val candidates: Array[Int] = Array[Int](candidateID)
        val counts: Array[Int] = Array[Int](candidateCounts)
        this.createCandidateMapForSurfaceForm(surfaceFormID, candidates, counts)

        println("\tcandidates")
        println("\tcandidates size:  %s".format(this.candidateMap.candidates.size))

        this.candidateMap.candidates(surfaceFormID).foreach { candidate =>
          println("\t" + candidate)
        }

        return true
      }
    }

    try {
      // checking if the surfaceForm actually has a candidates array already
      this.candidateMap.candidates(surfaceFormID).size
    } catch {
      case e: Exception => {
        // creating the candidate array in case the sf did not have one before

        println("\tcreating candidate map array for " + surfaceFormID)

        this.candidateMap.candidates(surfaceFormID) = Array[Int](candidateID)
        this.candidateMap.candidateCounts(surfaceFormID) = Array[Short](quantiziedCandidateCounts)

        println("\tcandidates")

        this.candidateMap.candidates(surfaceFormID).foreach { candidate =>
          println("\t" + candidate)
        }

      }
    }

    // if the candidate array exist, then check if the candidate Topic is inside
    if (!this.checkCandidateInSFCandidates(surfaceFormID, candidateID)) {
      println("\tadding the candidate(" + candidateID + ") to candidates of " + surfaceFormID)
      this.addNewCandidateToSF(surfaceFormID, candidateID, candidateCounts)
    }

  }

  /*
  * appends listOfCandidates to the end of the candidate
  * appends listOfCounts to the end of candidateCounts
  *
  * This is used when a surfaceForm is introduced to the model.
  * */
  def createCandidateMapForSurfaceForm(surfaceFormID: Int, listOfCandidates: Array[Int], listOfCounts: Array[Int]) {
    this.candidateMap.candidates = Array concat (this.candidateMap.candidates, Array(listOfCandidates))

    // transofrming the counts into quantized equivalents
    val listOfQuantizedCounts:Array[Short] = listOfCounts.map(getQuantiziedCounts(_))

    this.candidateMap.candidateCounts = Array concat (this.candidateMap.candidateCounts, Array(listOfQuantizedCounts))
  }

  def createCandidateMapForNewSurfaceForm(surfaceFormIds:List[Int]): Unit ={

    println("candidates before expansion :  %s".format( this.candidateMap.candidates.size))

    val  listOfCounts:Array[Int] =  surfaceFormIds.toList.map(i => 0).toArray
    // transofrming the counts into quantized equivalents
    val listOfQuantizedCounts:Array[Array[Short]] = listOfCounts.map(c => Array[Short]())
    val listOfCandidates:Array[Array[Int]]  = surfaceFormIds.toList.map(i => Array[Int]()).toArray

    println("expanding candidate arrays with new sfs...%s".format(surfaceFormIds.size))
    println("expanding listOfCandidates arrays with new sfs...%s".format(listOfCandidates.size))

    this.candidateMap.candidates = Array concat (this.candidateMap.candidates, listOfCandidates)
    this.candidateMap.candidateCounts = Array concat (this.candidateMap.candidateCounts, listOfQuantizedCounts)

    println("candidates after expansion :  %s".format( this.candidateMap.candidates.size))
  }

  /*
* returns the AVG candidate counts for a given SF
* This value is used when creating a new association between an SF and a Topic
* */
  def getAVGSupportForSF(surfaceFormID: Int): Int = {
    val candidateCounts = this.candidateMap.candidateCounts(surfaceFormID)
    if (candidateCounts.isInstanceOf[Array[Short]]) {
      return (candidateCounts.map(x => this.getCountFromQuantiziedValue(x)).sum  / candidateCounts.size.toDouble).toInt
    }
    return 0
  }

  /*
  * Checks if a candidateId is already in the candidate array of a surfaceForm.
  * */
  def checkCandidateInSFCandidates(surfaceFormID: Int, candidateID: Int): Boolean = {
    for (candidate: Int <- candidateMap.candidates(surfaceFormID)) {
      if (candidate == candidateID)
        return true
    }
    return false
  }

  def changePercentageOfContextVector(surfaceFormAnnotatedCount:Int, surfaceFormID: Int, dbpediaId: Int, surfaceFormCounts:Int, percentageOfVector: Double): Unit ={


    val indexOfCandidateInArray = this.candidateMap.candidates(surfaceFormID).indexWhere { case (x) => x == dbpediaId }
    val candidateCount = PercentageOfContextVector.candidateSupport(surfaceFormCounts, percentageOfVector)

    val maxCandidateCount = (surfaceFormAnnotatedCount * 0.9).toInt
    val newCandidateCount = Math.min(maxCandidateCount, candidateCount)

    val newQuantizedCount:Short = getQuantiziedCounts(newCandidateCount)

    println("\t updating candidate support from: " +  this.candidateMap.candidates(surfaceFormID)(indexOfCandidateInArray) + " to:" + newQuantizedCount)

    this.candidateMap.candidateCounts(surfaceFormID)(indexOfCandidateInArray) = newQuantizedCount
  }

  /*
  * Increments the candidates Counts for a given surfaceForm and candidate
  * */
  def updateCountsOfCandidate(surfaceFormID: Int, candidateID: Int, boostValue: Int) {
    // update the candidate count value
    println("updating candidate count value")
    val indexOfCandidateInArray = this.candidateMap.candidates(surfaceFormID).indexWhere { case (x) => x == candidateID }
    val newQuantizedCount:Short = getQuantiziedCounts(boostValue)
    this.candidateMap.candidates
    this.candidateMap.candidateCounts(surfaceFormID)(indexOfCandidateInArray) = newQuantizedCount
  }

  /*
  * Add a new topic candidate to the list of candidates of a SurfaceForm
  * */
  def addNewCandidateToSF(surfaceFormID: Int, candidateID: Int, candidateCounts: Int) {
    if (!this.checkCandidateInSFCandidates(surfaceFormID, candidateID)) {
      this.candidateMap.candidates(surfaceFormID) = this.candidateMap.candidates(surfaceFormID) :+ candidateID
      val quantizedCount: Short = getQuantiziedCounts(candidateCounts)
      this.candidateMap.candidateCounts(surfaceFormID) = this.candidateMap.candidateCounts(surfaceFormID) :+ quantizedCount
      return 1
    }
    return 0
  }

  /*
  * Remove association between an SF and a DbpediaURI
  * */
  def removeAssociation(surfaceFormID: Int, candidateID: Int) {
    val indexOfCandidateInArray = this.candidateMap.candidates(surfaceFormID).indexWhere { case (x) => x == candidateID }
    this.candidateMap.candidates(surfaceFormID) = ArrayUtils.dropIndex(this.candidateMap.candidates(surfaceFormID), indexOfCandidateInArray)
    this.candidateMap.candidateCounts(surfaceFormID) = ArrayUtils.dropIndex(this.candidateMap.candidateCounts(surfaceFormID), indexOfCandidateInArray)
  }

  /*
  * get all candidates associated to sourceSurfaceForm
  * and associates them also to destinationSurfaceForm
  * */
  def copyCandidates(sourceSurfaceForm: SurfaceForm, destinationSurfaceForm: SurfaceForm) {

    // get the candidates associated to the sourceSF
    var newDestinationCandidates = this.candidateMap.candidates(sourceSurfaceForm.id).clone()
    var newDestinationCandidatesCounts = this.candidateMap.candidateCounts(sourceSurfaceForm.id).clone()

    // add the candidates associated to the destinationSF but not to the sourceSF
    val setOfCandidatesTopics: collection.immutable.Set[Int] = collection.immutable.Set[Int](newDestinationCandidates: _*)
    val currentDestinationCandidates = this.candidateMap.candidates(destinationSurfaceForm.id).zip(this.candidateMap.candidateCounts(destinationSurfaceForm.id))

    currentDestinationCandidates.foreach {
      case (topicId, count) =>

        // if candidate is not already in the new candidate list then add it
        if (!setOfCandidatesTopics.contains(topicId)) {
          newDestinationCandidates = newDestinationCandidates :+ topicId
          newDestinationCandidatesCounts = newDestinationCandidatesCounts :+ count
        }

    }

    // update the destinationSF candidate arrays
    this.candidateMap.candidates(destinationSurfaceForm.id) = newDestinationCandidates
    this.candidateMap.candidateCounts(destinationSurfaceForm.id) = newDestinationCandidatesCounts

  }
}

