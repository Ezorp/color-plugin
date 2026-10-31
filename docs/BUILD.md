## Build Guide

### Prerequisites
- JDK 8 (1.8)
- Maven 3.8+
- Internet access on the first build, to download the Spigot API from the Spigot repository

Verify Java and Maven:
```bash
java -version
mvn -v
```

### Clone and Build
```bash
git clone https://github.com/Ezorp/color-plugin.git color
cd color
mvn clean package -DskipTests
```

Once the dependencies are cached in `~/.m2`, you can add `-o` to build offline.

Artifacts:
- The plugin JAR is generated in `target/`, named after the version in `pom.xml` (e.g. `color-1.0.2.jar`).

### Notes
- The project targets Spigot `1.8.8-R0.1-SNAPSHOT` and compiles with Java 8 source/target.
- The plugin has no runtime dependencies other than the Spigot server itself.
- The version appears in both `pom.xml` and `src/main/resources/plugin.yml`. Keep them in sync when releasing.
- There is no automated test suite. Test changes on a local Spigot 1.8.8 server.
