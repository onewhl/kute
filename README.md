# kut
[![Build](https://github.com/onewhl/kut/actions/workflows/gradle-build.yml/badge.svg?branch=main)](https://github.com/onewhl/kut/actions/workflows/gradle-build.yml)

Collects unit tests from Java repositories.

# Usage

1. Clone the repository

   ```git clone https://github.com/onewhl/kut```
2. Build jar file ```./gradlew shadowJar```, it will appear in the directory ```kut/build/libs```

3. Run the command

   ```java -jar ./build/libs/kut-1.0-SNAPSHOT-all.jar --projects="path/to/projects/file" --outputFormat="json" --outputPath="path/to/result/file"```

### Supported options

| Name                 | Description                                                                       |
|----------------------|-----------------------------------------------------------------------------------|
| ```--projects```     | Path to the file with paths to the projects to analyze.                           |
| ```--outputFormat``` | Format to store results in. Supported types: ```json```, ```sqlite```, ```csv```. |
| ```--outputPath```   | Path to put file with results to.                                                 |
