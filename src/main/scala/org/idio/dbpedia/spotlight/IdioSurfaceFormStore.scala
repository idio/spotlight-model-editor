package org.idio.dbpedia.spotlight

/**
 * Created by dav009 on 23/12/2013.
 */

import org.dbpedia.spotlight.db.memory.{MemoryStore,MemorySurfaceFormStore}
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import java.io.{File, FileInputStream}

class IdioSurfaceFormStore(val pathtoFolder:String){

  var sfStore:MemorySurfaceFormStore = MemoryStore.loadSurfaceFormStore(new FileInputStream(new File(pathtoFolder,"sf.mem")))

  /*
  * Adds a new surfaceForm to the surfaceFormStore
  * */
  def addSurfaceForm(surfaceText:String):Int =  {
    println("largo...")
    println(this.sfStore.stringForID.length)
    this.sfStore.stringForID = this.sfStore.stringForID :+ surfaceText
    println(this.sfStore.stringForID.length)
    // the counts for the new surface form is the avg of the counts for the other surface forms
    this.sfStore.annotatedCountForID = this.sfStore.annotatedCountForID :+ 1
    this.sfStore.totalCountForID = this.sfStore.totalCountForID :+ 1

    // update internal indexes
    println("updating surface form internal indexes...")
    this.sfStore.createReverseLookup()

    var surfaceForm = this.sfStore.getSurfaceForm(surfaceText)

    return surfaceForm.id
  }

  /*
  * returns the id of a surfaceForm, if it doesnt exist, it creates it.
  * */
  def getAddSurfaceForm(surfaceText:String):Int = {

    // look for existing surfaceForm
    try{
      var surfaceForm = this.sfStore.getSurfaceForm(surfaceText)
      return surfaceForm.id
    } catch{

      case e: SurfaceFormNotFoundException => println("creating surface form...")
    }
    // create one in case it cant be found
    var surfaceFormId = this.addSurfaceForm(surfaceText)

    return surfaceFormId
  }
}
