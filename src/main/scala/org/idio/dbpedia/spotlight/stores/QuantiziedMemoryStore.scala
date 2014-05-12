package org.idio.dbpedia.spotlight.stores

import org.idio.dbpedia.spotlight.stores.CustomQuantiziedCountStore

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
class QuantiziedMemoryStore {

  var quantizedCountStore: CustomQuantiziedCountStore = null

  /*
  * Returns the quantizied value of a count.
  * If the count is already in the store it will return it
  * otherwise it will be created in the quantiziedStore
  * */
  def getQuantiziedCounts(count:Int):Short ={
    return this.quantizedCountStore.quantizedStore.addCount(count)
  }

  /*
  *  given a Quantizied value it returns the original count value
  * */
  def getCountFromQuantiziedValue(quantiziedValue:Short):Int ={
    return quantizedCountStore.quantizedStore.getCount(quantiziedValue)
  }
}
