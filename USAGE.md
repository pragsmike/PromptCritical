# PromptCritical Usage Guide

This guide explains how to use the `pcrit` command-line tool to set up, run, and manage prompt evolution experiments.

## Core Philosophy

PromptCritical treats prompt engineering as a scientific, evolutionary process. The goal is to move beyond manually tweaking prompts and instead use a data-driven loop. With the new `evolve` command, this entire loop is now automated.

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

This command creates the `my-first-experiment` directory and populates it with everything you need to start.

### Step 2: Bootstrap the Initial Population

Now, move into your new directory and run the `bootstrap` command. This reads the manifest, ingests the prompts, and creates `gen-0`.

```bash
cd my-first-experiment
pcrit bootstrap .
```
After this step, your experiment is live and has a testable population in `generations/gen-000/`.

### Step 3: Configure The Experiment

The `init` command creates a default `evolution-parameters.edn` file. You should review this file and customize it for your needs. At a minimum, you must specify which LLM models to test against.

```clojure
// File: my-first-experiment/evolution-parameters.edn
{
 ;; This section is REQUIRED for the 'evaluate' and 'evolve' commands.
 :evaluate {:models ["openai/gpt-4o-mini" "ollama/qwen3:8b"]
            ;; This key is optional.
            :judge-model "openai/gpt-4o"}

 ;; This section is optional for the 'vary' command.
 ;; If omitted, it will use a sensible default.
 :vary {:model "openai/gpt-4o-mini"}
}
```

### Step 4: Run the Automated Evolution Loop

With the experiment bootstrapped and configured, you can now start the automated evolutionary process with the `evolve` command. The following command will run the full `vary` → `evaluate` → `select` cycle 5 times, using the text files in `path/to/my/inputs/` for every evaluation step.

```bash
pcrit evolve . \
  --generations 5 \
  --inputs path/to/my/inputs/```

The system will now run autonomously, printing its progress for each generation. You can also set a cost limit to prevent runaway spending:

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

## Manual Workflow Commands (for advanced use)

The `evolve` command automates the following three core functions. You can also run them manually for more direct control over the evolutionary cycle.

*   **`pcrit vary .`**
    Produces new candidate prompts by applying meta-prompts to the *current* population.

*   **`pcrit evaluate . --name "my-contest" --inputs ...`**
    Runs all prompts in the current population against a corpus of inputs using the **Failter** tool to generate performance scores.

*   **`pcrit select . --from-contest "my-contest"`**
    Uses the scores from a contest report to select the fittest prompts and create a **new generation folder** containing only the survivors.

## Core Concepts Reference

### The Experiment Directory

Everything related to a single experiment lives in one directory. This is the first argument you will pass to most `pcrit` commands. It contains the prompt database, generation data, and links to key prompts.

### Prompt Types

Every prompt ingested into the database is automatically analyzed and assigned a `:prompt-type` in its metadata. This type defines its role in the system.

*   **:object-prompt**: A prompt that performs a task on an external input (e.g., summarizing a document). These are the main members of a population.
*   **:meta-prompt**: A prompt that operates on *another prompt*. It is used during the `vary` step to create new prompt variations.

### Template Variables

The prompt types are determined by the presence of special template variables. Your prompt files **must** use these specific names for the system to work correctly.

| Variable Name     | Required For     | Used By          | Value Provided By                                                  |
| :---------------- | :--------------- | :--------------- | :----------------------------------------------------------------- |
| `{{INPUT_TEXT}}`  | `:object-prompt` | `pcrit evaluate` | The content of a file from the `--inputs` directory during a Failter run. |
| `{{OBJECT_PROMPT}}` | `:meta-prompt`   | `pcrit vary`     | The body of another prompt being mutated or combined.              |
