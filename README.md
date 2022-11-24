# kut

[![Build](https://github.com/onewhl/kut/actions/workflows/gradle-build.yml/badge.svg?branch=main)](https://github.com/onewhl/kut/actions/workflows/gradle-build.yml)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

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

   ```java -jar ./build/libs/kut-1.0-all.jar --projects="path/to/projects/file" --output-format="json" --output-path="path/to/result/file"```

### Supported options

```kut``` is a command-line application that could be run with the following options:

| Name                  | Description                                                                         | 
|-----------------------|-------------------------------------------------------------------------------------|
| ```--projects```      | Path to the file with links to projects on GitHub.                                  |
| ```--output-format``` | Format to store results in. Supported types: ```json```, ```csv```, ```sqlite```.   |
| ```--output-path```   | Path to put file with results to.                                                   |
| ```--repo-storage```   | Path to the directory to clone repositories to.                                     |
| ```--io-threads```    | Number of threads used for downloading Git repos. Use 0 for common pool. Default: 1 |
| ```--cpu-threads```   | Number of threads used for processing projects. Use 0 for common pool. Default: 0   |
