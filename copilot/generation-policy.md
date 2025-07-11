### 📑 Implementation Note — “Single-creator” Generation Policy

*(For the PromptCritical core team – Clojure code-base)*

---

#### 1  What’s changing & why

| Item                                | Old behaviour                                                                                                                            | **New behaviour**                                                                                                                                                           | Rationale                                                                                                                                                                                     |
| ----------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Directory that defines a generation | `bootstrap` creates **gen-000**. <br>`vary` **also** creates **gen-(N+1)** each time it runs. <br>`select` later creates another folder. | `bootstrap` still seeds **gen-000**. <br>**Only `select`** may mint **gen-(N+1)** (freeze-point). <br>`vary` & `evaluate` mutate files *inside* the **current** generation. | • Simpler mental model (“generation == post-selection population”).<br>• Fewer folders / less disk churn.<br>• Immutable snapshots preserved (once `select` runs, that folder never changes). |
| Provenance / audit trail            | Automatic, because each `vary` produced its own folder.                                                                                  | Recorded **inside prompt headers** (`:parents`, `:generator`, and new `:selection` maps).                                                                                   | Keep complete lineage without extra side-logs.                                                                                                                                                |
| Baseline-first vs explore-first     | Each command created snapshots for free.                                                                                                 | Both loops still work; experimenter must commit/tag if they want mid-run checkpoints before `select`.                                                                       | Acceptable trade-off for a tidier tree.                                                                                                                                                       |

---

#### 2  High-level flow after the change

```
bootstrap         ; seeds + creates gen-000/
vary              ; adds offspring + :generator metadata to prompts in gen-000
vary …            ; (optional) adds more offspring, same folder
evaluate          ; writes contests/… and updates :fitness
select            ; decides survivors, adds :selection header,
                  ; creates gen-001/population/ with symlinks to winners
--- loop repeats ---
```

*Generation folders are immutable after `select`; prompt files are mutable **only** while they live in the current generation.*

---

#### 3  Files & code touch-points

| File / namespace                                            | Change                                                                                                                                                                                                                                                               |
| ----------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`components/command/src/pcrit/command/vary.clj`**         | • **Remove** call to `pop/create-new-generation!`. <br>• Enhance existing `metadata-fn` so each offspring gets:<br>`clj{:parents   [...ids...]            ; already available<br> :generator {:model m :meta-prompt p :algo algo<br>             :vary-run run-id}}` |
| **`components/command/src/pcrit/command/select.clj`**       | • *Already* creates the next gen – keep. <br>• After selecting survivors, update each with<br>`clj(update prompt :header assoc :selection {:select-run id :policy pol})`                                                                                             |
| **`components/population/src/pcrit/population/core.clj`**   | • No structural change; `create-new-generation!` now called **only** by `bootstrap!` and `select!`.                                                                                                                                                                  |
| **`components/llm/test/...` & `population/test/...`**       | • Adjust tests: `vary` must **not** increment generation count.<br>• Add assertion that offspring header contains `:generator` & parent IDs.                                                                                                                         |
| **Docs –** `docs/USAGE.md`, `docs/evo-process.md`, `README` | • Update examples & diagrams:<br>  – Remove arrow showing `vary → gen-NNN`.<br>  – Add note: “Generation frozen only after `select`.”<br>  – Explain header keys `:generator`, `:selection`.                                                                         |
| **`docs/prompt-representation.md`**                         | • Mark `:selection` stanza as part of canonical header.<br>• Note that `:generator` includes optional `:vary-run` timestamp.                                                                                                                                         |
| **`scripts/ci/e2e.clj` (if present)**                       | • Adapt any path expectations for new folder counts.                                                                                                                                                                                                                 |

---

#### 4  Trade-offs to acknowledge in the docs

* **Pros** – fewer dirs, immutable snapshots, lineage embedded in artefacts.
* **Cons** – intermediate vary batches share one folder; to replay an exact mid-run state you must rely on VCS tagging or run `select` more frequently.
* **Concurrency** – generation-level lock already prevents header write races.
* **Header growth** – acceptable; can compact older details into a summary field if needed.

Implement the bullets above, regenerate the snapshot pack, and the docs + tests will align with the new single-creator policy.
