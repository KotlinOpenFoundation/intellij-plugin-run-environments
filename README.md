![Logo](assets/logo-text.png)

# Run Environments - IntelliJ Plugin

[![Build](https://github.com/KotlinOpenFoundation/intellij-plugin-run-environments/actions/workflows/build.yml/badge.svg)](https://github.com/KotlinOpenFoundation/intellij-plugin-run-environments/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/KotlinOpenFoundation/intellij-plugin-run-environments/graph/badge.svg)](https://codecov.io/gh/KotlinOpenFoundation/intellij-plugin-run-environments)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)][license]
[![Version](https://img.shields.io/jetbrains/plugin/v/33953.svg)][marketplace]
[![Version (pre-release)](https://img.shields.io/jetbrains/plugin/v/33953-eap.svg?label=pre-release)][marketplace]

A toolbar selector that injects `run.env.json`-defined environment variables into run configurations - switch
environments without editing the run configuration itself.

## Usage

Define named environments - `dev`, `uat`, `prod`, a local override, whatever you need - in a `run.env.json` file
(checked in) and an optional `run.private.env.json` (typically gitignored, for secrets/local overrides), using the
same format as the HTTP Client's environment files:

```json
{
  "dev": {
    "HOST": "dev.example.com"
  },
  "prod": {
    "HOST": "example.com"
  }
}
```

Drop one at the project root, or per module for a multi-module project with a different environment set per service.
A "Run with:" selector then appears in the main toolbar, next to the run widget:

- pick an environment, hit Run - its variables are injected into the launched process
- each entry's submenu links to the file(s) it's defined in, to edit or create them
- no environment file at all? the selector offers to create one at the project root instead of being a dead end

The selector only appears when the currently selected run configuration can actually receive the injected
variables, so it never sits there silently doing nothing for a configuration type it doesn't support.

### Supported run configurations

- Application / JUnit / TestNG / Kotlin (JVM) / JAR
- Maven's default (non-wrapper) execution
- Gradle (explicit run configuration)
- Any configuration delegated to "Build and run using: Gradle"

## Installation

Install from the JetBrains Marketplace: search for **Run Environments** in your IDE's
Settings/Preferences → Plugins → Marketplace.

## Contributing

See [CONTRIBUTING.md][contributing] for the project's module layout, extension points, and how to build, run, and
test the plugin locally.

## License

Licensed under the [Apache License, Version 2.0][license].

## Links

- [Kotlin Open Foundation][kof]

[license]: ./LICENSE
[contributing]: ./CONTRIBUTING.md
[marketplace]: https://plugins.jetbrains.com/plugin/33953
[kof]: https://kotlinopenfoundation.github.io/
