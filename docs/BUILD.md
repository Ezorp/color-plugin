## Build Guide

### Prerequisites
- JDK 8 (1.8)
- Maven 3.8+
- Internet access to download dependencies (Spigot API, ProtocolLib as provided)

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

Artifacts:
- The plugin JAR will be generated in `target/` (e.g., `color-0.1.XX.jar`).

### Notes
- The project targets Spigot `1.8.8-R0.1-SNAPSHOT`.
- `ProtocolLib` is declared with `scope: provided`; it's required at runtime on the server for packet-based coloring, but not bundled inside the JAR.

