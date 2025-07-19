# PromptCritical System Design

**Version 2.2 · 2025‑07‑14**
**Status:** *v0.4 Advanced Evolution Strategies Complete*

---

## 1  Purpose & Scope

PromptCritical is a data-driven, evolutionary framework for discovering high-performance prompts for Large Language Models (LLMs). This document describes the current architecture, which follows **Polylith** conventions to realize an experiment loop of ***bootstrap → vary → evaluate → select***.

The design has two guiding principles:

1.  **Immutable provenance.** Every prompt artefact carries its full lineage, a cryptographic hash, and a creation timestamp.
2.  **Replaceable adapters.** External tools (Failter, future surrogate critics, dashboards) are integrated behind thin, testable boundaries.

---

## 2  Polylith Architecture Overview

The codebase is organized into re-usable **components** and runnable **bases**. This separation ensures that core logic is reusable by any entry point (e.g., CLI, a future web UI). Note that LLM interaction and cost calculation have been extracted to an external `pcrit-llm` library.

| Layer | Name (ns prefix) | Role |
| :--- | :--- | :--- |
| **Component** | `pcrit.command` | **Reusable, high-level workflows** (e.g., `bootstrap!`, `evolve!`) |
| **Component** | `pcrit.experiment` | **Defines the logical Experiment context** passed between components |
| **Component** | `pcrit.expdir` | **Manages the physical layout** of an experiment directory |
| **Component** | `pcrit.failter` | **Generates `spec.yml` and runs the `failter run` command** |
| **Component** | `pcrit.pdb` | **Immutable prompt database** (file I/O, locking, ID generation) |
| **Component** | `pcrit.pop` | **Population domain model** and prompt analysis (`:prompt-type`) |
| **Component** | `pcrit.reports` | **Generates human-readable CSV reports** from contest results |
| **Component** | `pcrit.results` | **Parses raw `failter-report.json`** into canonical data structures |
| **Component** | `pcrit.config` | Central configuration map & helpers |
| **Component** | `pcrit.log` | Structured logging and log setup |
| **Component** | `pcrit.test-helper` | Shared utilities for the test suite |
| **Base** | `pcrit.cli` | **Command‑line interface** (`pcrit …`) entry point |

---

## 3  Core Components

The descriptions from the previous version remain accurate.

---

## 4  Data Model – Prompt Record

The descriptions from the previous version remain accurate.

---

## 5  Experiment Flow (v0.4)

```
bootstrap → [ vary → evaluate → select ]*
```
The high-level `evolve` command automates the core loop, which is now enhanced with pluggable strategies for variation and selection.

1.  **Bootstrap** (`pcrit bootstrap <exp-dir>`)
    *   The `cli` base calls the `pcrit.command/bootstrap!` function to create `gen-0`.

2.  **Evolve** (`pcrit evolve <exp-dir> --generations N ...`)
    *   The `cli` base calls `pcrit.command/evolve!`, which loops for `N` generations.
    *   In each loop, it calls the core command functions in sequence.
    *   **`vary!`**: Now supports multiple strategies (e.g., `:refine`, `:crossover`) configured in `evolution-parameters.edn`. This step breeds new candidate prompts.
    *   **`evaluate!`**: Orchestrates a `Failter` contest to score all prompts in the current population.
    *   **`select!`**: Now supports multiple policies (e.g., `--policy top-N=5`, `--policy tournament-k=2`). This step winnows the population and creates the next generation.
    *   The loop tracks cumulative cost and can halt early if a budget is exceeded.

---

## 6  Concurrency & Integrity Guarantees

The descriptions from the previous version remain accurate.

---

## 7  Extensibility Roadmap

*(Future work will detail this section.)*

---

## 8  Open Issues & Next Steps

*   **Implement advanced operators (v0.4)**: (`✅ Done`) The `select` and `vary` commands now support pluggable strategies, including Tournament Selection and Crossover, to promote population diversity.
*   **Refactor test helpers (v0.4)**: (`✅ Done`) Test setup logic has been consolidated into shared helper namespaces, improving test reliability and maintainability.
*   **Design Surrogate Critic (v0.5)**: This is the next major feature. The goal is to design and implement a component that can pre-filter prompt variations using cheaper heuristics or models before they are sent to the expensive `Failter` evaluation. This will be critical for managing cost at scale.
*   **Add end‑to‑end smoke test**: Implement a test for the full `bootstrap` → `evolve` loop (using a mocked Failter) in the CI matrix. This remains a high priority for ensuring system stability.

---

*Last updated 2025‑07‑14*
