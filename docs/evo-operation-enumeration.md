## Operations

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

## Coverage

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
