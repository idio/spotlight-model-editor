package org.idio.dbpedia.spotlight

/**
 * Created by dav009 on 31/12/2013.
 */

import org.dbpedia.spotlight.db.memory.{MemoryStore,MemoryTokenTypeStore}
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import java.io.{File, FileInputStream}
import org.dbpedia.spotlight.db.stem.SnowballStemmer
import scala.collection.mutable.HashMap

class IdioTokenResourceStore(val pathtoFolder:String, stemmerLanguage:String) {

  var tokenStore:MemoryTokenTypeStore= MemoryStore.loadTokenTypeStore(new FileInputStream(new File(pathtoFolder,"tokens.mem")))
  var stemmer:SnowballStemmer = new SnowballStemmer(stemmerLanguage)

  /*
  * Prints the Tokens in the Store
  * */
  def showTokens(){
    for (token:String<-this.tokenStore.tokenForId){
        val counts = this.tokenStore.counts(tokenStore.idFromToken.get(token))
        println(token+"--"+this.tokenStore.getTokenType(token)+"---"+ counts)
    }
  }

  /*
  * boost the counts of a tokenId
  * */
  def raiseCountsForToken(token:String, boost:Int){
      val tokenId:Int = tokenStore.idFromToken.get(token)
      this.tokenStore.counts(tokenId) = tokenStore.counts(tokenId) + boost
  }

  /*
  * Returns the Id of a token if it already exists.
  * Otherwise it adds the token to the store and returns its Id
  * */
  def getOrCreateToken(token:String):Int = {

    if (this.tokenStore.idFromToken.containsKey(token)){
       return this.tokenStore.idFromToken.get(token)
    }

    this.tokenStore.tokenForId = this.tokenStore.tokenForId :+ token
    this.tokenStore.counts = this.tokenStore.counts :+ 1

    this.tokenStore.createReverseLookup()
    return this.tokenStore.idFromToken.get(token)
  }

  def export(pathToFolder:String){
    MemoryStore.dump(this.tokenStore, new File(pathToFolder,"sf.mem"))
  }

  /*
  * Returns a stemmed version of a Token.
  * */
  def stemToken(token:String):String = {
    var stemmedToken:String = stemmer.stem(token)
    return stemmedToken
  }

  /*
  * Transform ContextWords into ContextTokens(stemmed)
  * Returns a map from Token to Token Frequency
  * */
  def getContextTokens(contextWords:Array[String], contextCounts:Array[Int]):HashMap[String,Int] = {
    val contextTokenMap:HashMap[String,Int] = new HashMap[String, Int]()

    (contextWords, contextCounts).zipped foreach { (word, counts) =>
    {
      val stemmedWord:String = this.stemToken(word)
      var currentCount = 0
      if (contextTokenMap.contains(stemmedWord)){
        currentCount = contextTokenMap.get(stemmedWord).get
      }
      contextTokenMap.put(stemmedWord, currentCount + counts)

    }
    }
    return contextTokenMap
  }


}
