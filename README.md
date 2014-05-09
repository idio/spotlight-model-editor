# Spotlight Model's Editor

Idio's Spotlight Model Editor allows you to create associations between word forms and 
a DBPedia topic or concept, thus improving the coverage of the topic extraction tool we use.

In order to use the Model Editor, you will need:

- (Oracle) Java 1.7
- Scala 2.9.x (If you want to run it interatively from a terminal)
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
3. do `mvn package`
4. call

```
java -Xmx4000M -jar  target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar explore path-to-turkish/tr/model/
```

## Importing Project
1. Get IntelliJ
2. Go to `File`>`Import Project` -> `Select POM Project`
3. Give enough RAM to run the project. Go to `Preferences` -> `Compiler` and add '-Xmx5G' to 'Aditional VM options',
3. Navigate to the `SpotlightModelReader` class, right click `Main` and select `run scala console`, enjoy

## Editing a model
start by freeing  as much ram as possible.

Each of the following tools addressing a `command` refers to calling the jar as follows

```
java -Xmx15360M -Xms15360M -jar idio-spotlight-model-0.1.0-jar-with-dependencies.jar <command> <subcommand> arg1 arg2
```


### Exploring a Model

- **command**: `explore`
- **arg1**: path to dbpedia spotlight model,`/mnt/share/spotlight/en/model`
- **result**: outputs 40 SurfaceForms with their respective candidates, priors and statistics

example:
```
java -Xmx15360M -Xms15360M -jar  target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar explore path-to-turkish/tr/model/
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
java -jar .... topic search path/to/model Michael_Schumacher‎
```

#### Check the Context words and counts of a topic
- **command**: `topic`
- **subcommand**: `check-context`
- **arg1**: path to dbpedia spotlight model,`/mnt/share/spotlight/en/model`
- **arg2**: piped separated list of dbpediaUris

example:
```
java -jar .... topic check-context /mnt/share/spotlight/en/model Barack_Obama\|United_States
```

#### Set the Context Words of a Topic

- **command**: `topic`
- **subcommand**: `clean-set-context`
- **arg1**: `pathToSpotlightModel/model`
- **arg2**: pathToFile
- **result**: The context words and counts for the topics in the file will be cleared. The specified context Words will be stemmed and added with their respect counts to the context vector of the given topics.

each line of the given input file should be like: 

```
dbpediaUri <tab> contextWordsSeparatedByPipe <tab> countsSeparatedBytab
```

example:
```
java -jar .... topic clean-set-context /mnt/share/spotlight/en/model folder/fileWithContextChanges 
```


### Surface Forms

All surface forms related actions are carried out using the `surfaceform` command followed by one of the following subcommands:

 - `stats` : printing stats of a surface form
 - `candidates` : printing the list of candidates of a surface form
 - `make-spottable` : making surfaceforms spottable
 - `make-no-spottable` : making surfaceforms no spottable
 - `copy-candidates` : adding to a `surfaceformA` all candidates of a `surfaceFormB`

#### stats of a surface form

- **subcommand**: `surfaceform`
- **subcommand**: `stats`
- **arg1**: path to dbpedia spotlight model,`/mnt/share/spotlight/en/model`
- **arg2**: surfaceForm
- **result**:  outputs statistics of the given surfaceForm

example :
```
java -jar .... surfaceform stats ~/Downloads/tr/model/ evrimleri
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
java -jar .... surfaceform candidates ~/Downloads/tr/model/ evrimleri
```
would check the candidate topics for the surface form `evrimleri`

### Making a list of Surface Forms not Spottable
- **command**: `surfaceform`
- **subcommand**: `make-no-spottable`
- **arg1**: path to dbpedia spotlight model,`/mnt/share/spotlight/en/model`
- **arg2**: 
     - list of Surface Forms separated by `|`. i.e: `how\|How\|Hello\ World`
     - file containing a surfaceForm  per line ( if option `-f` is passed)
- **result**: Each `SF` won't be spottable anymore

```
java -jar .... surfaceform make-no-spottable path/to/model surfaceForm1\|surfaceForm2\|
```

```
java -Xmx15360M -Xms15360M -jar idio-spotlight-model-0.1.0-jar-with-dependencies.jar surfaceform make-no-spottable path/to/model pathTo/File/withSF -f
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
java -jar .... surfaceform copy-candidates path/to/model pathToFile
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
java -jar .... surfaceform make-spottable path/to/model surfaceForm1\|surfaceForm2\|
```

```
java -jar .... surfaceform make-spottable path/to/model pathTo/File/withSF -f
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
java -jar .... association remove /mnt/share/spotlight/en/model /path/to/file/file_with_associations
```

### Updating Model From File
When updating the model with lots of `SF`, `Topics` and `Context Words` best is to do it from a file.
each line of the file should follow the format:

```
dbpedia_id <tab> surfaceForm1|surfaceForm2... <tab> contextW1|contextW2... <tab> contextW1Counts|ContextW2Counts
```

#### Insight
Before doing actual changes to the model it might be useful to see how many `SF`,`dbpedia topics` and links between those two are missing.
```java -jar .... file-update check path/to/en/model path_to_file/with/model/changes```.

#### Updating a model From File (All in One Go)
make sure you have enough ram to hold all the models that should be around `-Xmx15000M`.
do:

```
java -jar .... file-update all path/to/en/model path_to_file/with/model/changes
```

#### Updating a model From File (Two Steps)
If you don't have enough ram you can update the `SF` and `DbpediaTopics` in one step and the `Context Words` in other, this will require less memory.

1. go to the model folder and rename `context.mem` to `context2.mem` this will avoid the jar to avoid loading the `context store`
2. calling the following command will update the `surfaceform store`, `resource store` and `candidate store`: ```java -Xmx4000M -jar  target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar file-update all path/to/en/model path_to_file/with/model/changes```.
3. a new file `path_to_file/with/model/changes_just_context` will be generated after running the previous command.This file contains dbpediaIds(internal model's indexes) to contextWords, and it can be processed in the following step.
4. rename `context2.mem` to `context.mem`, and rename every other file in the model folder to something else.( if this is not done, the stores will be loaded and they will consume all your ram) 
5. calling the following will update the `context store`: 
```
java -jar .... file-update context path/to/en/model path_to_file/with/model/changes_just_context
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

4. Install the necessary dbpedia spotlight .jar file and compile ```
cd dbpedia-model-editor
mvn install:install-file -Dfile=/usr/share/java/dbpedia-spotlight-0.6.jar -DgroupId=org.dbpedia -DartifactId=spotlight -Dversion=0.6 -Dpackaging=jar
mvn package ```

5. You can now use the model editor for a variety of tasks, for example, to remove SFs Topics associations stored in tab separated file, you can run (we assume the model is located in `/mnt/share/spotlight/`). 
`sudo java -Xmx15360M -Xms15360M -jar idio-spotlight-model-0.1.0-jar-with-dependencies.jar association remove-association /mnt/share/spotlight/en/model ~/remove_associations`
The command would re-export the model, so you can just zip and upload the file to S3 to be used.

6. Write the changes you made into a changelog, so we can duplicate them from scratch if needed [need to decide where to store changes] .

