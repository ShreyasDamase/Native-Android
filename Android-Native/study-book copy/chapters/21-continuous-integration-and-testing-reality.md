# Chapter 21: Continuous Integration and Testing Reality

## Code Files To Open

- `.github/workflows/gradle.yml`
- `iosApp/iosAppTests/iosAppTests.swift`
- `iosApp/iosAppUITests/iosAppUITests.swift`


## 21.1 CI workflow

The GitHub workflow runs builds/tests for:

- iOS simulator target
- JVM target
- Android unit test targets

This tells you the repo is set up for multi-target validation.

## 21.2 Current testing depth

When I ran Gradle verification, the build succeeded, but many test tasks were `NO-SOURCE`.

That means:

- the project builds successfully
- test infrastructure exists
- actual tests are minimal

This is common in architecture samples.

## 21.3 What you should learn from this

The repo is strong as an architecture reference.

It is not strong as a testing reference.

So when you build your own app, keep the architecture lessons, but add much stronger tests than this sample provides.

---

