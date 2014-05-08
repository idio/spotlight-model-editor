package org.idio.dbpedia.spotlight

/**
 * Created by dav009 on 19/12/2013.
 */
import org.idio.dbpedia.spotlight.utils._
import org.dbpedia.spotlight.db.memory.MemoryOntologyTypeStore;

object Main {

  def getSpotlightModel(pathToSpotlightModelFolder: String): CustomSpotlightModel = {
    var spotlightModelReader = new CustomSpotlightModel(pathToSpotlightModelFolder)
    return spotlightModelReader
  }

  def main(args: Array[String]) {

    val commandLineParser = new CommandLineParser()
    val parsingResult:Option[CommandLineConfig] = commandLineParser.parse(args)

    parsingResult match {
      case Some(commandLineConfig) => runCommand(commandLineConfig)
      case None => println("please enter a valid command...")
    }
  }

  def runCommand(commandLineConfig:CommandLineConfig){

    val mainCommand: String = commandLineConfig.commands(0)
    val subCommand: String = commandLineConfig.commands.lift(1).getOrElse("")
    val pathToModelFolder: String = commandLineConfig.pathToModelFolder

    lazy val spotlightModelReader = Main.getSpotlightModel(commandLineConfig.pathToModelFolder)

    (mainCommand, subCommand) match{

      // makes a piped(|) separated list of SF spottable.
      // this is done boosting its annotationProbability
      case ("surfaceform", "make-spottable") => {

        val surfaceTexts:Array[String] = {
          if(commandLineConfig.file){
            commandLineConfig.argument.split('|')
          }else{
            val sourceFile = scala.io.Source.fromFile(commandLineConfig.argument)
            sourceFile.getLines().toArray.map{ line =>
              val surfaceForm = line.trim()
              surfaceForm
            }

          }
        }

        surfaceTexts.foreach{ surfaceForm:String =>
          println("making  spottable:"+ surfaceForm)
        }
        surfaceTexts.foreach(spotlightModelReader.makeSFSpottable)
        println("exporting new model.....")
        spotlightModelReader.exportModels(pathToModelFolder)
      }

      // makes a piped(|) separated list of SF not spottable.
      // this is done reducing its annotationProbability
      case ("surfaceform", "make-no-spottable") => {

        val surfaceTexts:Array[String] = {
           if(commandLineConfig.file){
             commandLineConfig.argument.split('|')
           }else{
             val sourceFile = scala.io.Source.fromFile(commandLineConfig.argument)
             sourceFile.getLines().toArray.map{ line =>
               val surfaceForm = line.trim()
               surfaceForm
             }
           }
        }

        surfaceTexts.foreach{ surfaceForm:String =>
          println("making not spottable:"+ surfaceForm)
        }
        surfaceTexts.foreach(spotlightModelReader.makeSFNotSpottable)
        println("exporting new model.....")
        spotlightModelReader.exportModels(pathToModelFolder)
      }

      // show statistics about a sf
      case ("surfaceform", "stats") => {
        val surfaceText = commandLineConfig.argument
        println("getting statistics for surfaceText.....")
        spotlightModelReader.getStatsForSurfaceForm(surfaceText)
      }

      // shows the candidates of a surface form
      case ("surfaceform", "candidates") => {
        val surfaceForm: String = commandLineConfig.argument
        val topicUris = spotlightModelReader.getCandidates(surfaceForm)
        println("Candidate Topics for SF: " + surfaceForm)
        topicUris.foreach({ topicUri: String => println("\t" + topicUri) })
      }

      // copies the candidates of a sf to another sf
      case ("surfaceform", "copy-candidates") => {
        val sourceFile = scala.io.Source.fromFile(commandLineConfig.argument)
        sourceFile.getLines().foreach{ line =>
          val Array(sourceSurfaceForm, destinySurfaceForm) = line.trim().split("\t")
          spotlightModelReader.copyCandidates(sourceSurfaceForm, destinySurfaceForm)
        }
        println("exporting new model.....")
        spotlightModelReader.exportModels(pathToModelFolder)
      }

      /*
      * Removes all the context words and context counts of a dbepdia topic
      * and sets the context words and cotnext counts specified in the command line
      * */
      case("topic", "clean-set-context") => {

        val sourceFile = scala.io.Source.fromFile(commandLineConfig.argument)
        sourceFile.getLines().foreach{ line =>
          val Array(dbpediaURI, contextWords, contextCounts) = line.trim().split("\t")
          println("context words for.." + dbpediaURI + " will be deleted")
          println("context words for.." + dbpediaURI + " will be set as given in input")
          val integerContextCounts = contextCounts.split("|").map(_.toInt)
          spotlightModelReader.replaceAllContext(dbpediaURI, contextWords.split("|"), integerContextCounts)
        }

        println("exporting new model.....")
        spotlightModelReader.exportModels(pathToModelFolder)
      }

      //show context words
      case("topic", "check-context") => {
        val dbpediaURIS = commandLineConfig.argument.split('|')
        dbpediaURIS.foreach(spotlightModelReader.prettyPrintContext)
      }

      // checks whether a dbpedia URI exists or not
      case("topic", "search") => {
        val dbpediaURI = commandLineConfig.argument
        val searchResult: Boolean = spotlightModelReader.searchForDBpediaResource(dbpediaURI)
        if (searchResult) {
          println(dbpediaURI + " exists")
        } else {
          println(dbpediaURI + " NOT FOUND")
        }
      }


      //checks existence of Dbpedia's Ids, SF, and links between SF's and Dbpedia's ids.
      case("association", "remove-association") => {

        val pathToFileWithSFTopicPairs = commandLineConfig.argument
        val sourceFile = scala.io.Source.fromFile(pathToFileWithSFTopicPairs)

        sourceFile.getLines().foreach { line =>
          val splittedLine = line.trim().split("\t")
          val dbpediaURI = splittedLine(0)
          val surfaceFormText = splittedLine(1)
          spotlightModelReader.removeAssociation(surfaceFormText, dbpediaURI)
          println("removed association: " + dbpediaURI + " -- " + surfaceFormText)
        }
        println("exporting new model.....")
        spotlightModelReader.exportModels(pathToModelFolder)
      }

      // exports context to a file
      case("context", "export") =>{
        println("exporting contexts.....")
        val pathToFile = commandLineConfig.argument
        spotlightModelReader.exportContextStore(pathToFile)
      }

      // outputs the properties for 40 Surface forms.
      case("explore", "") =>{
        spotlightModelReader.showSomeSurfaceForms()
      }

      // update model from file
      case("file-update", "all") => {
        val pathToFileWithAdditions = commandLineConfig.argument
        val modelUpdater: ModelUpdateFromFile = new ModelUpdateFromFile(pathToModelFolder, pathToFileWithAdditions)
        modelUpdater.loadNewEntriesFromFile()
      }

      // update context words from file
      case("file-update", "only-context") => {
        val pathToFileWithAdditions =  commandLineConfig.argument
        val modelUpdater: ContextUpdateFromFile = new ContextUpdateFromFile(pathToModelFolder, pathToFileWithAdditions)
        modelUpdater.loadContextWords()
      }

      //checks existence of Dbpedia's Ids, SF, and links between SF's and Dbpedia's ids.
      case("file-update", "check") => {
        val pathToFileWithResources = commandLineConfig.argument
        val modelExplorer: ModelExplorerFromFile = new ModelExplorerFromFile(pathToModelFolder, pathToFileWithResources)
        modelExplorer.checkEntitiesInFile()
      }

    }

  }




  }

