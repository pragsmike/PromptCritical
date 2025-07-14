# PromptCritical: Reproducible Evolution of Human-Readable Prompts

*A reproducible platform for evolving large–language–model prompts, one small,
auditable step at a time.*

## ✨ What is PromptCritical?

PromptCritical is a **data‑driven, experiment‑oriented toolchain** that breeds and evaluates prompts for LLMs. It automates the cycle of:

```
vary → evaluate → select
```

so you can focus on defining **fitness metrics** and **mutation strategies**, not on plumbing. A `bootstrap` command seeds the initial `gen-0` population, after which the main loop can begin.

PromptCritical stands out by pairing a **multi-model judging
architecture**—where evolved prompts face a panel of independent LLM “critics”
rather than a single, potentially biased scorer—with an **immutable,
hash-addressed prompt database** that captures every ancestral mutation and
evaluation in cryptographically verifiable form. This provenance layer functions
as a “time machine”: you can rewind any prompt’s lineage, replay an entire
evolutionary run bit-for-bit, or fork experiments without losing auditability.
Together, the contest-style fitness and tamper-evident record make
PromptCritical uniquely reproducible, bias-resistant, and production-friendly in
a field where ad-hoc scripts and opaque metrics still dominate.

## 🚀 Quick Start

Get your first experiment running in two commands:

```bash
# 1. Create a new experiment skeleton in a directory named "my-exp"
pcrit init my-exp

# 2. Ingest the seed prompts and create generation 0
pcrit bootstrap my-exp
```
You now have a complete, runnable experiment in the `my-exp` directory. See the [Usage Guide](USAGE.md) for the next steps (`evolve`, `vary`, `evaluate`, `select`).


## Key Ingredients

| Ingredient | Purpose |
| :--- | :--- |
| **Polylith workspace** | Re‑usable components, clean boundaries, lightning‑fast incremental tests |
| **Immutable Prompt DB** | Atomic, hash‑verified store with per‑file lock‑healing |
| **Git as temporal database** | Second layer of tamper-detection, allows experiment branching and backtracking |
| **Failter integration** | Runs large‑scale prompt contests and collects scores |
| **Evolution engine** | Varies, evaluates, and selects prompts to improve fitness |

### Git as Temporal Database

Using git for population snapshots is attractive because:
- Every generation is a commit with full diff history
- You can branch for experimental evolution strategies
- Merge conflicts become meaningful (competing evolutionary pressures)
- You get distributed replication of your entire evolutionary history for free

## Documentation

For more information, see:
   * [OVERVIEW](docs/OVERVIEW.md)
   * [DESIGN](docs/DESIGN.md)
   * [RISKS](docs/RISKS.md)

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
│   ├── experiment/   ; Defines the logical experiment context
│   ├── expdir/       ; Manages experiment directory layout
│   ├── pdb/          ; The immutable prompt database
│   ├── pop/          ; Population domain model & analysis
│   ├── reports/      ; Parses contest result files (e.g., report.csv)
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
| `bootstrap` | ✅ | Initializes an experiment, ingests seed prompts, and creates `gen-0`. |
| `evaluate`  | ✅ | Runs the active population in a contest and collects results. |
| `evolve`    | ✅ | **Automates the `vary` → `evaluate` → `select` loop for N generations.** |
| `init`      | ✅ | Creates a new, minimal experiment skeleton directory. |
| `select`    | ✅ | Creates a **new generation** of survivors based on evaluation scores. |
| `stats`     | ✅ | Displays cost and score statistics for a contest or generation. |
| `vary`      | ✅ | Adds new prompt variations to the *current* generation's population. |


---

### 📖 Terminology & File-Naming Glossary

| Term                                               | Notes                                                   | Meaning                                                                                                                                                                       |
|----------------------------------------------------|---------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **init**                                           | Creates the initial experiment files.                   | One-time step that scaffolds a runnable experiment directory, including the `seeds/` folder, `bootstrap.edn`, and default configurations.                                     |
| **bootstrap**                                      | Creates `gen-0`.                                        | One-time step that ingests prompts from `seeds/` into the prompt database, creates named links, and populates `gen-0` with the initial set of **object-prompts**.                |
| **vary**                                           | Mutates the current generation.                         | Generates new candidate prompts by mutating or recombining existing ones, adding them to the *current* population directory (`generations/gen-000/population`, …). Now supports multiple strategies like `:refine` and `:crossover`. |
| **evaluate**                                       | Runs scoring but does **not** decide winners.           | Orchestrates a Failter **contest** for every prompt in the current population and collects the raw fitness metrics into `report.csv`.                                         |
| **contest**                                        | *Contest* = noun; *evaluate* = verb/command.            | A single Failter run that scores a set of prompts on a target document. It is the core operation *inside* **evaluate**.                                                       |
| **select**                                         | Selection strategy is pluggable. Creates new generation. | Picks the top-performing prompts according to `report.csv` and creates a **new generation folder** populated with symlinks to the survivors. Now supports policies like `top-N` and `tournament`. |
| **stats**                                          | An analysis command. Does not mutate state.             | Reads one or more `report.csv` files and displays aggregated statistics about cost and performance scores.                                                                    |
| **population (`generations/gen-NNN/population/`)** | See *Directory Layout* section.                         | Folder tree that holds every generation’s prompt files. Each generation gets its own numbered sub-directory.                                                                  |
| **experiment directory (`expdir/`)**               | Portable & reproducible.                                | Root folder that bundles prompt generations, results, Failter specs, and metadata for a single evolutionary run.                                                              |
| **`report.csv`**                                   | Failter produces this.                                  | Canonical filename for evaluation output: one row per prompt plus columns for fitness metrics, metadata, and prompt hash.                                                     |
| **template placeholders**                          | *Only these two names are recognized by the templater.* | Literal strings substituted when a prompt is rendered:  <br>`{{INPUT_TEXT}}` – the evaluation text corpus<br>`{{OBJECT_PROMPT}}` – a prompt being operated on.                |
| **seed prompt**                                    | Seeds are version-controlled.                           | Hand-crafted prompt placed in `seeds/` that kicks off **bootstrap**.                                                                                                          |

> Use this table as the **single source of truth** when writing docs, code comments, or CLI help.

---

## 📦 Current State (v0.4): Advanced Evolution Strategies

The project has undergone a significant architectural refactoring into a clean Polylith structure. With the completion of the v0.4 milestone, the evolutionary engine is now equipped with more sophisticated, pluggable operators for selection and mutation, mitigating the risk of premature convergence on suboptimal prompts.

*   **Tournament Selection**: The `select` command now supports a `tournament-k=N` policy, which helps preserve population diversity by giving lower-scoring prompts a chance to survive into the next generation.
*   **Crossover Mutation**: The `vary` command can be configured to use a `:crossover` strategy, which breeds the top two performers of a generation to create a new hybrid offspring.
*   **Automated `evolve` Loop**: The high-level `evolve` command composes these steps into a fully automated loop, allowing for multi-generation experiments to be run with a single command.

---

## 🛠 External Dependency — Failter

PromptCritical does **not** implement scoring or judgement itself. Instead we treat [**Failter**](https://github.com/pragsmike/failter) as a **black box** experiment runner:

*   We generate a `spec.yml` file that defines the entire contest for Failter.
*   We shell-out to the single, idempotent `failter run --spec <path>` command.
*   We parse the resulting JSON report to gather fitness data for the `select` step.

---

## 🗺 Roadmap Snapshot

| Milestone | New Capability |
|-----------|----------------|
| **v0.2**  | DONE Implement core commands (`init`, `stats`, etc). |
| **v0.3**  | DONE Automated `evolve` command that composes the v0.2 commands. |
| **v0.4**  | DONE Advanced selection & mutation operators (tournament, crossover). |
| **v0.5**  | Surrogate critic to pre-filter variants before Failter. |
| **v0.6**  | Experiment recipes (EDN/YAML) and CLI replayability. |
| **v0.7**  | Reporting dashboard (`pcrit.web` base). |
| **v1.0**  | Distributed workers, advanced semantic validators. |

---

## Getting Involved

1.  **Clone** and run the tests:
    ```bash
    git clone https://github.com/pragsmike/promptcritical
    cd promptcritical
    make test
2.  **Read** the design documents in the `docs/` directory.
3.  **Hack** on the next milestone—PRs welcome!

---

**PromptCritical**: because great prompts shouldn’t be accidental.
