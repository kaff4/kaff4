# kaff4 - Kotlin AFF4[-L]

![License](https://img.shields.io/github/license/Nava2/kaff4)

![Build](https://img.shields.io/github/actions/workflow/status/Nava2/kaff4/gradle-check-pristine.yml)
![Release](https://img.shields.io/github/v/release/Nava2/kaff4)

The kaff4 project is a [kotlin](https://kotlinlang.org/) implementation of
the [AFF4 file format](https://github.com/aff4/Standard).

This library is built to run in any JVM-based environment.

## Warranty

This software has no warranty, it might eat your lunch for all we know. Use at your own risk and cross-validate with
other tools. We are not responsible for any usages or outcomes from using this software.

## What works today

* Reading
    * Physical Images
    * Logical Images
        * Querying files/directories is not "first party" but can be easily implemented by querying
* Writing via `Aff4ContainerBuilder`
    * Physical Images
* Hashing
    * `MD5`, `Sha1`, `Sha256`, and `Sha512` types are supported
    * `Blake2b` is *not* supported (see [#77](https://github.com/Nava2/kaff4/issues/77))
* Compression
    * `lz4`, `snappy`, `deflate`, "none"
* Striped (segmented) images

## License - MIT

See `LICENSE` in the root of this project.

For individual projects, see `./gradlew :path:to:project:licenseReport`

## Examples

To see this project in use, the `kaff4-reader` provides a simple CLI that can verify supported images. For example:

```shell 
java -jar ./kaff4-reader-0.0.0-SNAPSHOT.jar verify test_images/base-linear_striped/Base-Linear_1.aff4
```

## Attributions

* [IntervalTree](https://github.com/Nava2/kinterval-tree) - [MIT](https://github.com/Nava2/kinterval-tree/LICENSE)
    - Mason M Lai
    - Kevin Brightwell

For individual projects, see `./gradlew :path:to:project:licenseReport`

## Development Dependencies

* Java 17 JVM: `brew install --cask temurin-17`

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