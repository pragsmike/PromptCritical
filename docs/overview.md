# Overview

**PromptCritical** aims to be a reproducible **prompt-evolution platform**:
*store every prompt immutably → run controlled experiments → read the scores
back → breed the next generation*.

Our goal is to produce new prompts, trending upward in efficacy at causing an LLM
to perform a particular task.

Initially, we use a single task as our target: to clean up blog posts scraped from real web sites.
These texts are typically polluted by ads, subscription offers, teasers to related articles, and the like.
Those items are just noise that contribute nothing to the narrative of the post.
By removing them, we make the text more valuable as grist for semantic analysis.

We choose this task because it is bounded, it is easy to evaluate performance, and it is inherently useful
to other investigations.

## Prompt store

  The prompt store is the “source-of-truth” layer; everything else is plumbing
  that moves prompts *into* black-box evaluators (like **Failter**) and moves
  fitness scores *back* into the metadata.


  **The Immutable Prompt Store Design** treats prompts as content-addressable
  objects with SHA-1 integrity and full lineage tracking. This solves the
  reproducibility crisis in prompt engineering where people can't even remember
  what they tried last week. The `.prompt` file format with UTF-8 + NFC
  canonicalization shows serious attention to the subtle details that make or
  break data integrity.


## Git as Temporal Database

Using git for population snapshots is attractive because:
- Every generation is a commit with full diff history
- You can branch for experimental evolution strategies
- Merge conflicts become meaningful (competing evolutionary pressures)
- You get distributed replication of your entire evolutionary history for free

## Terms

- **Object Prompt**: The actual working prompt that performs the text transformation task
- **Meta Prompt**: The instruction that tells an LLM how to improve/mutate an object prompt
- **Seed Prompt**: The initial handwritten object prompt (generation 0)
- **Fitness Evaluation**: The Failter experiment that scores how well object prompts clean web pages

## Surrogate fitness metrics for cheaper evolution

PromptCritical, as an evolutionary system, computes fitness metrics:
   * about each prompt individually (eg complexity)
   * about the population of prompts as a whole (eg diversity)

The real test of a prompt is how well it causes an LLM to perform the task.
