# PromptCritical Usage Guide

This guide explains how to use the `pcrit` command-line tool to set up, run, and manage prompt evolution experiments.

## Core Philosophy

PromptCritical treats prompt engineering as a scientific, evolutionary process. The goal is to move beyond manually tweaking prompts and instead use a data-driven loop:

1.  **Bootstrap**: Ingest initial, hand-written prompts and create the first population (`gen-0`).
2.  **Vary**: Produce a new generation of prompts by applying meta-prompts (mutation, crossover) to the previous generation.
3.  **Evaluate**: Run the prompts against a corpus of inputs using the **Failter** tool to generate performance scores.
4.  **Select**: Use the scores to select the fittest prompts to survive to the next generation.

Steps 2 through 4 are repeated in a cycle, with each iteration producing a new, more refined generation of prompts. This entire process is managed from the command line within a dedicated **Experiment Directory**.

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

### Step 1: Set Up and Bootstrap the Experiment

First, create your experiment directory and the necessary seed files.

1.  **Create a directory for your experiment:**
    ```bash
    mkdir my-first-experiment
    mkdir my-first-experiment/seeds
    ```

2.  **Create your raw prompt files** inside the `seeds/` directory.

3.  **Create the bootstrap manifest file** (`bootstrap.edn`).

4.  **Run the `bootstrap` command:** This reads your manifest, ingests the prompts, and automatically creates the first population (`gen-0`).

    ```bash
    pcrit bootstrap my-first-experiment
    ```
    After this step, your experiment is initialized and you have a testable population in `generations/gen-000/`.

### Step 2: Configure The Experiment

Next, you **must** create an `evolution-parameters.edn` file in your experiment's root directory to control the `evaluate` and `vary` commands.
NOTE: Models default to `mistral` if not specified.

1.  **Create the configuration file:** `my-first-experiment/evolution-parameters.edn`
2.  **Add parameters.** At a minimum, you must specify which models to test against in the `evaluate` command.

    ```clojure
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
### Step 3: Evaluate the Initial Population

NOTE: After bootstrap you may evaluate immediately to capture a baseline — or
vary first to explore the space before scoring. Choose the sequence that serves
your research goal.

With the experiment configured, you can evaluate the performance of `gen-0`.

1.  **Gather your evaluation data.** You will need a directory of input files.
2.  **Run the `evaluate` command.**

    ```bash
    pcrit evaluate my-first-experiment \
      --generation 0 \
      --name "initial-cleanup-contest" \
      --inputs path/to/my/inputs/
    ```
    This command will read your `:models` configuration, run the Failter contest, and place the resulting `report.csv` in a contest subdirectory. You can also override the judge model from the command line:
    ```bash
    pcrit evaluate my-experiment/ --inputs ... --judge-model "anthropic/claude-3-sonnet"
    ```


### Step 4: Select the Survivors

After evaluating a generation, you can use the scores to create a new generation comprised of only the best performers.

```bash
# This is the future goal (v0.2)
pcrit select my-first-experiment --from-contest "initial-cleanup-contest"
```

This will create a new generation directory (e.g., `gen-001/`) containing only the surviving prompts.

### Step 5: Vary the Population to Create the Next Generation

Now, you can apply your meta-prompts to the surviving population to breed new candidates for the next round of evaluation.

```bash
pcrit vary my-first-experiment```
This command loads the latest generation (e.g., `gen-001`), applies meta-prompts to create new candidates, and saves the result as a new generation (e.g., `gen-002`).

By repeating the `evaluate`, `select`, and `vary` steps, you iteratively improve your prompt population.

## Choosing your loop order

After `bootstrap` seeds **gen-000**, you have two common entry points into the cycle:

  * **evaluate → select → vary** (baseline-first). Run `evaluate` immediately to capture how the hand-crafted prompts perform *unchanged*; keep that CSV as a control. Then `select` your top performers and let `vary` generate the first novel population.
  * **vary → evaluate → select** (explore-first). Skip the baseline, mutate the seeds straight away with `vary`, and score the brand-new offspring instead. This is useful when your seeds are only rough sketches and you want the LLM to expand the search space quickly.

 Both orders are valid; pick the one that best fits your research question. You can even alternate—e.g., baseline once, then run several *vary → evaluate → select* rounds—by calling the commands in the sequence that matches your experimental design.
