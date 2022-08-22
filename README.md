# kut

Collects unit tests from Java repositories.

# Usage

1. Clone the repository

   ```git clone https://github.com/onewhl/kut```
2. Build jar file

   ```./gradlew shadowJar```, it will appear in the directory ```kut/build/libs```
3. Run the command

   ```java -jar ./build/libs/kut-1.0-SNAPSHOT-all.jar --path="path/to/project" --output="path/to/result/file"```

### Supported options

```
  --path          Path to the project to analyze.
  --output        Path to put file with results to.
```