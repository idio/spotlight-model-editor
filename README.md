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
