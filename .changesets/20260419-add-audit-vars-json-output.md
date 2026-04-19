---
"javachanges": patch
---

Add `--format json` support to `audit-vars`.

- Keep the existing text output as the default for local operators.
- Return a single JSON object on stdout for successful audits, audit mismatches, and common platform precondition failures.
- Update the CLI reference, output contracts, and command cookbook to document the new audit JSON contract.
