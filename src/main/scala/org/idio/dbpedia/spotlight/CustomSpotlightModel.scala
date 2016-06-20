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

package org.idio.dbpedia.spotlight

import org.dbpedia.spotlight.exceptions.{DBpediaResourceNotFoundException, SurfaceFormNotFoundException}
import org.dbpedia.spotlight.model.{ Candidate, TokenType, OntologyType }
import org.dbpedia.spotlight.db.memory.MemoryStore
import java.io.File
import java.io.{ FileNotFoundException, FileInputStream }
import java.util.{Locale, Properties}
import collection.mutable.HashMap
import scala.collection.mutable.HashSet
import java.io.PrintWriter
import scala.collection.JavaConverters._
import org.idio.dbpedia.spotlight.stores._
import org.dbpedia.spotlight.db.model.{Stemmer, TextTokenizer}
import org.dbpedia.spotlight.db.tokenize.LanguageIndependentTokenizer
import org.dbpedia.spotlight.db.stem.SnowballStemmer
import org.dbpedia.spotlight.db.FSASpotter
import org.idio.dbpedia.spotlight.utils.CustomTokenizer

object Store extends Enumeration {
  type Store = Value
  val SurfaceStore, CandidateStore, ResourceStore, ContextStore, TokenStore, QuantiziedStore = Value
}

class CustomSpotlightModel(val pathToFolder: String) {

  // Load the properties file
  val properties: Properties = new Properties()
  val propertyFolder = new File(pathToFolder).getParent()
  properties.load(new FileInputStream(new File(propertyFolder, "model.properties")))

  val usedStores = scala.collection.mutable.HashSet[Store.Value]()

  /*
   The reason for all these Try/Catch is to allow loading the models locally.
   Allowing to load only the files which are needed as opposed to the whole model.
  */
  //load the Stores

  lazy val customQuantizedCountStore: CustomQuantiziedCountStore = try{
    usedStores.add(Store.QuantiziedStore)
    new CustomQuantiziedCountStore(pathToFolder)
  }catch{
    case ex: FileNotFoundException =>{
      println(ex.getMessage)
      null
    }
  }


  lazy val customDbpediaResourceStore: CustomDbpediaResourceStore = try {
    usedStores.add(Store.ResourceStore)
    new CustomDbpediaResourceStore(pathToFolder, customQuantizedCountStore)
  } catch {
    case ex: FileNotFoundException => {
      println(ex.getMessage)
      null
    }
  }

  lazy val customCandidateMapStore: CustomCandidateMapStore = try {
    usedStores.add(Store.CandidateStore)
    new CustomCandidateMapStore(pathToFolder, customDbpediaResourceStore.resStore, customSurfaceFormStore.sfStore, customQuantizedCountStore)
  } catch {
    case ex: Exception => {
      println(ex.getMessage)
      null
    }
  }

  lazy val customSurfaceFormStore: CustomSurfaceFormStore = try {
    usedStores.add(Store.SurfaceStore)
    new CustomSurfaceFormStore(pathToFolder, customQuantizedCountStore)
  } catch {
    case ex: FileNotFoundException => {
      println(ex.getMessage)
      null
    }
  }

  lazy val customTokenTypeStore: CustomTokenResourceStore = try {
    usedStores.add(Store.TokenStore)
     new CustomTokenResourceStore(pathToFolder, properties.getProperty("stemmer"), locale)
  } catch {
    case ex: FileNotFoundException => {
      println(ex.getMessage)
      null
    }
  }

  lazy val customContextStore: CustomContextStore =
  try {
    usedStores.add(Store.ContextStore)
    new CustomContextStore(pathToFolder, customTokenTypeStore.tokenStore, customQuantizedCountStore)
  } catch {
    case ex: FileNotFoundException => {
      println(ex.getMessage)
      null
    }
  }

  val localeParsed = properties.getProperty("locale").split("_")
  val locale = new Locale(localeParsed(0), localeParsed(1))

  lazy val tokenizer:CustomTokenizer = new CustomTokenizer(customTokenTypeStore, locale)

  lazy val customFSAStore: CustomFSAStore =
    try {
      new CustomFSAStore(pathToFolder, customTokenTypeStore, tokenizer)
    } catch {
      case ex: FileNotFoundException => {
        println(ex.getMessage)
        null
      }
    }

  /*
  * Serializes the current model in the given folder
  * */
  def exportModels(pathToFolder: String) {
    println("exporting models to.." + pathToFolder)

    try{
      if(usedStores.contains(Store.QuantiziedStore))
         MemoryStore.dump(this.customQuantizedCountStore.quantizedStore, new File(pathToFolder,"quantized_counts.mem"))
    }catch{
      case ex: Exception =>{
        println(ex.getMessage)
      }
    }

    try {
      if(usedStores.contains(Store.ResourceStore))
         MemoryStore.dump(this.customDbpediaResourceStore.resStore, new File(pathToFolder, "res.mem"))
    } catch {
      case ex: Exception => {
        println(ex.getMessage)
      }
    }

    try {
      if(usedStores.contains(Store.CandidateStore))
          MemoryStore.dump(this.customCandidateMapStore.candidateMap, new File(pathToFolder, "candmap.mem"))
    } catch {
      case ex: Exception => {
        println(ex.getMessage)
      }
    }

    try {
      if(usedStores.contains(Store.SurfaceStore))
         MemoryStore.dump(this.customSurfaceFormStore.sfStore, new File(pathToFolder, "sf.mem"))
    } catch {
      case ex: Exception => {
        println(ex.getMessage)
      }
    }

    try {
      if(usedStores.contains(Store.TokenStore) ||
         usedStores.contains(Store.SurfaceStore) ||
         usedStores.contains(Store.ContextStore) ){
         this.customSurfaceFormStore.sfStore.quantizedCountStore = this.customQuantizedCountStore.quantizedStore
         this.customTokenTypeStore.addAllTokensInSurfaceFormStore(this.customSurfaceFormStore.sfStore)
         MemoryStore.dump(this.customTokenTypeStore.tokenStore, new File(pathToFolder, "tokens.mem"))
      }
    } catch {
      case ex: Exception => {
        println(ex.getMessage)
      }
    }

    try {
      if(usedStores.contains(Store.ContextStore)){
         this.customContextStore.sortTokensInContextStore()
         MemoryStore.dump(this.customContextStore.contextStore, new File(pathToFolder, "context.mem"))
      }
    } catch {
      case ex: Exception => {
        println(ex.getMessage)
      }
    }

    try{
      if(usedStores.contains(Store.TokenStore) ||
        usedStores.contains(Store.SurfaceStore) ||
        usedStores.contains(Store.ContextStore) ){
            println("Creating FSA....")
            println("updating quantized store for sfstore")
            this.customSurfaceFormStore.sfStore.quantizedCountStore = this.customQuantizedCountStore.quantizedStore
            println("building FSA Spotter")
            val fsaDict = FSASpotter.buildDictionary(this.customSurfaceFormStore.sfStore, tokenizer.tokenizer)
            println("dumping FSA")
            MemoryStore.dump(fsaDict, new File(propertyFolder, "fsa_dict.mem"))
      }
    }catch{
      case ex: Exception =>{
        println(ex.getMessage)
      }
    }

    println("finished exporting models to.." + pathToFolder)

  }

  def getQuantiziedCounts(count:Int):Short ={
    return this.customQuantizedCountStore.quantizedStore.addCount(count)
  }

  def getCountFromQuantiziedValue(quantiziedValue:Short):Int ={
    return this.customQuantizedCountStore.quantizedStore.getCount(quantiziedValue)
  }

  /*
  * Links the Sf with the DbpediaURI, if they are not linked.
  * If the Sf does not exist it will create it
  * if the Dbpedia Resource does not exist it will create it
  * */
  def addNewSFDbpediaResource(surfaceFormText: String, candidateURI: String, types: Array[String]): (Int, Int) = {

    // create or get the surfaceForm
    val surfaceFormID: Int = this.customSurfaceFormStore.getAddSurfaceForm(surfaceFormText)
    this.customSurfaceFormStore.boostCountsIfNeeded(surfaceFormID)
    // These default values are related to the default values for support filters for the annotation endpoint
    var defaultSupportForDbpediaResource: Int = 30
    val defaultSupportForCandidate: Int = 30

    var avgSupportCandidate = defaultSupportForCandidate

    // calculate the default support value based on the current support for the candidates for the given SF
    try {
      val candidates: Array[Int] = this.customCandidateMapStore.candidateMap.candidates(surfaceFormID)
      var sumSupport: Double = 0.0
      for (candidate <- candidates) {
        val dbpediaResource = this.customDbpediaResourceStore.resStore.getResource(candidate)
        sumSupport = sumSupport + dbpediaResource.support
      }
      val avgSupport = (sumSupport / candidates.length).toInt
      if (avgSupport > defaultSupportForDbpediaResource) {
        defaultSupportForDbpediaResource = avgSupport
      }

      avgSupportCandidate = math.max(defaultSupportForCandidate, this.customCandidateMapStore.getAVGSupportForSF(surfaceFormID)) + 10
    } catch {
      case e: Exception => println("\tusing default support for.." + candidateURI)
    }

    // create or get the dbpedia Resource
    val dbpediaResourceID: Int = this.customDbpediaResourceStore.getAddDbpediaResource(candidateURI, defaultSupportForDbpediaResource, types)

    //update the candidate Store
    this.customCandidateMapStore.addOrCreate(surfaceFormID, dbpediaResourceID, avgSupportCandidate)

    return (surfaceFormID, dbpediaResourceID)

  }

  /*
  * Adds the words in contextWords to  dbpediaResource's context.
  * Words already in the DbpediaResource's context won't be added (their counts will not be modified )
  * Words will be tokenized, their stems are added to the tokenStore if they dont exist.
  * */
  def addNewContextWords(dbpediaResourceID: Int, contextWords: Array[String], contextCounts: Array[Int]) {
    //update the context Store
    println("\trying to update context for: " + dbpediaResourceID)
    try {

      this.customContextStore.createDefaultContextStore(dbpediaResourceID)
      val contextTokenCountMap: HashMap[String, Int] = this.customTokenTypeStore.getContextTokens(contextWords, contextCounts)

      println("\tadding new context tokens to the context array")
      // Add the stems to the token Store and to the contextStore
      for (token <- contextTokenCountMap.keySet) {

        val tokenID: Int = this.customTokenTypeStore.getOrCreateToken(token)
        val tokenCount: Int = contextTokenCountMap.get(token).get
        this.customContextStore.addContext(dbpediaResourceID, tokenID, tokenCount)

        println("\t\tadded token to context array - " + token)
      }

    } catch {
      case ex: Exception => {
        println(ex.getMessage + "\n")
        ex.printStackTrace()
        println("\tNot Context Store found....")
        println("\tSkipping Context Tokens....")
      }
    }
  }

  /*
  * Attach a surfaceform to a candidateTopic
  * if SurfaceForm does not exist it is created
  * if candidateTopic does not exist it is created
  * */
  def addNew(surfaceFormText: String, candidateURI: String, types: Array[String], contextWords: Array[String], contextCounts: Array[Int]): (Int, Int) = {
    val (surfaceFormID, dbpediaResourceID) = this.addNewSFDbpediaResource(surfaceFormText, candidateURI, types)
    this.addNewContextWords(dbpediaResourceID, contextWords, contextCounts)
    return (surfaceFormID, dbpediaResourceID)
  }

  /*
  * Removes all the context words and context counts of a dbepdia topic
  * and sets the context words and context counts specified in the command line.
  *
  * */
  def replaceAllContext(dbpediaURI: String, contextWords: Array[String], contextCounts: Array[Int]) {
    val dbpediaId = this.customDbpediaResourceStore.resStore.getResourceByName(dbpediaURI).id
    // remove all items in dbpediaId's context words and counts
    this.customContextStore.cleanContextWords(dbpediaId)
    // add the specified context words and counts
    this.addNewContextWords(dbpediaId, contextWords, contextCounts)
  }

  /*
  * Returns true if the dbpedia topic with the given URI exists in the resource Store
  * */
  def searchForDBpediaResource(candidateURI: String): Boolean = {
    try {
      this.customDbpediaResourceStore.resStore.getResourceByName(candidateURI)
      return true
    } catch {
      case e: Exception => {
        println(e.getMessage)
        println(e.getStackTrace)
        return false
      }
    }
  }

  /*
  * Add a new Context Token for a dbpedia URI.
  * */
  def addContextToken(dbpediaResourceURI: String, token: String, count: Int) {
    val dbpediaResourceId: Int = this.customDbpediaResourceStore.resStore.getResourceByName(dbpediaResourceURI).id
    val tokenId: Int = this.customTokenTypeStore.getOrCreateToken(token)
    this.customContextStore.addContext(dbpediaResourceId, tokenId, count)
  }

  /*
  * Prints the context of a DbpediaResoruceURI
  * */
  def prettyPrintContext(dbpediaResourceURI: String) {
    val dbpediaResourceID: Int = this.customDbpediaResourceStore.resStore.getResourceByName(dbpediaResourceURI).id
    var tokens: Array[Int] = this.customContextStore.contextStore.tokens(dbpediaResourceID)
    var quantiziedCounts:Array[Short] = this.customContextStore.contextStore.counts(dbpediaResourceID)

    val counts:Array[Int] = new Array[Int](quantiziedCounts.size)
    // transforming from quantizied values to real counts
    for(i<- 0 until quantiziedCounts.size){
      counts(i) = getCountFromQuantiziedValue(quantiziedCounts(i))
    }

    println("Contexts for " + dbpediaResourceURI + " Id:" + dbpediaResourceID)
    for (i <- 0 to tokens.size - 1) {
      println("\t" + this.customTokenTypeStore.tokenStore.getTokenTypeByID(tokens(i)) + "--" + counts(i))
    }
  }

  // Print the stats of a surface form in the Main SF Store
  def getStatsForSFInUppercaseSf(surfaceFormText: String){
    val surfaceForm = this.customSurfaceFormStore.sfStore.getSurfaceForm(surfaceFormText)

    println("Surface form id:" + surfaceForm.id)
    println("")
    println("Annotated count of SF:")
    println("\t" + surfaceForm.annotatedCount)
    println("Total counts of SF:")
    println("\t" + surfaceForm.totalCount)
    println("Annotation probability")
    println("\t" + surfaceForm.annotationProbability)

    val candidates: Set[Candidate] = this.customCandidateMapStore.candidateMap.getCandidates(surfaceForm)

    for (candidate <- candidates) {

      println("---------------" + candidate + "---------------------")
      val dbpediaResource = candidate.resource
      println(dbpediaResource.getFullUri)
      println("\tId:" + candidate)
      println("\tSupport")
      println("\t\t" + dbpediaResource.support)
      println("\tAnnotated_count")
      println("\t\t" + surfaceForm.annotatedCount)
      println("\tPrior")
      println("\t\t" + dbpediaResource.prior)
    }
  }

  // Print the stats of a surface form in the lowercase surface form store
  def getStatsForSFInLowercaseSf(surfaceFormText: String){
    println("Looking in lowercase SF store.....")
    val (counts, candidatesUpperSF) = this.customSurfaceFormStore.findInLowerCaseSurfaceForm(surfaceFormText)
    if(candidatesUpperSF.isDefined){
        println("-------------------------------")
        println(surfaceFormText)
        println("\tcounts:" + counts.getOrElse(0))
        println("")
        println("candidates in uppercase SurfaceForm Store: ")
        for (candidate <- candidatesUpperSF.getOrElse(Array[String]())) {
          println("\t" + candidate)
        }
    }else{
      println("NOT FOUND")
    }

  }

  /*
  * Prints the statistics for a surfaceForm and its candidates
  * */
  def getStatsForSurfaceForm(surfaceFormText: String) {

    try{
       getStatsForSFInUppercaseSf(surfaceFormText)
    }catch{
      case e: Exception => getStatsForSFInLowercaseSf(surfaceFormText)
    }
  }

  /*
  * Prints the first X surface forms and their respective candidates
  */
  def showSomeSurfaceForms(numberOfSurfaceForms: Int) {
    val someSurfaceForms = this.customSurfaceFormStore.sfStore.iterateSurfaceForms.slice(0, numberOfSurfaceForms)
    for (surfaceForm <- someSurfaceForms) {
      println(surfaceForm.name + "-" + surfaceForm.id)
      for (candidate <- this.customCandidateMapStore.candidateMap.getCandidates(surfaceForm)) {

        println("\t" + candidate.resource.getFullUri + "\t" + candidate.resource.uri)

        val dbpediaTypes: List[OntologyType] = candidate.resource.types

        for (dbpediaType: OntologyType <- dbpediaTypes) {
          println("\t\t" + dbpediaType.typeID)
        }

      }

    }
  }

  /*
  *  Makes an SF unspottable by reducing its annotationProbability to 0.1
  * */
  def makeSFNotSpottable(surfaceText: String) {
    if (!surfaceText.contains(" "))
      this.customSurfaceFormStore.decreaseSpottingProbabilityByString(surfaceText, 0.00001)
    else
      this.customSurfaceFormStore.decreaseSpottingProbabilityByString(surfaceText, 0.00005)
  }

  /*
*  Makes an SF spottable by reducing its annotationProbability to 0.1
* */
  def makeSFSpottable(surfaceText: String) {
    this.customSurfaceFormStore.boostCountsIfNeededByString(surfaceText)
  }

  /*
  * Updates the SurfaceStore by adding the SF in the Set in a single Batch.
  * If an SF is already in the stores it wont be added.
  * */
  def addSetOfSurfaceForms(setOfSF: scala.collection.Set[String]) {
    val listOfNewSurfaceFormIds = this.customSurfaceFormStore.addSetOfSF(setOfSF)
    // adds the candidate array for the SF which were added.

    this.customCandidateMapStore.createCandidateMapForNewSurfaceForm(listOfNewSurfaceFormIds)

  }
  /*
    Takes all topic candidates for the surfaceForm1
    and associate them to surfaceForm2.
    Assumes that both SurfaceForms exists in the model
  */
  def copyCandidates(surfaceTextSource: String, surfaceTextDestination: String) {

    val sourceSurfaceForm = this.customSurfaceFormStore.sfStore.getSurfaceForm(surfaceTextSource)
    val destinationSurfaceForm = this.customSurfaceFormStore.sfStore.getSurfaceForm(surfaceTextDestination)

    this.customCandidateMapStore.copyCandidates(sourceSurfaceForm, destinationSurfaceForm)

  }

  /*
  * Removes the link between an SF and a Dbpedia Topic
  * */
  def removeAssociation(surfaceFormText: String, dbpediaURI: String) {
    try {
      val surfaceFormId = this.customSurfaceFormStore.sfStore.getSurfaceForm(surfaceFormText).id

      val dbpediaId = this.customDbpediaResourceStore.resStore.getResourceByName(dbpediaURI).id
      this.customCandidateMapStore.removeAssociation(surfaceFormId, dbpediaId)

    } catch {
      case e: SurfaceFormNotFoundException => {
        println("Surface form not found:" + surfaceFormText)
      }
      case e: DBpediaResourceNotFoundException =>{
        println("DBpediaResource not found:" + dbpediaURI)
      }
      case e: Exception => {
        println("\t Given dbpediaURI or SF: " + dbpediaURI + " , " + surfaceFormText + " could not be found")
      }
      case e: ArrayIndexOutOfBoundsException => {
        println("\t No association between " + surfaceFormText + " and " + dbpediaURI + " existed before")
      }
    }

  }


  /*
  * Change % of context vector
  * */
  def updatePercentageOfContextVector(surfaceForm: String, dbpediaURI: String, percentageOfVector: Double): Unit ={

    try{
      val surfaceFormId = this.customSurfaceFormStore.sfStore.getSurfaceForm(surfaceForm).id
      val surfaceFormCounts = this.customSurfaceFormStore.sfStore.getSurfaceForm(surfaceForm).totalCount
      val dbpediaId = this.customDbpediaResourceStore.resStore.getResourceByName(dbpediaURI).id
      val surfaceFormAnnotatedCount = this.customSurfaceFormStore.sfStore.getSurfaceForm(surfaceForm).annotatedCount
      this.customCandidateMapStore.changePercentageOfContextVector(surfaceFormAnnotatedCount, surfaceFormId, dbpediaId, surfaceFormCounts, percentageOfVector)

    } catch {
      case e: SurfaceFormNotFoundException => {
        println("Surface form not found:" + surfaceForm)
      }
      case e: DBpediaResourceNotFoundException =>{
        println("DBpediaResource not found:" + dbpediaURI)
      }
      case e: Exception => {
        println("\t Given dbpediaURI or SF: " + dbpediaURI + " , " + surfaceForm + " could not be found")
      }
    }


  }


  /**
   *  Given an SF return the list of candidate Topics
   */
  def getCandidates(surfaceFormText: String): Set[String] = {
    val surfaceForm = this.customSurfaceFormStore.sfStore.getSurfaceForm(surfaceFormText)
    val candidates: Set[Candidate] = this.customCandidateMapStore.candidateMap.getCandidates(surfaceForm)
    val topicUris = candidates.map({ candidate: Candidate =>
      candidate.resource.getFullUri
    })
    return topicUris
  }

  /*
  * Adds a set of tokens to the token type store.
  * It only generates reverse look ups once.
  * */
  def addSetOfTokens(contextWords: scala.collection.Set[String]) {
    val stemmedContextWords: scala.collection.Set[String] = contextWords.map { contextword: String =>
      this.customTokenTypeStore.stemToken(contextword)
    }.toSet
    this.customTokenTypeStore.addSetOfTokens(stemmedContextWords)
  }

  /*
  * Saves the context Store to a plain File
  * */
  def exportContextStore(pathToFile: String) {
    val writer = new PrintWriter(new File(pathToFile))
    val dbpediaIds = this.customDbpediaResourceStore.resStore.idFromURI.values().asScala
    for (dbpediaTopicID <- dbpediaIds) {
      var lineInformation: scala.collection.mutable.ArrayBuffer[String] = scala.collection.mutable.ArrayBuffer[String]()

      try {
        val dbpediaResource = this.customDbpediaResourceStore.resStore.getResource(dbpediaTopicID)
        val contextCounts: scala.collection.mutable.Map[TokenType, Int] = this.customContextStore.contextStore.getContextCounts(dbpediaResource).asScala

        lineInformation += dbpediaResource.uri
        for ((tokenType, count) <- contextCounts) {
          lineInformation += tokenType.tokenType + ":" + count
        }
        val writeLine = lineInformation.mkString("\t") + "\n"
        writer.write(writeLine)

      } catch {
        case e: Exception => {
          println("\t Not Context found for" + dbpediaTopicID)
        }
      }
    }
    writer.close()
  }

  /*
   * Given a map having lowerCaseSF as keys and list of UppercaseSF as values
   * it will update the internal lowerSurfaceForm maps.
   * */
  def addMapOfLowerCaseSurfaceForms(mapOfLowerCaseSfs:collection.mutable.HashMap[String, Array[String]]){
    this.customSurfaceFormStore.addMapOfLowerCaseSurfaceForms(mapOfLowerCaseSfs)
  }

  def getLowerCasesSFInStore(setOfLowerCasesSF:Set[String])={
    this.customSurfaceFormStore.findSetOfSurfaceFormsInMainStore(setOfLowerCasesSF)
  }

  // returns the spots returned by the fsa for the given sf
  def getFSASpots(surfaceForm:String): Array[String] = {
    return customFSAStore.getFSASpots(surfaceForm)
  }

}
