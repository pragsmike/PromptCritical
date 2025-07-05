# PromptCritical: Evolving prompts in a closed category

*A reproducible platform for evolving large–language–model prompts, one small,
auditable step at a time.*

## ✨ What is PromptCritical?

PromptCritical is a **data‑driven, experiment‑oriented toolchain** that breeds and evaluates prompts for LLMs. It automates the cycle of:

```
bootstrap → contest (Failter) → record → evolve
```

so you can focus on defining **fitness metrics** and **mutation strategies**, not on plumbing.

Key ingredients:

| Ingredient | Purpose |
| :--- | :--- |
| **Polylith workspace** | Re‑usable components, clean boundaries, lightning‑fast incremental tests |
| **Immutable Prompt DB** | Atomic, hash‑verified store with per‑file lock‑healing |
| **Failter integration** | Runs large‑scale prompt contests and collects scores |
| **Evolution engine** (*WIP*) | Selects, mutates & cross‑breeds prompts toward higher fitness |

---

## ✨ Aspirational Goals

Prompt engineering still feels like folklore. PromptCritical’s long-term mission is to turn it into a **data-driven, evolutionary workflow**:

1.  **Store every prompt immutably** with lineage, hashes, and timestamps.
2.  **Run controlled experiments** that score those prompts on real tasks (Latency / Cost / Accuracy / Consistency).
3.  **Breed the next generation**—mutate, crossover, and select—using the recorded scores as fitness.
4.  **Repeat automatically**, producing prompts that keep pace with new LLM releases and changing task definitions.

---

## 🏗 Workspace Layout (Polylith)

The project follows Polylith conventions, organizing the codebase into re-usable **components** and runnable **bases**. This ensures logic is reusable by any interface (e.g., the CLI or a future web service).

```
workspace/
├── components/
│   ├── command/      ; Reusable, high-level user commands
│   ├── expdir/       ; Manages experiment directory layout
│   ├── pdb/          ; The immutable prompt database
│   ├── pop/          ; Population domain model & analysis
│   ├── config/       ; Runtime configuration (EDN → map)
│   ├── log/          ; Structured logging facade
│   ├── llm/          ; Thin HTTP client for LLMs
│   └── test-helper/  ; Shared utilities for testing
└── bases/
    └── cli/          ; `pcrit` command‑line entry point
```

### CLI Overview

| Command | Status | Description |
| :--- | :--- | :--- |
| `bootstrap` | ✅ | Seed the Prompt DB from a manifest |
| `contest` | 🔜 | Package prompts, run Failter, collect report |
| `record` | 🔜 | Record and analyze scores from `report.csv` |
| `evolve` | 🔜 | Generate next generation via mutation/crossover |

---

## 📦 Current State (Post-Refactoring)

The initial v0.1 work is complete, and the project has undergone a significant architectural refactoring. The codebase is now organized into a clean Polylith structure with clear, single-responsibility components.

*   **`pcrit.command`**: Provides reusable, high-level workflow functions (e.g., `bootstrap!`) that can be called by any base.
*   **`pcrit.expdir`**: Manages the physical filesystem layout of an experiment directory.
*   **`pcrit.pdb`**: The robust, concurrent, and immutable prompt database.
*   **`pcrit.pop`**: Handles core prompt domain logic, including ingestion and analysis (e.g., assigning a `:prompt-type` to every prompt).

The `bootstrap` command is fully implemented according to this improved architecture.

---

## 🛠 External Dependency — Failter

PromptCritical does **not** implement scoring or judgement itself. Instead we treat [**Failter**](https://github.com/pragsmike/failter) as a **black box** experiment runner:

*   We build a directory that matches Failter’s required structure (`inputs/`, `templates/`, `model-names.txt`, …).
*   We shell-out to `failter experiment → evaluate → report`.
*   We parse the resulting `report.csv` to record fitness data for the evolution loop.

---

## 🚧 Current Milestone (v0.2): The Vertical Slice

The immediate goal is a **“bootstrap → contest → record” vertical slice**. This will prove the system can orchestrate an external evaluator and round-trip the results.

1.  **Bootstrap an Experiment** (`✅ Implemented`)
    Creates an initial population from a manifest file.
    ```bash
    pcrit bootstrap my-experiment/
    ```

2.  **Run a Contest** (`🔜 In Development`)
    Packages prompts, runs them through Failter, and collects the results.
    ```bash
    pcrit contest my-experiment/ --prompts P1,P2 --inputs ...
    ```

3.  **Record the Scores** (`🔜 In Development`)
    Parses the `report.csv` from Failter and records the fitness data in the experiment's history, linking scores back to the prompts that earned them.

---

## 🗺 Roadmap Snapshot

| Milestone | New Capability |
| :--- | :--- |
| **v0.2** | Bootstrap → Contest → Record (described above) |
| **v0.3** | Basic mutation & crossover operators |
| **v0.4** | Simple (µ+λ) evolutionary loop driven by contest scores |
| **v0.5** | Surrogate critic to pre-filter variants before Failter |
| **v0.6** | Experiment recipes (EDN/YAML) and CLI replayability |
| **v0.7** | Reporting dashboard (`pcrit.web` base) |
| **v1.0** | Distributed workers, advanced semantic validators |

---

## Getting Involved

1.  **Clone** and run the tests:
    ```bash
    git clone https://github.com/pragsmike/promptcritical
    cd promptcritical
    make test
    ```
2.  **Read** the design documents in the `docs/` directory.
3.  **Hack** on the next milestone—PRs welcome!

---

**PromptCritical**: because great prompts shouldn’t be accidental.
