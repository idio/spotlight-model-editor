package org.idio.dbpedia.spotlight.stores

import java.io.{File, FileInputStream}

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

import org.dbpedia.spotlight.db.memory.{MemoryStore, MemoryQuantizedCountStore}
import java.io.{File, FileInputStream}
import org.dbpedia.spotlight.db.stem.SnowballStemmer
import scala.collection.mutable.HashMap

/**
 * @author David Przybilla david.przybilla@idioplatform.com
 **/
class CustomQuantiziedCountStore(val quantizedStore: MemoryQuantizedCountStore, val pathtoFolder:String) {

  def this(pathToFolder:String){
     this( MemoryStore.loadQuantizedCountStore(new FileInputStream(new File(pathToFolder, "quantized_counts.mem"))), pathToFolder)
  }



  /*
  * Takes an int value and tries to add it to the Quantizied Store
  * in case it exists it returns its quantizied version
  * otherwise it will add it and return its quantizied version
  * */
  def addCount(count: Int): Short={
    return quantizedStore.addCount(count)
  }

}
