# PromptCritical: Evolving prompts in a closed category

*A reproducible platform for evolving large–language–model prompts, one small,
auditable step at a time.*

## ✨ What is PromptCritical?

PromptCritical is a **data‑driven, experiment‑oriented toolchain** that breeds and evaluates prompts for LLMs.  It automates the cycle of

```
seed → contest (Failter) → ingest → evolve
```

so you can focus on defining **fitness metrics** and **mutation strategies**, not on plumbing.

Key ingredients:

| Ingredient                   | Purpose                                                                  |
| ---------------------------- | ------------------------------------------------------------------------ |
| **Polylith workspace**       | Re‑usable components, clean boundaries, lightning‑fast incremental tests |
| **Immutable Prompt DB**      | Atomic, hash‑verified store with per‑file lock‑healing                   |
| **Failter integration**      | Runs large‑scale prompt contests and collects scores                     |
| **Evolution engine** (*WIP*) | Selects, mutates & cross‑breeds prompts toward higher fitness            |


---

## ✨ Aspirational Goals

Prompt engineering still feels like folklore.  PromptCritical’s long-term
mission is to turn it into a **data-driven, evolutionary workflow**:

1. **Store every prompt immutably** with lineage, hashes, and timestamps.
2. **Run controlled experiments** that score those prompts on real tasks
   (Latency / Cost / Accuracy / Consistency).
3. **Breed the next generation**—mutate, crossover, and select—using the
   recorded scores as fitness.
4. **Repeat automatically**, producing prompts that keep pace with new LLM
   releases and changing task definitions.

When complete, a single command should spin up an experiment, push thousands of
prompt variants through external evaluators, and surface the winning prompts
with full provenance.

---

## 🏗  Workspace Layout (Polylith)

```
workspace/
├── components/
│   ├── pdb/      ; immutable prompt DB (file I/O, locks)
│   ├── pop/      ; population & evolution logic
│   ├── config/   ; runtime configuration (EDN → map)
│   ├── log/      ; structured logging facade
│   └── llm/      ; thin HTTP client & future surrogate critic
└── bases/
    └── cli/      ; `pcrit` command‑line entry point
```

Components expose **public APIs** only; everything else is private.  Polylith’s tooling ensures dependency hygiene and incremental builds.

### CLI Overview

| Command     | Status | Description                                     |
| ----------- | ------ | ----------------------------------------------- |
| `bootstrap` | ✅      | Seed the Prompt DB from a manifest (EDN)        |
| `contest`   | ✅      | Package prompts, run Failter, collect report    |
| `ingest`    | ✅      | Annotate prompts with scores from `report.csv`  |
| `evolve`    | 🔜     | Generate next generation via mutation/crossover |

Run `./bin/pcrit help <command>` for full usage.


---

## 📦 Current State (v0.1)

NOTE: This codebase is organized using [Polylith](https://polylith.gitbook.io/polylith) conventions for Clojure code.
These are slightly different than for Python code, so be careful not to be confused.

| Layer | Status | Notes |
|-------|--------|-------|
| **Prompt database** | **Implemented** | Immutable file format (`*.prompt`), UTF-8 + NFC canonicalisation, SHA-1 integrity, per-file `.lock` protocol, atomic writes, unique `Pnnn` IDs. |
| **Seed generation** | *partial* | pcrit.pop |
| **Failter contest packaging** | *Not yet* | |
| **Score ingestion & evolution loop** | *Not yet* | |

The existing codebase gives you:

* `pcrit.pdb.core/create-prompt` – write a prompt file safely.
* `pcrit.pdb.core/read-prompt` – read & checksum-verify.
* `pcrit.pdb.core/update-metadata` - write new metadata to a prompt file
* Proven concurrency: multiple processes can create or annotate prompts without
  corrupting the store.
* `pcrit.pop.core` - data structures for evolution experiment, bootstrapping population

Everything else is still ahead of us.

---

## 🛠  External Dependency — **Failter**

PromptCritical does **not** implement scoring or judgement itself.
Instead we treat [**Failter**](https://github.com/pragsmike/failter) as a **black
box** experiment runner:

* We build a directory that matches Failter’s required structure
  (`inputs/`, `templates/`, `model-names.txt`, …).
* We shell-out to `failter experiment → evaluate → report`.
* We parse the resulting `report.csv` and write the scores back into each
  prompt’s YAML front-matter.

You’ll need Failter installed and runnable from the command line before the next
PromptCritical milestone.

---

## 🚧 Next Incremental Step (v0.2)

The immediate goal is a **“bootstrap → contest → record” vertical slice**:

1. **Seed creator**
   Creates initial population

   ```bash
   pcrit bootstrap experiment-spec-dir        # creates P001, P002, …
   ```

2. **Contest packager & runner**

   ```bash
   pcrit contest \
         --prompt-ids P001 P002 \
         --inputs   data/inputs/ \
         --models   models.txt \
         --exp-id   exp-2025-07-01
   ```

   *Creates the contest directory, executes the three CLI stages, waits for
   completion.*

3. **Score recorder**

   Ingests the raw scores from the contest runner (failter) into the contest directory,
   and creates a record with information about the contest and its participants that is
   not provided by failter.

   The prompt files are NOT changed by this, because a prompt's performance
   is not intrinsic to the prompt itself.

   ```yaml
   contest-score: 87.5
   contest-model: openai/gpt-4o-mini
   contest-directory:   contest-2025-07-01
   ```

This slice will prove that PromptCritical can:

* translate its internal prompt store into Failter’s format,
* call an external evaluator, and
* round-trip fitness data back into the immutable evolution history.

---

## 🗺  Roadmap Snapshot

| Milestone | New Capability                                                   |
| --------- | ---------------------------------------------------------------- |
| **v0.2**  | Bootstrap → Contest → Record (described above)                   |
| **v0.3**  | Basic mutation & crossover operators writing new prompt files    |
| **v0.4**  | Simple (µ+λ) evolutionary loop driven by `contest-score`         |
| **v0.5**  | Surrogate critic to pre-filter variants before Failter           |
| **v0.6**  | Experiment recipes (EDN/YAML) and CLI replayability              |
| **v0.7**  | Reporting dashboard (CLI table + optional web UI)                |
| **v1.0**  | Distributed workers, KG/AMR semantic validators, SHA-256 upgrade |

Small steps, each one shippable and testable.

---

## Getting Involved

1. **Clone** and run the tests

   ```bash
   git clone https://github.com/pragsmike/promptcritical
   cd promptcritical
   clj -M:test
   ```
2. **Read** the spec in `docs/prompt-representation.md`.
3. **Hack** on the next milestone—PRs welcome!

---

**PromptCritical**: because great prompts shouldn’t be accidental.


