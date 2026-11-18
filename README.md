# Library of Things

A lending platform for communities (apartment buildings, dorms, neighbourhoods) where members
lend everyday items to each other (tools, camping gear, electronics, kitchen equipment) instead
of everyone buying their own.

This repository contains the core lending engine, written in Java and tested with JUnit 5.

## Requirements

- Java 17 or newer
- Maven 3.9+

## Build and test

```bash
mvn test
```

The code coverage report is generated at `target/site/jacoco/index.html`.

## Try it in the terminal

```bash
mvn -q compile
java -cp target/classes com.libraryofthings.Main
```

Choose `13` to load sample members and items, then try borrowing, returning and renewing.

## Project structure

```
src/main/java/com/libraryofthings/   application code
src/test/java/com/libraryofthings/   JUnit 5 tests
docs/                                requirements and test documentation
```
