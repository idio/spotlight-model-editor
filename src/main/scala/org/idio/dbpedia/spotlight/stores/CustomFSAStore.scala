package org.idio.dbpedia.spotlight.stores

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

import org.dbpedia.spotlight.db.memory.{ MemoryStore, MemoryResourceStore }
import java.io.{File, FileInputStream}
import org.dbpedia.spotlight.db.FSASpotter
import opennlp.tools.util.Span
import org.dbpedia.spotlight.model.{Text, Token}
import org.idio.dbpedia.spotlight.utils.CustomTokenizer

class CustomFSAStore(val modelFolder: String, val customTokenStore: CustomTokenResourceStore, val customTokenizer: CustomTokenizer) {


  val fsaDictionary = MemoryStore.loadFSADictionary(new FileInputStream(new File(modelFolder, "fsa_dict.mem")))


  private def transverse(sentence:Seq[Token]):  Array[Span] ={

    var spans = Array[Span]()
    val ids = sentence.map(_.tokenType.id)

    sentence.zipWithIndex.foreach {
      case(t: Token, i: Int) =>{
        var currentState = FSASpotter.INITIAL_STATE
        var j = i

        do {
          //Get the transition for the next token:
          val (endState, nextState) = fsaDictionary.next(currentState, ids(j))

          //Add a span if this is a possible spot:
          if (endState == FSASpotter.ACCEPTING_STATE)
            spans :+= new Span(i, j+1, "m")

          //Keep traversing the FSA until a rejecting state or the end of the sentence:
          currentState = nextState
          j += 1
        } while ( currentState != FSASpotter.REJECTING_STATE && j < sentence.length )
      }

    }

    spans

  }


  def getFSASpots(surfaceText: String): Array[String] = {
    val text = new Text(surfaceText)
    val tokens: Seq[Token] = customTokenizer.tokenizer.tokenize(text)
    val spans: Array[Span] = transverse(tokens)

    spans.map{
       span:Span =>
         List.range(span.getStart, span.getEnd).map(tokens).map(_.token).mkString(" ")
    }

  }

}
