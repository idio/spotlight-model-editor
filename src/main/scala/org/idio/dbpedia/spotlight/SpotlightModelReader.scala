package org.idio.dbpedia.spotlight

/**
 * Created by dav009 on 19/12/2013.
 */
import org.dbpedia.spotlight.db.memory.{MemoryResourceStore,MemoryStore,MemoryCandidateMapStore,MemorySurfaceFormStore}
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import org.idio.dbpedia.spotlight.utils.{ContextUpdateFromFile, ModelUpdateFromFile}
import java.io.{FileInputStream, File}



object Main{

  def getSpotlightModel(pathToSpotlightModelFolder:String):IdioSpotlightModel = {
    var spotlightModelReader = new IdioSpotlightModel(pathToSpotlightModelFolder)
    return spotlightModelReader
  }

  def main(args:Array[String]){
    val action:String= args(0)
    val pathToModelFolder = args(1)

    if (!action.contains("file")){
      // reads the dbpedia models
      println("reading models...")
      val spotlightModelReader =  Main.getSpotlightModel(pathToModelFolder)

      // get the statistics for a surface form
      if (action.equals("check")){
        val surfaceText = args(2)
        println("getting statistics for surfaceText.....")
        spotlightModelReader.getStatsForSurfaceForm(surfaceText)
      }

      //show context words
      if (action.equals("show-context")){
        val dbpediaURI = args(2)
        spotlightModelReader.prettyPrintContext(dbpediaURI)
      }

      // checks whether a dbpedia URI exists or not
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
        val contextWords = Array[String]()
        val contextCounts = Array[Int]()
        spotlightModelReader.addNew(surfaceForm,dbpediaURI,types,contextWords,contextCounts)
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

      // outputs the properties for 40 Surface forms.
      if (action.equals("explore")){
        spotlightModelReader.showSomeSurfaceForms()
      }
    }else{
      // update model from file
      if (action.equals("file-update-sf-dbpedia")){
        val pathToModel = args(2)
        val pathToFileWithAdditions = args(3)
        val modelUpdater:ModelUpdateFromFile = new ModelUpdateFromFile(pathToModel, pathToFileWithAdditions)
        modelUpdater.loadNewEntriesFromFile()
      }

      // update context words from file
      if (action.equals("file-update-context")){
        val pathToModel = args(2)
        val pathToFileWithAdditions = args(3)
        val modelUpdater:ContextUpdateFromFile = new ContextUpdateFromFile(pathToModel, pathToFileWithAdditions)
        modelUpdater.loadContextWords()
      }

    }

  }
}
