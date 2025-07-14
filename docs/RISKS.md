# PromptCritical Project Risks

**Version 3.0 · 2025‑07‑14**
**Status:** *Reviewed post-v0.4 Advanced Evolution Strategies*

Here are ten key risks for the PromptCritical project, ranked from most to least severe.

## 1. **Premature Convergence to Local Optima**
**Risk**: A simplistic or overly greedy selection strategy could cause the population to lose diversity and converge on a suboptimal solution, preventing the discovery of better alternatives.

**Mitigation**:
- **(Largely Mitigated)** The primary mitigation is the `select` command's support for **Tournament Selection** via the `--policy tournament-k=N` flag. This strategy is explicitly designed to preserve population diversity by giving lower-scoring individuals a chance to survive based on their performance within a smaller, random subset of the population.
- The `top-N` policy remains available for more exploitative, "greedy" selection when desired.
- The `vary` command's mutation operators continue to introduce new genetic material.
- Monitor population diversity metrics in `contest-metadata.edn` to detect convergence.

## 2. **Evaluation Brittleness/Inconsistency**
**Risk**: The `Failter` judges (LLM-based) may produce inconsistent scores for the same prompt across different runs, making fitness comparisons unreliable and rendering the `select` command's decisions random.

**Mitigation**:
- Run multiple evaluation trials per prompt within a contest and average the scores. (This is a planned `Failter` feature).
- Implement evaluation confidence intervals in contest result analysis.
- Add deterministic, rule-based metrics alongside LLM evaluation for a stable baseline.
- Track evaluation variance in `contest-metadata.edn` to detect judge instability.

## 3. **Exponential Cost Growth**
**Risk**: As population size or the number of generations grows, API costs from the `vary` and `evaluate` commands could quickly become prohibitive.

**Mitigation**:
- Implement cost budgets and tracking per-experiment. The `evolve` command includes a `--max-cost` flag.
- Use cheaper models in the `vary` step by configuring them in `evolution-parameters.edn`, reserving more expensive models for the `evaluate` step.
- Develop "surrogate critics" (cheaper models or heuristics) to pre-filter prompts before a full evaluation contest. (v0.5 Milestone)
- Implement early-stopping rules based on cost thresholds.

## 4. **Data Leakage in Evaluation**
**Risk**: If the documents used in an evaluation contest were part of the judging LLM's training data, the resulting fitness scores would be meaningless.

**Mitigation**:
- Use very recent content (post-dating the LLM's training cut-off) for evaluation corpora.
- Maintain a diverse evaluation corpus from multiple sources.
- Document all data sources and dates in `contest-metadata.edn`.

## 5. **Prompt Template Fragility**
**Risk**: The `{{FIELD}}` template system could break as prompts evolve during the `vary` step, causing runtime failures during the `evaluate` step.

**Mitigation**:
- The `pcrit.pop` component validates template fields upon ingestion.
- The meta-prompts used by the `vary` command must be carefully designed to preserve or correctly modify template variables.
- The `evaluate` command has a validation step to ensure all prompts in a population have the required `{{INPUT_TEXT}}` field before starting a contest.

## 6. **Meta-Prompt Bias/Limitations**
**Risk**: The initial meta-prompts used by the `vary` command may have inherent biases that constrain the evolutionary search space and prevent novel solutions from emerging.

**Mitigation**:
- **(Partially Mitigated)** The introduction of the `:crossover` mutation strategy provides an alternative evolutionary pathway that is not dependent on a single meta-prompt's phrasing. It combines existing high-performing prompts to generate novel solutions through a different mechanism.
- The system still supports multiple, diverse meta-prompts (e.g., "improve," "rephrase," "make more concise").
- Future versions may still explore meta-prompt evolution (i.e., evolve the evolvers).

## 7. **Scalability Bottlenecks**
**Risk**: The file-based prompt store and symlink system may not scale efficiently to thousands of prompts and generations.

**Mitigation**:
- The `pcrit.pdb` component abstracts the storage mechanism, allowing the file-based system to be replaced with a more scalable backend (e.g., a database) in the future without changing the core logic.
- Profile system performance with large prompt populations.
- Implement prompt store cleanup/archiving for old generations.

## 8. **Reproducibility Failures**
**Risk**: Despite immutable storage, subtle differences in LLM API responses, model versions, or evaluation conditions could make experiments difficult to reproduce perfectly.

**Mitigation**:
- Record exact model versions and API parameters in `contest-metadata.edn`.
- The `Failter` spec records all parameters, and its artifacts directory provides for idempotent, resumable runs.
- Implement experiment replay functionality in a future version.
- Document all external dependencies and their versions.

## 9. **Overfitting to Evaluation Corpus**
**Risk**: Prompts could become highly specialized to the specific documents used for evaluation, performing poorly on new, unseen content.

**Mitigation**:
- Maintain separate "training" and "testing" evaluation corpora.
- Rotate evaluation content periodically.
- Include diverse content types (different topics, formats, styles).
- Implement cross-validation with multiple evaluation sets in later versions.

## 10. **Integration Complexity with Failter**
**Risk**: The dependency on `Failter` for evaluation creates a potential single point of failure and adds complexity to the system.

**Mitigation**:
- **(Largely Mitigated)** The `evaluate` command integrates with a simple, robust `failter run --spec <path>` command. The declarative `spec.yml` and structured JSON output dramatically reduce integration complexity.
- The `pcrit.failter` component remains a clean abstraction layer, isolating the rest of the system from the specifics of the `Failter` tool.
- Maintain comprehensive integration tests for the `evaluate` -> `Failter` boundary.
