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
      case None => println("Please enter a valid command...")
    }
  }

  def runCommand(commandLineConfig:CommandLineConfig){

    val mainCommand: String = commandLineConfig.commands(0)
    val subCommand: String = commandLineConfig.commands.lift(1).getOrElse("")
    val pathToModelFolder: String = commandLineConfig.pathToModelFolder

    lazy val spotlightModelReader = Main.getSpotlightModel(pathToModelFolder)

    (mainCommand, subCommand) match{

      case("surfaceform", "make-spottable") | ("surfaceform", "make-unspottable") => {

        val surfaceTexts:Array[String] = {
          if(!commandLineConfig.file){
            commandLineConfig.argument.split('|')
          }else{
            val sourceFile = scala.io.Source.fromFile(commandLineConfig.argument)
            sourceFile.getLines().toArray.map{ _.trim()}
          }
        }

        subCommand match {

            // Force a list of surface forms to be spottable
            case "make-spottable" => {
              surfaceTexts.foreach{ surfaceForm:String =>
                println("Making  spottable:"+ surfaceForm)
                spotlightModelReader.makeSFSpottable(surfaceForm)
              }
            }

            // Lower the prob of a list of surfaceforms
            case "make-unspottable" => {
              surfaceTexts.foreach{ surfaceForm:String =>
                println("Making unspottable:"+ surfaceForm)
                spotlightModelReader.makeSFNotSpottable(surfaceForm)
              }
            }

        }

        println("Exporting new model.....")
        spotlightModelReader.exportModels(pathToModelFolder)
      }

      // Show statistics about an sf
      case ("surfaceform", "stats") => {
        val surfaceText = commandLineConfig.argument
        println("Getting statistics for surfaceText.....")
        spotlightModelReader.getStatsForSurfaceForm(surfaceText)
      }


      // Show the candidates of a surface form
      case ("surfaceform", "candidates") => {
        val surfaceForm: String = commandLineConfig.argument
        val topicUris = spotlightModelReader.getCandidates(surfaceForm)
        println("Candidate Topics for SF: " + surfaceForm)
        topicUris.foreach({ topicUri: String => println("\t" + topicUri) })
      }

      // Copy the candidates of an SF to another Sf
      case ("surfaceform", "copy-candidates") => {
        val sourceFile = scala.io.Source.fromFile(commandLineConfig.argument)
        sourceFile.getLines().foreach{ line =>
          val Array(sourceSurfaceForm, destinationSurfaceForm) = line.trim().split("\t")
          spotlightModelReader.copyCandidates(sourceSurfaceForm, destinationSurfaceForm)
        }
        println("Exporting new model.....")
        spotlightModelReader.exportModels(pathToModelFolder)
      }

      /*
      * Removes all the context words and context counts of a dbepdia topic
      * and sets the context words and context counts specified in the command line
      * */
      case("topic", "clean-set-context") => {

        val sourceFile = scala.io.Source.fromFile(commandLineConfig.argument)
        sourceFile.getLines().toArray.foreach{ line =>
          val Array(dbpediaURI, contextWords, contextCounts) = line.trim().split("\t")
          println("Context words for.." + dbpediaURI + " will be deleted and set as given in input")
          val integerContextCounts = contextCounts.trim().split('|').map(_.toInt)
          spotlightModelReader.replaceAllContext(dbpediaURI, contextWords.trim().split('|'), integerContextCounts)
        }

        println("Exporting new model.....")
        spotlightModelReader.exportModels(pathToModelFolder)
      }

      //show context words
      case("topic", "check-context") => {
        val dbpediaURIS = commandLineConfig.argument.split('|')
        dbpediaURIS.foreach(spotlightModelReader.prettyPrintContext)
      }

      // Check whether a dbpedia URI exists or not
      case("topic", "search") => {
        val dbpediaURI = commandLineConfig.argument
        val searchResult: Boolean = spotlightModelReader.searchForDBpediaResource(dbpediaURI)
        if (searchResult) {
          println(dbpediaURI + " exists")
        } else {
          println(dbpediaURI + " NOT FOUND")
        }
      }


      // Check existence of Dbpedia's Ids, SF, and links between SF's and Dbpedia's ids.
      case("association", "remove") => {

        val pathToFileWithSFTopicPairs = commandLineConfig.argument
        val sourceFile = scala.io.Source.fromFile(pathToFileWithSFTopicPairs)

        sourceFile.getLines().foreach { line =>
          val splitLine = line.trim().split("\t")
          val dbpediaURI = splitLine(0)
          val surfaceFormText = splitLine(1)
          spotlightModelReader.removeAssociation(surfaceFormText, dbpediaURI)
          println("Removed association: " + dbpediaURI + " -- " + surfaceFormText)
        }
        println("Exporting new model.....")
        spotlightModelReader.exportModels(pathToModelFolder)
      }

      case("association", "percentage_context_vector") =>{
        val pathToFileWithSFTopicPairs = commandLineConfig.argument
        val sourceFile = scala.io.Source.fromFile(pathToFileWithSFTopicPairs)

        sourceFile.getLines().foreach { line =>
          val splitLine = line.trim().split("\t")
          val dbpediaURI = splitLine(0)
          val surfaceFormText = splitLine(1)
          val percentageOfContextVector = splitLine(2).toDouble
          spotlightModelReader.updatePercentageOfContextVector(surfaceFormText, dbpediaURI, percentageOfContextVector)
          println("Updating association's support : " + dbpediaURI + " -- " + surfaceFormText + " - to allow new percentage of context vector : " + percentageOfContextVector )
        }
        println("Exporting new model.....")
        spotlightModelReader.exportModels(pathToModelFolder)
      }

      // Export context to a file
      case("context", "export") =>{
        println("Exporting contexts.....")
        val pathToFile = commandLineConfig.argument
        spotlightModelReader.exportContextStore(pathToFile)
      }

      // Output the properties of a defined number of surface forms
      case("explore", "") =>{
        val numberOfSurfaceForms = commandLineConfig.argument.toInt
        spotlightModelReader.showSomeSurfaceForms(numberOfSurfaceForms)
      }

      // update model from file
      case("file-update", "all") => {
        val pathToFileWithAdditions = commandLineConfig.argument
        val modelUpdater: ModelUpdateFromFile = new ModelUpdateFromFile(pathToModelFolder, pathToFileWithAdditions)
        modelUpdater.loadNewEntriesFromFile()
      }

      // Update context words from file
      case("file-update", "context-only") => {
        val pathToFileWithAdditions =  commandLineConfig.argument
        val modelUpdater: ContextUpdateFromFile = new ContextUpdateFromFile(pathToModelFolder, pathToFileWithAdditions)
        modelUpdater.loadContextWords()
      }

      // Check the existence of Dbpedia's Ids, SF, and links between SF's and Dbpedia's ids.
      case("file-update", "check") => {
        val pathToFileWithResources = commandLineConfig.argument
        val modelExplorer: ModelExplorerFromFile = new ModelExplorerFromFile(pathToModelFolder, pathToFileWithResources)
        modelExplorer.checkEntitiesInFile()
      }

      // looking if a sf is spottable by the fsa
      case("fsa", "find") => {
        val surfaceForms = commandLineConfig.argument.split('|')
        surfaceForms.foreach{ surfaceForm: String =>
               val spots: Array[String] = spotlightModelReader.getFSASpots(surfaceForm)
               println("------------------------")
               println(surfaceForm)
               println("\tspots:")
               spots.foreach{
                   spot: String =>
                     println("\t\t" + spot)
               }
               println("--------------------------")
        }

      }

    }

  }

  }
