# Understanding and Managing Costs in PromptCritical

Large Language Model (LLM) API calls cost money. As you run evolutionary experiments with dozens or hundreds of prompts over multiple generations, these costs can add up. PromptCritical is designed not only to track these costs but also to give you the tools and understanding needed to manage them effectively.

This document explains how PromptCritical tracks LLM usage, how it calculates monetary cost from that usage, and what strategies you can employ to run effective experiments while minimizing expense.

## The Golden Rule: Usage vs. Cost

To understand cost in PromptCritical, you must first understand the core principle of its accounting system: **we store immutable usage data as the source of truth and calculate cost from it.**

*   **Usage Data (The Fact):** This is the record of what happened during an API call. It consists of the `model` name used, the number of `tokens_in` (input), and the number of `tokens_out` (output). This data is an immutable fact—it will be true forever.

*   **Cost Data (The Calculation):** This is the monetary cost in USD. Since model pricing changes over time, this value is considered a point-in-time calculation based on the usage data.

PromptCritical tracks and calculates costs generated during the two key phases of the evolutionary loop: prompt generation (`vary`) and prompt evaluation (`evaluate`).

## The Price Table

The heart of the cost calculation system is the `price-table` defined within PromptCritical's internal configuration. This table maps model names to their cost per 1,000 tokens for both input and output.

> You can review the current price table within the project's source code to see which models are recognized and at what rates. PromptCritical uses this internal table for all calculations.

## How Costs are Calculated: The Two Pathways

### Pathway 1: The `vary` Command (Prompt Generation)

When you run `pcrit vary`, PromptCritical calls an LLM to generate new "offspring" prompts from existing ones. The cost of this creative step is calculated and recorded immediately.

1.  **Action:** The `vary` command sends a meta-prompt and a parent prompt to an LLM.
2.  **Usage Tracking:** The system receives a response from the LLM API that includes the tokens used for both the input and the generated output.
3.  **Cost Calculation:** PromptCritical immediately uses its internal **price table** to calculate the cost of that single API call.
4.  **Storage:** The full usage and cost data (`:model`, `:token-in`, `:token-out`, `:cost-usd-snapshot`, etc.) is written directly into the YAML header of the newly created prompt file in the `pdb/`.

### Pathway 2: The `evaluate` Command (Contest Scoring)

When you run `pcrit evaluate`, the system orchestrates the external `Failter` tool to run a contest, scoring each prompt in the population against your input documents.

1.  **Action:** The `evaluate` command shells out to `Failter`, passing it the population of prompts and the input directory.
2.  **Usage Tracking (by Failter):** `Failter` performs all the necessary LLM calls to score the prompts. For each prompt it evaluates, it records the `model` used, the `tokens_in`, and the `tokens_out` in its final output file, `report.csv`.
3.  **Cost Calculation (by PromptCritical):** After the contest is complete, commands like `pcrit stats` read the `report.csv` file. For each row in the report, **PromptCritical then calculates the cost itself** by applying its internal **price table** to the token counts provided by Failter.

This approach ensures that even as Failter's reporting format changes, PromptCritical maintains a consistent and centralized method for calculating evaluation costs.

## Practical Strategies for Minimizing Costs

Your experiment's cost is a direct function of the models you choose and the number of API calls you make. Here are four practical strategies to keep costs down.

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
