### 1   Big-picture purpose (one sentence)

**PromptCritical** aims to be a reproducible **prompt-evolution platform**:
*store every prompt immutably → run controlled experiments → read the scores back → breed the next generation*.

The prompt database we just finished is the “source-of-truth” layer; everything else is plumbing that moves prompts *into* black-box evaluators (like **Failter**) and moves fitness scores *back* into the metadata.

---

### 2   Immediate next increment — *“seed → Failter → ingest”* vertical slice

| Stage                        | What it does                                                                                                                                                                          | Why it’s the next, smallest useful step                                     |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| **2.1  Seed creator**        | Pull a handful of prompt templates (or let the user type them), write them to the DB with `generator:"seed-cli"`.                                                                     | Gives us real prompt files to play with and exercise the write path.        |
| **2.2  Experiment packager** | Materialise a **Failter experiment directory** from a set of prompt IDs, an `inputs/` bundle, and a list of LLM model names.  (Exactly the structure shown in the Failter guide.)     | Proves we can translate our internal store into Failter’s on-disk contract. |
| **2.3  Failter runner**      | Shell-out: `clj -M:run experiment …`, `evaluate …`, `report …`.  Wait for completion.                                                                                                 | Lets us treat Failter purely as a black box.  No new scoring code needed.   |
| **2.4  Result ingester**     | Parse `report.csv` (and optionally the per-trial `.eval` YAMLs) and write the numeric score back into each prompt’s YAML (`failter-score`, `failter-model`, `failter-experiment-id`). | Closes the loop: DB → Failter → DB.  Now every prompt has a fitness value.  |

*Success criterion:* running one CLI command (`pcrit run-failter --inputs dir/ --prompts P1 P2 P3`) produces (a) a populated Failter folder and (b) updated prompt files with `failter-score` fields.

---

### 3   Minimal modules to code for that slice

1. **`pcrit.seed`** – make prompt IDs from template files or STDIN.
2. **`pcrit.failter.package`** – given ✱prompt-ids✱ + config, build the experiment directory.
3. **`pcrit.failter.invoke`** – wrapper that shells out, streams logs, handles non-zero exits.
4. **`pcrit.failter.ingest`** – parser for `report.csv` → `update-metadata` calls.
5. **Small CLI dispatcher** – `pcrit.cli/run-failter`.

Everything re-uses the prompt-DB locking/write APIs we already trust.

---

### 4   Road-map sketch after that

| Phase                        | Capability added                                                          | New piece              |
| ---------------------------- | ------------------------------------------------------------------------- | ---------------------- |
| **3  Variation ops**         | LLM-powered mutation / crossover → writes new child prompts.              | `pcrit.variation.*`    |
| **4  Simple evolution loop** | Pick top-k by `failter-score`, generate λ children, repeat.               | `pcrit.evolve.simple`  |
| **5  Surrogate scorer**      | Cheap LLM critic predicts score, sends only promising prompts to Failter. | `pcrit.eval.surrogate` |
| **6  Experiment registry**   | EDN/YAML recipe that records inputs, models, evolution params.            | `pcrit.experiment`     |
| **7  Reporting UI**          | CLI tables or small web dashboard reading the DB.                         | `pcrit.report`         |
| …                            | KG validators, SHA-256 migration, distributed workers, etc.               |                        |

Each step is still thin and testable because **Failter stays a black box**: we only ever write files it understands and read the scores it emits.

---

### 5   Action list for the next sprint

1. Define a minimal **EDN config schema**:

   ```edn
   {:experiment-id "exp-2025-07-01"
    :prompt-ids    ["P12" "P13"]
    :inputs-dir    "data/inputs"
    :models        ["openai/gpt-4o-mini" "ollama/qwen3:8b"]}
   ```
2. Implement `pcrit.failter.package` to create the directory tree and copy/link prompt bodies into `templates/`.
3. Wrap Failter CLI with robust error capture (`ProcessBuilder`, async log pipe).
4. Parse Failter’s `report.csv`; update each prompt’s metadata under a lock.
5. Add one integration test that seeds two prompts, runs a tiny Failter job with a stub harness, and asserts that `failter-score` appears in the DB.

With that slice shipped, we can finally *see* numbers attached to our prompts—making subsequent evolution work concrete and test-driven.
