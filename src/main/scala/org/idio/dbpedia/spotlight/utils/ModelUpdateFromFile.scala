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

package org.idio.dbpedia.spotlight.utils

import org.idio.dbpedia.spotlight.CustomSpotlightModel
import scala.collection.immutable

/**
 * Allows to update a Model (Sf, DbpediaResources, ContextWords) from a file
 * The format of each line of the file is:
 * dbpediaURI tab surfaceForm1|surfaceForm2... tab contextW1|contextW2.. tab contextW1Count|contextW2Count..
 */
class ModelUpdateFromFile(pathToModelFolder: String, pathToFile: String) {

  /*
  * loads everything using the entries in the file.
  * and exports a new model.
  * if there is no context.mem it will load just the SF, and Dbpedia Resources.
  * */
  def loadNewEntriesFromFile() {

    val fileParser = new ModelFileParser(this.pathToFile)
    println("Parsing " + this.pathToFile )

    val (setOfUpperCaseSF,
         setOfLowerCaseSF,
         setOfDbpediaURIS,
         lowerSfMap,
         parsedLines, setOfContextWords) = fileParser.parseFile()

    println("Finished parsing .." + this.pathToFile)

    var customSpotlightModel: CustomSpotlightModel = new CustomSpotlightModel(this.pathToModelFolder)

    //the SurfaceFormStore might internally contain lowercases already!
    //This checks if some of the lowercases being added are in stringForID
    val lowerCasesAlreadyInMainSFStore:immutable.HashMap[String,Int] =
                                                customSpotlightModel.getLowerCasesSFInStore(lowerSfMap.keySet.toSet)


    // trying to add all set of SF's in a single go, so that the reverseMaps are just built once.
    println("adding SFs to Main SF Store")
    customSpotlightModel.addSetOfSurfaceForms(setOfUpperCaseSF ++ lowerCasesAlreadyInMainSFStore.keySet)

    println("adding Lowercase SFs to lowercase store")
    // adding lower case SF, rebuilding lowercase map
    customSpotlightModel.addMapOfLowerCaseSurfaceForms(lowerSfMap)

    val contextFileWriter = new java.io.PrintWriter(this.pathToFile + "_just_context")

    parsedLines.foreach { parsedLine: Entry =>

       //lowercases Sf existing in the main SurfaceForm Store need to add the topic as candidate
       val lineLowerCaseSFsInMainStore = parsedLine.lowerCaseSF.map{ lowerCaseSurfaceForm:String =>
        lowerCasesAlreadyInMainSFStore.get(lowerCaseSurfaceForm) match {
          case Some(sfId) => Option[String](lowerCaseSurfaceForm)
          case None => None
         }
       }.flatten

       // Gathering the UpperCaseSfs with the lower cases SF's in Store
       val allSFBindsToTopics = parsedLine.upperCaseSurfaceForms ++ lineLowerCaseSFsInMainStore



      allSFBindsToTopics.foreach { surfaceForm: String =>
        println("SF: " + surfaceForm)
        println("Topic: " + parsedLine.dbpediaURI)
        println("Types: " + parsedLine.types.mkString(" "))
        println("Context: " + parsedLine.contextWordsArray.mkString(" "))

        // Updates the model connecting Sf-> Topic
        // Topic -> Context Words
        // Context words -> Context counts
        val (surfaceFormId, dbpediaResourceId) = customSpotlightModel.addNew(
          surfaceForm,
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
