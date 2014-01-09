# Spotlight Model's Editor

Idio's Spotlight Model Editor allows you to create associations between word forms and 
a DBPedia topic or concept, thus improving the coverage of the topic extraction tool we use.

In order to use the Model Editor, you will need:

- (Oracle) Java 1.7
- Scala >= 2.9.x
- Compiling DBPedia Spotlight from source (see below)
- Compiling Spotlight Model Editor (this tool) from source (see below)
- A pre-computed language model (downloaded from [here](http://spotlight.sztaki.hu/downloads/) )

We also recommend using [IntelliJ](http://www.jetbrains.com/idea/), for editing the code. See below, 
for instructions on how to set up a project.

## Compiling

We assume that you have the correct versions of Java and Scala, also make sure you have `mvn` in your system.
The language models consume a lot of computational resources, so in these instructions we use the model for 
Turkish (located in the `tr` folder). Feel free to play with other languages, if you have a big machine.

### Compiling Dbpedia Spotlight

DBpedia Spotlight's jar is one of the dependencies. These steps will guide you on how to compile spotlight

1. clone `https://github.com/dbpedia-spotlight/dbpedia-spotlight`
2. compile dbpedia spotlight: 
  - `mvn package`

after this step there should be a `dbpedia-spotlight-0.6-jar-with-dependencies.jar` in the `./dist/target` folder.

### Compiling Idio's Dbpedia Model Editor

1. Clone this repo
2. go to the repo's folder and do:

  ```
  mvn install:install-file -Dfile=path_to_spotlight_jar/dbpedia-spotlight-0.6-jar-with-dependencies.jar -DgroupId=org.dbpedia -DartifactId=spotlight -Dversion=0.6 -Dpackaging=jar
  ```
3. do `mvn compile`
4. do `mvn package`
5. call

```
java -Xmx4000M -jar  target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar explore path-to-turkish/tr/model/
```


## Editing
1. Get IntelliJ
2. Go to `File`>`Import Project` -> `Select POM Project`
4. Edit files
3. Right click `Main` and select `run scala console`, enjoy

## Editing a model
start by freeing  as much ram as possible.

### Exploring a Model

outputs 40 SurfaceForms with their respective candidates, priors and statistics

```
java -Xmx4000M -jar  target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar explore path-to-turkish/tr/model/
```


### Searching a Topic

looks for a given `DbpediaId` in the Model and returns whether that topic exists or not in the model
```
java -Xmx4000M -jar  target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar search path-to-turkish/tr/model/ dbpediaId
```

i.e :
```
java -jar .... search path/to/model Michael_Schumacher‎
```

### Getting Data about a SurfaceForm

Given a surfaceForm it outputs its topic candidates and statistics

```
java -Xmx4000M target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar check /path/To/Model surfaceForm
```

i.e:
```
java -Xmx4000M target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar check ~/Downloads/tr/model/ evrimleri
```
would check the candidate topics and statistics for the surface form `evrimleri`


### Adding SurfaceForms and Topics
attach the given `dbpediaId` as a candidate topic for the  given `surfaceForm`. 
- creates `dbpediaId` if it does not exist in the model
- creates `surfaceForm` if it does not exists in the model

The dbpedia types should be separated by pipes

```
java -Xmx4000M target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar update /path/To/Model/ surfaceForm dbpediaId dbpediaTypesSeparatedByPipe

```

i.e:

```
java -Xmx4000M target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar update ~/Downloads/tr/model/ ikimono_sf ikimono_topic

```
would add the topic `ikimono_topic` for the surface form `ikimono_sf`, note that `ikimono_topic` has no `dbpedia_types`.

### Boosting the probability of a Topic for a given Surface Form.
You can boost the probability of a topic being picked by boosting its counts for a given surface form.
This can be done by calling `boost`.
It will increment the counts of the given DbpediaID(topic) for the given surfaceForm by countBoost

```
java -Xmx4000M target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar boost ~/Downloads/tr/model/ surfaceForm dbpediaId countBoost
```

i.e:
```
java -Xmx4000M target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar boost ~/Downloads/tr/model/ evrimleri Yıldız_evrimi 100
```
 
it will boost by 100 the counts of the topic `Yıldız_evrimi` when trigerred by surface form `evrimleri` 

### Updating Model From File
When updating the model with lots of `SF`, `Topics` and `Context Words` best is to do it from a file.
each line of the file should follow the format:

```
dbpedia_id <tab> surfaceForm1|surfaceForm2... <tab> contextW1|contextW2... <tab> contextW1Counts|ContextW2Counts
```

#### All in one go
make sure you have enough ram to hold all the models that should be around `-Xmx15000M`.
do:

```
java -Xmx4000M -jar  target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar file-update-sf-dbpedia path/to/en/model path_to_file/with/model/changes
```

#### In two Steps
If you don't have enough ram you can update the `SF` and `DbpediaTopics` in one step and the `Context Words` in other, this will require less memory.

To update only `sf` and `DbpediaTopics` do:

1. go to the model folder and rename `context.mem` to `context2.mem` this will avoid the jar to avoid loading the `context store`
2. calling the following command will update the `surfaceform store`, `resource store` and `candidate store`: ```java -Xmx4000M -jar  target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar file-update-sf-dbpedia path/to/en/model path_to_file/with/model/changes```.
3. a new file `path_to_file/with/model/changes_just_context` will be generated after running the previous command.This file contains dbpediaIds(internal model's indexes) to contextWords, and it can be processed in the following step.
4. rename `context2.mem` to `context.mem`, and rename every other file in the model folder to something else.( if this is not done, the stores will be loaded and they will consume all your ram) 
5. calling the following will update the `context store`: 
```
java -Xmx4000M -jar  target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar file-update-context path/to/en/model path_to_file/with/model/changes_just_context
```
6. rename all files to their usual conventions and enjoy a fresh baked model

#### Insight
Before doing actual changes to the model it might be useful to see how many `SF`,`dbpedia topics` and links between those two are missing.
```java -Xmx4000M -jar  target/idio-spotlight-model-0.1.0-jar-with-dependencies.jar file-check path/to/en/model path_to_file/with/model/changes```.


# Using the scala console

Best way to play the models and modify them  is to use the scala console.

## Starting a scala console
- make sure your scala is 2.9.2
- start a scala console by doing:
```
scala  -classpath /path/to/jar/idio-spotlight-model-0.1.0-jar-with-dependencies.jar  -J-Xmx4000M
```

`Xmx4000M` is the size of the java heap, this has to be big enough to be able to hold the models.

## Playing with the models

Once you start a scala console you can use it like `ipython` to create instances of the scala classes we have, to load the models, check if dbpedia id's exist, add new dbpedia ids, add new surface forms etc..

Example:

```
var spotlightModel = org.idio.dbpedia.spotlight.Main.getSpotlightModel( "/Users/dav009/Downloads/tr/model/")
spotlightModel.showSomeSurfaceForms()
spotlightModel.getStatsForSurfaceForm("evrimleri")
spotlightModel.searchForDBpediaResource("ikimono_gakari_dbpedia_uri")
spotlightModel.addNew("ikimono_gakari_sf","ikimono_gakari_dbpedia_uri",1,Array())
spotlightModel.exportModels("/new/path/of/folder/model/")
```
