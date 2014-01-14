package org.idio.dbpedia.spotlight

/**
 * Created by dav009 on 23/12/2013.
 */

import org.dbpedia.spotlight.db.memory.{MemoryStore,MemorySurfaceFormStore}
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import java.io.{File, FileInputStream}
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

class IdioSurfaceFormStore(val pathtoFolder:String){

  var sfStore:MemorySurfaceFormStore = MemoryStore.loadSurfaceFormStore(new FileInputStream(new File(pathtoFolder,"sf.mem")))

  /*
  * Updates the internal arrays for a new SurfaceForm
  * */
  private def addSF(surfaceText:String){
    println("\t adding a new surface form..."+surfaceText)
    this.sfStore.stringForID = this.sfStore.stringForID :+ surfaceText
    // the counts for the new surface form is the avg of the counts for the other surface forms
    this.sfStore.annotatedCountForID = this.sfStore.annotatedCountForID :+ 1
    this.sfStore.totalCountForID = this.sfStore.totalCountForID :+ 1
  }

  /*
  * Adds a new surfaceForm to the surfaceFormStore.
  * It does NOT check whether it exists
  * returns the Id of the new SF
  * */
  def addSurfaceForm(surfaceText:String):Int =  {
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
  def addListOfSF(setOfSurfaceForms:HashSet[String]):ListBuffer[Int]={

    val listOfNewSurfaceFormIds:ListBuffer[Int] = ListBuffer[Int]()
    val listOfNewSurfaceForms:ListBuffer[String] = ListBuffer[String]()

    for(surfaceForm<-setOfSurfaceForms){
      try{
        val sf = this.sfStore.getSurfaceForm(surfaceForm)
        this.boostCountsIfNeeded(sf.id)
        println("\t found..\t"+surfaceForm)
      } catch{
        case e: SurfaceFormNotFoundException => {
          this.addSF(surfaceForm)
          listOfNewSurfaceForms += surfaceForm
        }
      }
    }
    println("\t updating the SF index")
    this.sfStore.createReverseLookup()

    for(surfaceForm<-listOfNewSurfaceForms){
      val sf = this.sfStore.getSurfaceForm(surfaceForm)
      listOfNewSurfaceFormIds += sf.id
      this.boostCountsIfNeeded(sf.id)
    }

   return listOfNewSurfaceFormIds
  }

  /*
  * Raises the SF counts to pass the minimum threshold needed to be spottable
  * */
  def boostCountsIfNeeded(surfaceFormID:Int){
    val annotationProbability = this.sfStore.annotatedCountForID(surfaceFormID) / this.sfStore.totalCountForID(surfaceFormID).toDouble
    if (annotationProbability<0.27){
      var newAnnotatedCount = (0.27 * this.sfStore.totalCountForID(surfaceFormID).toDouble).toInt + 1
      this.sfStore.annotatedCountForID(surfaceFormID) = newAnnotatedCount
    }
  }

  /*
  * Given a SurfaceForm if it exists returns its Id
  * otherwise it creates it, rebuild the internal index, and return the SF ID
  * */
  def getAddSurfaceForm(surfaceText:String):Int = {

    // look for existing surfaceForm
    try{
      var surfaceForm = this.sfStore.getSurfaceForm(surfaceText)
      return surfaceForm.id
    } catch{

      case e: SurfaceFormNotFoundException => println("creating surface form...")
    }
    // create sf in case it cant be found
    var surfaceFormId = this.addSurfaceForm(surfaceText)

    return surfaceFormId
  }
}
