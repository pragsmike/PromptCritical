# Population Bootstrapping in Detail

The goal of the initial phase of an experiment is to generate the first population of candidate prompts. We call this process "bootstrapping the population." This is accomplished with two commands: `pcrit init` and `pcrit bootstrap`.

This document details that initial sequence.

## The Modern Workflow: `init` then `bootstrap`

The recommended way to start a new experiment is with the `pcrit init` command:

```bash
pcrit init my-new-experiment
```

This command creates a new directory (`my-new-experiment`) and populates it with a ready-to-use skeleton, including:
*   A `seeds/` directory containing example object and meta prompts.
*   A `bootstrap.edn` manifest file that points to those seed prompts.
*   A default `evolution-parameters.edn` configuration file.

Once `init` is complete, the `bootstrap` command can be run to ingest these files and create the first testable population.

---

## The `pcrit bootstrap` Command in Detail

The `pcrit bootstrap` command's purpose is to read the files from the experiment skeleton, ingest them into the prompt database, create named links, and establish the initial, testable population: `gen-0`.

1.  **Run Bootstrap**:
    ```bash
    # Run this after 'pcrit init <dir>'
    pcrit bootstrap my-new-experiment/
    ```

**Result**: The `bootstrap` command performs several actions in one go:
*   It populates the immutable prompt database (`pdb/`) with your prompts (as `P1`, `P2`, etc.), based on the contents of `bootstrap.edn`.
*   It creates symbolic links in the `links/` directory (e.g., `links/seed`, `links/improve`) for easy access to key prompts.
*   It identifies all prompts you ingested that are of type `:object-prompt`.
*   It creates the `generations/gen-000/` directory and populates its `population/` subdirectory with links to those initial object-prompts.

At this point, you have `gen-0`, the first testable population. You are now ready to begin the main evolutionary loop.

### Next Steps: The `evaluate` → `select` → `vary` Loop

Once `gen-0` exists, you can begin the main cycle:
1.  Run `pcrit evaluate --generation 0 ...` to run the initial population in a contest and get fitness scores.
2.  Run `pcrit select ...` to use those scores to choose the survivors, which creates `gen-001`.
3.  Run `pcrit vary ...` on the `gen-001` population to breed new candidates.
4.  Repeat the cycle.

## Reviewing the Scaffold Prompts

The `pcrit init` command generates the following kinds of prompts in your `seeds/` directory to get you started.

### Meta-Prompt Design

You'll need at least one type of meta-prompt to start:
-   **Incremental Improver**: `"Analyze the weaknesses of the following prompt and generate an improved, more robust version. {{OBJECT_PROMPT}}"`

You might also include others for more diversity:
-   **Variant Generator**: `"Generate 3 distinct alternative prompts that achieve the same core objective as the following prompt, but use different strategies or phrasing. {{OBJECT_PROMPT}}"`

### Seed Object Prompt Structure

Here's the starting template for a seed prompt focused on cleaning web pages. Because it contains `{{INPUT_TEXT}}`, it will be identified as an `:object-prompt` and will be included in `gen-0` by the `bootstrap` command.
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

Instructions: Process the following web page content and return only the cleaned version.

{{INPUT_TEXT}}
```
