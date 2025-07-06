**Prompt Critical “pack” review**

---

### 1  What already works and should be amplified 🚀

| Area                                    | Strength                                                                                                                                         | Why it matters                                                                                                                                          |
| --------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Immutable, hash-addressed Prompt DB** | Atomic writes, per-file locks, SHA-verified body, canonical UTF-8/LF text ensure bit-exact provenance                                            | Gives you the reproducibility claim that underpins the whole project; keep this as a non-negotiable invariant.                                          |
| **Polylith decomposition**              | Separation into `pcrit.pdb`, `expdir`, `pop`, `command`, etc., with explicit `interface.clj` layers                                              | Makes it trivial to add new “bases” (e.g., web dashboard, distributed workers). Preserve the pattern and finish migrating any stray logic out of bases. |
| **Extensive, focused test-suite**       | Unit tests cover canonicalisation, lock contention, stale-lock healing, CRLF parsing, etc.                                                       | Maintains confidence while refactoring; extend the same discipline to `contest` and `record`.                                                           |
| **Risk register & onboarding docs**     | Clear articulation of convergence, cost, brittleness, etc., plus hard-won “lessons learned”                                                      | Creates a shared culture; keep the living risk list and successor letters.                                                                              |
| **Template-aware prompt analysis**      | `pop.analysis/analyze-prompt-body` infers `:prompt-type`, counts words, identifies template fields — metadata you’ll need for smarter operators  | This is the seed for diversity metrics and structural mutations; expand it rather than replacing it.                                                    |

---

### 2  Inconsistencies, gaps & unstated assumptions 🧐

| # | Observation                                                                                                                                                      | Impact                                                                                                                                                      | Suggested fix                                                                                                                          |                                                                                            |
| - | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| 1 | **Template variable drift**: spec says allowed variables are `{{INPUT_TEXT}}` / `{{OBJECT_PROMPT}}`, but sample prompts use `{{INPUT}}` and `{{EXAMPLE-INPUT}}`  | Prompts will be mis-typed (`:static-prompt`) and silently excluded from contests                                                                            | Write a linter in `pop.analysis` and fail bootstrap if unknown placeholders appear.                                                    |                                                                                            |
| 2 | **Context-map refactor half-done**: docs call it “most critical”, but almost every fn still takes `exp-dir` directly                                             | API churn risk as more state (config, cost-budget, RNG) sneaks in                                                                                           | Do the refactor now while call-sites are few; pass a `{:exp-dir … :config … :logger …}` map everywhere.                                |                                                                                            |
| 3 | **SHA-1 for integrity**                                                                                                                                          | SHA-1 is collision-prone and formally deprecated                                                                                                            | Switch to SHA-256 (or BLAKE3 for speed); keep SHA-1 field for back-compat but mark as legacy.                                          |                                                                                            |
| 4 | **Failter integration assumes POSIX symlinks**                                                                                                                   | `Files/createSymbolicLink` breaks on Windows without elevated privileges                                                                                    | Add a “copy vs symlink” strategy in `expdir/link-prompt!` chosen at runtime.                                                           |                                                                                            |
| 5 | **No cost accounting yet**: risk #3 documented, but no budget or telemetry in code                                                                               | Real-world runs can explode API spend                                                                                                                       | Add a `:budget-usd` key in the context map; instrument `llm.core/call-model` to subtract usage and abort when exhausted.               |                                                                                            |
| 6 | **Evaluation variance unaddressed**                                                                                                                              | Risk #2 notes jittery LLM scoring; code still expects a single `report.csv`                                                                                 | When you implement `record`, store *all* replicates and confidence intervals; support majority-vote critics.                           |                                                                                            |
| 7 | **Un-tested network-FS semantics**                                                                                                                               | Spec warns about NFS but tests assume local FS                                                                                                              | Spin up an integration test that mounts tmpfs + NFS (or local `sshfs`) in CI to detect non-atomic rename fallbacks.                    |                                                                                            |
| 8 | **Roadmap / docs drift**                                                                                                                                         | README “current milestone v0.2” still lists `contest` as “🔜”, yet code for bootstrap is already merged; DESIGN.md status line says “Refactoring for v0.3”  | New contributors mis-align                                                                                                             | Add a “docs CI” check that fails when roadmap dates don’t match `project.clj` release tag. |
| 9 | **Assumes deterministic Failter output**                                                                                                                         | Evolution loop relies on single leaderboard value per prompt                                                                                                | Persist the *raw* Failter per-input scores so you can later try different aggregation functions without rerunning expensive LLM calls. |                                                                                            |

---

### 3  Improvements & extensions 🔧

1. **Finish the vertical slice**

   * Implement `contest` + `record` using the context map.
   * Store `results.csv` *and* the full Failter spec directory under `gen-NNN/contests/…` as already designed.

2. **Diversity-aware selection**
   Add Simpson or Jaccard diversity metrics over n-gram shingles of prompt text; penalise generations whose diversity falls below a threshold (see EvoPrompt’s fitness shaping below).

3. **Surrogate critics**
   Implement the v0.5 roadmap item early by training a cheap local model (e.g., Text-Embedding-ada-002 + logistic reg) to predict Failter score bucket; filter obvious low performers.

4. **Cost & carbon tracking**
   Attach cost (USD) and carbon (g CO₂e) to every LLM call; roll-up in generation metadata.

5. **Research mode vs Production mode**
   Flag that switches logging to DEBUG, keeps failed generations, dumps intermediate prompts; default prod mode keeps only lineage and winners.

---

### 4  Relevant research you can piggyback on 📚

| Theme                              | Key work                                                                                                                                                                                                        | Why it’s relevant                                                                                                                                  |
| ---------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Evolutionary prompt search**     | **EvoPrompt** (Zhou et al., 2023) introduces LLM-aided mutation + survival-of-the-fittest loop across 31 datasets; shows diversity preservation techniques that mitigate premature convergence ([arxiv.org][1]) | Validates your “LLM as mutator, Failter as judge” architecture; borrow their n-gram diversity penalty.                                             |
|                                    | **GAAPO** (2024) blends classic GA with bespoke prompt generators to accelerate convergence ([arxiv.org][2])                                                                                                    | Suggests running multiple specialised meta-prompts in parallel and tracking which operator produced each child (already a mitigation in RISKS #6). |
|                                    | **Evolutionary Prompt Optimization for VLMs** discovers emergent reasoning in multimodal tasks via evolution ([arxiv.org][3])                                                                                   | Evidence that your framework could generalise beyond text-only tasks.                                                                              |
| **Frameworks you could integrate** | **DSPy** (Stanford, 2024) compiles declarative specs into optimised prompts/weights; includes “BeamSearch + Self-Refine” algorithms ([github.com][4], [dspy.ai][5])                                             | You can wrap DSPy as an alternative “contest backend” or as a surrogate critic.                                                                    |
|                                    | **PromptBreeder / GeneticPromptLab** (OSS) offer GA operators & scoring hooks ([github.com][6])                                                                                                                 | Drop-in source of operator ideas (e.g., two-point crossover on sentence spans).                                                                    |
| **Automated prompt discovery**     | **AutoPrompt** (Shin et al., 2020/21) showed gradient-free prompt token search for factual probes ([researchgate.net][7])                                                                                       | Early evidence that token-level search improves with careful fitness design.                                                                       |
| **Literature digests**             | Awesome-LLM-Prompt-Optimization & PromptPapers curated lists ([github.com][8], [github.com][9])                                                                                                                 | Track new operator or scoring ideas and add them to your mutation library.                                                                         |

---

### 5  Quick-win action list for the next sprint ✅

1. **Template enforcement linter** in bootstrap—fail fast on unknown `{{…}}`.
2. **Replace SHA-1 → SHA-256**, update util & tests.
3. **Context-map refactor** across `command`, `pop`, `expdir`.
4. **Implement `contest` directory writer** with copy/symlink abstraction + Windows fallback.
5. **Add `:budget-usd` and per-call cost logging** in `llm.core`.
6. **Write integration test** that runs a tiny mocked Failter and asserts `record` populates generation metadata.

Do these and you’ll unblock v0.2, close several top-10 risks, and align docs with code.

---

### 6  Bottom line

Prompt Critical already has **rock-solid provenance mechanics and a thoughtful modular architecture**. Resolve the few spec drifts, finish the context-map and contest pipeline, and plug in research-proven evolutionary operators. That will turn the project from a promising skeleton into a self-improving prompt factory.

[1]: https://arxiv.org/html/2309.08532?utm_source=chatgpt.com "EvoPrompt: Connecting LLMs with Evolutionary Algorithms Yields ..."
[2]: https://arxiv.org/html/2504.07157v3?utm_source=chatgpt.com "GAAPO: Genetic Algorithmic Applied to Prompt Optimization - arXiv"
[3]: https://arxiv.org/html/2503.23503v1?utm_source=chatgpt.com "Evolutionary Prompt Optimization Discovers Emergent Multimodal ..."
[4]: https://github.com/stanfordnlp/dspy?utm_source=chatgpt.com "DSPy: The framework for programming—not prompting—language ..."
[5]: https://dspy.ai/?utm_source=chatgpt.com "DSPy"
[6]: https://github.com/AmanPriyanshu/GeneticPromptLab?utm_source=chatgpt.com "AmanPriyanshu/GeneticPromptLab - GitHub"
[7]: https://www.researchgate.net/publication/347236711_AutoPrompt_Eliciting_Knowledge_from_Language_Models_with_Automatically_Generated_Prompts?utm_source=chatgpt.com "AutoPrompt: Eliciting Knowledge from Language Models with ..."
[8]: https://github.com/jxzhangjhu/Awesome-LLM-Prompt-Optimization?utm_source=chatgpt.com "jxzhangjhu/Awesome-LLM-Prompt-Optimization - GitHub"
[9]: https://github.com/thunlp/PromptPapers?utm_source=chatgpt.com "thunlp/PromptPapers: Must-read papers on prompt-based tuning for ..."
