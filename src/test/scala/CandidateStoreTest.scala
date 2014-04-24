/**
 * Created by dav009 on 24/04/2014.
 */

import org.dbpedia.spotlight.db.memory.{MemoryResourceStore, MemoryCandidateMapStore}
import org.dbpedia.spotlight.model.SurfaceForm
import org.idio.dbpedia.spotlight.IdioCandidateMapStore
import org.scalatest.mock.MockitoSugar.mock
import org.junit.Test
import org.junit.Assert._

class CandidateStoreTest {

  @Test
  def testCopyCandidates(){

    // mock needed objects
    val candidateMap:MemoryCandidateMapStore = new MemoryCandidateMapStore()
    val resStore:MemoryResourceStore = mock[MemoryResourceStore]

    //mock candidate store
    candidateMap.candidates = Array[Array[Int]]( Array[Int](1, 2, 3), Array[Int](3, 5) )
    candidateMap.candidateCounts= Array[Array[Int]]( Array[Int](50, 1, 100),  Array[Int](14, 55))

    val sourceSurfaceForm =  new SurfaceForm("sf1", 0, 100, 200)
    val destinySurfaceForm = new  SurfaceForm("sf2", 1, 50, 200)


    var idioCandidateMapStore:IdioCandidateMapStore = new IdioCandidateMapStore(candidateMap, "", resStore)

    // test copy candidates
    idioCandidateMapStore.copyCandidates(sourceSurfaceForm, destinySurfaceForm)

    val expectedCandidateCounts = collection.immutable.Map[Int, Int](1->50, 2->1, 3->100, 5->55)

    //check the candidate topics set
    val candidateTopicsForDestinySurfaceForm = idioCandidateMapStore.candidateMap.candidates(1)
    assertTrue(collection.immutable.Set[Int](candidateTopicsForDestinySurfaceForm:_*)==expectedCandidateCounts.keySet)

    //check the candidate counts
    candidateTopicsForDestinySurfaceForm.zip(candidateMap.candidateCounts(1)).foreach{ case (topicKey, counts)=>
      assertTrue(expectedCandidateCounts(topicKey) == counts)
    }


  }


}
