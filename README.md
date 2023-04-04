# kaff4

The kaff4 project is a [kotlin](https://kotlinlang.org/) implementation of
the [AFF4 file format](https://github.com/aff4/Standard).

This library is build to run in any JVM-based environment.

## What works today

* Reading
    * Physical Images
    * Logical Images
        * Querying files/directories is not "first part" but supported
* Writing via `Aff4ContainerBuilder`
    * Physical Images

### Hashing

* `MD5`, `Sha1`, `Sha256`, and `Sha512` types are supported
* **Blake2b** is not supported

### Compression

* `ZipSegment`, and `snappy` are supported
* All others are not supported at this time - these can be supported by creating a module similar to
  `aff4-compression:aff4-compression-snappy`

## Attributions

* [IntervalTree](https://github.com/charcuterie/interval-tree/blob/65dc2fc8f754127aa09fba0dff6f43b10ac151cb/src/datastructures/IntervalTree.java)
    - [MIT License (MIT)](https://github.com/charcuterie/interval-tree/blob/65dc2fc8f754127aa09fba0dff6f43b10ac151cb/LICENSE)
    - Mason M Lai

For individual projects, see `./gradlew :path:to:project:licenseReport`

## License - MIT

See `LICENSE` in the root of this file.

For individual projects, see `./gradlew :path:to:project:licenseReport`

## TODO

* General
    * [ ] Add "plugin" support for optional features
    * [ ] Package into jitpack
* [ ] Add documentation on general architecture
    * Eclipse RDF4J, witness scoping, queries
* [x] Add attributions for licenses
* [ ] CLI
    * [ ] Add create command
    * [ ] Create report for `verify` subcommand
* [ ] Logical
    * [ ] Implement file system walking

## Development Dependencies

* Java 19 JVM: `brew install --cask temurin`

### Releasing

```shell
# Clean the repo first to not have any old artifacts
./gradlew clean

# Verify the repo is in good shape
./gradlew check

# Tag a version
git tag v0.0.0

# Publish a new build - BE MINDFUL OF SHELL HISTORY PRESERVING ENVIRONMENT VARIABLES
ORG_GRADLE_PROJECT_signingKey=${SIGNING_KEY} \                                                                                                                   [14:52:36]
ORG_GRADLE_PROJECT_signingPassword=${SIGNING_PASSWORD} \
OSSRH_USERNAME=${OSSRH_USERNAME} \
OSSRH_PASSWORD=${OSSRH_PASSWORD} \
RELEASE=1 \
./gradlew build publishToSonatype closeAndReleaseSonatypeStagingRepository

# Push tags to github
git push --tags

# Create a new release: https://github.com/Nava2/kaff4/releases
```