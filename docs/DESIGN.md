# PromptCritical System Design

**Version 1.7 · 2025‑07‑10**
**Status:** *Implementing `select` command*

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
| **Component** | `pcrit.command` | **Reusable, high-level workflows** (e.g., `bootstrap!`, `vary!`) |
| **Component** | `pcrit.experiment` | **Defines the logical Experiment context** passed between components |
| **Component** | `pcrit.expdir` | **Manages the physical layout** of an experiment directory |
| **Component** | `pcrit.failter` | **Adapter for the external Failter toolchain** |
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
│   ├── command/      ; high-level user commands
│   ├── experiment/   ; logical experiment context
│   ├── expdir/       ; experiment directory layout logic
│   ├── failter/      ; adapter for the external failter tool
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
*   **`bootstrap!`**: The implementation of the bootstrap process.
*   **`vary!`**: The implementation of the population breeding process.
*   **`evaluate!`**: The implementation of the population evaluation process.

### 3.2 Experiment Directory (`pcrit.expdir.*`)

This component is the single source of truth for the physical file system layout of an experiment.
*   Provides functions to get paths to standard subdirectories (`get-pdb-dir`, `get-generation-dir`, `get-contest-dir`).
*   Handles creation of the directory structure and symbolic links.

### 3.3 Prompt Database (`pcrit.pdb.*`)

*   **Atomic writes & fsync.** Guarantees crash‑safe updates via `atomic-write!`.
*   **Per‑file locks.** Implements a self‑healing lockfile protocol.
*   **ID generation.** Atomically assigns unique `Pnnn` identifiers.

### 3.4 Population & Analysis (`pcrit.pop.*`)

Holds the core domain logic for prompts and populations, but *not* high-level orchestration.
*   **Ingestion & Management**: Provides functions to ingest raw prompts, load a population from a generation, and create new generation directories.
*   **Prompt Analysis**: The `analyze-prompt-body` function inspects prompt text to add critical metadata, including the `:prompt-type`.

### 3.5 Failter Adapter (`pcrit.failter.*`)

This component is a thin adapter that encapsulates all interaction with the external `failter` toolchain.
*   Prepares the `failter-spec` directory with the required symlinks.
*   Executes `failter` sub-commands via `clojure.java.shell`.
*   Captures and stores the resulting `report.csv`.

---

## 4  Data Model – Prompt Record

The prompt remains the central data artifact. Upon creation, its header is now enriched with a `:prompt-type` to make its intended role explicit.

```clojure
{:header {:id "P123",
          :created-at "…",
          :sha1-hash "…",
          :spec-version "1",
          :prompt-type :object-prompt,
          :parents ["P1"],
          :generator {:model "mistral", :meta-prompt "P2"}
          ...}
 :body   "Canonical prompt text…\n"}
```

*   **Prompt Types**: Inferred from special template variables (`{{INPUT_TEXT}}` for `:object-prompt`, `{{OBJECT_PROMPT}}` for `:meta-prompt`). This allows the system to validate prompts at creation time.
*   **Ancestry**: When the `vary` command creates a new prompt, it populates the `:parents` and `:generator` fields to preserve its lineage.

---

## 5  Experiment Flow (v0.2)

```
bootstrap → evaluate → select → vary
```

1.  **Bootstrap** (`pcrit bootstrap <exp-dir>`)
    *   The `cli` base calls the `pcrit.command/bootstrap!` function.
    *   The command component orchestrates `expdir` and `pop` to create the directory structure and ingest the initial prompts from `bootstrap.edn`, creating `gen-0`.

2.  **Evaluate** (`pcrit evaluate <exp-dir> --name <contest-name> ...`)
    *   The `cli` base calls the `pcrit.command/evaluate!` function.
    *   The command validates user options (generation, contest name, inputs).
    *   It then orchestrates the `pcrit.failter` component to prepare a `failter-spec` directory, execute the external `failter` toolchain, and capture the resulting `report.csv`.

3.  **Vary** (`pcrit vary <exp-dir>`)
    *   Loads `evolution-parameters.edn` using the `pcrit.config` component.
    *   Loads the population from the latest generation.
    *   Applies meta-prompts to generate new offspring prompts.
    *   Creates a new generation directory containing links to the full new population (survivors + offspring).

4.  **Select** (`pcrit select <exp-dir>`)
    *   Reads `report.csv` from one or more contests.
    *   Applies a selection strategy to determine which prompts survive.
    *   Creates a new generation directory containing links to only the surviving members.

---

## 6  Concurrency & Integrity Guarantees

1.  **Atomic replace** for every file write.
2.  **Per‑resource lockfiles** with jittered retries and stale‑lock healing.
3.  **Hash verification** on every read.

---

## 7  Extensibility Roadmap

*(Future work will detail this section.)*

---

## 8  Open Issues & Next Steps

*   **Implement `select` command**: Use contest results from the `evaluate` step to eliminate less-fit members and create a new, smaller survivor population.
*   **Add end‑to‑end smoke test**: Implement a test for the full `bootstrap` → `evaluate` → `select` → `vary` loop (using a mocked Failter) in the CI matrix.

---

*Last updated 2025‑07‑10*
