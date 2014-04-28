
/*
* This is a tool script for exploring and modifying the contents of the spotlight stores.
* Adjust the Java heap options to your needs, If you are using all the stores use around 15gs
*
* Usage: JAVA_OPTS="-Xmx9000M -Xms9000M" scala
* once in the scala console type: :load pathToScript/explore.scala
*
* */
:cp /usr/share/java/dbpedia-spotlight-0.6.jar
import java.io.{File, FileInputStream}
import org.dbpedia.spotlight.db.memory.{MemoryStore, MemoryContextStore, MemorySurfaceFormStore,
                                        MemoryResourceStore, MemoryCandidateMapStore, MemoryTokenTypeStore}
import org.dbpedia.spotlight.model.{Candidate, SurfaceForm, DBpediaResource}
 
val pathtoFolder= "/mnt/share/spotlight/en/model"

//Loading all the stores
lazy val resStore:MemoryResourceStore = MemoryStore.loadResourceStore(
                                                      new FileInputStream(new File(pathtoFolder,"res.mem")))

lazy val sfStore:MemorySurfaceFormStore = MemoryStore.loadSurfaceFormStore(
                                                      new FileInputStream(new File(pathtoFolder,"sf.mem")))

lazy val candidateMap:MemoryCandidateMapStore = MemoryStore.loadCandidateMapStore(
                                                    new FileInputStream(new File(pathtoFolder,"candmap.mem")),
                                                    resStore)

// make sure you give enough heap if you are ever using any of these two following stores:
lazy val tokenStore:MemoryTokenTypeStore = MemoryStore.loadTokenTypeStore(
                                                    new FileInputStream(new File(pathtoFolder,"tokens.mem")))

lazy val contextStore:MemoryContextStore = MemoryStore.loadContextStore(
                                                    new FileInputStream(new File(pathtoFolder,"context.mem")),
                                                    tokenStore)
