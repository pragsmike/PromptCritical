# Evolution Process

PromptCritical optimizes prompts for a particular task by carrying out an evolutionary process to explore a prompt fitness landscape.

## Evolution Cycle Theory

Each "generation" has as its population a set of prompts. A typical cycle involves mutating the population, evaluating it, and then selecting the winners to form the *next* generation.

The core functions operating on a population are:
*   `vary`: population → population (adds new members to the current population)
*   `evaluate`: population → scores (runs a contest and records scores)
*   `select`: population × scores → population (creates the next generation from the winners)

A user composes these steps to create an `evolve` endofunction that takes one generation's population and produces the next. The key change is that a generation's directory is a mutable workspace *until* the `select` command runs, at which point a new, immutable generation folder is created.

## Evolution Experiment

The unit of work is the **experiment**. It consists of a number of generations, each representing a snapshot of the evolving population *after a selection event*.

*   The `bootstrap` command creates `gen-000`.
*   The `vary` and `evaluate` commands operate **inside** the latest generation's directory, mutating its state by adding new prompts or contest results.
*   The `select` command reads the results from the current generation, picks the winners, and creates the **next generation folder** (`gen-001`), which contains the new starting population.

The experiment process runs for several cycles, each culminating in a `select` command that mints a new generation. The entire state of the experiment resides within a single **experiment directory**.

> **Note on Terminology:** The `failter` tool also uses the term "experiment" for its evaluation runs. To avoid confusion, in PromptCritical we call a single `failter` run a **contest**.

### Experiment Directory Structure

The specification and current state of an experiment reside under its root directory. It contains:
*   `seeds/`: The raw text files for the initial seed and meta-prompts.
*   `links/`: Symlinks to notable prompts in the database (e.g., `seed`, `refine`).
*   `pdb/`: The immutable prompt database where all prompt artifacts are stored.
*   `generations/`: Contains a subdirectory for each generation (`gen-000`, `gen-001`, etc.).
*   `bootstrap.edn`: The manifest file for the `bootstrap` command.
*   `evolution-parameters.edn`: **(Implemented)** Optional file for controlling the evolution algorithms.

The directory structure for a contest is designed for clear auditing and integration with Failter:
```
<experiment-root>/
├── seeds/
│   └── (Raw .txt files for initial prompts)
├── links/
│   └── (Symbolic links to key prompts like 'seed', 'refine')
├── pdb/
│   └── (The central, immutable store for all prompt artifacts)
├── generations/
│   └── gen-000/
│       ├── population/
│       │   ├── P001.prompt -> ../../../pdb/P001.prompt
│       │   └── ...
│       └── contests/
│           └── 2025-07-08-web-cleanup/
│               ├── failter-spec/
│               │   ├── inputs/
│               │   ├── templates/
│               │   └── ...
│               ├── report.csv
│               └── contest-metadata.edn
├── bootstrap.edn
└── evolution-parameters.edn
```

### Key Directory Concepts

*   **`generations/gen-NNN/population/`**: This directory defines the active set of object-prompts for a given generation via symbolic links into the `pdb/`. This directory is mutable until the `select` command is run for this generation.
*   **`generations/gen-NNN/contests/<contest-name>/`**: This is the self-contained record of a single evaluation run (a contest).
*   **`.../failter-spec/`**: This subdirectory is prepared specifically for the Failter tool, with its `inputs/`, `templates/`, etc., populated with symlinks.
*   **`.../report.csv`**: The raw output from a Failter run is stored here, providing an immutable record of performance. The `select` command uses this file as its input.

### The Evolutionary Steps in Practice

An experiment begins with a one-time `bootstrap` command, followed by a cycle of `vary`, `evaluate`, and `select`.

1.  **`bootstrap`**: This command reads the `bootstrap.edn` manifest, ingests the raw prompts from the `seeds/` directory into the immutable `pdb/`, creates named links, and **populates `gen-0`** with the initial object-prompts. This creates the first testable population.

2.  **`vary`**: This is the "breeding" step. It loads the population from the latest generation, applies meta-prompts to create new prompt variations (offspring), and then **adds symlinks for these new offspring to the population directory of the *current* generation**. You can run `vary` multiple times to add more candidates before an evaluation.

3.  **`evaluate`**: This is the "testing" step. The `pcrit evaluate` command targets a specific generation's population, packages it into a `failter-spec` directory, and runs a **contest**. The resulting scores are saved to a `report.csv` file within that generation's `contests/` subdirectory.

4.  **`select`**: This is the "winnowing" and "generation-creating" step. The `pcrit select` command reads a `report.csv` file from a contest, applies a selection strategy, and creates a **new generation folder** (e.g., `gen-001`) containing only the symlinks to the surviving prompts. This action "freezes" the previous generation as a complete, immutable historical record.
