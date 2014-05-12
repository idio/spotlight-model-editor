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
