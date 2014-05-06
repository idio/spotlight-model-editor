package org.idio.dbpedia.spotlight.utils

import scala.collection.mutable.HashSet

/*
* Represents a parsed Line of the input file
* */
class Entry(val upperCaseSurfaceForms:Array[String],val dbpediaURI:String,val types:Array[String],val contextWordsArray:Array[String],val contextCounts:Array[Int], val lowerCaseSF:Array[String]){

}

/**
 * Parses a Model File.
 * Extracting the SF-Topics relations, Topics-ContextWords relations
 * Created by dav009 on 03/01/2014.
 */
class ModelFileParser(pathToFile:String){


  // All uppercase SFs
  val setOfSurfaceForms:HashSet[String] = new HashSet[String]()

  // All lower case SFs attached to the uppercase SFs
  val lowerCasesMap:collection.mutable.HashMap[String, Array[String]] = new collection.mutable.HashMap[String, Array[String]]()

  // All dbpedia URis
  val setOfDbpediaURIS:HashSet[String] = new HashSet[String]()

  val setOfContextWords:HashSet[String] = new HashSet[String]()

  // The parsed information per line
  val parsedLines:scala.collection.mutable.ArrayBuffer[Entry] = new scala.collection.mutable.ArrayBuffer[Entry]()


  /*
* Parses an input line.
* Returns the SurfaceForm, DbpediaID, Types, ContextWords, ContextCounts
* */
  private def parseLine(line:String):(Array[String], String, Array[String], Array[String], Array[Int]) = {
    val splittedLine = line.trim.split("\t")

    // Get the data from the splitted line
    val dbpediaURI = splittedLine(0)
    val surfaceForms = splittedLine(1).split('|')
    val types = new Array[String](0)
    var contextWordsArray = new Array[String](0)
    var contextCounts = new Array[Int](0)

    // If the line contains context words and context counts
    if (splittedLine.size>2){
      val contextStringCounts = splittedLine(3).split('|')
      contextWordsArray = splittedLine(2).split('|')

      // Cast Context Counts to Integers
      contextCounts = contextStringCounts.map(_.toInt)
    }

    // get a set of the surfaceForms(get rid of repeated elements)
    val allSurfaceForms=HashSet[String](surfaceForms:_*)

    allSurfaceForms.remove("")

    (allSurfaceForms.toArray, dbpediaURI, types, contextWordsArray, contextCounts)
  }

  /*
  * Gets all the SF parsed in a line of the input file.
  * It separates the SF in the given line among Uppercase and lowercase.
  * Lowercase SF's of the uppercases Sf are also generated
  * */
  private def expandSurfaceFormsFromLine(setOfSurfaceForms:Array[String]):(HashSet[String],HashSet[String])={
    val subSetOflowerCaseFormsForLine = new HashSet[String]()
    val subSetOfUpperCaseFormsForLine = new HashSet[String]()
    setOfSurfaceForms.foreach{ surfaceForm =>
      val hasUpperLetter:Boolean = surfaceForm.exists(_.isUpper)
      if (hasUpperLetter){
        // if it has at least one letter being uppercase
        subSetOfUpperCaseFormsForLine.add(surfaceForm)
      }
      // adds the lower case form of the surfaceForm
      subSetOflowerCaseFormsForLine.add(surfaceForm.toLowerCase)
    }

    return (subSetOflowerCaseFormsForLine, subSetOfUpperCaseFormsForLine)
  }

  /*
  * Adds the lowercases in setOfLowerCaseSF to the map of lowerCases.
  * Attach each lowercase to the list defined in: setOfUpperCaseSF
  * */
  private def updateLowercaseMap(setOfLowerCaseSF:HashSet[String], setOfUpperCaseSF:HashSet[String]){
    setOfLowerCaseSF.foreach{
      lowercaseSf =>
        if (lowercaseSf!=""){
          val currentBindForLowerCaseSF = lowerCasesMap.getOrElse(lowercaseSf, Array[String]())
          lowerCasesMap.put(lowercaseSf, HashSet[String]((currentBindForLowerCaseSF++setOfUpperCaseSF):_*).toArray)
          if(lowerCasesMap.get(lowercaseSf).get.size < 1){
            println("warning...this SF won't be able to be matched to an Uppercase SF")
            println("\t\""+lowercaseSf+"\"")
            println("\tsize:"+lowerCasesMap.get(lowercaseSf).size)
          }
        }
    }
  }


  /*
 * Parses the input File and outputs a set of SF and dbpedia URIs
 * */
  def parseFile():(HashSet[String], HashSet[String], collection.mutable.HashMap[String, Array[String]], scala.collection.mutable.ArrayBuffer[Entry], collection.mutable.HashSet[String])={


    val source = scala.io.Source.fromFile(this.pathToFile)
    val lines = source.getLines()

    lines.foreach{ line =>
      val (surfaceForms, dbpediaURI, types, contextWordsArray, contextCounts) = this.parseLine(line)

      //get all SurfaceForms expanding them to lower and uppercases and separating them in two sets
      val (subSetOflowerCaseFormsForLine:HashSet[String],
      subSetOfUpperCaseFormsForLine:HashSet[String]) = expandSurfaceFormsFromLine(surfaceForms)

      this.parsedLines +=  new Entry(subSetOfUpperCaseFormsForLine.toArray, dbpediaURI,
        types, contextWordsArray, contextCounts, subSetOflowerCaseFormsForLine.toArray)

      // adding the uppercase SF found in the line to the overall set
      this.setOfSurfaceForms ++= subSetOfUpperCaseFormsForLine

      // Update the lowercaseMap and their bindings (lowercaseSF -> [UppercaseForms..]
      this.updateLowercaseMap(subSetOflowerCaseFormsForLine, subSetOfUpperCaseFormsForLine)


      this.setOfContextWords ++=contextWordsArray

      //get all DbpediaUris into a Set
      this.setOfDbpediaURIS.add(dbpediaURI)
    }

    source.close()

    return (this.setOfSurfaceForms, this.setOfDbpediaURIS, lowerCasesMap, this.parsedLines, this.setOfContextWords)
  }

}