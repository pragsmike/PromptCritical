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

## The Standard Workflow

Here is a step-by-step guide to running your first experiment.

### Step 1: Set Up and Bootstrap the Experiment

First, create your experiment directory and the necessary seed files.

1.  **Create a directory for your experiment:**
    ```bash
    mkdir my-first-experiment
    mkdir my-first-experiment/seeds
    ```

2.  **Create your raw prompt files** inside the `seeds/` directory. Be sure to include at least one object-prompt (`{{INPUT_TEXT}}`) and at least one meta-prompt (`{{OBJECT_PROMPT}}`).

3.  **Create the bootstrap manifest file** (`bootstrap.edn`). This file gives logical names to your seed files.

4.  **Run the `bootstrap` command:** This reads your manifest, creates the experiment's internal structure (`pdb/`), ingests the prompts, and automatically creates the first population (`gen-0`) from your seed object-prompts.

    ```bash
    pcrit bootstrap my-first-experiment
    ```
    After this step, your experiment is initialized and you have a testable population in `generations/gen-000/`.

### Step 2: Configure The Experiment (Optional)

You can control the evolutionary process by creating an `evolution-parameters.edn` file in your experiment's root directory.

For example, to change the model used by the `vary` command, create the file `my-first-experiment/evolution-parameters.edn` with the following content:
```clojure
{:vary {:model "gpt-4-turbo"}}
```
If this file is not present, the system will use sensible defaults (e.g., the "mistral" model).

### Step 3: Evaluate the Initial Population

With `gen-0` created, you can immediately evaluate its performance.

1.  **Gather your evaluation data.** You will need a directory of input files.
2.  **Run the `evaluate` command.**

    ```bash
    # This is the future goal (v0.2)
    pcrit evaluate my-first-experiment \
      --generation 0 \
      --name "initial-cleanup-contest" \
      --inputs path/to/my/inputs/
    ```
    This command will execute the **Failter** pipeline and place the resulting `report.csv` in a contest subdirectory.

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
pcrit vary my-first-experiment
```
This command loads the latest generation (e.g., `gen-001`), applies meta-prompts to create new candidates, and saves the result as a new generation (e.g., `gen-002`).

By repeating the `evaluate`, `select`, and `vary` steps, you iteratively improve your prompt population.
