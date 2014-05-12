package org.idio.dbpedia.spotlight.utils

import org.idio.dbpedia.spotlight.CustomSpotlightModel
/**
 * Allows to update a Model (Sf, DbpediaResources, ContextWords) from a file
 * The format of each line of the file is:
 * dbpediaURI tab surfaceForm1|surfaceForm2... tab contextW1|contextW2.. tab contextW1Count|contextW2Count..
 * Created by dav009 on 03/01/2014.
 */
class ModelUpdateFromFile(pathToModelFolder: String, pathToFile: String) {

  /*
  * loads everything using the entries in the file.
  * and exports a new model.
  * if there is no context.mem it will load just the SF, and Dbpedia Resources.
  * */
  def loadNewEntriesFromFile() {

    val fileParser = new ModelFileParser(this.pathToFile)
    println("Parsing INPUT-FILE")

    val (setOfUpperCaseSF,
         setOfLowerCaseSF,
         setOfDbpediaURIS,
         lowerSfMap,
         parsedLines, setOfContextWords) = fileParser.parseFile()

    println("Finished parsing INPUT-FILE")

    var customSpotlightModel: CustomSpotlightModel = new CustomSpotlightModel(this.pathToModelFolder)

    // trying to add all set of SF's in a single go, so that the reverseMaps are just built once.
    println("adding SFs")
    customSpotlightModel.addSetOfSurfaceForms(setOfUpperCaseSF ++ setOfLowerCaseSF)

    val contextFileWriter = new java.io.PrintWriter(this.pathToFile + "_just_context")

    parsedLines.foreach { parsedLine: Entry =>

      // Gathering the UpperCaseSfs with the lower cases SF's in Store
      val allSFBindsToTopics = parsedLine.upperCaseSurfaceForms ++ parsedLine.lowerCaseSF

      allSFBindsToTopics.foreach { surfaceForm: String =>
        println("SF: " + surfaceForm)
        println("Topic: " + parsedLine.dbpediaURI)
        println("Types: " + parsedLine.types.mkString(" "))
        println("Context: " + parsedLine.contextWordsArray.mkString(" "))

        // Updates the model connecting Sf-> Topic
        // Topic -> Contxt Words
        // Context words -> Context counts
        val (surfaceFormId, dbpediaResourceId) = customSpotlightModel.addNew(surfaceForm,
          parsedLine.dbpediaURI,
          parsedLine.types,
          parsedLine.contextWordsArray,
          parsedLine.contextCounts)

        contextFileWriter.println(dbpediaResourceId + "\t" + parsedLine.contextWordsArray.mkString("|") + "\t"
                                        + parsedLine.contextCounts.mkString("|"))

        println("----------------------------")
      }

    }
    contextFileWriter.close()
    println("serializing the new model.....")
    customSpotlightModel.exportModels(this.pathToModelFolder)
    println("finished serializing the new model.....")
  }

}
