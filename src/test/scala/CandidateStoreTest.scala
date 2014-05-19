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

import org.dbpedia.spotlight.db.memory.{MemoryQuantizedCountStore, MemoryResourceStore, MemoryCandidateMapStore}
import org.dbpedia.spotlight.model.SurfaceForm
import org.idio.dbpedia.spotlight.stores.{CustomQuantiziedCountStore, CustomCandidateMapStore}
import org.scalatest.mock.MockitoSugar.mock
import org.junit.Test
import org.junit.Assert._

class CandidateStoreTest {

  @Test
  def testCopyCandidates(){

    // mock needed objects
    val candidateMap:MemoryCandidateMapStore = new MemoryCandidateMapStore()
    val resStore:MemoryResourceStore = mock[MemoryResourceStore]
    val quantiziedStore:CustomQuantiziedCountStore = mock[CustomQuantiziedCountStore]


    val candidateCounts1 = Array[Int](50, 1, 100)
    val candidateCounts2 = Array[Int](14, 55)
    // adding candidate counts to quantizied Store
    val quantizedCounts1 = candidateCounts1.map(quantiziedStore.addCount)
    val quantizedCounts2 = candidateCounts2.map(quantiziedStore.addCount)

    //mock candidate store
    candidateMap.candidates = Array[Array[Int]]( Array[Int](1, 2, 3), Array[Int](3, 5) )
    candidateMap.candidateCounts= Array[Array[Short]]( quantizedCounts1,  quantizedCounts2)


    val sourceSurfaceForm =  new SurfaceForm("sf1", 0, 100, 200)
    val destinySurfaceForm = new  SurfaceForm("sf2", 1, 50, 200)


    var customCandidateMapStore:CustomCandidateMapStore = new CustomCandidateMapStore(candidateMap, "", resStore, quantiziedStore)

    // test copy candidates
    customCandidateMapStore.copyCandidates(sourceSurfaceForm, destinySurfaceForm)

    val expectedCandidateCounts = collection.immutable.Map[Int, Short](1->quantiziedStore.addCount(50),
                                                                     2->quantiziedStore.addCount(1),
                                                                     3->quantiziedStore.addCount(100),
                                                                     5->quantiziedStore.addCount(55))

    //check the candidate topics set
    val candidateTopicsForDestinySurfaceForm = customCandidateMapStore.candidateMap.candidates(1)
    assertTrue(collection.immutable.Set[Int](candidateTopicsForDestinySurfaceForm:_*)==expectedCandidateCounts.keySet)

    //check the candidate counts
    candidateTopicsForDestinySurfaceForm.zip(candidateMap.candidateCounts(1)).foreach{ case (topicKey, counts)=>
      assertTrue(expectedCandidateCounts(topicKey) == counts)
    }


  }


}
