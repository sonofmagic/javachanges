---
description: Public LLM-facing documentation endpoints for javachanges, including llms.txt and the full documentation bundle.
---

# LLM Access

`javachanges` publishes stable documentation endpoints for AI tools, crawlers, and agent workflows.

## Public endpoints

| Resource | URL | Purpose |
| --- | --- | --- |
| Landing page | `https://javachanges.icebreaker.top/llms-access` | Human-readable index for AI-oriented docs access |
| Compact index | `https://javachanges.icebreaker.top/llms.txt` | Small link list pointing to the main English docs |
| Full bundle | `https://javachanges.icebreaker.top/llms-full.txt` | Single-file export of the full English documentation set |

## Notes

- `llms.txt` is intended for discovery and lightweight retrieval.
- `llms-full.txt` is intended for offline ingestion, long-context agents, and documentation sync jobs.
- The generated LLM bundle currently includes English documentation pages only.
- Chinese documentation remains available on the website, but is intentionally excluded from the generated LLM bundle to avoid duplicate bilingual context.

## Recommended usage

For AI systems that only need a site index, start with:

```text
https://javachanges.icebreaker.top/llms.txt
```

For AI systems that need the entire docs corpus in one request, use:

```text
https://javachanges.icebreaker.top/llms-full.txt
```

If you need normal page navigation, use the documentation homepage:

```text
https://javachanges.icebreaker.top/
```
