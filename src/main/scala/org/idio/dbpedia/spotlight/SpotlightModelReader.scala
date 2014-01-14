package org.idio.dbpedia.spotlight

/**
 * Created by dav009 on 19/12/2013.
 */
import org.dbpedia.spotlight.db.memory.{MemoryResourceStore,MemoryStore,MemoryCandidateMapStore,MemorySurfaceFormStore}
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import org.idio.dbpedia.spotlight.utils.{ContextUpdateFromFile, ModelUpdateFromFile, ModelExplorerFromFile}
import org.dbpedia.spotlight.db.memory.MemoryOntologyTypeStore;
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

      action match{

        // prints all types in type store
        case "show-resource-types" =>{
          val typeStore = spotlightModelReader.idioDbpediaResourceStore.resStore.ontologyTypeStore.asInstanceOf[MemoryOntologyTypeStore]
          for(ontologyType<-typeStore.idFromName.keySet().toArray){
            println(ontologyType)
          }
        }

        // get the statistics for a surface form
        case "check" =>{
          val surfaceText = args(2)
          println("getting statistics for surfaceText.....")
          spotlightModelReader.getStatsForSurfaceForm(surfaceText)
        }

        //show context words
        case "show-context" =>{
          val dbpediaURI = args(2)
          spotlightModelReader.prettyPrintContext(dbpediaURI)
        }

        /*
        * Removes all the context words and context counts of a dbepdia topic
        * and sets the context words and cotnext counts specified in the command line
        * */
        case "clean-set-context" =>{
          var dbpediaURI = args(2)
          var contextWords = args(3).split('|')
          var contextCounts =args(4).split('|') map(_.toInt)
          println("context words for.."+dbpediaURI+" will be deleted")
          println("context words for.."+dbpediaURI+" will be set as given in input")
          spotlightModelReader.replaceAllContext(dbpediaURI, contextWords, contextCounts)
          println("exporting new model.....")
          spotlightModelReader.exportModels(pathToModelFolder)
        }

        // checks whether a dbpedia URI exists or not
        case "search" =>{
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
        case "update" =>{
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
        case "boost" =>{
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
        case "explore" =>{
          spotlightModelReader.showSomeSurfaceForms()
        }
      }

    }else{

        action match{

              // update model from file
              case "file-update-sf-dbpedia" => {
                val pathToFileWithAdditions = args(2)
                val modelUpdater:ModelUpdateFromFile = new ModelUpdateFromFile(pathToModelFolder, pathToFileWithAdditions)
                modelUpdater.loadNewEntriesFromFile()
              }

              // update context words from file
              case "file-update-context" =>{
                val pathToFileWithAdditions = args(2)
                val modelUpdater:ContextUpdateFromFile = new ContextUpdateFromFile(pathToModelFolder, pathToFileWithAdditions)
                modelUpdater.loadContextWords()
              }

              //checks existence of Dbpedia's Ids, SF, and links between SF's and Dbpedia's ids.
              case "file-check" =>{
                val pathToFileWithResources = args(2)
                val modelExplorer:ModelExplorerFromFile = new ModelExplorerFromFile(pathToModelFolder, pathToFileWithResources)
                modelExplorer.checkEntitiesInFile()
              }

        }

    }

  }
}
