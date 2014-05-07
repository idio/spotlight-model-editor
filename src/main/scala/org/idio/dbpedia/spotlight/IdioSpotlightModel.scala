package org.idio.dbpedia.spotlight

/**
 * Created by dav009 on 23/12/2013.
 */
import org.dbpedia.spotlight.model.{ Candidate, TokenType, OntologyType }
import org.dbpedia.spotlight.db.memory.MemoryStore
import java.io.File
import java.io.{ FileNotFoundException, FileInputStream }
import java.util.Properties
import collection.mutable.HashMap
import scala.collection.mutable.HashSet
import java.io.PrintWriter
import scala.collection.JavaConverters._
import org.idio.dbpedia.spotlight.stores._

class IdioSpotlightModel(val pathToFolder: String) {

  // Load the properties file
  val properties: Properties = new Properties()
  val propertyFolder = new File(pathToFolder).getParent()
  properties.load(new FileInputStream(new File(propertyFolder, "model.properties")))

  var idioDbpediaResourceStore: IdioDbpediaResourceStore = null
  var idioCandidateMapStore: IdioCandidateMapStore = null
  var idioSurfaceFormStore: IdioSurfaceFormStore = null
  var idioTokenTypeStore: IdioTokenResourceStore = null
  var idioContextStore: IdioContextStore = null

  /*
   The reason for all these Try/Catch is to allow loading the models locally.
   Allowing to load only the files which are needed as opposed to the whole model.
  */
  //load the Stores
  try {
    this.idioDbpediaResourceStore = new IdioDbpediaResourceStore(pathToFolder)
  } catch {
    case ex: FileNotFoundException => {
      println(ex.getMessage)
    }
  }

  try {
    this.idioCandidateMapStore = new IdioCandidateMapStore(pathToFolder, idioDbpediaResourceStore.resStore)
  } catch {
    case ex: Exception => {
      println(ex.getMessage)
    }
  }

  try {
    this.idioSurfaceFormStore = new IdioSurfaceFormStore(pathToFolder)
  } catch {
    case ex: FileNotFoundException => {
      println(ex.getMessage)
    }
  }
  try {
    this.idioTokenTypeStore = new IdioTokenResourceStore(pathToFolder, properties.getProperty("stemmer"))
  } catch {
    case ex: FileNotFoundException => {
      println(ex.getMessage)
    }
  }
  try {
    this.idioContextStore = new IdioContextStore(pathToFolder, idioTokenTypeStore.tokenStore)
  } catch {
    case ex: FileNotFoundException => {
      println(ex.getMessage)
    }
  }

  /*
  * Serializes the current model in the given folder
  * */
  def exportModels(pathToFolder: String) {
    println("exporting models to.." + pathToFolder)
    try {
      MemoryStore.dump(this.idioDbpediaResourceStore.resStore, new File(pathToFolder, "res.mem"))
    } catch {
      case ex: Exception => {
        println(ex.getMessage)
      }
    }

    try {
      MemoryStore.dump(this.idioCandidateMapStore.candidateMap, new File(pathToFolder, "candmap.mem"))
    } catch {
      case ex: Exception => {
        println(ex.getMessage)
      }
    }

    try {
      MemoryStore.dump(this.idioSurfaceFormStore.sfStore, new File(pathToFolder, "sf.mem"))
    } catch {
      case ex: Exception => {
        println(ex.getMessage)
      }
    }

    try {
      MemoryStore.dump(this.idioTokenTypeStore.tokenStore, new File(pathToFolder, "tokens.mem"))
    } catch {
      case ex: Exception => {
        println(ex.getMessage)
      }
    }

    try {
      MemoryStore.dump(this.idioContextStore.contextStore, new File(pathToFolder, "context.mem"))
    } catch {
      case ex: Exception => {
        println(ex.getMessage)
      }
    }

    println("finished exporting models to.." + pathToFolder)

  }

  /*
  * Links the Sf with the DbpediaURI, if they are not linked.
  * If the Sf does not exist it will create it
  * if the Dbpedia Resource does not exist it will create it
  * */
  def addNewSFDbpediaResource(surfaceFormText: String, candidateURI: String, types: Array[String]): (Int, Int) = {

    // create or get the surfaceForm
    val surfaceFormID: Int = this.idioSurfaceFormStore.getAddSurfaceForm(surfaceFormText)
    this.idioSurfaceFormStore.boostCountsIfNeeded(surfaceFormID)
    var defaultSupportForDbpediaResource: Int = 11
    val defaultSupportForCandidate: Int = 30

    var avgSupportCandidate = defaultSupportForCandidate

    // calculate the default support value based on the current support for the candidates for the given SF
    try {
      val candidates: Array[Int] = this.idioCandidateMapStore.candidateMap.candidates(surfaceFormID)
      var avgSupport: Double = 0.0
      for (candidate <- candidates) {
        val dbpediaResource = this.idioDbpediaResourceStore.resStore.getResource(candidate)
        avgSupport = avgSupport + dbpediaResource.support
      }
      val calculatedSupport = (avgSupport / candidates.length).toInt
      if (calculatedSupport > defaultSupportForDbpediaResource) {
        defaultSupportForDbpediaResource = calculatedSupport
      }

      avgSupportCandidate = math.max(defaultSupportForCandidate, this.idioCandidateMapStore.getAVGSupportForSF(surfaceFormID)) + 10
    } catch {
      case e: Exception => println("\tusing default support for.." + candidateURI)
    }

    // create or get the dbpedia Resource
    val dbpediaResourceID: Int = this.idioDbpediaResourceStore.getAddDbpediaResource(candidateURI, defaultSupportForDbpediaResource, types)

    //update the candidate Store
    this.idioCandidateMapStore.addOrCreate(surfaceFormID, dbpediaResourceID, avgSupportCandidate)

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

      this.idioContextStore.createDefaultContextStore(dbpediaResourceID)
      val contextTokenCountMap: HashMap[String, Int] = this.idioTokenTypeStore.getContextTokens(contextWords, contextCounts)

      println("\tadding new context tokens to the context array")
      // Add the stems to the token Store and to the contextStore
      for (token <- contextTokenCountMap.keySet) {

        val tokenID: Int = this.idioTokenTypeStore.getOrCreateToken(token)
        val tokenCount: Int = contextTokenCountMap.get(token).get
        this.idioContextStore.addContext(dbpediaResourceID, tokenID, tokenCount)

        println("\t\tadded token to context array - " + token)
      }

    } catch {
      case ex: Exception => {
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
  * and sets the context words and cotnext counts specified in the command line.
  *
  * */
  def replaceAllContext(dbpediaURI: String, contextWords: Array[String], contextCounts: Array[Int]) {
    val dbpediaId = this.idioDbpediaResourceStore.resStore.getResourceByName(dbpediaURI).id
    // remove all items in dbpediaId's context words and counts
    this.idioContextStore.cleanContextWords(dbpediaId)
    // add the specified context words and counts
    this.addNewContextWords(dbpediaId, contextWords, contextCounts)
  }

  /*
  * Returns true if the dbpedia topic with the given URI exists in the resource Store
  * */
  def searchForDBpediaResource(candidateURI: String): Boolean = {
    try {
      this.idioDbpediaResourceStore.resStore.getResourceByName(candidateURI)
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
    val dbpediaResourceId: Int = this.idioDbpediaResourceStore.resStore.getResourceByName(dbpediaResourceURI).id
    val tokenId: Int = this.idioTokenTypeStore.getOrCreateToken(token)
    this.idioContextStore.addContext(dbpediaResourceId, tokenId, count)
  }

  /*
  * Prints the context of a DbpediaResoruceURI
  * */
  def prettyPrintContext(dbpediaResourceURI: String) {
    val dbpediaResourceID: Int = this.idioDbpediaResourceStore.resStore.getResourceByName(dbpediaResourceURI).id
    var tokens: Array[Int] = this.idioContextStore.contextStore.tokens(dbpediaResourceID)
    var counts: Array[Int] = this.idioContextStore.contextStore.counts(dbpediaResourceID)
    println("Contexts for " + dbpediaResourceURI + " Id:" + dbpediaResourceID)
    for (i <- 0 to tokens.size - 1) {
      this.idioTokenTypeStore.tokenStore.idFromToken
      println("\t" + this.idioTokenTypeStore.tokenStore.getTokenTypeByID(tokens(i)) + "--" + counts(i))
    }
  }

  /*
  * Prints the statistics for a surfaceForm and its candidates
  * */
  def getStatsForSurfaceForm(surfaceFormText: String) {
    val surfaceForm = this.idioSurfaceFormStore.sfStore.getSurfaceForm(surfaceFormText)

    println("surface form id:" + surfaceForm.id)
    println("")
    println("annotated count of SF:")
    println("\t" + surfaceForm.annotatedCount)
    println("total counts of SF:")
    println("\t" + surfaceForm.totalCount)
    println("annotation probability")
    println("\t" + surfaceForm.annotationProbability)

    val candidates: Set[Candidate] = this.idioCandidateMapStore.candidateMap.getCandidates(surfaceForm)

    for (candidate <- candidates) {

      println("---------------" + candidate + "---------------------")
      val dbpediaResource = candidate.resource
      println(dbpediaResource.getFullUri)
      println("\tid:" + candidate)
      println("\tsupport")
      println("\t\t" + dbpediaResource.support)
      println("\tannotated_count")
      println("\t\t" + surfaceForm.annotatedCount)
      println("\tprior")
      println("\t\t" + dbpediaResource.prior)
    }
  }

  /*
  * Prints the first 40 surface forms and their respective candidates
  */
  def showSomeSurfaceForms() {
    val someSurfaceForms = this.idioSurfaceFormStore.sfStore.iterateSurfaceForms.slice(0, 40)
    for (surfaceForm <- someSurfaceForms) {
      println(surfaceForm.name + "-" + surfaceForm.id)
      for (candidate <- this.idioCandidateMapStore.candidateMap.getCandidates(surfaceForm)) {

        println("\t" + candidate.resource.getFullUri + "\t" + candidate.resource.uri)

        val dbpediaTypes: List[OntologyType] = candidate.resource.types

        for (dbpediaType: OntologyType <- dbpediaTypes) {
          println("\t\t" + dbpediaType.typeID)
        }

      }

    }
  }

  /*
  *  Makes a SF not spottable by reducing its annotationProbability to 0.1
  * */
  def makeSFNotSpottable(surfaceText: String) {
    if (!surfaceText.contains(" "))
      this.idioSurfaceFormStore.decreaseSpottingProbabilityByString(surfaceText, 0.1)
    else
      this.idioSurfaceFormStore.decreaseSpottingProbabilityByString(surfaceText, 0.005)
  }

  /*
*  Makes a SF  spottable by reducing its annotationProbability to 0.1
* */
  def makeSFSpottable(surfaceText: String) {
    this.idioSurfaceFormStore.boostCountsIfNeededByString(surfaceText)
  }

  /*
  * Updates the SurfaceStore by adding the SF in the Set in a single Batch.
  * If a SF is already in the stores it wont be added.
  * */
  def addSetOfSurfaceForms(setOfSF: scala.collection.Set[String]) {
    val listOfNewSurfaceFormIds = this.idioSurfaceFormStore.addSetOfSF(setOfSF)
    // adds the candidate array for the SF which were added.
    listOfNewSurfaceFormIds.foreach(
      surfaceFormId =>
        this.idioCandidateMapStore.createCandidateMapForSurfaceForm(surfaceFormId, new Array[Int](0), new Array[Int](0)))
  }
  /*
    takes all topic candidates for the surfaceForm1
    and associate them to surfaceForm2.
    Assumes that both SurfaceForms exists in the model
  */
  def copyCandidates(surfaceTextSource: String, surfaceTextDestination: String) {

    val sourceSurfaceForm = this.idioSurfaceFormStore.sfStore.getSurfaceForm(surfaceTextSource)
    val destinySurfaceForm = this.idioSurfaceFormStore.sfStore.getSurfaceForm(surfaceTextDestination)

    this.idioCandidateMapStore.copyCandidates(sourceSurfaceForm, destinySurfaceForm)

  }

  /*
  * Removes the link between a SF and a Dbpedia Topic
  * */
  def removeAssociation(surfaceFormText: String, dbpediaURI: String) {
    try {
      val surfaceFormId = this.idioSurfaceFormStore.sfStore.getSurfaceForm(surfaceFormText).id

      val dbpediaId = this.idioDbpediaResourceStore.resStore.getResourceByName(dbpediaURI).id
      this.idioCandidateMapStore.removeAssociation(surfaceFormId, dbpediaId)

    } catch {
      case e: Exception => {
        println("\t given dbpediaURI or SF: " + dbpediaURI + " , " + surfaceFormText + " could not be found")
      }
      case e: ArrayIndexOutOfBoundsException => {
        println("\t no association between " + surfaceFormText + " and " + dbpediaURI + " existed before")
      }
    }

  }

  /**
   *  given a SF returns the list of candidate Topics
   */
  def getCandidates(surfaceFormText: String): Set[String] = {
    val surfaceForm = this.idioSurfaceFormStore.sfStore.getSurfaceForm(surfaceFormText)
    val candidates: Set[Candidate] = this.idioCandidateMapStore.candidateMap.getCandidates(surfaceForm)
    val topicUris = candidates.map({ candidate: Candidate =>
      candidate.resource.getFullUri
    })
    return topicUris
  }

  /*
  * Adds a set of tokens to the token type store.
  * It only generte reverse look ups once.
  * */
  def addSetOfTokens(contextWords: scala.collection.Set[String]) {
    val stemmedContextWords: scala.collection.Set[String] = contextWords.map { contextword: String =>
      this.idioTokenTypeStore.stemToken(contextword)
    }.toSet
    this.idioTokenTypeStore.addSetOfTokens(stemmedContextWords)
  }

  /*
  * Saves the context Store to a plain File
  * */
  def exportContextStore(pathToFile: String) {
    val writer = new PrintWriter(new File(pathToFile))
    val dbpediaIds = this.idioDbpediaResourceStore.resStore.idFromURI.values().asScala
    for (dbpediaTopicID <- dbpediaIds) {
      var lineInformation: scala.collection.mutable.ArrayBuffer[String] = scala.collection.mutable.ArrayBuffer[String]()

      try {
        val dbpediaResource = this.idioDbpediaResourceStore.resStore.getResource(dbpediaTopicID)
        val contextCounts: scala.collection.mutable.Map[TokenType, Int] = this.idioContextStore.contextStore.getContextCounts(dbpediaResource).asScala

        lineInformation += dbpediaResource.uri
        for ((tokenType, count) <- contextCounts) {
          lineInformation += tokenType.tokenType + ":" + count
        }
        val writeLine = lineInformation.mkString("\t") + "\n"
        writer.write(writeLine)

      } catch {
        case e: Exception => {
          println("\t not found context for" + dbpediaTopicID)
        }
      }
    }
    writer.close()
  }

}