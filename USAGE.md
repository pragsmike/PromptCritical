# PromptCritical Usage Guide

This guide explains how to use the `pcrit` command-line tool to set up, run, and manage prompt evolution experiments.

## Core Philosophy

PromptCritical treats prompt engineering as a scientific, evolutionary process. The goal is to move beyond manually tweaking prompts and instead use a data-driven loop. A generation folder represents a population at a specific state. The core workflow mutates and evaluates this population *in-place* and then *selects* the winners to create the next generation.

1.  **Bootstrap**: Ingest initial, hand-written prompts and create the first population folder (`gen-0`).
2.  **Vary**: Produce new candidate prompts by applying meta-prompts (mutation, crossover) to the *current* population. This adds new prompts to the current generation's folder.
3.  **Evaluate**: Run all prompts in the current population against a corpus of inputs using the **Failter** tool to generate performance scores. This creates a `report.csv` inside the current generation's `contests/` directory.
4.  **Select**: Use the scores from a contest report to select the fittest prompts and create a **new generation folder** containing only the survivors.

Steps 2 through 4 are repeated in a cycle, with each `select` command producing a new, more refined generation. This entire process is managed from the command line within a dedicated **Experiment Directory**.

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
### Step 3: Vary the Population to Create New Candidates

Now, you can apply your meta-prompts to the population to breed new candidates for evaluation. The `vary` command adds new prompts to the current generation.

```bash
pcrit vary my-first-experiment
```
This command loads the population from `gen-000`, applies meta-prompts to create new candidates, and adds them to the `generations/gen-000/population/` directory. You can run this command multiple times to generate more variations before evaluating.

### Step 4: Evaluate the Current Population

With the experiment configured and the population expanded, you can evaluate the performance of all prompts in `gen-0`.

1.  **Gather your evaluation data.** You will need a directory of input files.
2.  **Run the `evaluate` command.**

    ```bash
    pcrit evaluate my-first-experiment \
      --generation 0 \
      --name "initial-cleanup-contest" \
      --inputs path/to/my/inputs/
    ```
    This command will run the Failter contest and place the resulting `report.csv` in `generations/gen-000/contests/initial-cleanup-contest/`.

### Step 5: Select the Survivors to Create the Next Generation

After evaluating, use the scores from the contest report to create a new generation comprised of only the best performers. This is the only command that creates a new generation folder. By default, it selects the top 5 prompts based on score.

```bash
pcrit select my-first-experiment --from-contest "initial-cleanup-contest"
```

This will read the report, pick the winners based on the selection policy (e.g., top 5), and create a new generation directory (`gen-001/`) containing only symlinks to the surviving prompts. The process then repeats from Step 3 (`vary`), but now operating on the new, more refined generation.

To change the selection policy, you can use the `--policy` flag. For example, to keep the top 10 prompts:
```bash
pcrit select my-first-experiment \
  --from-contest "initial-cleanup-contest" \
  --policy "top-N=10"
```

## Choosing your loop order

After `bootstrap` seeds **gen-000**, you have two common entry points into the cycle:

  * **evaluate → select → vary** (baseline-first). Run `evaluate` immediately on `gen-000` to capture how the hand-crafted prompts perform *unchanged*. Then run `select` to create `gen-001` with the top performers, and finally run `vary` on `gen-001` to generate new candidates for the next round.
  * **vary → evaluate → select** (explore-first). Skip the baseline. Run `vary` immediately on `gen-000` to mutate the seeds straight away. Then run `evaluate` on the expanded `gen-000` population and `select` the winners into `gen-001`. This is useful when your seeds are only rough sketches and you want to expand the search space quickly.

Both orders are valid. The commands `vary` and `evaluate` operate on the latest generation, while `select` reads from the latest generation to create the next one.
