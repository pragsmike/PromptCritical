# PromptCritical System Design

**Version 1.2  ·  2025‑07‑04**
**Status:** *In development (milestone v0.2 ➜ v0.3)*

---

## 1  Purpose & Scope

PromptCritical is a **data‑driven, evolutionary framework** for discovering
high‑performance prompts for Large Language Models (LLMs). This document
describes the *current* (v0.2) architecture after the migration to the
**Polylith** structure and outlines how each building block collaborates to
realise an experiment loop of ***bootstrap → contest (Failter) → record →
evolve***.

The design has two guiding principles:

1. **Immutable provenance.** Every prompt artefact carries its full lineage, cryptographic hash and creation timestamp.
2. **Replaceable adapters.** External tools (Failter, future surrogate critics, dashboards) are integrated behind thin, testable boundaries.

---

## 2  Polylith Architecture Overview

PromptCritical follows [Polylith](https://polylith.gitbook.io/polylith) conventions for Clojure code, organising the codebase into **components** (re‑usable building blocks) and **bases** (runnable entry‑points).  At the time of writing the workspace contains:

| Layer         | Name (ns prefix) | Role                                                         |
| ------------- | ---------------- | ------------------------------------------------------------ |
| **Component** | `pcrit.pdb`      | Immutable prompt database (file I/O, locking, ID generation) |
| **Component** | `pcrit.pop`      | Population & evolution data‑model and algorithms             |
| **Component** | `pcrit.config`   | Central configuration map & helpers                          |
| **Component** | `pcrit.log`      | Structured logging and log setup                             |
| **Component** | `pcrit.llm`      | Thin HTTP client façade for LLM and future surrogate critic  |
| **Base**      | `pcrit.cli`      | Command‑line interface (`pcrit …`) powering automation       |

```
workspace/
├── components/
│   ├── pdb/        ; prompt DB internals
│   ├── pop/        ; population + evo logic
│   ├── config/     ; runtime config
│   ├── log/        ; logging helpers
│   └── llm/        ; LLM HTTP client
└── bases/
    └── cli/        ; main – invokes components
```

### 2.1  Why Polylith?

* **Clear contracts.** Components expose stable, *public* interfaces; internal details are private by default.  Down‑stream code can only depend on “what is promised”.
* **Incremental builds/tests.** Polylith’s tooling runs unit tests only for components affected by a change – vital for fast, experiment‑heavy workflows.
* **Multi‑base future.** Additional bases (e.g., `pcrit.web` dashboard, distributed worker daemons) can reuse the same components without code duplication.

---

## 3  Core Components

### 3.1  Prompt Database (`pcrit.pdb.*`)

* **Atomic writes & fsync.** All file operations go through `pcrit.pdb.io/atomic‑write!`, guaranteeing crash‑safe updates.
* **Per‑file **\`\`**.** `pcrit.pdb.lock` implements a self‑healing lockfile protocol with stale‑lock recovery.
* **ID generation.** `pcrit.pdb.id/get‑next‑id!` atomically assigns `Pnnn` identifiers.
* **Public API.** `pcrit.pdb.core` exposes `create‑prompt`, `read‑prompt`, `update‑metadata`.

### 3.2  Population & Evolution (`pcrit.pop.*`)

Holds the *domain model* of an evolutionary experiment:

* `Population` – a vector of **prompt records** plus derived fitness metadata.
* `bootstrap` – ingests seed prompts & mutation operators from a manifest to create **generation 0**.
* `evolve` ( composite of breed-vie-winnow steps) (planned v0.3) – selects survivors, applies mutation/crossover operators, and writes new prompts back to the PDB.

### 3.3  Configuration (`pcrit.config.*`)

Centralised EDN map loaded at startup (LLM endpoint, lock‑timeouts, etc.) so ops teams can override via ENV or profiles.

### 3.4  LLM Client (`pcrit.llm.*`)

Thin wrapper around HTTP JSON endpoints.  Initially used only for *health checks* in the CLI, but will host the **surrogate critic** in v0.5.

### 3.5  Logging (`pcrit.log.*`)

Unified `log/info | warn | error` macros; auto‑initialised in every base.

---

## 4  Data Model – Prompt Record

The on‑disk and in‑memory shape is unchanged since v1.1, retaining:

```clojure
{:header {:id "P123" :created-at "…" :sha1-hash "…" :spec-version "1" …}
 :body   "Canonical prompt text…\n"}
```

*Canonicalisation* (UTF‑8 + NFC, single LF line‑end, trailing newline) ensures deterministic hashes.

---

## 5  Experiment Flow (v0.2)

```
bootstrap → contest (pack → run Failter) → ingest (report.csv) → prepare generation N+1
```

1. **Bootstrap** (`pcrit bootstrap manifest.edn`)
      *Seeds* the PDB with seed & mutator prompts and writes `gen‑000/`.
2. **Contest** (`pcrit contest …`)
      Packages selected prompts plus input corpus & model list into a contest (what failter calls an experiment) directory and shell‑executes:

   ```bash
   failter experiment && failter evaluate && failter report
   ```
3. **Record**
      Parses `report.csv`, updates the generation's contest record.

*All steps persist artefacts in structured sub‑directories under **`generations/`** to guarantee full audit‑trail.*

---

## 6  Concurrency & Integrity Guarantees

1. **Atomic replace** for every file write.
2. **Per‑resource lockfiles** with jittered retries and stale‑lock healing.
3. **Hash verification** on every read; warnings logged if mismatch detected.

---

## 7  Extensibility Roadmap

| Milestone | Increment                                                               |
| --------- | ----------------------------------------------------------------------- |
| **v0.3**  | Mutation & crossover operators – produce new prompts via meta‑prompting |
| **v0.4**  | Simple `(µ + λ)` evolutionary loop driven by `contest-score`            |
| **v0.5**  | Local surrogate critic to pre‑filter variants before Failter        |
| **v0.6**  | Experiment recipe DSL (EDN/YAML) & CLI replayability                    |
| **v0.7**  | Reporting dashboard (`pcrit.web` base)                                  |
| **v1.0**  | Distributed workers, KG/AMR semantic validators, SHA‑256 upgrade        |

---

## 8  Open Issues & Next Steps

* Expose lock back‑off parameters via `pcrit.config`.
* Expand `pcrit.pop` tests to cover template field extraction edge‑cases.
* Add an end‑to‑end smoke test (bootstrap → Failter mock → ingest) to the CI matrix.
* Document Python‑side Polylith conventions for future surrogate critic code.

---

*Last updated 2025‑07‑04*
