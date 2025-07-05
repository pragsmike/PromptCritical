Of course. The recent refactoring work has significantly improved the architecture, and the design document must be updated to reflect this new, clearer structure.

Here is the revised `docs/DESIGN.md`.

---

# PromptCritical System Design

**Version 1.3 · 2025‑07‑05**
**Status:** *Refactoring for v0.3*

---

## 1  Purpose & Scope

PromptCritical is a **data‑driven, evolutionary framework** for discovering high‑performance prompts for Large Language Models (LLMs). This document describes the current architecture, which follows **Polylith** conventions to realize an experiment loop of ***bootstrap → contest (Failter) → record → evolve***.

The design has two guiding principles:

1.  **Immutable provenance.** Every prompt artefact carries its full lineage, a cryptographic hash, and a creation timestamp.
2.  **Replaceable adapters.** External tools (Failter, future surrogate critics, dashboards) are integrated behind thin, testable boundaries.

---

## 2  Polylith Architecture Overview

The codebase is organized into re-usable **components** and runnable **bases**. This separation ensures that core logic is reusable by any entry point (e.g., CLI, a future web UI).

| Layer | Name (ns prefix) | Role |
| :--- | :--- | :--- |
| **Component** | `pcrit.command` | **Reusable, high-level workflows** (e.g., `bootstrap!`) |
| **Component** | `pcrit.expdir` | **Manages the physical layout** of an experiment directory |
| **Component** | `pcrit.pdb` | **Immutable prompt database** (file I/O, locking, ID generation) |
| **Component** | `pcrit.pop` | **Population domain model** and prompt analysis (`:prompt-type`) |
| **Component** | `pcrit.config` | Central configuration map & helpers |
| **Component** | `pcrit.log` | Structured logging and log setup |
| **Component** | `pcrit.llm` | Thin HTTP client façade for LLMs |
| **Component** | `pcrit.test-helper` | Shared utilities for the test suite |
| **Base** | `pcrit.cli` | **Command‑line interface** (`pcrit …`) entry point |

```
workspace/
├── components/
│   ├── command/      ; high-level user commands (bootstrap, contest)
│   ├── expdir/       ; experiment directory layout logic
│   ├── pdb/          ; prompt DB internals
│   ├── pop/          ; population + analysis logic
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

### 3.1 Orchestration (`pcrit.command.*`)

This component contains the high-level, end-to-end logic for user-facing commands. It orchestrates calls to other, lower-level components to execute a workflow.
*   **`bootstrap!`**: The implementation of the bootstrap process, reusable by any base.

### 3.2 Experiment Directory (`pcrit.expdir.*`)

This component is the single source of truth for the physical file system layout of an experiment.
*   Provides functions to get paths to standard subdirectories (`get-pdb-dir`, `get-links-dir`).
*   Handles creation of the directory structure and symbolic links.

### 3.3 Prompt Database (`pcrit.pdb.*`)

*   **Atomic writes & fsync.** Guarantees crash‑safe updates via `atomic-write!`.
*   **Per‑file locks.** Implements a self‑healing lockfile protocol.
*   **ID generation.** Atomically assigns unique `Pnnn` identifiers.

### 3.4 Population & Analysis (`pcrit.pop.*`)

Holds the core domain logic for prompts and populations, but *not* high-level orchestration.
*   **Ingestion Primitives**: Provides functions to read and ingest raw prompts from manifests.
*   **Prompt Analysis**: The `analyze-prompt-body` function inspects prompt text to add critical metadata, including the `:prompt-type`.

---

## 4  Data Model – Prompt Record

The prompt remains the central data artifact. Upon creation, its header is now enriched with a `:prompt-type` to make its intended role explicit.

```clojure
{:header {:id "P123",
          :created-at "…",
          :sha1-hash "…",
          :spec-version "1",
          :prompt-type :object-prompt,  ; <--- New, critical metadata
          ...}
 :body   "Canonical prompt text…\n"}
```

*   **Prompt Types**: Inferred from special template variables (`{{INPUT_TEXT}}` for `:object-prompt`, `{{OBJECT_PROMPT}}` for `:meta-prompt`). This allows the system to validate prompts at creation time.

---

## 5  Experiment Flow (v0.2)

```
bootstrap → contest (pack → run Failter) → record (report.csv) → prepare generation N+1
```

1.  **Bootstrap** (`pcrit bootstrap <exp-dir>`)
    *   The `cli` base calls the `pcrit.command/bootstrap!` function.
    *   The command component orchestrates `expdir` and `pop` to create the directory structure and ingest the initial prompts from `bootstrap.edn`.
2.  **Contest** (`pcrit contest …`)
    *   Packages selected prompts into a Failter-compatible directory and executes `failter`.
3.  **Record**
    *   Parses `report.csv` and updates the experiment's history.

---

## 6  Concurrency & Integrity Guarantees

1.  **Atomic replace** for every file write.
2.  **Per‑resource lockfiles** with jittered retries and stale‑lock healing.
3.  **Hash verification** on every read.

---

## 7  Extensibility Roadmap

*(This section remains unchanged.)*

---

## 8  Open Issues & Next Steps

*   **Refactor to use a `Context Map`**: The most critical next step is to refactor all command functions to accept a single `ctx` map instead of multiple arguments (`exp-dir`, `config`, etc.). This will simplify function signatures and make the application state more explicit and easier to reason about.
*   **Add end‑to‑end smoke test**: Implement a test for the full `bootstrap` → `contest` → `record` loop (using a mocked Failter) in the CI matrix.
*   **Expose lock back‑off parameters**: Move hard-coded locking timeouts into `pcrit.config`.

---

*Last updated 2025‑07‑05*
