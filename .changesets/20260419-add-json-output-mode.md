---
"javachanges": patch
---

Add `--format json` output support for `render-vars`, `doctor-local`, and `doctor-platform`.

- Keep text output as the default for local operators.
- Return a single JSON object on stdout in JSON mode, with exit codes representing success or failure.
- Document the structured-output contract in the CLI reference, output contracts guide, and command cookbook.
