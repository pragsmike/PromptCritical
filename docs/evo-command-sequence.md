### The Implemented Design: Generation-Specific Populations

The active population for any stage of the evolution is explicitly defined by the directory for that generation. A specific subdirectory, `population/`, inside each `gen-NNN` directory holds the active members.

*   **`generations/gen-NNN/population/`**: This directory contains symbolic links to all the *active object-prompts* that constitute the population for generation `N`.

This design has several key advantages:
1.  **Explicitness:** It provides a single, clear location to see the exact members of any given generation's population.
2.  **Immutability:** The canonical prompts remain untouched in `pdb/`. The population list is just a set of pointers. A generation folder becomes effectively immutable once the `select` command is run to create the *next* generation.
3.  **Separation of Concerns:** It naturally separates the *object-prompts* to be evaluated in a contest from the *meta-prompts* used for evolution.
4.  **Auditability:** The state of the population at every stage of the experiment's history is preserved on the filesystem.

---

### The `init` → `bootstrap` → `vary` → `evaluate` → `select` Workflow

Here is a trace of the lifecycle of an evolutionary cycle, starting from a fresh experiment.

#### State 0: After `pcrit init`

The user first runs `pcrit init my-experiment`. This command creates the `my-experiment` directory and populates it with a standard skeleton, including a `seeds/` directory with example prompts and a `bootstrap.edn` manifest file ready for the next step.

#### State 1: After `pcrit bootstrap`

The user runs `pcrit bootstrap my-experiment`. This command ingests the seed prompts created by `init`, creates top-level links, and creates `gen-0`, populating it with any object-prompts found in the manifest. The experiment now has an initial population ready for evolution.

**Directory Structure:**
```
my-experiment/
├── links/
│   ├── improve -> ../pdb/P2.prompt
│   ├── crossover -> ../pdb/P3.prompt
│   └── seed -> ../pdb/P1.prompt
├── pdb/
│   ├── P1.prompt   ; The seed :object-prompt
│   ├── P2.prompt   ; A :meta-prompt (:refine)
│   ├── P3.prompt   ; A :meta-prompt (:crossover)
│   └── pdb.counter ; Contains "3"
├── seeds/
│   ├── improve-meta-prompt.txt
│   ├── crossover-meta-prompt.txt
│   └── seed-object-prompt.txt
├── bootstrap.edn
└── generations/
    └── gen-000/
        └── population/
            └── P1.prompt -> ../../../pdb/P1.prompt
```

#### State 2: After `pcrit vary`

The user runs `vary` to "breed" the population, creating new candidates. This command **mutates the current generation in-place** and its behavior depends on the `:strategy` configured in `evolution-parameters.edn`.

**User Command:** `pcrit vary my-experiment/`

**Behavior 1: `:refine` strategy (default)**
1.  The `vary` command identifies the latest generation, `gen-000`.
2.  It applies the `refine` meta-prompt to each member of the `gen-000` population, generating new object-prompts, say `P4` and `P5`.
3.  It adds symlinks for the new offspring (`P4`, `P5`) into the **existing `gen-000/population/` directory**.

**Directory Structure after `:refine`:**
```
my-experiment/
└── generations/
    └── gen-000/
        └── population/
            ├── P1.prompt -> ../../../pdb/P1.prompt
            ├── P4.prompt -> ../../../pdb/P4.prompt
            └── P5.prompt -> ../../../pdb/P5.prompt
```

**Behavior 2: `:crossover` strategy (on `gen-1`)**
Let's assume we've already run `evaluate` and `select` on `gen-0`, and now we are at `gen-1`.
1.  The `vary` command identifies the latest generation, `gen-1`.
2.  It looks for contest reports inside the *previous* generation's directory (`gen-0/contests/`).
3.  It finds the top two performing prompts from the latest report (e.g., `P1` and `P3`).
4.  It applies the `crossover` meta-prompt to these two parents, generating a single new offspring, `P6`.
5.  It adds a symlink for the new offspring (`P6`) into the **current `gen-1/population/` directory**.

#### State 3: After `pcrit evaluate`

The user evaluates the now-expanded population of the current generation.

**User Command:** `pcrit evaluate my-experiment/ --name "my-contest" --inputs ...`

**Actions:**
1.  The `evaluate` command resolves the path to the latest generation directory.
2.  It creates a contest directory: `.../gen-N/contests/my-contest/`.
3.  It generates a `spec.yml` file in that directory, defining the contest for Failter.
4.  It shells out to `failter run --spec ...`.
5.  It captures the JSON output from Failter and processes it into the final `report.csv`.

**Final Directory Structure of the Contest:**
```my-experiment/
└── generations/
    └── gen-N/
        ├── population/
        │   └── ... (unchanged)
        └── contests/
            └── my-contest/
                ├── spec.yml                  ; <-- Declarative spec for Failter
                ├── failter-artifacts/        ; <-- Idempotent state dir for Failter
                │   └── ...
                ├── failter-report.json       ; <-- Raw JSON output from Failter
                ├── report.csv                ; <-- Final, cost-augmented report
                └── contest-metadata.edn      ; <-- Auditable record of the pcrit run
```

#### State 4: After `pcrit select`

The user now runs the `select` command to winnow the population. This is the command that **creates the next generation**.

**User Command:** `pcrit select my-experiment/ --from-contest "my-contest"`

**Actions:**
1.  The `select` command reads `report.csv` from the specified contest.
2.  It applies a selection policy (e.g., `top-N=5` or `tournament-k=2`), picking survivors.
3.  It creates a new generation directory (`gen-N+1`) containing links to only the survivors.

**Directory Structure After Selection:**
```
my-experiment/
└── generations/
    ├── gen-N/
    │   └── ... (unchanged, now a historical record)
    └── gen-N+1/
        └── population/
            ├── P1.prompt -> ../../../pdb/P1.prompt
            └── P4.prompt -> ../../../pdb/P4.prompt
```
This completes one full cycle. The experiment now has a new generation, ready for the next `vary` or `evaluate` command.
