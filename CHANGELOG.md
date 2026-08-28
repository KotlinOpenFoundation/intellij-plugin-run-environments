<!-- Keep a Changelog guide -> https://keepachangelog.com -->
# Run Environments IntelliJ Plugin Changelog

## [Unreleased]

### Added

- Auto-detection of `run.env.json` and `run.private.env.json` files
  - in the project root
  - in submodules
  - in common stored run configuration directories: `.run/` and `.idea/runConfigurations/`
- Run environment selection with submenu with links to edit its environment files, or to create the missing file
- Environment selector offers to create a missing `run.env.json` or `run.private.env.json` in the project root and opens it
- Selected environment's variables injection into selected run configurations
- JVM-based run configurations support
- Grade-delegated run configurations support
- Environment selector only shows for supported run configurations
