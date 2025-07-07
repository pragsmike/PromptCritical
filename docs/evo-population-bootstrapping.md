# Population Bootstrapping in Detail

The goal of the initial phase of an experiment is to generate the first population of candidate prompts. We call this process "bootstrapping the population." This is accomplished not with a single command, but with two distinct steps: `pcrit bootstrap` followed by the first run of `pcrit vary`.

This document details that initial sequence.

## Terminology

First, a quick review of the core concepts:
- **Object Prompt**: The actual working prompt that performs a task (e.g., cleaning web page content). These are the individuals in our population.
- **Meta Prompt**: An instruction that tells an LLM how to change an object prompt (e.g., "improve this prompt" or "generate three variants"). These are our evolutionary operators.
- **Seed Prompt**: The initial, hand-written object prompt that serves as the starting point for evolution.

## The Initial Sequence

### Step 1: `pcrit bootstrap` — Ingesting the Raw Materials

The process begins with the `bootstrap` command. The sole purpose of this command is to ingest your hand-written seed files into the immutable prompt database (`pdb`) and create named links for them.

1.  **Create Seed Files**: You write your initial object prompt and a few meta-prompts as plain text files in the `seeds/` directory.
2.  **Create a Manifest**: You create a `bootstrap.edn` file that gives logical names to your seed files.
3.  **Run Bootstrap**:
    ```bash
    pcrit bootstrap my-experiment/
    ```
**Result**: The `bootstrap` command populates the `pdb/` with your prompts (as `P1`, `P2`, etc.) and creates symlinks like `links/seed` and `links/refine`. **It does not create any generations.** At this point, you simply have the raw materials for evolution stored in the database.

### Step 2: `pcrit vary` — Creating Generation 0

With the seeds in place, you run the `vary` command for the first time. This is the "breeding" step that creates the very first population, `gen-000`.

1.  **Run Vary**:
    ```bash
    # This is a future goal (v0.2)
    pcrit vary my-experiment/
    ```
2.  **The Process**:
    *   The `vary` command will load the initial `seed` object prompt (`P1`).
    *   It will then apply the meta-prompts (e.g., `refine` and `vary`) to the seed prompt. For example:
        *   Applying an "improve this prompt" meta-prompt might generate a new prompt, `P4`.
        *   Applying a "generate 3 variants" meta-prompt might generate `P5`, `P6`, and `P7`.
    *   It creates a new generation directory, `generations/gen-000/`.
    *   It calls `pop/create-new-generation!`, providing the full list of prompts for this generation: the original seed plus all its new offspring (`P1`, `P4`, `P5`, `P6`, `P7`). This populates the `.../gen-000/population/` directory with links to these prompts.

**Result**: You now have `gen-000`, the first testable population. There have been no evaluations yet, but you have a diverse set of candidates ready for a contest.

### Next Steps: `evaluate` and `select`

Once `gen-000` exists, you can begin the main evolutionary loop:
1.  Run `pcrit evaluate --generation 0 ...` to run the population in a contest and get fitness scores.
2.  Run `pcrit select ...` to use those scores to choose the survivors, creating `gen-001`.
3.  Run `pcrit vary ...` again on the `gen-001` population to continue the cycle.

## Example Prompts for Bootstrapping

These are examples of the kinds of prompts you would place in your `seeds/` directory.

### Meta-Prompt Design

You'll need at least two types of meta-prompts to start:
-   **Incremental Improver**: `"Analyze the weaknesses of the following prompt and generate an improved, more robust version. {{OBJECT_PROMPT}}"`
-   **Variant Generator**: `"Generate 3 distinct alternative prompts that achieve the same core objective as the following prompt, but use different strategies or phrasing. {{OBJECT_PROMPT}}"`

### Potential Seed Object Prompt Structure

Here's a starting template for a seed prompt focused on cleaning web pages.
```
Task: Clean junk content from scraped web pages while preserving the main article text.

Remove:
- Advertisement blocks
- Social media sharing buttons
- "Subscribe to newsletter" prompts
- "Related articles" teasers
- Navigation menus
- Comment sections
- Cookie consent banners

Preserve:
- Main article title and body text
- Author information
- Publication date
- Relevant images with captions

Example Input:
[messy web page content]

Example Output:
[cleaned version]

Instructions: Process the following web page content and return only the cleaned version.

{{INPUT_TEXT}}
```
