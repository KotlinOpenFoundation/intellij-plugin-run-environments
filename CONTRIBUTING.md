# Contributing

## Plugin structure

This is a modular IntelliJ Platform plugin using **content modules**, split so the UI stays frontend-only and the
business logic stays backend-only - this is what makes it work natively in **[split mode][docs:remote-dev]**, not
just in the ordinary monolithic IDE. Frontend and backend communicate exclusively through RPC (the `shared` module's
`EnvironmentsApi`).

```
.
├── .run/                    Predefined Run/Debug configurations (runIde, backend, frontend, split mode)
├── shared/                  RPC contracts + DTOs both sides depend on: EnvironmentsApi, EnvironmentInfo, ...
├── frontend/                UI only: the toolbar combo box, its actions, and the RPC client-side service
├── backend/                 Project-scoped business logic: environment aggregation, file scanning/creation,
│                            the RPC server-side implementation, and the two extension points below
├── backend-java/            Optional module: injects variables into JVM-based run configurations
├── backend-gradle/          Optional module: injects variables into Gradle-executed run configurations
├── gradle/
│   ├── wrapper/             Gradle Wrapper
│   └── libs.versions.toml   Version catalog
├── src
│   ├── main/resources/META-INF/  Root plugin.xml, plugin icon
│   └── test/                Every test in the plugin - see "Tests and coverage" below
├── build.gradle.kts         Root build - assembles the final plugin, wires Kover's aggregated report
└── settings.gradle.kts      Gradle project settings
```

Each module ships its own plugin descriptor next to its sources (e.g.
`frontend/src/main/resources/kotlinopenfoundation-intellij-plugin-run-environments.frontend.xml`), referenced from
the root [plugin.xml][file:plugin.xml] via `<content><module .../></content>`.

`backend-java` and `backend-gradle` load `optional="true"` - each depends on an external plugin (Java/Maven, Gradle)
that might not be installed, so their absence degrades gracefully to "that run configuration type just isn't
supported" instead of failing to load the whole plugin.

## Extending

Other plugins can contribute additional environment sources or injection support for other run configuration types
without depending on this plugin's internals, via two extension points declared in the `backend` module:

- `io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.environmentSource` -
  implement [`EnvironmentSource`][file:EnvironmentSource] to contribute environments from somewhere other than a
  `run.env.json` file (a `.env` file, deployment config, ...).
- `io.github.kotlinopenfoundation.tools.intellij.plugin.runenvironments.environmentInjectionSupport` -
  implement [`EnvironmentInjectionSupport`][file:EnvironmentInjectionSupport] to inject variables into another run
  configuration type, and to tell the frontend it's now supported.

## Building and running

```bash
./gradlew build          # compiles, tests, verifies the plugin
./gradlew runIde         # launches a sandbox IDE with the plugin installed (monolith mode)
```

The `.run/` directory has predefined Run/Debug configurations for the split-mode scenario:

| Configuration                    | What it does                                                          |
|----------------------------------|-----------------------------------------------------------------------|
| Run IDE with Plugin (Backend)    | Runs [`:runIdeBackend`][docs:intellij-platform-gradle-plugin-runIde]  |
| Run IDE with Plugin (Frontend)   | Runs [`:runIdeFrontend`][docs:intellij-platform-gradle-plugin-runIde] |
| Run IDE with Plugin (Split Mode) | Runs both simultaneously, launching the plugin in actual split mode   |

## Tests and coverage

Every test lives in the root project's `src/test`, while the code under test lives in `shared`, `frontend`,
`backend`, `backend-java`, and `backend-gradle` - each module applies Kover and is pulled into the root project's
report via a `kover(...)` dependency, so a single run covers the whole plugin:

```bash
./gradlew check              # runs tests, then regenerates the Kover reports below
./gradlew koverHtmlReport     # build/reports/kover/html/index.html
./gradlew koverXmlReport      # build/reports/kover/report.xml, JaCoCo-schema-compatible, for CI/SonarQube
```

Generated code - `$serializer` classes of `kotlinx.serialization` and the RPC plugin's `$_generated_` stubs - is
filtered out of the report in [build.gradle.kts][file:build.gradle.kts].

> [!NOTE]
> IntelliJ's own built-in "Run with Coverage" action uses its own JaCoCo-based coverage runner, which doesn't
> instrument tests correctly under the IntelliJ Platform Test Framework - it's not wired up here, and will
> misleadingly show 0% coverage if used. Read the Kover HTML report instead.

## Useful links

- [IntelliJ Platform Plugin SDK][docs]
- [IntelliJ Platform Gradle Plugin Documentation][docs:intellij-platform-gradle-plugin-docs]
- [Plugin Content Modules / Split Mode][docs:remote-dev]

[docs]: https://plugins.jetbrains.com/docs/intellij
[docs:plugin.xml]: https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html
[docs:publishing]: https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html
[docs:remote-dev]: https://plugins.jetbrains.com/docs/intellij/plugin-content-modules.html
[docs:intellij-platform-gradle-plugin-docs]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
[docs:intellij-platform-gradle-plugin-runIde]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#runIde

[file:build.gradle.kts]: ./build.gradle.kts
[file:plugin.xml]: ./src/main/resources/META-INF/plugin.xml
[file:EnvironmentSource]: ./backend/src/main/kotlin/io/github/kotlinopenfoundation/tools/intellij/plugin/runenvironments/backend/api/EnvironmentSource.kt
[file:EnvironmentInjectionSupport]: ./backend/src/main/kotlin/io/github/kotlinopenfoundation/tools/intellij/plugin/runenvironments/backend/api/EnvironmentInjectionSupport.kt
