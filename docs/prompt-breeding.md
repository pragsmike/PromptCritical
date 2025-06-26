Your plan—to evolve a **population of prompts** with LLM-driven mutation +
crossover and use **Failter’s quantitative grades as the fitness signal**—is
squarely in line with a fast-growing research thread sometimes called *automatic
prompt engineering* or *prompt evolution*. Below is a short map of the landscape
and how your idea fits into it.

---

### 1 · How your pipeline would look in practice

| Stage                  | What happens                                                                                                                                            | How Failter fits in                                                            |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| **Seed**               | Hand-written or “folded” prompt(s) start the population.                                                                                                | —                                                                              |
| **Mutate / Recombine** | A *meta-prompt* instructs an LLM (could be a cheaper model) to ① improve a single prompt, **or** ② merge two high-fitness prompts, producing offspring. | —                                                                              |
| **Evaluate**           | Each candidate prompt is run through Failter’s `experiment → evaluate` steps on a fixed set of inputs and target models.                                | Failter produces YAML grades, run-time, cost, etc.—your **fitness vector**.    |
| **Select**             | Keep the top *k* prompts (or use tournament / Pareto), optionally apply diversity pressure.                                                             | Parse Failter’s report programmatically; it’s already machine-readable.        |
| **Loop**               | Repeat for *N* generations or until marginal improvement stalls.                                                                                        | Use Failter’s idempotency to avoid re-evaluating unchanged prompt–model pairs. |

Because Failter stores every trial as a file, you can drive the whole loop with a lightweight Clojure (or Python) script:

1. Write candidate prompt into `templates/`.
2. Let Failter create results + `.eval`.
3. Read back the grade/cost from front-matter → compute fitness.
4. Update the population and iterate.

---

### 2 · Key prior-art clusters to draw inspiration from

| Approach                                                | Core idea                                                                                                                                         | What to borrow                                                                                  |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| **Prompt Folding**                                      | Recursive “compress and improve” of a prompt by the model itself.                                                                                 | Good *meta-prompt* templates for single-prompt refinement.                                      |
| **Self-Refine** (Wu et al., 2023)                       | Feedback-→-refine loops where the model critiques and rewrites its own answer until quality converges. ([selfrefine.info][1])                     | Structure your *improve-prompt* meta-prompt to ask for a critique **then** a rewritten version. |
| **Automatic Prompt Engineer (APE)** (Zhou et al., 2022) | LLM proposes many candidate instructions; a scoring oracle picks the best. ([promptingguide.ai][2])                                               | Use Failter as that “oracle”; APE’s breadth-first sampling gives diversity.                     |
| **Promptbreeder** (Fernando et al., 2023)               | *Evolutionary algorithm* where an LLM not only mutates task prompts but evolves its own *mutation prompts*. ([openreview.net][3], [arxiv.org][4]) | Let your meta-prompt itself be evolvable—a neat second layer.                                   |
| **Reflexion** (Shinn et al., 2023)                      | Agents write “reflections” after each failure and use them to guide the next attempt. ([promptingguide.ai][5])                                    | Store reflections as Failter `.thoughts` to condition later mutations.                          |
| **EvoPrompt / GAAPO / StablePrompt** (2023-25)          | Classical genetic or RL algorithms applied to discrete prompt tokens. ([arxiv.org][6], [arxiv.org][7], [arxiv.org][8])                            | Useful crossover / selection heuristics and multi-objective fitness (score + length + cost).    |

---

### 3 · Design tips for your implementation

1. **Dual-model pipeline**
   Use a *cheap, fast* model for mutation/crossover generation and a *higher-quality* judge (as you already do in Failter) for evaluation. This keeps the loop affordable.

2. **Multi-objective fitness**
   Failter already outputs *grade*, *execution-time*, and *cost*. Treat fitness as a tuple—e.g., maximize grade, minimize cost & prompt length—then use Pareto or weighted sums.

3. **Diversity preservation**
   Evolutionary prompt search can converge prematurely. Keep a novelty score (e.g., edit distance or semantic distance between prompts) to maintain exploration.

4. **Recombination prompt**
   A simple yet effective meta-prompt:

   > “You will be given Prompt A and Prompt B that both solve the same task.
   > Combine the strongest elements of each while removing redundancy.
   > The result must stay under **X** tokens.”

5. **Logging lineage**
   Add `:parent-prompts` or a hash to the YAML front-matter so you can trace ancestry of high-performing prompts.

6. **Human-in-the-loop checkpoints**
   Insert manual review every *k* generations to catch degenerate solutions or subtle bias.

---

### 4 · Where to look next

* **Code & tooling**

  * `promptbreeder` and `evo-prompt` GitHub repos have reference implementations of GA-style loops you can adapt.
  * The *Open DeScent* project (2024) offers a generic black-box optimiser that can call any shell command—handy for wrapping Failter evaluations.

* **Benchmarks & Datasets**

  * *BigBench Hard* or *LAMBADA* style datasets give objective pass/fail metrics, making fitness less noisy than subjective grading tasks.
  * For text-cleanup tasks (your current focus), build a gold-standard “clean” target to compute automated diff-based scores in addition to Judge-LLM grades.

* **Evaluation research**

  * “Large Language Model Judge” (Liu et al., 2023) analyses when LLM graders agree with humans—useful for calibrating Failter’s judge model choice.

---

### 5 · Bottom line

Your evolutionary-style prompt search vision is well-grounded in current
literature and should integrate cleanly with Failter’s experiment / evaluate /
report triad. By:

1. Treating **Failter’s YAML reports as the fitness function**,
2. Using LLM-driven **mutation and crossover meta-prompts**, and
3. Borrowing selection & diversity ideas from Promptbreeder, EvoPrompt, and GAAPO,

you can build a fully automated **prompt-breeding laboratory** that iteratively
discovers high-performing, low-cost prompts for any text-filtering task you care
about.

Happy breeding! 🚀

[1]: https://selfrefine.info/?utm_source=chatgpt.com "Self-Refine: Iterative Refinement with Self-Feedback"
[2]: https://www.promptingguide.ai/techniques/ape?utm_source=chatgpt.com "Automatic Prompt Engineer (APE)"
[3]: https://openreview.net/forum?id=HKkiX32Zw1&utm_source=chatgpt.com "Self-Referential Self-Improvement via Prompt Evolution | OpenReview"
[4]: https://arxiv.org/abs/2309.16797?utm_source=chatgpt.com "Promptbreeder: Self-Referential Self-Improvement Via Prompt Evolution"
[5]: https://www.promptingguide.ai/techniques/reflexion?utm_source=chatgpt.com "Reflexion | Prompt Engineering Guide"
[6]: https://arxiv.org/abs/2309.08532?utm_source=chatgpt.com "EvoPrompt: Connecting LLMs with Evolutionary Algorithms Yields ..."
[7]: https://arxiv.org/html/2504.07157v3?utm_source=chatgpt.com "GAAPO: Genetic Algorithmic Applied to Prompt Optimization - arXiv"
[8]: https://arxiv.org/abs/2410.07652?utm_source=chatgpt.com "StablePrompt: Automatic Prompt Tuning using Reinforcement ... - arXiv"
