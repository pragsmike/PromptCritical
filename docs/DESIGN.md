# PromptCritical System Design

**Version 2.1 · 2025‑07‑14**
**Status:** *v0.3 Automated Evolution Complete*

---

## 1  Purpose & Scope

PromptCritical is a data-driven, evolutionary framework for discovering high-performance prompts for Large Language Models (LLMs). This document describes the current architecture, which follows **Polylith** conventions to realize an experiment loop of ***bootstrap → vary → evaluate → select***.

The design has two guiding principles:

1.  **Immutable provenance.** Every prompt artefact carries its full lineage, a cryptographic hash, and a creation timestamp.
2.  **Replaceable adapters.** External tools (Failter, future surrogate critics, dashboards) are integrated behind thin, testable boundaries.

---

## 2  Polylith Architecture Overview

The codebase is organized into re-usable **components** and runnable **bases**. This separation ensures that core logic is reusable by any entry point (e.g., CLI, a future web UI).

| Layer | Name (ns prefix) | Role |
| :--- | :--- | :--- |
| **Component** | `pcrit.command` | **Reusable, high-level workflows** (e.g., `bootstrap!`, `evolve!`) |
| **Component** | `pcrit.experiment` | **Defines the logical Experiment context** passed between components |
| **Component** | `pcrit.expdir` | **Manages the physical layout** of an experiment directory |
| **Component** | `pcrit.failter` | **Generates `spec.yml` and runs the `failter run` command** |
| **Component** | `pcrit.pdb` | **Immutable prompt database** (file I/O, locking, ID generation) |
| **Component** | `pcrit.pop` | **Population domain model** and prompt analysis (`:prompt-type`) |
| **Component** | `pcrit.reports` | **Parses contest results** (legacy CSV & new Failter JSON) and writes reports |
| **Component** | `pcrit.config` | Central configuration map & helpers |
| **Component** | `pcrit.log` | Structured logging and log setup |
| **Component** | `pcrit.llm` | Thin HTTP client façade for LLMs |
| **Component** | `pcrit.test-helper` | Shared utilities for the test suite |
| **Base** | `pcrit.cli` | **Command‑line interface** (`pcrit …`) entry point |

```
workspace/
├── components/
│   ├── command/      ; high-level user commands (init, bootstrap, evolve, etc.)
│   ├── experiment/   ; logical experiment context
│   ├── expdir/       ; experiment directory layout logic
│   ├── failter/      ; adapter for the external failter tool
│   ├── pdb/          ; prompt DB internals
│   ├── pop/          ; population + analysis logic
│   ├── reports/      ; contest result parsing
│   ├── config/       ; runtime config
│   ├── log/          ; logging helpers
│   ├── llm/          ; LLM HTTP client
│   └── test-helper/  ; shared test utilities
└── bases/
    └── cli/          ; main – invokes components
```

### 2.1  Why Polylith?

*   **Clear contracts.** Components expose stable, public interfaces, hiding implementation details.
*   **Incremental builds/tests.** Polylith’s tooling runs tests only for affected components, enabling rapid development.
*   **Multi‑base future.** Additional bases (e.g., `pcrit.web` dashboard) can reuse the `pcrit.command` component without code duplication.

---

## 3  Core Components

The descriptions from the previous version remain accurate.

---

## 4  Data Model – Prompt Record

The descriptions from the previous version remain accurate.

---

## 5  Experiment Flow (v0.3)

```
bootstrap → [ vary → evaluate → select ]*
```
The new `evolve` command automates the core loop.

1.  **Bootstrap** (`pcrit bootstrap <exp-dir>`)
    *   The `cli` base calls the `pcrit.command/bootstrap!` function to create `gen-0`.

2.  **Evolve** (`pcrit evolve <exp-dir> --generations N ...`)
    *   The `cli` base calls `pcrit.command/evolve!`, which loops for `N` generations.
    *   In each loop, it calls `pcrit.command/vary!`, `pcrit.command/evaluate!`, and `pcrit.command/select!` in sequence.
    *   It tracks cumulative cost and can halt early if a budget is exceeded.

---

## 6  Concurrency & Integrity Guarantees

The descriptions from the previous version remain accurate.

---

## 7  Extensibility Roadmap

*(Future work will detail this section.)*

---

## 8  Open Issues & Next Steps

*   **Implement `evolve` command (v0.3)**: (`✅ Done`) The `evolve!` function and corresponding CLI command have been implemented, automating the core evolutionary cycle.
*   **Add end‑to‑end smoke test**: Implement a test for the full `bootstrap` → `evolve` loop (using a mocked Failter) in the CI matrix. This is the next priority.
*   **Refactor test helpers**: Consolidate test setup logic into a shared `test-helper` namespace to reduce duplication and improve test reliability.

---

*Last updated 2025‑07‑14*
