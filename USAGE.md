# PromptCritical Usage Guide

This guide explains how to use the `pcrit` command-line tool to set up, run, and manage prompt evolution experiments.

## Core Philosophy

PromptCritical treats prompt engineering as a scientific, evolutionary process. The goal is to move beyond manually tweaking prompts and instead use a data-driven loop:

1.  **Bootstrap**: Create an initial population of prompts from seed files.
2.  **Contest**: Run the prompts against a corpus of inputs using the **Failter** tool to generate performance scores.
3.  **Record**: Ingest the scores from the contest.
4.  **Evolve**: Use the scores to select the fittest prompts and create a new generation through mutation and crossover.

This entire process is managed from the command line within a dedicated **Experiment Directory**.

## Core Concepts

### 1. The Experiment Directory

Everything related to a single experiment lives in one directory. This is the first argument you will pass to most `pcrit` commands. It contains the prompt database, generation data, and links to key prompts.

### 2. Prompt Types

Every prompt ingested into the database is automatically analyzed and assigned a `:prompt-type` in its metadata. This type defines its role in the system.

*   **:object-prompt**: This is a prompt that performs a task on an external input (e.g., summarizing a document). It is designed to be evaluated in a contest.
*   **:meta-prompt**: This is a prompt that operates on *another prompt*. It is used during the `evolve` step to create new prompt variations (e.g., "Rephrase this prompt to be more concise: {{OBJECT_PROMPT}}").
*   **:static-prompt**: A prompt with no special template fields. It can be used for analysis but not directly in the evolution or contest loops.
*   **:invalid-mixed-type**: A prompt that incorrectly contains both special template fields. It cannot be used.

### 3. Template Variables

The prompt types are determined by the presence of special template variables. Your prompt files **must** use these specific names for the system to work correctly.

| Variable Name | Required For | Used By | Value Provided By |
| :--- | :--- | :--- | :--- |
| `{{INPUT_TEXT}}` | `:object-prompt` | `pcrit contest` | The content of a file from the `--inputs` directory during a Failter run. |
| `{{OBJECT_PROMPT}}` | `:meta-prompt` | `pcrit evolve` | The body of another prompt being mutated or combined. |

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

4.  **Run the `bootstrap` command:** This reads your manifest, creates the experiment's internal structure (`pdb/`, `links/`), ingests the prompts, and assigns them unique IDs (`P1`, `P2`, etc.).

    ```bash
    pcrit bootstrap my-first-experiment
    ```

    After this step, your directory will be fully initialized and ready for a contest. You will find symbolic links like `my-first-experiment/links/seed` pointing to the ingested prompt file in the `pdb/` directory.

### Step 2: Run a Contest

Now, you can test your object-prompts against a corpus of inputs.

1.  **Gather your data.** You will need a directory of input files, and optionally, a directory of corresponding "ground truth" (perfectly edited) files.
2.  **Run the `contest` command.** You must tell it which prompts to include, where to find the input data, and which models to use.

    ```bash
    pcrit contest my-first-experiment \
      --prompts P1,P3,P5 \
      --inputs path/to/my/inputs/ \
      --ground-truth path/to/my/ground_truth_files/ \
      --models-file path/to/my/models.txt
    ```
    This command will:
    *   Create a new contest subdirectory inside `generations/gen-000/contests/`.
    *   Prepare a `failter-spec` directory correctly formatted for the Failter tool.
    *   Execute the `failter` command-line pipeline.
    *   Place the resulting `report.csv` in the contest directory.

### Step 3: Evolve Your Population (Future)

After running one or more contests, you will run the `evolve` command, which uses the scores from the contests to breed a new generation of prompts using your meta-prompts.

```bash
# This is the future goal
pcrit evolve my-first-experiment
```

This will create a `gen-001` directory containing new prompts ready for the next round of contests. By repeating the `contest` and `evolve` steps, you iteratively improve your prompt population.
