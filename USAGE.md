# PromptCritical Usage Guide

This guide explains how to use the `pcrit` command-line tool to set up, run, and manage prompt evolution experiments.

## Core Philosophy

PromptCritical treats prompt engineering as a scientific, evolutionary process. The goal is to move beyond manually tweaking prompts and instead use a data-driven loop. With the `evolve` command, this entire loop is automated.

The core workflow is:
1.  **`init`**: Create a new, ready-to-use experiment skeleton.
2.  **`bootstrap`**: Ingest the initial prompts and create the first population (`gen-0`).
3.  **`evolve`**: Automatically run the `vary` → `evaluate` → `select` cycle for a specified number of generations or until a cost budget is met.

The manual commands (`vary`, `evaluate`, `select`) are still available for fine-grained control or custom workflows, but `evolve` is the new standard for running an experiment.

## The Standard Workflow: `init` → `bootstrap` → `evolve`

Here is a step-by-step guide to running your first fully automated experiment.

### Step 1: Initialize the Experiment Directory

The easiest way to start is with the `init` command. It creates a new directory containing a runnable set of seed prompts and configuration files.

```bash
pcrit init my-first-experiment
```

This command creates the `my-first-experiment` directory and populates it with everything you need to start, including default meta-prompts for refining and crossover.

### Step 2: Bootstrap the Initial Population

Now, move into your new directory and run the `bootstrap` command. This reads the manifest, ingests the prompts, creates named links, and establishes `gen-0`.

```bash
cd my-first-experiment
pcrit bootstrap .
```
After this step, your experiment is live and has a testable population in `generations/gen-000/`.

### Step 3: Configure The Experiment

The `init` command creates a default `evolution-parameters.edn` file. You should review this file and customize it for your needs. At a minimum, you must specify which LLM models to test against. You can also now configure the variation strategy.

```clojure
// File: my-first-experiment/evolution-parameters.edn
{
 ;; This section is REQUIRED for the 'evaluate' and 'evolve' commands.
 :evaluate {:models ["openai/gpt-4o-mini"]
            ;; This key is optional.
            :judge-model "openai/gpt-4o"}

 ;; This section is optional. It controls the 'vary' command.
 ;; If omitted, it will use the default :refine strategy.
 :vary {;; The LLM to use for creating new prompts.
        :model "openai/gpt-4o-mini"
        ;; The mutation strategy to use.
        ;; :refine - Applies the 'improve' meta-prompt to each parent. (Default)
        ;; :crossover - Breeds the top two parents from the previous generation.
        :strategy :refine}
}
```

### Step 4: Run the Automated Evolution Loop

With the experiment bootstrapped and configured, you can now start the automated evolutionary process with the `evolve` command. The following command will run the full `vary` → `evaluate` → `select` cycle 5 times.

```bash
pcrit evolve . --generations 5 --inputs path/to/my/inputs/
```

The system will now run autonomously. You can also set a cost limit to prevent runaway spending:

```bash
pcrit evolve . \
  --generations 20 \
  --max-cost 10.00 \
  --inputs path/to/my/inputs/
```

## Analyzing Results with the `stats` Command

After you've run one or more `evaluate` contests (either manually or via `evolve`), you can analyze their performance and cost using the `stats` command.

**To see stats for a specific contest:**
```bash
# Contest names are generated automatically by 'evolve' and printed in the log
pcrit stats . --from-contest "evo-1752495082947-gen-0"
```

**To see aggregated stats for all contests in a generation:**
```bash
pcrit stats . --generation 3
```
If you omit the `--generation` flag, it will show stats for the latest generation by default.

## Advanced Selection and Variation

With the v0.4 update, you now have more control over the evolutionary process.

### Advanced Selection with `--policy`

The `select` command (and by extension, the `evolve` command) can use different selection policies to choose survivors. This is controlled by the `--policy` flag.

*   **Top-N (Default):** `pcrit select . --policy top-N=5`
    This is a "greedy" strategy that keeps only the absolute best performers. It's efficient but can lead to a lack of diversity.

*   **Tournament Selection:** `pcrit select . --policy tournament-k=2`
    This strategy runs many small "tournaments". In each, it picks `k` prompts at random from the population and keeps only the winner of that small group. This gives lower-scoring prompts a chance to survive, preserving genetic diversity.

### Advanced Variation with `:strategy`

As shown in the configuration example, the `vary` step can be configured in `evolution-parameters.edn` to use different mutation strategies.

*   **:refine (Default):** For each prompt in the population, this strategy uses the `refine` meta-prompt to generate an improved version.
*   **:crossover:** This strategy ignores the current population, looks at the results of the *previous* generation's contest, and breeds the top two performers to create a single new hybrid prompt.

## Core Concepts Reference

### The Experiment Directory

Everything related to a single experiment lives in one directory. This is the first argument you will pass to most `pcrit` commands. It contains the prompt database, generation data, and links to key prompts.

### Prompt Types

Every prompt ingested into the database is automatically analyzed and assigned a `:prompt-type` in its metadata. This type defines its role in the system.

*   **:object-prompt**: A prompt that performs a task on an external input (e.g., summarizing a document). These are the main members of a population.
*   **:meta-prompt**: A prompt that operates on *another prompt*. It is used during the `vary` step to create new prompt variations.

### Template Variables

Your prompt files **must** use these specific names for the system to work correctly.

| Variable Name     | Required For     | Used By          | Value Provided By                                                  |
| :---------------- | :--------------- | :--------------- | :----------------------------------------------------------------- |
| `{{INPUT_TEXT}}`  | `:object-prompt` | `pcrit evaluate` | The content of a file from the `--inputs` directory during a Failter run. |
| `{{OBJECT_PROMPT}}` | `:meta-prompt`   | `pcrit vary`     | The body of another prompt being mutated or combined.              |
| `{{OBJECT_PROMPT_A}}` | `:meta-prompt`   | `pcrit vary`     | The body of the first parent in a `:crossover` operation.          |
| `{{OBJECT_PROMPT_B}}` | `:meta-prompt`   | `pcrit vary`     | The body of the second parent in a `:crossover` operation.         |
