# Spotlight Model's Editor

Idio's Spotlight Model Editor allows you to manually tweak dbpedia spotlight's models.
Thus it allows you to manually:

  - Add new Surface Forms
  - Add new Topics
  - Create associations between surface forms and dbpedia uris
  - Remove associations between surface forms and dbpedia uris
  - Make surface forms spottable
  - Make surface forms unspottable
  - Modify the context vectors

In order to use the Model Editor, you will need:

- (Oracle) Java 1.7
- Scala 2.9.x (If you want to run it interactively from a terminal)
- Compiling Spotlight Model Editor (this tool) from source (see below)
- A pre-computed language model (downloaded from [here](http://spotlight.sztaki.hu/downloads/) )

We also recommend using [IntelliJ](http://www.jetbrains.com/idea/), for editing the code. See below, 
for instructions on how to set up a project.

- [Compiling](#compiling)
    - [Compiling Idio's Dbpedia Model Editor](#compiling-idios-dbpedia-model-editor)
    - [Importing Project](#importing-project)
- [Editing a model](#editing-a-model)
    - [Searching a Topic](#searching=a-topic)
    - [Getting Data about a SurfaceForm](#getting-data-about-a-surfaceform)
    - [Making a list of Surface Forms NOT Spottable](#making-a-list-of-surface-forms-not-spottable)
    - [Making a list of Surface Forms Spottable](#making-a-list-of-surface-forms-spottable)
    - [Set the Context Words of a Topic](#set-the-context-words-of-a-topic)
    - [Deleting Associations between SF and Topics](#deleting-associations-between-sf-and-topics)
- [Updating Model From File](#updating-model-from-file)
    - [Insight](#insight)
    - [Updating a model From File (All in One Go)](#updating-a-model-from-file-all-in-one-go)
    - [Updating a model From File (Two Steps)](#updating-a-model-from-file-two-steps)
    
- [Using the scala console](#using-the-scala-console)
    - [Starting a scala console](#starting-a-scala-console)
    - [Playing with the models](#playing-with-the-models)

- [Practical tips for updating a model](#practical-tips-for-updating-a-model)

## Compiling

We assume that you have the correct versions of Java and mvn in your system.

The language models consume a lot of computational resources, so in these instructions we use the model for 
Turkish (located in the `tr` folder). Feel free to play with other languages, if you have a big machine.


### Compiling Idio's Dbpedia Model Editor

1. Clone this repo
2. go to the repo's folder
3. do `mvn package appassembler:assemble`
4. call

```
sh target/bin/model-editor explore path-to-model/en/model/ 20
```

it should print the stats for 20 surface forms


Step 3 generates a jar with all the dependencies in `target` folder. Then it generates a script with default values for calling the jar. The script calls the jar with default values for the heap (15g). If you want to override this value you can modify: (i) the pom `appassembler-maven-plugin` settings in the pom, or (ii) call the jar directly `java -xmx.. -jar ...` followed by the commands shown in this readme.

## Importing Project
1. Get IntelliJ
2. Go to `File`>`Import Project` -> `Select POM Project`
3. Give enough RAM to run the project. Go to `Preferences` -> `Compiler` and add '-Xmx5G' to 'Aditional VM options',
3. Navigate to the `SpotlightModelReader` class, right click `Main` and select `run scala console`, enjoy

## Editing a model
start by freeing  as much ram as possible.

Each of the following tools addressing a `command` refers to calling the jar/script as follows

using the generated script:
```
sh target/bin/model-editor <command> <subcommand> arg1 arg2
```

using the generated jar:
```
java -Xmx15g -Xms15g -jar target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar <command> <subcommand> arg1 arg2
```



### Exploring a Model

- **command**: `explore`
- **arg1**: path to dbpedia spotlight model,`/mnt/share/spotlight/en/model`
- **arg2**: number of surface forms
- **result**: outputs arg2 number of SurfaceForms with their respective candidates, priors and statistics

example:
```
sh target/bin/model-editor explore path-to-turkish/tr/model/ 40
```

### Topics

All topic related actions are carried out using the `topic` command followed by one of the following subcommands:

 - `search` : checking if a topic is in the stores
 - `check-context` : printing the context of a topic
 - `clean-set-context` : cleaning and setting the context of a topic


#### Searching a Topic
- **command**: `topic`
- **subcommand**: `search`
- **arg1**: path to dbpedia spotlight model,`/mnt/share/spotlight/en/model`
- **arg2**: dbpediaURI
- **result**: looks for a given `DbpediaId` in the Model and returns whether that topic exists or not in the model

i.e :
```
sh target/bin/model-editor topic search path/to/model Michael_Schumacher‎
```

#### Check the Context words and counts of a topic
- **command**: `topic`
- **subcommand**: `check-context`
- **arg1**: path to dbpedia spotlight model,`/mnt/share/spotlight/en/model`
- **arg2**: piped separated list of dbpediaUris

example:
```
sh target/bin/model-editor topic check-context /mnt/share/spotlight/en/model Barack_Obama\|United_States
```

#### Set the Context Words of a Topic

- **command**: `topic`
- **subcommand**: `clean-set-context`
- **arg1**: `pathToSpotlightModel/model`
- **arg2**: pathToFile
- **result**: The context words and counts for the topics in the file will be cleared. The specified context Words will be stemmed and added with their respective counts to the context vector of the given topics.

each line of the given input file should be like: 

```
dbpediaUri <tab> contextWordsSeparatedByPipe <tab> countsSeparatedByPipe
```

the size of `contextWordsSeparatedByPipe` and `countsSeparatedByPipe` should be the same

example:
```
sh target/bin/model-editor topic clean-set-context /mnt/share/spotlight/en/model folder/fileWithContextChanges 
```


### Surface Forms

All surface forms related actions are carried out using the `surfaceform` command followed by one of the following subcommands:

 - `stats` : printing stats of a surface form
 - `candidates` : printing the list of candidates of a surface form
 - `make-spottable` : making surfaceforms spottable
 - `make-unspottable` : making surfaceforms unspottable
 - `copy-candidates` : adding to a `surfaceformA` all candidates of a `surfaceFormB`

#### stats of a surface form

- **subcommand**: `surfaceform`
- **subcommand**: `stats`
- **arg1**: path to dbpedia spotlight model,`/mnt/share/spotlight/en/model`
- **arg2**: surfaceForm
- **result**:  outputs statistics of the given surfaceForm

example :
```
sh target/bin/model-editor surfaceform stats ~/Downloads/tr/model/ evrimleri
```
outputs statistics for the surface form `evrimleri`


#### getting the candidate topics of a surface form

- **command**: `surfaceform`
- **subcommand**: `candidates`
- **arg1**: path to dbpedia spotlight model,`/mnt/share/spotlight/en/model`
- **arg2**: surfaceForm
- **result**:  outputs the candidate topics of a surface form

example :
```
sh target/bin/model-editor surfaceform candidates ~/Downloads/tr/model/ evrimleri
```
would check the candidate topics for the surface form `evrimleri`

### Making a list of Surface Forms Unspottable
- **command**: `surfaceform`
- **subcommand**: `make-unspottable`
- **arg1**: path to dbpedia spotlight model,`/mnt/share/spotlight/en/model`
- **arg2**: 
     - list of Surface Forms separated by `|`. i.e: `how\|How\|Hello\ World`
     - file containing a surfaceForm  per line ( if option `-f` is passed)
- **result**: Each `SF` won't be spottable anymore

```
sh target/bin/model-editor surfaceform make-unspottable path/to/model surfaceForm1\|surfaceForm2\|
```

```
sh target/bin/model-editor surfaceform make-unspottable path/to/model pathTo/File/withSF -f
```

### Copy Candidates

- **command**: `surfaceform`
- **subcommand**: `copy-candidates`
- **arg1**: path to dbpedia spotlight model,`/mnt/share/spotlight/en/model`
- **arg2**: path to file containing pairs of surfaceForm. each line should be :
       
       ```
        <originSurfaceForm> <tab> <destinySurfaceForm>
       ```

- **result**: copies the candidate topics from each `originSurfaceForm` as candidates topics to `destinySurfaceForm` 


example: 


```
sh target/bin/model-editor surfaceform copy-candidates path/to/model pathToFile
```

### Making a list of Surface Forms Spottable

- **command**: `surfaceform`
- **subcommand**: `make-spottable`
- **arg1**: path to dbpedia spotlight model,`/mnt/share/spotlight/en/model`
- **arg2**: 
     - list of Surface Forms separated by `|`. i.e: `how\|How\|Hello\ World`
     - file containing a surfaceForm  per line ( if option `-f` is passed)
- **result**: Each `SF` will be spottable


example: 


```
sh target/bin/model-editor surfaceform make-spottable path/to/model surfaceForm1\|surfaceForm2\|
```

```
sh target/bin/model-editor surfaceform make-spottable path/to/model pathTo/File/withSF -f
```


### Associations

All surface forms related actions are carried out using the `association` command followed by one of the following subcommands:

 - `remove`

### Deleting Associations between SF and Topics

- **command**: `association`
- **subcommand**: `remove`
- **arg1**: `pathToSpotlightModel/model`
- **arg2**: pathToInputFile
- **result**: All associations between SFs and Topics in the given input file will be deleted from the model.

Every line in the input file describes an association which will be deleted, each line should follow the format:

```
dbpediaURI <tab> Surface Form
```

example:
```
sh target/bin/model-editor association remove /mnt/share/spotlight/en/model /path/to/file/file_with_associations
```

### FSA

#### Checking if a SF is spottable via FSA

- **command**: `fsa`
- **subcommand**: `find`
- **arg1**: `pathToSpotlightModel/model`
- **arg2**: piped separated list of surface forms
- **result**: the FSA spots for each surface forms


example:
```
sh target/bin/model-editor fsa find /mnt/share/spotlight/en/model Nintendo\ Wii\|barack
```

### Updating Model From File
When updating the model with lots of `SF`, `Topics` and `Context Words` best is to do it from a file.
each line of the file should follow the format:

```
dbpedia_id <tab> surfaceForm1|surfaceForm2... <tab> contextW1|contextW2... <tab> contextW1Counts|ContextW2Counts
```

#### Insight
Before doing actual changes to the model it might be useful to see how many `SF`,`dbpedia topics` and links between those two are missing.
```sh target/bin/model-editor file-update check path/to/en/model path_to_file/with/model/changes```.

#### Updating a model From File (All in One Go)
make sure you have enough ram to hold all the models that should be around 15g.
do:

```
sh target/bin/model-editor file-update all path/to/en/model path_to_file/with/model/changes
```

#### Updating a model From File (Two Steps)
If you don't have enough ram you can update the `SF` and `DbpediaTopics` in one step and the `Context Words` in other, this will require less memory.

1. go to the model folder and rename `context.mem` to `context2.mem` this will avoid the jar to avoid loading the `context store`
2. calling the following command will update the `surfaceform store`, `resource store` and `candidate store`: ```sh target/bin/model-editor file-update all path/to/en/model path_to_file/with/model/changes```.
3. a new file `path_to_file/with/model/changes_just_context` will be generated after running the previous command.This file contains dbpediaIds(internal model's indexes) to contextWords, and it can be processed in the following step.
4. rename `context2.mem` to `context.mem`, and rename every other file in the model folder to something else.( if this is not done, the stores will be loaded and they will consume all your ram) 
5. calling the following will update the `context store`: 
```
sh target/bin/model-editor file-update context-only path/to/en/model path_to_file/with/model/changes_just_context
```
6. rename all files to their usual conventions and enjoy a fresh baked model

steps 1-4 could be applied while ignoring 5 and 6 when: 
- wanting to add `SFs`
- wanting to link `SFs` with already existing `Dbpedia Topic`

steps 5-6 could be applied while ignoring 1-4 when:
- wanting to add Context words to a `Dbpedia Topic`

**Important**:  
- `step 1-4` will only add `SF` and `Dbpedia Topics` if they dont exist.
- `step 1-4` will make all specified `SF`  spottable
- `step 5-6` Only ADDS context words to the context of a dbpedia Topic.

# Using the scala console

Best way to play the models and modify them  is to use the scala console.

## Starting a scala console
- make sure your scala is 2.9.X
- start a scala console by doing:
```
JAVA_OPTS="-Xmx15000M -Xms15000M" scala 
```

## Playing with the models

Once you start a scala console you can use it like `ipython` to create instances of the scala classes we have, to load the models, check if dbpedia id's exist, add new dbpedia ids, add new surface forms etc..

do:  `:cp pathTo/ModelEditor.jar`

This will load the classes inside the model editor. After that you should be able to play with the classes inside the jar.

Example:

```
var spotlightModel = org.idio.dbpedia.spotlight.Main.getSpotlightModel( "/Users/dav009/Downloads/tr/model/")
spotlightModel.showSomeSurfaceForms()
spotlightModel.getStatsForSurfaceForm("evrimleri")
spotlightModel.searchForDBpediaResource("ikimono_gakari_dbpedia_uri")
spotlightModel.addNew("ikimono_gakari_sf","ikimono_gakari_dbpedia_uri",1,Array())
spotlightModel.exportModels("/new/path/of/folder/model/")
```
-----------

`tools/explore.scala` contains a script which can be preloaded into the scala terminal. It imports the classes and stores needed to play with the model at a low level.
In order to use it:

1. do `JAVA_OPTS="-Xmx9000M -Xms9000M" scala`  note: Adjust the Java heap options to your needs, If you are using all the stores use around 15g

2. once you are in the scala console do: `:load tools/explore.scala` . this will preload the objects:
    - `resStore`: resource store
    - `sfStore`: surface form store
    - `candidateMap`: candidate store
    - `tokenStore`: token type store
    - `contextStore`: context token store



## Practical tips for updating a model

Given that the models are quite big (2 GB compressed), downloading, modifying and uploading them would be very
 time consuming from your local machine. Plus, the operations require a lot of ram, so you better boot a dev instance, and do the changes from there.
 Here's a list of steps.

1. Create the new instance:
`knife ec2 server create -r "role[spotlight]" -I ami-6d3f9704 -G default -x ubuntu --node-name "dev-spotlight" --environment "development" -f m2.xlarge --availability-zone us-east-1d --secret-file path/to/secret/file`

2. ssh into the new instance and install dependencies:
`sudo apt-get install openjdk-6-jdk maven scala unzip`

3. Clone the dbpedia model editor repo:
`git clone git@github.com:idio/spotlight-model-editor.git`

4. compile ```
cd dbpedia-model-editor
mvn package ```

5. You can now use the model editor for a variety of tasks, for example, to remove SFs Topics associations stored in tab separated file, you can run (we assume the model is located in `/mnt/share/spotlight/`). 
`sh target/bin/model-editor association remove-association /mnt/share/spotlight/en/model ~/remove_associations`
The command would re-export the model, so you can just zip and upload the file to S3 to be used.

6. Write the changes you made into a changelog, so we can duplicate them from scratch if needed [need to decide where to store changes] .

## License

Copyright 2014 Idio

Licensed under the Apache License, Version 2.0: [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)