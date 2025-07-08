# PromptCritical Usage Guide

This guide explains how to use the `pcrit` command-line tool to set up, run, and manage prompt evolution experiments.

## Core Philosophy

PromptCritical treats prompt engineering as a scientific, evolutionary process. The goal is to move beyond manually tweaking prompts and instead use a data-driven loop:

1.  **Bootstrap**: Create an initial population of prompts from seed files.
2.  **Vary**: Produce new population members by applying meta-prompts (mutation, crossover) to existing ones.
3.  **Evaluate**: Run the prompts against a corpus of inputs using the **Failter** tool to generate performance scores.
4.  **Select**: Use the scores to select the fittest prompts to survive to the next generation.

Steps 2 through 4 are repeated in a cycle, with each iteration producing a new, more refined generation of prompts. This entire process is managed from the command line within a dedicated **Experiment Directory**.

## Core Concepts

### 1. The Experiment Directory

Everything related to a single experiment lives in one directory. This is the first argument you will pass to most `pcrit` commands. It contains the prompt database, generation data, and links to key prompts.

### 2. Prompt Types

Every prompt ingested into the database is automatically analyzed and assigned a `:prompt-type` in its metadata. This type defines its role in the system.

*   **:object-prompt**: This is a prompt that performs a task on an external input (e.g., summarizing a document). It is designed to be evaluated in a contest.
*   **:meta-prompt**: This is a prompt that operates on *another prompt*. It is used during the `vary` step to create new prompt variations (e.g., "Rephrase this prompt to be more concise: {{OBJECT_PROMPT}}").
*   **:static-prompt**: A prompt with no special template fields. It can be used for analysis but not directly in the evolution or contest loops.
*   **:invalid-mixed-type**: A prompt that incorrectly contains both special template fields. It cannot be used.

### 3. Template Variables

The prompt types are determined by the presence of special template variables. Your prompt files **must** use these specific names for the system to work correctly.

| Variable Name | Required For | Used By | Value Provided By |
| :--- | :--- | :--- | :--- |
| `{{INPUT_TEXT}}` | `:object-prompt` | `pcrit evaluate` | The content of a file from the `--inputs` directory during a Failter run. |
| `{{OBJECT_PROMPT}}` | `:meta-prompt` | `pcrit vary` | The body of another prompt being mutated or combined. |

## The Standard Workflow

Here is a step-by-step guide to running your first experiment.

### Step 1: Set Up and Bootstrap the Experiment

First, create your experiment directory and the necessary seed files.

1.  **Create a directory for your experiment:**
    ```bash
    mkdir my-first-experiment
    mkdir my-first-experiment/seeds
    ```

2.  **Create your raw prompt files** inside the `seeds/` directory. For example:
    *`seeds/seed-prompt.txt`*
    ```
    Please clean the following web page content, removing all ads, navigation, and comments. Preserve only the main article text.

    {{INPUT_TEXT}}
    ```
    *`seeds/refine-prompt.txt`*
    ```
    Rewrite the following prompt to be clearer and more direct:
    {{OBJECT_PROMPT}}
    ```

3.  **Create the bootstrap manifest file** in the root of your experiment directory. This file tells `pcrit` which files to ingest and what logical names to give them.

    *`my-first-experiment/bootstrap.edn`*
    ```clojure
    {:seed   "seeds/seed-prompt.txt"
     :refine "seeds/refine-prompt.txt"}
    ```

4.  **Run the `bootstrap` command:** This reads your manifest, creates the experiment's internal structure (`pdb/`, `links/`), and ingests the prompts, assigning them unique IDs (`P1`, `P2`, etc.).

    ```bash
    pcrit bootstrap my-first-experiment
    ```
    After this step, your directory will be initialized with your starting prompts.

### Step 2: Vary the Population

Next, you will create a new generation of prompts by applying your meta-prompts to the existing population.

```bash
# This is the future goal (v0.2)
pcrit vary my-first-experiment
```
This command will load the latest population, use meta-prompts like `refine` to create new candidates, and save the result as a new generation (e.g., in a `generations/gen-000/` directory).

### Step 3: Evaluate the Population

Now, you can run the prompts from a specific generation in a contest to see how they perform.

1.  **Gather your evaluation data.** You will need a directory of input files and, optionally, a directory of corresponding "ground truth" (perfectly edited) files.
2.  **Run the `evaluate` command.** You must tell it which generation to test, give the contest a name, and provide the paths to your data.

    ```bash
    # This is the future goal (v0.2)
    pcrit evaluate my-first-experiment \
      --generation 0 \
      --name "web-cleanup-v1" \
      --inputs path/to/my/inputs/ \
      --ground-truth path/to/my/ground_truth_files/ \
      --models-file path/to/my/models.txt
    ```
    This command will create a new contest subdirectory (e.g., `.../contests/web-cleanup-v1/`), prepare the files for the **Failter** tool, execute the Failter pipeline, and place the resulting `report.csv` in the contest directory.

### Step 4: Select the Survivors

After evaluating a generation, you can use the scores from the contest to "winnow" the population, creating a new generation comprised of only the best performers.

```bash
# This is the future goal (v0.2)
pcrit select my-first-experiment --from-contest "web-cleanup-v1"
```

This will create a new generation directory (e.g., `gen-001/`) containing links to only the surviving prompts. By repeating the `vary`, `evaluate`, and `select` steps, you iteratively improve your prompt population.
