# 1  System Overview

The system keeps a **population of prompts** for a single, well-defined task.
Each generation it:

1. **Generates variants** of the current prompts (mutation, crossover, paraphrase, etc.).
2. **Filters** those variants against hard constraints (length, safety, domain rules).
3. **Scores** every surviving prompt with an external black-box evaluation harness.
4. **Selects** the fittest prompts (plus some diversity) to form the next generation.
5. **Repeats** until a termination rule fires (max generations, plateaued fitness, or budget).

Only the grey boxes below require an LLM; the rest are ordinary software modules.

```
 ┌──────────┐    ┌───────────────┐
 │  Seed    │    │  Population   │
 │Creation* │    │   (Gen t)     │
 └────┬─────┘    └──────┬────────┘
      │                │
      │ 1. Variation*  │
      ▼                ▼
 ┌──────────┐    ┌───────────────┐
 │ New Draft│    │   Filtering*  │
 │ Prompts* │    │ & Validation  │
 └────┬─────┘    └──────┬────────┘
      │                │
      │ 2. Harness Eval│
      ▼                ▼
 ┌──────────┐    ┌───────────────┐
 │  Scores  │    │  Selection    │
 └────┬─────┘    └──────┬────────┘
      │                │
      └────► Update Population ──► repeat / stop
```

\*LLM-dependent steps.

---

# 2  Operation Enumeration

| #  | Operation                               | Purpose in Pipeline                                       | LLM Needed?               |
| -- | --------------------------------------- | --------------------------------------------------------- | ------------------------- |
| 1  | **Load task spec**                      | Read task description & hard constraints                  | No                        |
| 2  | **Seed-prompt generation**              | Create initial population                                 | **Yes**                   |
| 3  | **Prompt mutation**                     | Produce small edits of a parent prompt                    | **Yes**                   |
| 4  | **Prompt crossover**                    | Splice content from two parents                           | **Yes**                   |
| 5  | **Constraint-aware drafting**           | Generate *new* prompts that satisfy domain constraints    | **Yes**                   |
| 6  | **Semantic & policy filter**            | Detect contradictions, disallowed content                 | **Yes** (optional hybrid) |
| 7  | **Length / syntax check**               | Enforce max tokens, valid encoding                        | No                        |
| 8  | **Duplicate removal & novelty scoring** | Keep population diverse                                   | No                        |
| 9  | **Local (cheap) fitness guess**         | Ask LLM to predict harness score to cull bad drafts early | **Yes (optional)**        |
| 10 | **Format prompt for harness**           | Wrap with system headers, few-shot examples               | No                        |
| 11 | **Send to evaluation harness**          | Get ground-truth score                                    | No (external)             |
| 12 | **Selection / replacement**             | Choose next generation                                    | No                        |
| 13 | **Lineage & metadata logging**          | Track ancestry, scores                                    | No                        |
| 14 | **Termination check**                   | Stop on plateau, budget, or max gen                       | No                        |
| 15 | **Best-prompt summarisation**           | Output top prompts + rationale                            | **Yes**                   |

---

# 3  LLM-Dependent Operations — Templates & Inputs

| Step Name                                   | Prompt Template Outline (sketch)                                                                                                                                                                                                                                                                   | Required Inputs                                                   |
| ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| **Seed\_Prompt\_Generation**                | *“You are designing prompts for the task **{task\_description}**.  Produce **{n}** diverse candidate prompts that: 1) follow the style guide **{style\_rules}**, 2) stay within **{token\_limit}** tokens, 3) avoid prohibited content **{policy\_summary}**.  List each prompt on its own line.”* | task\_description, n, style\_rules, token\_limit, policy\_summary |
| **Prompt\_Mutation**                        | *“Here is a parent prompt:\n>>> {parent\_prompt}\n---\nMake **{k}** variants that preserve intent but differ in wording, order, or emphasis.  Keep them ≤ {token\_limit} tokens and obey these constraints: {constraints}.”*                                                                       | parent\_prompt, k, token\_limit, constraints                      |
| **Prompt\_Crossover**                       | *“Combine the useful parts of these two prompts:\nA) {prompt\_A}\nB) {prompt\_B}\nMake **{m}** hybrids that integrate the best instructions from each while satisfying: {constraints}.  Give one hybrid per bullet.”*                                                                              | prompt\_A, prompt\_B, m, constraints                              |
| **Constraint\_Aware\_Drafting**             | *“Write **{k}** brand-new prompts for **{task\_description}** that explicitly satisfy all domain rules:\n{domain\_rules}\nEach prompt ≤ {token\_limit} tokens.”*                                                                                                                                   | task\_description, domain\_rules, k, token\_limit                 |
| **Semantic\_Policy\_Filter**                | *“Evaluate the following prompt for (a) contradiction, (b) policy violations, (c) missing mandatory clauses.  Output PASS or FAIL plus a short reason.\nPrompt:\n>>> {candidate\_prompt}\nRules:\n{policy\_rules}”*                                                                                | candidate\_prompt, policy\_rules                                  |
| **Surrogate\_Fitness\_Estimate** (optional) | *“Predict on a 0–100 scale how well this prompt will perform on **{task\_metric}** for **{task\_description}**.  Give only the numeric estimate and ≤ 20-word justification.\nPrompt:\n>>> {candidate\_prompt}”*                                                                                   | candidate\_prompt, task\_description, task\_metric                |
| **Best\_Prompt\_Summary**                   | *“Summarise the strengths and weaknesses of the top **{top\_k}** prompts below, then recommend the single best prompt.\nPrompts + scores:\n{prompt\_score\_table}”*                                                                                                                                | prompt\_score\_table, top\_k                                      |

---

# 4  Coverage Check

| Evolution Phase              | Covered Operations |
| ---------------------------- | ------------------ |
| **Initialisation**           | 1, 2               |
| **Variation**                | 3, 4, 5            |
| **Validation**               | 6, 7, 8            |
| **Pre-screening (optional)** | 9                  |
| **Evaluation**               | 10, 11             |
| **Selection & Book-keeping** | 12, 13             |
| **Termination**              | 14                 |
| **Post-processing**          | 15                 |

All necessary LLM-driven steps are specified with templates; all non-LLM steps are ordinary software logic or calls to the black-box harness.

---

# 5  Termination Criteria Examples (non-LLM)

* **Max generations** reached
* **Fitness plateau** (no improvement ≥ ε for G generations)
* **Budget** exhausted (time, money, or harness calls)

---

## Deliverable

The tables above give a technical team every LLM prompt they need to implement, which inputs to pass, and which surrounding operations can be coded with standard tooling, enabling a complete evolutionary-prompt pipeline without exposing the underlying evolutionary algorithm or the evaluation harness itself.
