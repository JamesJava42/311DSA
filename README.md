[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/j4-aS696)

## Java 21 upgrade notes

This project has been updated to target Java 21 (latest LTS). To build and run:

- Install a JDK 21 distribution (Adoptium/Eclipse Temurin, Oracle JDK 21, or other).
- Ensure `java` and `javac` on your PATH point to the JDK 21 installation.
- If using Ant, make sure Ant supports Java 21 or invoke the compiler with the `--release` flag via an updated `javac` task.

Example (macOS/zsh):

```bash
# Install using Homebrew (Temurin 21 as an example)
brew install --cask temurin21

# Verify
java -version
javac -version

# Build with Ant
ant clean build
```

If you cannot upgrade your local Ant or JDK, set `source` and `target` back to `1.8` in `build.xml`.

Note: If `ant` is not installed (as on some macOS systems), install it via Homebrew:

```bash
brew install ant
```

After installing Ant and JDK 21, run `ant clean build` from the project root.
