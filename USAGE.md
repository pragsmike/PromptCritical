# PromptCritical Usage Guide

This guide explains how to use the `pcrit` command-line tool to set up, run, and manage prompt evolution experiments.

## Core Philosophy

PromptCritical treats prompt engineering as a scientific, evolutionary process. The goal is to move beyond manually tweaking prompts and instead use a data-driven loop. The core workflow is now:

1.  **Init**: Create a new, ready-to-use experiment skeleton directory with default prompts and configuration.
2.  **Bootstrap**: Ingest the initial prompts from the skeleton and create the first population folder (`gen-0`).
3.  **Vary**: Produce new candidate prompts by applying meta-prompts (mutation, crossover) to the *current* population. This adds new prompts to the current generation's folder.
4.  **Evaluate**: Run all prompts in the current population against a corpus of inputs using the **Failter** tool to generate performance scores. This creates a `report.csv` inside the current generation's `contests/` directory.
5.  **Select**: Use the scores from a contest report to select the fittest prompts and create a **new generation folder** containing only the survivors.

Steps 3 through 5 are repeated in a cycle, with each `select` command producing a new, more refined generation. This entire process is managed from the command line within a dedicated **Experiment Directory**.

## Core Concepts

### 1. The Experiment Directory

Everything related to a single experiment lives in one directory. This is the first argument you will pass to most `pcrit` commands. It contains the prompt database, generation data, and links to key prompts.

### 2. Prompt Types

Every prompt ingested into the database is automatically analyzed and assigned a `:prompt-type` in its metadata. This type defines its role in the system.

*   **:object-prompt**: A prompt that performs a task on an external input (e.g., summarizing a document). These are the main members of a population.
*   **:meta-prompt**: A prompt that operates on *another prompt*. It is used during the `vary` step to create new prompt variations (e.g., "Rephrase this prompt to be more concise: {{OBJECT_PROMPT}}").
*   **:static-prompt**: A prompt with no special template fields. It can be used for analysis but not directly in the evolution or contest loops.
*   **:invalid-mixed-type**: A prompt that incorrectly contains both special template fields. It cannot be used.

### 3. Template Variables

The prompt types are determined by the presence of special template variables. Your prompt files **must** use these specific names for the system to work correctly.

| Variable Name     | Required For     | Used By          | Value Provided By                                                  |
| :---------------- | :--------------- | :--------------- | :----------------------------------------------------------------- |
| `{{INPUT_TEXT}}`  | `:object-prompt` | `pcrit evaluate` | The content of a file from the `--inputs` directory during a Failter run. |
| `{{OBJECT_PROMPT}}` | `:meta-prompt`   | `pcrit vary`     | The body of another prompt being mutated or combined.              |

NOTE: Allowed placeholders (INPUT_TEXT, OBJECT_PROMPT, …) are being formalised; expect expansion in the next release.

## The Standard Workflow

Here is a step-by-step guide to running your first experiment.

### Step 1: Initialize the Experiment Directory

The easiest way to start is with the `init` command. It creates a new directory containing a runnable set of seed prompts and configuration files.

```bash
pcrit init my-first-experiment
```

### Step 2: Bootstrap the Initial Population

Now, move into your new directory and run the `bootstrap` command. This reads the manifest, ingests the prompts created by `init`, and creates the first population (`gen-0`).

```bash
cd my-first-experiment
pcrit bootstrap .
```
After this step, your experiment is initialized and you have a testable population in `generations/gen-000/`.

### Step 3: Configure The Experiment

The `init` command creates a default `evolution-parameters.edn` file. You should review this file and customize it for your needs. At a minimum, you must specify which LLM models to test against.

```clojure
// File: my-first-experiment/evolution-parameters.edn
{
 ;; This section is REQUIRED for the 'evaluate' command.
 :evaluate {:models ["openai/gpt-4o-mini" "ollama/qwen3:8b"]
            ;; This key is optional.
            :judge-model "openai/gpt-4o"}

 ;; This section is optional for the 'vary' command.
 ;; If omitted, it will use a sensible default.
 :vary {:model "gpt-4-turbo"}
}
```

### Step 4: Vary the Population to Create New Candidates

Now, you can apply your meta-prompts to the population to breed new candidates for evaluation. The `vary` command adds new prompts to the current generation.

```bash
pcrit vary .
```

### Step 5: Evaluate the Current Population

With the population expanded, you can evaluate the performance of all prompts in `gen-0`. Note that upon completion, `evaluate` will log the total cost of the contest.

```bash
pcrit evaluate . \
  --generation 0 \
  --name "initial-cleanup-contest" \
  --inputs path/to/my/inputs/
```

### Step 6: Select the Survivors to Create the Next Generation

After evaluating, use the scores from the contest report to create a new generation comprised of only the best performers.

```bash
pcrit select . --from-contest "initial-cleanup-contest"
```

## Analyzing Results with the `stats` Command

After you've run one or more `evaluate` contests, you can analyze their performance and cost using the `stats` command. This command can show you statistics for a single contest or for an entire generation.

**To see stats for a specific contest:**
```bash
pcrit stats . --from-contest "initial-cleanup-contest"
```

**To see aggregated stats for all contests in a generation:**
```bash
pcrit stats . --generation 0
```
If you omit the `--generation` flag, it will show stats for the latest generation by default.

**Example Output:**
```
Stats for contest: initial-cleanup-contest
-------------------------------------------
Prompts evaluated:   50
Total Cost:          $2.4567
Average Cost:        $0.0491

Highest Score:       0.980 (id: P42)
Lowest Score:        0.650 (id: P18)
Average Score:       0.855
```

## Choosing your loop order

After `bootstrap` seeds **gen-000**, you have two common entry points into the cycle:

  * **evaluate → select → vary** (baseline-first). Run `evaluate` immediately on `gen-000` to capture how the hand-crafted prompts perform *unchanged*. Then run `select` to create `gen-001` with the top performers, and finally run `vary` on `gen-001` to generate new candidates for the next round.
  * **vary → evaluate → select** (explore-first). Skip the baseline. Run `vary` immediately on `gen-000` to mutate the seeds straight away. Then run `evaluate` on the expanded `gen-000` population and `select` the winners into `gen-001`. This is useful when your seeds are only rough sketches and you want to expand the search space quickly.

Both orders are valid. The commands `vary` and `evaluate` operate on the latest generation, while `select` reads from the latest generation to create the next one.
