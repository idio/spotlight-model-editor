# Spotlight Model's Editor

## Compiling

make sure you have `mvn` in your system

### Compiling Dbpedia Spotlight

DBpedia Spotlight's jar is one of the dependencies. These steps will guide you on how to compile spotlight

1. clone `https://github.com/dbpedia-spotlight/dbpedia-spotlight`
2. compile dbpedia spotlight: 
  - `mvn assembly:single`
  - `mvn package`

after this step there should be a `dbpedia-spotlight-0.6-jar-with-dependencies.jar` in the `target` folder.

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