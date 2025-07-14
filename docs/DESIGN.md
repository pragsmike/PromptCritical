# PromptCritical System Design

**Version 2.0 · 2025‑07‑14**
**Status:** *v0.3 Failter Refactoring Complete*

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
│   ├── command/      ; high-level user commands
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
bootstrap → vary → evaluate → select
```

1.  **Bootstrap** (`pcrit bootstrap <exp-dir>`)
    *   The `cli` base calls the `pcrit.command/bootstrap!` function.
    *   The command component orchestrates `expdir` and `pop` to create the directory structure and ingest the initial prompts from `bootstrap.edn`, creating `gen-0`.

2.  **Vary** (`pcrit vary <exp-dir>`)
    *   Loads `evolution-parameters.edn` using the `pcrit.config` component to determine the model for variation.
    *   Loads the population from the latest generation.
    *   Applies meta-prompts to generate new offspring prompts, adding them to the *current* generation.

3.  **Evaluate** (`pcrit evaluate <exp-dir> --name <contest-name> ...`)
    *   The `cli` base calls the `pcrit.command/evaluate!` function.
    *   The command loads `evolution-parameters.edn` using the `pcrit.config` component to get the list of models to test against and the default judge model.
    *   It then orchestrates the `pcrit.failter` component to generate a `spec.yml` file defining the entire contest.
    *   The `failter` component executes the external `failter run --spec <path>` command and captures the resulting JSON report from `stdout`.
    *   Finally, the `command` component passes this JSON data to `pcrit.reports`, which calculates costs for each run and writes the final, canonical `report.csv`.

4.  **Select** (`pcrit select <exp-dir> --from-contest <name>`)
    *   The `cli` base calls `pcrit.command/select!`.
    *   The command uses the `pcrit.reports` component to parse the specified `report.csv`.
    *   It applies a selection strategy (defaulting to `top-N=5`) to determine the survivors.
    *   It calls `pdb/update-metadata` to append a `:selection` event to each survivor's header.
    *   Finally, it calls `pop/create-new-generation!` to create the next generation directory populated with links to the surviving members.

---

## 6  Concurrency & Integrity Guarantees

The descriptions from the previous version remain accurate.

---

## 7  Extensibility Roadmap

*(Future work will detail this section.)*

---

## 8  Open Issues & Next Steps

*   **Implement `evolve` command (v0.3)**: Create a high-level command that composes the `vary`, `evaluate`, and `select` steps into an automated evolutionary loop, running for a specified number of generations.
*   **Add end‑to‑end smoke test**: Implement a test for the full `bootstrap` → `vary` → `evaluate` → `select` loop (using a mocked Failter) in the CI matrix.

---

*Last updated 2025‑07‑14*
