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

package org.idio.dbpedia.spotlight.stores


import org.dbpedia.spotlight.db.memory.{ MemoryStore, MemoryResourceStore }
import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.exceptions.DBpediaResourceNotFoundException
import org.idio.dbpedia.spotlight.stores.CustomQuantiziedCountStore

class CustomDbpediaResourceStore(val pathtoFolder: String,
                                 val countStore: CustomQuantiziedCountStore) extends QuantiziedMemoryStore {

  quantizedCountStore = countStore
  val resourceFile = new FileInputStream(new File(pathtoFolder, "res.mem"))
  var resStore: MemoryResourceStore = MemoryStore.loadResourceStore(resourceFile, quantizedCountStore.quantizedStore)

  /*
  * Creates the specified DbpediaResource in the internal Arrays
  * */
  private def addDbpediaURI(uri: String, support: Int) {
    //URI i.e: Click-through_rate
    //Types: TODO: Currently we don't handle the types as they should be

    val quantizedCounts:Short= getQuantiziedCounts(support)
    // Adds support for new Id
    this.resStore.supportForID = Array concat (resStore.supportForID, Array(quantizedCounts))
    // Adds uri for new Id
    this.resStore.uriForID = Array concat (resStore.uriForID, Array(uri))

    // Adds an empty type array for the new ID
    this.resStore.typesForID = Array concat (this.resStore.typesForID,  Array(Array[java.lang.Short]()))

  }

  def setSupport(resourceId:Int, support:Int){
    this.resStore.supportForID(resourceId) = getQuantiziedCounts(support)
  }

  /*
  * Checks if a given dpbediaID(URI) exists in the resource store
  * if it doesnt it creates it and returns its id
  * if it exists it returns its id
  * */
  def getAddDbpediaResource(uri: String, support: Int, types: Array[String]): Int = {
    try {
      val resourceID = this.resStore.getResourceByName(uri).id
      println("\tfound dbpedia resource for:" + uri + "--" + resourceID)
      setOntologyTypes(resourceID, types)
      return resourceID
    } catch {

      case e: DBpediaResourceNotFoundException => println("creating dbpedia Resource...")
    }
    val resourceID = this.addDbpediaResource(uri, support, types)
    println("\tcreated dbpedia Resource for: " + uri + "--" + resourceID)
    return resourceID
  }

  /*
  * Adds a new Dbpedia Resource to the store, and rebuilds the indexes.
  * It does NOT check previous existance
  * */
  def addDbpediaResource(uri: String, support: Int, types: Array[String]): Int = {

    // add the dbpedia URI to the arrays
    this.addDbpediaURI(uri, support)

    //update internal indexes
    this.resStore.createReverseLookup()

    // Adds types for new ID
    setOntologyTypes(this.resStore.getResourceByName(uri).id, types)

    return this.resStore.getResourceByName(uri).id
  }

  def printAllSupportValues() {
    this.resStore.idFromURI.keySet().toArray.foreach { uri =>
      val id = this.resStore.idFromURI.get(uri)
      val support = getCountFromQuantiziedValue(this.resStore.supportForID(id))
      println(uri + "\t" + support)
    }
  }

  /*
  * Given a list of string of types it return a list wth types ids:
  * i.e: [dbpdia:person, dbpedia:location] => [100, 392]..
  * */
  def getTypesIds(dbpediaTypes: Array[String]): Array[java.lang.Short] = {
    //var dbpediaTypesForResource2 = new Array[Array[java.lang.Short]](1)
    var dbpediaTypesForResource: Array[java.lang.Short] = new Array[java.lang.Short](dbpediaTypes.length)

    for (i <- 0 to dbpediaTypes.length - 1) {
      var currentType: String = dbpediaTypes(i)
      if (!currentType.equals("")) {

        try dbpediaTypesForResource(i) = resStore.ontologyTypeStore.getOntologyTypeByName(currentType).id
        catch {
          case ex: NullPointerException => {println("Could not find a type in Ontology Store for: " + currentType)}
        }
      }
    }

    //dbpediaTypesForResource2(0) = dbpediaTypesForResource.filter(_ != null)
    return dbpediaTypesForResource.filter(_ != null)
  }

  def setOntologyTypes(resourceID:Int, ontologyTypes:Array[String]) {
    this.resStore.typesForID(resourceID)  = (this.resStore.typesForID(resourceID) ++ this.getTypesIds(ontologyTypes)).distinct
  }


}
