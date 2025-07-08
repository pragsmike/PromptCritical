# Overview

**PromptCritical** is a reproducible **prompt-evolution platform**. It formalizes the ad-hoc process of prompt engineering into a clear, data-driven, and auditable cycle: *`bootstrap` → `vary` → `evaluate` → `select`*.

Our goal is to produce new prompts that demonstrate increasing efficacy at causing a Large Language Model (LLM) to perform a particular task.

Initially, we use a single task as our target: cleaning up blog posts scraped from the web. These texts are often polluted by ads, subscription offers, and related-article teasers. By removing this noise, we make the text more valuable as grist for downstream semantic analysis. We chose this task because it is bounded, its performance is easily evaluated, and it is inherently useful.

## Two-Layer Integrity and Provenance

The project's design for data integrity and historical provenance operates at two complementary levels: the immutable prompt store and the Git repository that contains it.

### 1. The Immutable Prompt Store

The prompt store (`pdb/`) is the "source-of-truth" layer for all prompt artifacts. It is designed for fine-grained, programmatic integrity.

*   **Content-Addressed Hashing:** The `pdb/` treats prompts as content-addressable objects. The SHA-1 hash of a prompt's body is stored in its header, ensuring the prompt text itself cannot be corrupted or altered without detection. This is the primary defense against processing errors.
*   **Immutable History:** The results of each evaluation **contest** are stored in their own files within the `generations/` directory, creating a separate, auditable record of performance in a specific context. This prevents prompt metadata from changing based on external evaluations.

### 2. Git as a Temporal Database

While the `pdb/` ensures the integrity of individual prompts, the Git repository provides a higher-level, cryptographic snapshot of the entire experiment's state over time. By committing the state of the experiment directory after key events (e.g., after each `select` command), Git provides:

*   **Tamper-Evident History:** Git's commit hashes create a cryptographically secure chain. Any retroactive, illicit change to a file in a past generation (e.g., altering a `results.csv` file to favor a different prompt) would be immediately detectable.
*   **Full State Snapshots:** Each commit captures the exact state of every file, including prompt headers, contest results, and population symlinks. This allows you to check out any past generation and see exactly what the state of the experiment was at that point in time.
*   **Branching for Experiments:** You can use `git branch` to explore alternative evolutionary paths (e.g., trying a different selection strategy) in parallel, with the ability to merge successful strategies back into the main line.

Together, the `pdb`'s internal hashing and Git's external snapshotting provide a robust, two-layer system for ensuring the reproducibility and integrity of an entire evolutionary run.

## Terms

-   **Object Prompt**: The actual working prompt that performs a task (e.g., "clean this text").
-   **Meta Prompt**: An instruction that tells an LLM how to change an object prompt (e.g., "improve this prompt"). These are the operators for the `vary` command.
-   **Seed Prompt**: The initial, hand-written object prompt that serves as the starting point for evolution.
-   **Evaluation & Contest**: The `evaluate` command is the user-facing action that runs a **contest**. A contest is the specific event of scoring a population against a dataset using the Failter tool.

## Surrogate Fitness Metrics for Cheaper Evolution

The true test of a prompt is its performance in an expensive `evaluate` step. To reduce costs, PromptCritical is designed to eventually leverage cheaper "surrogate" fitness metrics.

The system analyzes prompts for intrinsic properties (e.g., complexity, word count) and records these in their metadata. The long-term vision is to build a dataset correlating these cheap-to-compute intrinsic metrics with expensive-to-compute performance scores. This will allow us to train a predictive model (a "surrogate critic") that can pre-filter a population, eliminating likely bad performers *before* spending money on full LLM evaluations. This will make the evolutionary process dramatically more efficient.
