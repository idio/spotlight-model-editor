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

import org.dbpedia.spotlight.db.memory.{ MemoryStore, MemoryContextStore, MemoryTokenTypeStore }
import java.io.{ File, FileInputStream }
import org.idio.dbpedia.spotlight.utils.ArrayUtils


class CustomContextStore(val pathtoFolder: String,
                         val tokenStore: MemoryTokenTypeStore,
                         val countStore: CustomQuantiziedCountStore) extends QuantiziedMemoryStore {

  quantizedCountStore = countStore
  val contextMemFile = new FileInputStream(new File(pathtoFolder, "context.mem"))
  var contextStore: MemoryContextStore = MemoryStore.loadContextStore(contextMemFile, this.tokenStore, quantizedCountStore.quantizedStore)

  /*
  * Creates a context Array for a new DbpediaResource.
  * The default context Array contains a reference to the token with Id: 0.
  * */
  def createDefaultContextStore(dbpediaResourceID: Int) {
    try {
      this.contextStore.counts(dbpediaResourceID)
      this.contextStore.tokens(dbpediaResourceID)
    } catch {
      case ex: Exception => {
        println("\t creating default context array for")
        val defaultQuantiziedCount:Short = getQuantiziedCounts(1)
        this.contextStore.counts = this.contextStore.counts :+ Array[Short](defaultQuantiziedCount)
        this.contextStore.tokens = this.contextStore.tokens :+ Array[Int](0)
      }
    }
  }

  /*
  * adds a token to dbpediaResource's context.
  * It checks taht the token is not already in the dbpedia Resource's Context
  * */
  def addContext(dbpediaResourceID: Int, tokenID: Int, count: Int) {
    // check if the token is already in the context, if so dont do anything.
    if(this.contextStore.tokens(dbpediaResourceID) == null && this.contextStore.tokens(dbpediaResourceID) == null) {
      this.contextStore.tokens(dbpediaResourceID) = Array[Int]()
      this.contextStore.counts(dbpediaResourceID) = Array[Short]()
    }
    if (!(this.contextStore.tokens(dbpediaResourceID) contains tokenID)) {
      val quantiziedCount:Short = getQuantiziedCounts(count)
      this.contextStore.counts(dbpediaResourceID) = this.contextStore.counts(dbpediaResourceID) :+ quantiziedCount
      this.contextStore.tokens(dbpediaResourceID) = this.contextStore.tokens(dbpediaResourceID) :+ tokenID
    }
  }

  /*
  * Empty the context Words and context counts of a dbpedia Topic
  * */
  def cleanContextWords(dbpediaID: Int) {
    this.contextStore.counts(dbpediaID) = new Array[Short](0)
    this.contextStore.tokens(dbpediaID) = new Array[Int](0)
  }

  /*
  * Remove a token from a dbpediaResoruce's context
  * */
  def removeTokenFromContext(dbpediaResourceID: Int, tokenID: Int) {

    val indexOftokenId = this.contextStore.tokens(dbpediaResourceID).indexWhere { case (x) => x == tokenID }
    //remove from the tokens array
    this.contextStore.tokens(dbpediaResourceID) = ArrayUtils.dropIndex(this.contextStore.tokens(dbpediaResourceID), indexOftokenId)
    this.contextStore.counts(dbpediaResourceID) = ArrayUtils.dropIndex(this.contextStore.counts(dbpediaResourceID), indexOftokenId)
  }

  /**
   * Iterates the Context Store sorting the tokens by their token Id.
   */
  def sortTokensInContextStore(){
    for((currentTokens, i) <- contextStore.tokens.zipWithIndex ){
      if (currentTokens.isInstanceOf[Array[Int]] && currentTokens.size > 1){
        val (sortedTokens, counts) = currentTokens.zip(contextStore.counts(i)).sortBy(_._1).unzip
        contextStore.tokens(i) = sortedTokens.toArray
        contextStore.counts(i) = counts.toArray
      }
    }
  }

}
