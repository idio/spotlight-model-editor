package org.idio.dbpedia.spotlight

/**
 * Created by dav009 on 19/12/2013.
 */
import org.dbpedia.spotlight.db.memory.{MemoryResourceStore,MemoryStore,MemoryCandidateMapStore,MemorySurfaceFormStore}
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import java.io.{FileInputStream, File}



object Main{
  def main(args:Array[String]){
    val action:String= args(0)
    val pathToModelFolder = args(1)
    println("reading models...")
    val spotlightModelReader = new IdioSpotlightModel(pathToModelFolder)
    println("looking for click through rate")
    //var dbpediaResource = spotlightModelReader.resStore.getResourceByName("Click-through_rate")
    //println(dbpediaResource.id)
    //println(dbpediaResource.prior)

    if (action.equals("check")){
      val surfaceText = args(2)
      println("getting statistics for surfaceText.....")
      spotlightModelReader.getStatsForSurfaceForm(surfaceText)
    }

    if (action.equals("search")){
      val dbpediaURI = args(2)
      println("getting statistics for surfaceText.....")
      val searchResult:Boolean = spotlightModelReader.searchForDBpediaResource(dbpediaURI)
      if (searchResult){
        println(dbpediaURI+" exists")
      }else{
        println(dbpediaURI+" NOT FOUND")
      }


    }

    //attach surface form and topic, if they dont exist they are created
    if (action.equals("update")){
      val surfaceForm = args(2)
      val dbpediaURI = args(3)

      var types = Array[String]()
      if (args.length ==5)
        types = args(4).split("|")

      println("addding new sf and concept")
      spotlightModelReader.addNew(surfaceForm,dbpediaURI,1,types)
      println("getting the stats for the new surfaceForm")
      spotlightModelReader.getStatsForSurfaceForm(surfaceForm)

      //exporting for testing purpouses
      spotlightModelReader.exportModels("/Users/dav009/IdeaProjects/untitled/out/artifacts/untitled_jar")
    }


    //boost the values for a surfaceForm and a topic
    if (action.equals("boost")){
      val surfaceForm = args(2)
      val dbpediaURI = args(3)
      val boostValue = args(4).toInt

      println("statistics before the boost...")
      spotlightModelReader.getStatsForSurfaceForm(surfaceForm)

      spotlightModelReader.boostValue(surfaceForm, dbpediaURI, boostValue)

      println("statistics after the boost..")
      spotlightModelReader.getStatsForSurfaceForm(surfaceForm)

    }

    if (action.equals("explore")){
      spotlightModelReader.showSomeSurfaceForms()
    }


  }
}
