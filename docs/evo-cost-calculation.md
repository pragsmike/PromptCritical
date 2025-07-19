# Understanding and Managing Costs in PromptCritical

Large Language Model (LLM) API calls cost money. PromptCritical is designed not only to track these costs but also to give you the tools and understanding needed to manage them effectively.

This document explains how PromptCritical tracks and calculates LLM costs following the extraction of this logic into the external `pcrit-llm` library.

## The Golden Rule: Delegation to `pcrit-llm`

To understand cost in PromptCritical, you must first understand the core principle of its accounting system: **PromptCritical now delegates all cost calculation to the `pcrit-llm` library.**

The `pcrit-llm` library contains the canonical `price-table` and is the single source of truth for all cost-related logic. PromptCritical consumes this library to calculate costs at two different points in the evolutionary cycle.

## How Costs are Calculated: The Two Pathways

### Pathway 1: The `vary` Command (Pre-calculated Cost)

When you run `pcrit vary`, the system calls an LLM to generate new "offspring" prompts. The cost of this step is calculated by the `pcrit-llm` library and embedded directly into the new prompt artifact.

1.  **Action:** The `vary` command sends a meta-prompt and a parent prompt to the `pcrit-llm` library.
2.  **Usage Tracking & Cost Calculation (by `pcrit-llm`):** The `pcrit-llm` library performs the API call, receives the token usage from the provider, and **immediately calculates the monetary cost** using its internal price table.
3.  **Storage:** The response from `pcrit-llm` already contains a `:cost-usd-snapshot` value. PromptCritical writes this complete, pre-calculated metadata directly into the YAML header of the newly created prompt file in the `pdb/`.

### Pathway 2: The `stats` Command (Just-in-Time Cost)

The `evaluate` command's only job is to orchestrate the external `Failter` tool. It does **not** calculate cost. The `stats` command is now responsible for calculating the cost of an evaluation *after the fact*.

1.  **Action:** The `pcrit evaluate` command shells out to `Failter`, which runs the contest and records the raw **token usage** for each prompt in its `failter-report.json` output.
2.  **Usage Parsing:** When you run `pcrit stats`, it uses the `pcrit.results` component to parse the `failter-report.json` file and extract the raw token counts for each prompt.
3.  **Cost Calculation (by `pcrit stats`):** For each prompt result, the `stats` command then calls the `pcrit.llm.costs/calculate-cost` function from the external library, passing it the model name and token counts. This calculates the cost for the evaluation just-in-time for display.

This approach ensures that cost calculation remains consistent and centralized within the `pcrit-llm` library.

## Practical Strategies for Minimizing Costs

Your experiment's cost is a direct function of the models you choose and the number of API calls you make. These strategies remain critical for managing your budget.

#### 1. Use Cheaper Models for `vary`

The `vary` step often benefits from creativity and speed more than raw power. You can configure PromptCritical to use a cheaper, faster model for generating new prompts while reserving more expensive, powerful models for the final evaluation. This is done in the `evolution-parameters.edn` file.

```clojure
// File: my-experiment/evolution-parameters.edn
{
 ;; Use a powerful, expensive model for the final scoring contest.
 :evaluate {:models ["openai/gpt-4o"]}

 ;; Use a cheap, fast model for creating new prompt variations.
 :vary {:model "openai/gpt-4o-mini"}
}
```

#### 2. Control Population Size

The single biggest factor in the cost of an `evaluate` run is the number of prompts in the population. You can control this with the `--policy` flag of the `select` command. Selecting a smaller number of "survivors" for the next generation directly reduces the cost of the subsequent evaluation.

*   `pcrit select ... --policy top-N=5` (Cheaper)
*   `pcrit select ... --policy top-N=20` (More Expensive, but more exploratory)

#### 3. Limit the Evaluation Corpus

The cost of an `evaluate` contest is also proportional to the number of input files you provide. Running a contest against 10 documents will be roughly five times cheaper than running it against 50. For early, exploratory generations, consider using a smaller, representative sample of your input data.

#### 4. Leverage Surrogate Critics (Future Work)

The PromptCritical architecture is designed to eventually incorporate "surrogate critics"—cheaper models or even simple heuristics that can pre-filter a large population of newly varied prompts. This will allow the system to discard dozens of likely bad performers *before* paying the cost of a full `Failter` evaluation, dramatically improving the cost-efficiency of the evolutionary process.
