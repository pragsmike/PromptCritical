# PromptCritical: Evolving prompts in a closed category

*A reproducible platform for evolving large–language–model prompts, one small,
auditable step at a time.*

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

## 📦 Current State (v0.1)

| Layer | Status | Notes |
|-------|--------|-------|
| **Prompt database** | **Implemented** | Immutable file format (`*.prompt`), UTF-8 + NFC canonicalisation, SHA-1 integrity, per-file `.lock` protocol, atomic writes, unique `Pnnn` IDs. |
| **Seed generation** | *Not yet* | |
| **Failter experiment packaging** | *Not yet* | |
| **Score ingestion & evolution loop** | *Not yet* | |

The existing codebase gives you:

* `pcrit.pdb/create-prompt` – write a prompt file safely.  
* `pcrit.pdb/read-prompt` – read & checksum-verify.  
* Proven concurrency: multiple processes can create or annotate prompts without
  corrupting the store.

Everything else is still ahead of us.

---

## 🛠  External Dependency — **Failter**

PromptCritical does **not** implement scoring or judgement itself.  
Instead we treat [**Failter**](https://github.com/your-org/failter) as a **black
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

The immediate goal is a **“seed → Failter → ingest” vertical slice**:

1. **Seed creator**  
   ```bash
   pcrit seed my-prompts.txt        # creates P001, P002, …
````

2. **Experiment packager & runner**

   ```bash
   pcrit run-failter \
         --prompt-ids P001 P002 \
         --inputs   data/inputs/ \
         --models   models.txt \
         --exp-id   exp-2025-07-01
   ```

   *Creates the Failter directory, executes the three CLI stages, waits for
   completion.*
3. **Score ingester**
   Updates each prompt file with:

   ```yaml
   failter-score: 87.5
   failter-model: openai/gpt-4o-mini
   failter-exp:   exp-2025-07-01
   ```

This slice will prove that PromptCritical can:

* translate its internal prompt store into Failter’s format,
* call an external evaluator, and
* round-trip fitness data back into the immutable prompt history.

---

## 🗺  Roadmap Snapshot

| Milestone | New Capability                                                   |
| --------- | ---------------------------------------------------------------- |
| **v0.2**  | Seed → Failter → Ingest (described above)                        |
| **v0.3**  | Basic mutation & crossover operators writing new prompt files    |
| **v0.4**  | Simple (µ+λ) evolutionary loop driven by `failter-score`         |
| **v0.5**  | Surrogate LLM critic to pre-filter variants before Failter       |
| **v0.6**  | Experiment recipes (EDN/YAML) and CLI replayability              |
| **v0.7**  | Reporting dashboard (CLI table + optional web UI)                |
| **v1.0**  | Distributed workers, KG/AMR semantic validators, SHA-256 upgrade |

Small steps, each one shippable and testable.

---

## Getting Involved

1. **Clone** and run the tests

   ```bash
   git clone https://github.com/your-org/promptcritical
   cd promptcritical
   clj -M:test
   ```
2. **Read** the spec in `docs/prompt-representation.md`.
3. **Hack** on the next milestone—PRs welcome!

---

**PromptCritical**: because great prompts shouldn’t be accidental.


