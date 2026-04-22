# Repository Guidelines

## Project Structure & Module Organization

Core Java sources live in `src/main/java/io/github/sonofmagic/javachanges`, split between `core` for the CLI/runtime and `maven` for plugin Mojos. Tests live in `src/test/java` with matching package paths. Documentation content is in `docs/`, the VitePress site source is in `website/`, and runnable example repositories live under `examples/basic-monorepo/`. Release inputs and templates also matter here: `.changesets/` stores changeset files, `env/` holds release env examples, and `scripts/` contains local release helpers.

## Build, Test, and Development Commands

Use Maven for product code and `pnpm` only for docs and local release shortcuts.

- `mvn -B verify`: full CI-style build, tests, and packaging checks.
- `mvn -B test`: run the JUnit 5 suite without full publish-profile verification.
- `mvn -B -DskipTests compile exec:java -Dexec.args="status --directory $PWD"`: run the CLI from source.
- `pnpm snapshot:install`: install the current snapshot into the repo-local `.m2/repository`.
- `pnpm docs:build`: build the VitePress docs into `website/dist`.
- `pnpm docs:deploy:local`: rebuild docs and preview them locally with Wrangler.

## Coding Style & Naming Conventions

Follow the existing Java style: 4-space indentation, braces on the same line, and `UpperCamelCase` for classes with `lowerCamelCase` for methods and fields. Keep package names under `io.github.sonofmagic.javachanges.*`. Test classes should mirror the production class name with a `Test` suffix, for example `VersionSupportTest`. No dedicated formatter or linter is configured in this repo, so keep imports tidy and match surrounding code instead of reformatting unrelated files.

## Testing Guidelines

This project uses JUnit 5 with Surefire. Add or update tests in `src/test/java` whenever CLI behavior, release planning, or Maven plugin integration changes. Prefer focused unit tests using `@TempDir` for filesystem-heavy flows. Run `mvn -B test` locally before opening a PR; use `mvn -B verify` for CI-parity checks.

## Commit & Pull Request Guidelines

Recent history follows Conventional Commits such as `feat:`, `fix:`, `docs:`, and `chore(release):`. Keep commit subjects imperative and scoped to one change. If a PR adds user-visible functionality or changes release behavior, add a matching changeset in `.changesets/` using the package-map frontmatter format shown in `README.md`. Pull requests should include a short problem statement, the concrete behavior change, the verification commands you ran, and note the new changeset file when one is required. Link the related issue when applicable, and include screenshots only for docs/site UI changes under `website/` or `docs/`.
