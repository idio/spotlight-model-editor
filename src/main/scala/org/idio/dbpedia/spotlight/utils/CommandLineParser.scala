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

package org.idio.dbpedia.spotlight.utils



case class CommandLineConfig(commands:Array[String] = Array[String](),
                  file:Boolean = false,
                  argument:String = "",
                  pathToModelFolder:String = ""
                  )

class CommandLineParser {

  val parser = new scopt.OptionParser[CommandLineConfig]("spotlight-model-editor"){

    /*
    * Returns a command with the given command name and command help
    * */
    def getSimpleCommand(commandName: String, commandHelp: String): scopt.OptionDef[Unit, CommandLineConfig]={
      val returnCommand = cmd(commandName) action{ (x, c) =>
        c.copy(commands = c.commands :+ commandName)
      }
      returnCommand  text(commandHelp)
      returnCommand
    }

    /*
     * Returns a command with a single argument being mandatory.
     * */
    def getCommandSingleArg(commandName: String,
                            argumentHelp: String,
                            commandHelp: String): scopt.OptionDef[Unit, CommandLineConfig]={
      val returnCommand = getSimpleCommand(commandName, commandHelp)

      returnCommand.children (

        arg[String]("path-to-model-folder")  action { (x, c) =>
          c.copy(pathToModelFolder =  x) } text("Path to spotlight/en/model folder"),

        arg[String]("argument")  action { (x, c) =>
          c.copy(argument =  x) } text(argumentHelp)
      )
      returnCommand
    }

    /*
     * Returns a command accepting the file flag
     * */
    def getCommandAcceptingFileAsSingleArg(commandName: String,
                                           argumentHelp: String,
                                           commandHelp: String): scopt.OptionDef[Unit, CommandLineConfig]={
      val returnCommand = getCommandSingleArg(commandName, argumentHelp, commandHelp)

      returnCommand.children(
        opt[Unit]('f', "file") action { (x, c) =>
          c.copy(file = true) } text("If this flag is set the argument is a path to a file")
      )
      returnCommand
    }


    head("Spotlight-model-editor")

    // Defining the commands and subcommands
    val surfaceFormCommand =  getSimpleCommand("surfaceform", "surface Forms commands")

    surfaceFormCommand.children(
      getCommandAcceptingFileAsSingleArg("make-spottable",
                                         "list of surfaceforms(piped separated) or file with one sf per line",
                                          "make a list of sf spottable"),

      getCommandAcceptingFileAsSingleArg("make-unspottable",
                                         "list of surfaceforms(piped separated) or file with one sf per line",
                                         "make a list of sf not spottable"),
      getCommandSingleArg("stats",
                          "surface form",
                          "outputs statistics about a Surface Form"),

      getCommandSingleArg("candidates",
                          "surface form",
                          "outputs the candidates of an SF"),

      getCommandSingleArg("copy-candidates",
                          "file",
                          "copy candidates from origin surfaceforms to destiny surface forms")

    )

    val topicCommand =  getSimpleCommand("topic", "Topic (Dbpedia Uris) related Commands")
    topicCommand.children(
      getCommandSingleArg("clean-set-context",
                          "file",
                          "cleans the context of topics given in the file and set the given vectors" ),
      getCommandSingleArg("check-context", "dbpediaURI", "outputs the context of a topic"),
      getCommandSingleArg("search", "dbpediaURI", "outputs whether a topic is in the store or not")
    )

    val associationCommand = getSimpleCommand("association", "surfaceforms and candidate topics commands")
    associationCommand.children(
      getCommandSingleArg("remove", "file with pairs(topic-surfaceForm)", "remove associations between sf and topics"),
      getCommandSingleArg("percentage-context-vector", "file with Triples(topic-surfaceForm-%context vector)", "% of context vector used when matching entities")
    )

    val contextCommand =  getSimpleCommand("context", "context vectors commands")
    contextCommand.children(
      getCommandSingleArg("export", "path to output file", "dumps the context vectors to a file")
    )


    val addCommand =  getSimpleCommand("file-update", "doing big updates via file")
    addCommand.children(
      getCommandSingleArg("all",
                          "file with associations and contexts",
                          "updates the stores adding all sf,associations, and contexts words defined in the file"),

      getCommandSingleArg("context-only",
                          "file with topics and contexts",
                          "updates the contexts of the topics augmenting their context with the given counts and words"),

      getCommandSingleArg("check",
                          "file with topics and contexts",
                          "checks existence of Dbpedia's Ids, SF, and links between SF's and Dbpedia's ids.")
    )

    val exploreCommand =  getCommandSingleArg("explore",
                                              "shows some surface forms and their statistics",
                                              "number of surface forms to explore")

    val fsaCommand = getSimpleCommand("fsa", "querying the finate state automata")
    fsaCommand.children(
      getCommandSingleArg("find",
        "piped separated list of surfaceforms",
        "checks if each of the given surfaceform ends in a final valid state in the FSA")
    )

  }

  def parse(args:  Array[String]): Option[CommandLineConfig] ={
    parser.parse(args, CommandLineConfig())
  }

}
