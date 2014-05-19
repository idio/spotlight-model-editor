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


import org.dbpedia.spotlight.db.memory.{ MemoryStore, MemorySurfaceFormStore }
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import java.io.{ File, FileInputStream }
import scala.collection.immutable

class CustomSurfaceFormStore(val pathtoFolder: String, val countStore: CustomQuantiziedCountStore) extends QuantiziedMemoryStore{

  quantizedCountStore = countStore
  val sfMemFile = new FileInputStream(new File(pathtoFolder, "sf.mem"))
  var sfStore: MemorySurfaceFormStore = MemoryStore.loadSurfaceFormStore(sfMemFile, quantizedCountStore.quantizedStore)

  /*
  *  Given a lowercase surface form it returns the list of string candidate surface forms
  *  returns the counts of a sf, and the list of candidates surface forms
  * */
  def findInLowerCaseSurfaceForm(surfaceText: String): (Option[Int], Option[Array[String]]) = {
    if (sfStore.lowercaseMap.containsKey(surfaceText)){
        val candidates = sfStore.lowercaseMap.get(surfaceText)
        val counts = candidates(0)
       (Some(counts), Some(candidates.slice(1,candidates.size).map(sfStore.stringForID)))
    }else{
      (None, None)
   }
  }

  /*
  * Updates the internal arrays for a new SurfaceForm
  * */
  private def addSF(surfaceText: String) {
    println("\t adding a new surface form..." + surfaceText)
    this.sfStore.stringForID = this.sfStore.stringForID :+ surfaceText

    val defaultQuantiziedValue: Short = getQuantiziedCounts(1)

    // the counts for the new surface form is the avg of the counts for the other surface forms
    this.sfStore.annotatedCountForID = this.sfStore.annotatedCountForID :+ defaultQuantiziedValue
    this.sfStore.totalCountForID = this.sfStore.totalCountForID :+ defaultQuantiziedValue
  }

  /*
* Adds a list of surfaceforms directly to the low level maps.
* Assumes that the surfaceForms in the list does not exist already in the low level maps
* */
  private def addListOfNewSurfaceForms(listOfNewSurfaceForms: List[String]): List[Int] = {

    val indexFirstNewSf = this.sfStore.stringForID.length
    val indexLastNewSf = (this.sfStore.stringForID.length + listOfNewSurfaceForms.size)

    this.sfStore.stringForID = this.sfStore.stringForID ++ listOfNewSurfaceForms

    val defaultQuantiziedValue: Short = getQuantiziedCounts(1)
    val defaultValueList = List.fill(listOfNewSurfaceForms.size)(defaultQuantiziedValue)

    this.sfStore.annotatedCountForID = this.sfStore.annotatedCountForID ++ defaultValueList
    this.sfStore.totalCountForID = this.sfStore.totalCountForID ++ defaultValueList

    return List.range(indexFirstNewSf, indexLastNewSf)
  }

  /*
  * Adds a new surfaceForm to the surfaceFormStore.
  * It does NOT check whether it exists
  * returns the Id of the new SF
  * */
  def addSurfaceForm(surfaceText: String): Int = {
    //adding the SF to the internal arrays
    this.addSF(surfaceText)

    // update internal indexes
    println("\t updating the SF index")
    this.sfStore.createReverseLookup()
    var surfaceForm = this.sfStore.getSurfaceForm(surfaceText)

    return surfaceForm.id
  }

  /*
 * Adds a set of surfaceForms to the surfaceFormStore in a singleBatch,
 * It only adds sf which are not already in the store.
 * returns a list with the Ids of the added SurfaceForms
 * */
  def addSetOfSF(setOfSurfaceForms: scala.collection.Set[String]): List[Int] = {

    // Searching SF in the main Store
    val searchSurfaceFormResult = setOfSurfaceForms.toSeq.par.map(surfaceForm =>
      try {
        val sf = this.sfStore.getSurfaceForm(surfaceForm)
        println("\t found..\t" + surfaceForm)
        sf.id
      } catch {
        case e: SurfaceFormNotFoundException => {
          surfaceForm
        }
      })

    // Separating Existing SF from non existing
    val (listOfNewSurfaceForms, listOfExistingSFIds) = searchSurfaceFormResult.par.partition(_.isInstanceOf[String])

    // Adding the non-existent SF to the low level maps
    val listOfNewSurfaceFormIds: List[Int] = addListOfNewSurfaceForms(listOfNewSurfaceForms.toList.asInstanceOf[List[String]])

    // making all new SF spottable(updating Probabilities)
    val allSFIds: List[Int] = listOfExistingSFIds.toList.asInstanceOf[List[Int]] ++ listOfNewSurfaceFormIds
    allSFIds.foreach(
      surfaceFormId => boostCountsIfNeeded(surfaceFormId))

    // Rebuilding reverse lookups
    println("\t updating the SF index")
    this.sfStore.createReverseLookup()

    return listOfNewSurfaceFormIds
  }

  /*
  * Raises the SF counts to pass the minimum threshold needed to be spottable
  * makes the SF annotationProbability equals to 0.27, this is done by rising the annotatedCounts
  * */
  def boostCountsIfNeeded(surfaceFormID: Int) {

    val annotatedCountsForSurfaceForm = getCountFromQuantiziedValue(this.sfStore.annotatedCountForID(surfaceFormID))
    val totalCountsForSurfaceForm = getCountFromQuantiziedValue(this.sfStore.totalCountForID(surfaceFormID))

    val annotationProbability = annotatedCountsForSurfaceForm / totalCountsForSurfaceForm.toDouble
    if (annotationProbability < 0.5) {
      var newAnnotatedCount = (0.5 * totalCountsForSurfaceForm.toDouble).toInt + 1
      val newAnnotatedCountQuantizied = getQuantiziedCounts(newAnnotatedCount)
      this.sfStore.annotatedCountForID(surfaceFormID) = newAnnotatedCountQuantizied
    }
  }

  /*
* Raises the SF counts to pass the minimum threshold needed to be spottable
* makes the SF annotationProbability equals to 0.27, this is done by rising the annotatedCounts
* */
  def boostCountsIfNeededByString(surfaceFormText: String) {
    try {
      val surfaceFormId = this.sfStore.getSurfaceForm(surfaceFormText).id
      this.boostCountsIfNeeded(surfaceFormId)
    } catch {
      case e: SurfaceFormNotFoundException => {
        println("given SF:" + surfaceFormText + " does not exist")
      }
    }

  }

  /*
  * Reduces the SF counts making it less likely to be spotted.
  * Makes the SF annotationProbability equals to 0.1, this is done by reducing the annotatedCounts
  * */
  def decreaseSpottingProbabilityById(surfaceFormID: Int, spotProbability: Double) {

    val annotatedCountsForSurfaceForm = getCountFromQuantiziedValue(this.sfStore.annotatedCountForID(surfaceFormID))
    val totalCountsForSurfaceForm = getCountFromQuantiziedValue(this.sfStore.totalCountForID(surfaceFormID)).toDouble

    val annotationProbability = annotatedCountsForSurfaceForm / totalCountsForSurfaceForm.toDouble
    if (annotatedCountsForSurfaceForm < 2) {
      this.sfStore.totalCountForID(surfaceFormID) = getQuantiziedCounts(10)
    }

    if (annotationProbability > spotProbability) {
      var newAnnotatedCount = (spotProbability * totalCountsForSurfaceForm).toInt + 1
      this.sfStore.annotatedCountForID(surfaceFormID) = getQuantiziedCounts(newAnnotatedCount)
    }

  }

  /*
* Reduces the SF counts making it less likely to be spotted.
* Makes the SF annotationProbability equals to 0.1, this is done by reducing the annotatedCounts
* */
  def decreaseSpottingProbabilityByString(surfaceText: String, spotProbability: Double) {
    // looks for the id of the surfaceForm
    try {
      var surfaceForm = this.sfStore.getSurfaceForm(surfaceText)
      this.decreaseSpottingProbabilityById(surfaceForm.id, spotProbability)
      println("\t the counts for:" + surfaceText + "," + surfaceForm.id + " has been reduced.")
    } catch {
      case e: SurfaceFormNotFoundException => println("\tgiven surface form:" + surfaceText + " does not exist...")
    }
  }

  /*
  * Given a SurfaceForm if it exists returns its Id
  * otherwise it creates it, rebuild the internal index, and return the SF ID
  * */
  def getAddSurfaceForm(surfaceText: String): Int = {
    getAddUpperCaseSurfaceForm(surfaceText)
  }

  /*
    Receives a set of Sfs, returns a map from Sf-> surfaceForm Id for those Sf which could be found
    in the main SurfaceForm Store
    */
  def findSetOfSurfaceFormsInMainStore(setOfSurfaceForms: Set[String]):immutable.HashMap[String, Int]={

    val listOfSurfaceFormIdentifierTuples: Seq[(String, Int)] = setOfSurfaceForms.map{surfaceForm:String =>
      val identifier = this.sfStore.idForString.get(surfaceForm)
      if (identifier != null){
        Option((surfaceForm, identifier.asInstanceOf[scala.Int]))
      }else{
        None
      } }.flatten.toSeq

    immutable.HashMap[String, Int](listOfSurfaceFormIdentifierTuples:_*)
  }

  /*
    * Adds a lower case only if it doesnt exist.
    * If it exists it adds the candidaditaes SF to the list
    *
    * Note: **WARNING** IT ASSUMMES THAT sfStore reverseMap is updated
    * */
  def addLowerCaseSurfaceForm(surfaceText: String, candidatesSF: Array[String]) {

    /* Each lowercase surface Form has a list of Candidate Uppercase SF. i.e:
          "real time bidding" can have the following candidates SF's "RTB" , "Real Time Bidding"
      adding the candidate upperCaseSurfaceForms to the main Store is necessary so they have
      an identifier so we can bind lowercases SFs to its candidates.
    */
    val setOfCandidatesSF = scala.collection.mutable.HashSet[String](candidatesSF:_*)
    val listOfUppercaseSfIds = setOfCandidatesSF.map{ upperCaseSurfaceFormStore: String  =>
      this.getAddUpperCaseSurfaceForm(upperCaseSurfaceFormStore)
    }

    // the lowercase SF might have already had candidates, so get them
    var candidateSurfaceForms = this.sfStore.lowercaseMap.get(surfaceText)

    /*
    * the first element of the candidates is actually the lowercase SF's count
    * (a bit tricky, but it is defined by that in the spotlight's code)
    * so here I just add a default value of one, in case it didn't have any candidate before
    * */
    if (candidateSurfaceForms==null){
      candidateSurfaceForms = Array[Int](1)
    }

    print("Adding lower case\n")
    print("\t sf:"+surfaceText+"\n")
    print("\t candidateSize: "+candidatesSF.size +"\n")
    print("\n")

    // Concatenate the old candidates with the new ones
    val allCandidatesSF = scala.collection.mutable.HashSet[Int]((candidateSurfaceForms.tail++listOfUppercaseSfIds):_*).toArray

    // Find a  good value for the counts (based on the counts of the candidates surface form)
    val quantiziedCandidateCounts = allCandidatesSF.map{ candidateSfId: Int => this.sfStore.totalCountForID(candidateSfId)}

    val lowercaseCounts:Array[Int] = Array[Int](quantiziedCandidateCounts.map{
      quantiziedCandidateCount:Short => this.getCountFromQuantiziedValue(quantiziedCandidateCount)}.min)

    // update the lowercase store.
    this.sfStore.lowercaseMap.put(surfaceText, lowercaseCounts++allCandidatesSF  )

  }


  /*
   * Given a map having lowerCaseSF as keys and list of UppercaseSF as values
   * it will update the internal lowerSurfaceForm maps.
   * */
  def addMapOfLowerCaseSurfaceForms(mapOfLowerCaseSfs:collection.mutable.HashMap[String, Array[String]]){
    mapOfLowerCaseSfs.foreach{
      case(lowerSf, candidatesUppercaseSF) =>
        this.addLowerCaseSurfaceForm(lowerSf, candidatesUppercaseSF)
    }
  }

    /*
   * getAdd a SF with at least a letter uppercase
   * */
    def getAddUpperCaseSurfaceForm(surfaceText:String):Int ={
      // look for existing surfaceForm
      try{
        var surfaceForm = this.sfStore.getSurfaceForm(surfaceText)
        this.boostCountsIfNeeded(surfaceForm.id)
        return surfaceForm.id
      } catch{

        case e: SurfaceFormNotFoundException => println("creating surface form...")
      }
      // create sf in case it cant be found
      var surfaceFormId = this.addSurfaceForm(surfaceText)
      this.boostCountsIfNeeded(surfaceFormId)

      return surfaceFormId
    }

}
