# Population Bootstrapping in Detail

The goal of the initial phase of an experiment is to generate the first population of candidate prompts. We call this process "bootstrapping the population." This is accomplished with a single command: `pcrit bootstrap`.

This document details that initial step.

## Terminology

First, a quick review of the core concepts:
- **Object Prompt**: The actual working prompt that performs a task (e.g., cleaning web page content). These contain the `{{INPUT_TEXT}}` template variable and are the individuals in our population.
- **Meta Prompt**: An instruction that tells an LLM how to change an object prompt (e.g., "improve this prompt"). These contain the `{{OBJECT_PROMPT}}` template variable and are our evolutionary operators.
- **Seed Prompt**: The initial, hand-written prompts (both object and meta) that serve as the starting point for evolution.

## The Initial Sequence: `pcrit bootstrap`

The entire experiment begins with the `pcrit bootstrap` command. Its purpose is to ingest your hand-written seed files, create named links for them, and establish the initial, testable population: `gen-0`.

1.  **Create Seed Files**: You write your initial object prompts and meta-prompts as plain text files in the `seeds/` directory.

2.  **Create a Manifest**: You create a `bootstrap.edn` file that gives logical names to your seed files.

3.  **Run Bootstrap**:
    ```bash
    pcrit bootstrap my-experiment/
    ```

**Result**: The `bootstrap` command performs several actions in one go:
*   It populates the immutable prompt database (`pdb/`) with your prompts (as `P1`, `P2`, etc.).
*   It creates symbolic links in the `links/` directory (e.g., `links/seed`, `links/refine`) for easy access to key prompts.
*   It identifies all prompts you ingested that are of type `:object-prompt`.
*   It creates the `generations/gen-000/` directory and populates its `population/` subdirectory with links to those initial object-prompts.

At this point, you have `gen-0`, the first testable population. You are now ready to begin the main evolutionary loop.

### Next Steps: The `evaluate` → `select` → `vary` Loop

Once `gen-0` exists, you can begin the main cycle:
1.  Run `pcrit evaluate --generation 0 ...` to run the initial population in a contest and get fitness scores.
2.  Run `pcrit select ...` to use those scores to choose the survivors, which creates `gen-001`.
3.  Run `pcrit vary ...` on the `gen-001` population to breed new candidates, which creates `gen-002`.
4.  Repeat the cycle.

## Example Prompts for Bootstrapping

These are examples of the kinds of prompts you would place in your `seeds/` directory.

### Meta-Prompt Design

You'll need at least one type of meta-prompt to start:
-   **Incremental Improver**: `"Analyze the weaknesses of the following prompt and generate an improved, more robust version. {{OBJECT_PROMPT}}"`

You might also include others for more diversity:
-   **Variant Generator**: `"Generate 3 distinct alternative prompts that achieve the same core objective as the following prompt, but use different strategies or phrasing. {{OBJECT_PROMPT}}"`

### Potential Seed Object Prompt Structure

Here's a starting template for a seed prompt focused on cleaning web pages. Because it contains `{{INPUT_TEXT}}`, it will be identified as an `:object-prompt` and will be included in `gen-0`.
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
