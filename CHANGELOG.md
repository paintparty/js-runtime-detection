# Changelog

All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](https://keepachangelog.com/).

## [0.1.0] - 2026-11-09

### Added
- Initial release. Extracted from `sugar.js-runtime-detection` in [sugar](https://github.com/paintparty/sugar).
- `js-runtime`, `js-platform`, `js-server-runtime?`, `js-runtime-has-dom?`, `js-runtime-has-jsdom?`, `js-runtime-has-node-apis?` and `js-runtime-is-strict-node?`.
- Tests that compile with `:advanced` optimizations and run on Node, with and without jsdom globals.
- Tests that run in workerd (Cloudflare Workers) through Miniflare, with and without `nodejs_compat`, and on ClojureScript 1.9.946, the oldest supported version.

### Fixed
Compared with the version in sugar:
- `:advanced` builds no longer report Node as a browser. Nested property reads such as `process.versions.node` were renamed by the Closure compiler, so `js-server-runtime?` and `js-runtime-has-node-apis?` were `false` on Node. All reads now use string keys.
- jsdom in Node reports `js-runtime` as `:node`, as documented. It was `:browser`.
- `js-runtime-is-strict-node?` is `false` in Deno and Bun. Deno's Node compatibility layer sets `process.release.name` to `"node"`, so Deno was reported as strict Node. Cloudflare Workers with `nodejs_compat` do the same and are also excluded.
- Loading the namespace no longer prints assertion warnings when `:checked-arrays` is enabled.
- `js-runtime-has-dom?` returns a boolean. It returned the `document` object or `nil`.
