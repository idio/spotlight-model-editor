package org.idio.dbpedia.spotlight.stores

import org.dbpedia.spotlight.db.memory.{ MemoryStore, MemoryContextStore, MemoryTokenTypeStore }
import java.io.{ File, FileInputStream }

/**
 * Created by dav009 on 02/01/2014.
 */
class IdioContextStore(val pathtoFolder: String, val tokenStore: MemoryTokenTypeStore) {

  var contextStore: MemoryContextStore = MemoryStore.loadContextStore(new FileInputStream(new File(pathtoFolder, "context.mem")), this.tokenStore)

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
        this.contextStore.counts = this.contextStore.counts :+ Array[Int](1)
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
    if (!(this.contextStore.tokens(dbpediaResourceID) contains tokenID)) {
      this.contextStore.counts(dbpediaResourceID) = this.contextStore.counts(dbpediaResourceID) :+ count
      this.contextStore.tokens(dbpediaResourceID) = this.contextStore.tokens(dbpediaResourceID) :+ tokenID
    }
  }

  /*
  * Empty the context Words and context counts of a dbpedia Topic
  * */
  def cleanContextWords(dbpediaID: Int) {
    this.contextStore.counts(dbpediaID) = new Array[Int](0)
    this.contextStore.tokens(dbpediaID) = new Array[Int](0)
  }

  /*
  * Remove a token from a dbpediaResoruce's context
  * */
  def removeTokenFromContext(dbpediaResourceID: Int, tokenID: Int) {

    val indexOftokenId = this.contextStore.tokens(dbpediaResourceID).indexWhere { case (x) => x == tokenID }
    //remove from the tokens array
    this.contextStore.tokens(dbpediaResourceID) = this.contextStore.tokens(dbpediaResourceID).take(indexOftokenId) ++ this.contextStore.tokens(dbpediaResourceID).drop(indexOftokenId + 1)

    //remove from counts array
    this.contextStore.counts(dbpediaResourceID) = this.contextStore.counts(dbpediaResourceID).take(indexOftokenId) ++ this.contextStore.counts(dbpediaResourceID).drop(indexOftokenId + 1)

  }

}
