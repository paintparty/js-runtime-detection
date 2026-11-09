# Changelog

All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](https://keepachangelog.com/).

## [0.1.0] - 2026-11-09

### Added
- Initial release. Extracted from `sugar.js-runtime-detection` in [sugar](https://github.com/paintparty/sugar).
- `js-runtime`, `js-platform`, `js-server-runtime?`, `js-runtime-has-dom?`, `js-runtime-has-jsdom?`, `js-runtime-has-node-apis?` and `js-runtime-is-strict-node?`.

### Fixed
- `js-runtime-has-dom?` now returns a boolean. In sugar it returned the `document` object or `nil`.
