package org.idio.dbpedia.spotlight

/**
 * Created by dav009 on 19/12/2013.
 */
import org.idio.dbpedia.spotlight.utils.{ ContextUpdateFromFile, ModelUpdateFromFile, ModelExplorerFromFile }
import org.dbpedia.spotlight.db.memory.MemoryOntologyTypeStore;

object Main {

  def getSpotlightModel(pathToSpotlightModelFolder: String): CustomSpotlightModel = {
    var spotlightModelReader = new CustomSpotlightModel(pathToSpotlightModelFolder)
    return spotlightModelReader
  }

  def main(args: Array[String]) {
    val action: String = args(0)
    val pathToModelFolder = args(1)

    if (!action.contains("file")) {
      // reads the dbpedia models
      println("reading models...")
      val spotlightModelReader = Main.getSpotlightModel(pathToModelFolder)

      action match {

        case "export-context" => {
          println("exporting contexts.....")
          val pathToFile = args(2)
          spotlightModelReader.exportContextStore(pathToFile)
        }

        case "export-support" => {
          spotlightModelReader.customDbpediaResourceStore.printAllSupportValues()
        }

        // prints all types in type store
        case "show-resource-types" => {
          val typeStore = spotlightModelReader.customDbpediaResourceStore.resStore.ontologyTypeStore.asInstanceOf[MemoryOntologyTypeStore]
          typeStore.idFromName.keySet().toArray.foreach { ontologyType =>
            println(ontologyType)
          }
        }

        case "show-candidates" => {
          val surfaceForm: String = args(2)
          val topicUris = spotlightModelReader.getCandidates(surfaceForm)
          println("Candidate Topics for SF: " + surfaceForm)
          topicUris.foreach({ topicUri: String => println("\t" + topicUri) })
        }

        // get the statistics for a surface form
        case "check-sf" => {
          val surfaceText = args(2)
          println("getting statistics for surfaceText.....")
          spotlightModelReader.getStatsForSurfaceForm(surfaceText)
        }

        //show context words
        case "show-context" => {
          val dbpediaURIS = args(2).split('|')

          dbpediaURIS.foreach(spotlightModelReader.prettyPrintContext)
        }

        // makes a piped(|) separated list of SF not spottable.
        // this is done reducing its annotationProbability
        case "make-sf-not-spottable" => {
          val surfaceTexts = args(2).split('|')
          surfaceTexts.foreach(spotlightModelReader.makeSFNotSpottable)
          spotlightModelReader.exportModels(pathToModelFolder)
        }

        // Reads form a file a list of sf and make them not spottable
        case "make-sf-not-spottable-from-list" => {

          val pathToFileWithBannedSF = args(2)
          val sourceFile = scala.io.Source.fromFile(pathToFileWithBannedSF)

          sourceFile.getLines().foreach { line =>
            val surfaceForm = line.trim()
            spotlightModelReader.makeSFNotSpottable(surfaceForm)
            println("reduced surfaceForm counts for: " + surfaceForm)
          }

          spotlightModelReader.exportModels(pathToModelFolder)
        }

        // makes a piped(|) separated list of SF spottable.
        // this is done boosting its annotationProbability
        case "make-sf-spottable" => {
          val surfaceTexts = args(2).split('|')
          surfaceTexts.foreach(spotlightModelReader.makeSFSpottable)
          spotlightModelReader.exportModels(pathToModelFolder)
        }

        /*
        * Removes all the context words and context counts of a dbepdia topic
        * and sets the context words and cotnext counts specified in the command line
        * */
        case "clean-set-context" => {
          var dbpediaURI = args(2)
          var contextWords = args(3).split('|')
          var contextCounts = args(4).split('|') map (_.toInt)
          println("context words for.." + dbpediaURI + " will be deleted")
          println("context words for.." + dbpediaURI + " will be set as given in input")
          spotlightModelReader.replaceAllContext(dbpediaURI, contextWords, contextCounts)
          println("exporting new model.....")
          spotlightModelReader.exportModels(pathToModelFolder)
        }

        // checks whether a dbpedia URI exists or not
        case "search-topic" => {
          val dbpediaURI = args(2)
          println("getting statistics for surfaceText.....")
          val searchResult: Boolean = spotlightModelReader.searchForDBpediaResource(dbpediaURI)
          if (searchResult) {
            println(dbpediaURI + " exists")
          } else {
            println(dbpediaURI + " NOT FOUND")
          }
        }

        /*
          takes all topic candidates for the surfaceForm1
          and associate them to surfaceForm2.
          Assumes that both SurfaceForms exists in the model
        */
        case "copy-candidates" => {
          val surfaceFormTextSource = args(2)
          val surfaceFormTextDestiny = args(3)

          spotlightModelReader.copyCandidates(surfaceFormTextSource, surfaceFormTextDestiny)
          spotlightModelReader.exportModels(pathToModelFolder)
        }

        //checks existence of Dbpedia's Ids, SF, and links between SF's and Dbpedia's ids.
        case "remove-sf-topic-association" => {
          val pathToFileWithSFTopicPairs = args(2)
          val sourceFile = scala.io.Source.fromFile(pathToFileWithSFTopicPairs)

          sourceFile.getLines().foreach { line =>
            val splittedLine = line.trim().split("\t")
            val dbpediaURI = splittedLine(0)
            val surfaceFormText = splittedLine(1)
            spotlightModelReader.removeAssociation(surfaceFormText, dbpediaURI)
            println("removed association: " + dbpediaURI + " -- " + surfaceFormText)
          }
          spotlightModelReader.exportModels(pathToModelFolder)
        }

        // outputs the properties for 40 Surface forms.
        case "explore" => {
          spotlightModelReader.showSomeSurfaceForms()
        }
      }

    } else {

      action match {

        // update model from file
        case "file-update-sf-dbpedia" => {
          val pathToFileWithAdditions = args(2)
          val modelUpdater: ModelUpdateFromFile = new ModelUpdateFromFile(pathToModelFolder, pathToFileWithAdditions)
          modelUpdater.loadNewEntriesFromFile()
        }

        // update context words from file
        case "file-update-context" => {
          val pathToFileWithAdditions = args(2)
          val modelUpdater: ContextUpdateFromFile = new ContextUpdateFromFile(pathToModelFolder, pathToFileWithAdditions)
          modelUpdater.loadContextWords()
        }

        //checks existence of Dbpedia's Ids, SF, and links between SF's and Dbpedia's ids.
        case "file-check" => {
          val pathToFileWithResources = args(2)
          val modelExplorer: ModelExplorerFromFile = new ModelExplorerFromFile(pathToModelFolder, pathToFileWithResources)
          modelExplorer.checkEntitiesInFile()
        }

      }

    }

  }
}
