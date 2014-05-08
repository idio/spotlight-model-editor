package org.idio.dbpedia.spotlight.utils

/**
 * Created by dav009 on 07/05/2014.
 */


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
    def getCommandSingleArg(commandName: String, argumentHelp: String, commandHelp: String): scopt.OptionDef[Unit, CommandLineConfig]={
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
    def getCommandAcceptingFileAsSingleArg(commandName: String, argumentHelp: String, commandHelp: String): scopt.OptionDef[Unit, CommandLineConfig]={
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
      getCommandAcceptingFileAsSingleArg("make-spottable", "list of surfaceforms(piped separated) or file with one sf per line", "make a list of sf spottable"),
      getCommandAcceptingFileAsSingleArg("make-no-spottable", "list of surfaceforms(piped separated) or file with one sf per line"  , "make a list of sf not spottable"),
      getCommandSingleArg("stats", "surface form", "outputs statistics about a surface form"),
      getCommandSingleArg("candidates", "surface form", "outputs the candidates of a sf"),
      getCommandSingleArg("copy-candidates", "file", "copy candidates from origin surfaceforms to destiny surface forms")

    )

    val topicCommand =  getSimpleCommand("topic", "Topic (Dbpedia Uris) related Commands")
    topicCommand.children(
      getCommandSingleArg("clean-set-context","file", "cleans the context of topics given in the file and set the given vectors" ),
      getCommandSingleArg("check-context", "dbpediaURI", "outputs the context of a topic"),
      getCommandSingleArg("search", "dbpediaURI", "outputs whether a topic is in the store or not")
    )

    val associationCommand = getSimpleCommand("association", "surfaceforms and candidate topics commands")
    associationCommand.children(
      getCommandSingleArg("remove-association", "file with pairs(topic-surfaceForm)", "remove associations between sf and topics")
    )

    val contextCommand =  getSimpleCommand("context", "context vectors commands")
    contextCommand.children(
      getCommandSingleArg("export", "path to output file", "dumps the context vectors to a file")
    )


    val addCommand =  getSimpleCommand("file-update", "doing big updates via file")
    addCommand.children(
      getCommandSingleArg("all", "file with associations and contexts", "updates the stores adding all sf,associations, and contexts words defined in the file"),
      getCommandSingleArg("only-context", "file with topics and contexts", "updates the contexts of the topics augmenting their context with the given counts and words"),
      getCommandSingleArg("check", "file with topics and contexts", "checks existence of Dbpedia's Ids, SF, and links between SF's and Dbpedia's ids.")
    )

    val exploreCommand =  getSimpleCommand("explore", "shows some surface forms and their statistics")

  }

  def parse(args:  Array[String]): Option[CommandLineConfig] ={
    parser.parse(args, CommandLineConfig())
  }

}






