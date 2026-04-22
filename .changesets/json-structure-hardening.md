---
"javachanges": patch
---

Harden machine-readable JSON handling by replacing more hand-written JSON formatting and parsing with Jackson-based structured serialization.

- unify automation and env JSON output generation behind Jackson
- reduce regex-based GitLab API response parsing
- make release plan and changeset config JSON handling more robust
