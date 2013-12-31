package org.idio.dbpedia.spotlight

/**
 * Created by dav009 on 31/12/2013.
 */

import org.dbpedia.spotlight.db.memory.{MemoryStore,MemoryTokenTypeStore}
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException
import java.io.{File, FileInputStream}

class IdioTokenResourceStore(val pathtoFolder:String) {

  var tokenStore:MemoryTokenTypeStore= MemoryStore.loadTokenTypeStore(new FileInputStream(new File(pathtoFolder,"tokens.mem")))

  def showTokens(){
    for (token:String<-this.tokenStore.tokenForId){
        val counts = this.tokenStore.counts(tokenStore.idFromToken.get(token))
        println(token+"--"+this.tokenStore.getTokenType(token)+"---"+ counts)
    }
  }

  def raiseCountsForToken(token:String, boost:Int){
      val tokenId:Int = tokenStore.idFromToken.get(token)
      this.tokenStore.counts(tokenId) = tokenStore.counts(tokenId) + boost
  }

  def export(pathToFolder:String){
    MemoryStore.dump(this.tokenStore, new File(pathToFolder,"sf.mem"))
  }

}
