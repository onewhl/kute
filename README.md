# kut

[![Build](https://github.com/onewhl/kut/actions/workflows/gradle-build.yml/badge.svg?branch=main)](https://github.com/onewhl/kut/actions/workflows/gradle-build.yml)

```kut``` is a library that collects unit tests from Java and Kotlin repositories.

It automatically downloads projects from GitHub, parses source code to identify unit tests, searches for the
corresponding source methods, collects some metadata about the code, and stores the information in one of the following
formats: ```JSON```, ```CSV```
, ```SQLite```.

## Getting started

1. First, clone the repository

   ```git clone https://github.com/onewhl/kut```


2. Build jar file ```./gradlew shadowJar```, it will appear in the directory ```kut/build/libs```


3. To start processing the projects, run the command

   ```java -jar ./build/libs/kut-1.0-all.jar --projects="path/to/projects/file" --outputFormat="json" --outputPath="path/to/result/file"```

### Supported options

```kut``` is a command-line application that could be run with the following options:

| Name                 | Description                                                                       | 
|----------------------|-----------------------------------------------------------------------------------|
| ```--projects```     | Path to the file with links to projects on GitHub.                                |
| ```--outputFormat``` | Format to store results in. Supported types: ```json```, ```csv```, ```sqlite```. |
| ```--outputPath```   | Path to put file with results to.                                                 |
| ```--repoStorage```  | Path to the directory to clone repositories to.                                   |
