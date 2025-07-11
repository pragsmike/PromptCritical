### The Core Design Proposal: Generation-Specific Populations

The best place to indicate the active population is the directory for each generation. We create a specific subdirectory, `population/`, inside each `gen-NNN` directory.

*   **`generations/gen-NNN/population/`**: This directory will contain symbolic links to all the *active object-prompts* that constitute the population for generation `N`.

This design has several key advantages:
1.  **Explicitness:** It provides a single, clear location to see the exact members of any given generation's population.
2.  **Immutability:** The canonical prompts remain untouched in `pdb/`. The population list is just a set of pointers. A generation folder becomes effectively immutable once the `select` command is run to create the *next* generation.
3.  **Separation of Concerns:** It naturally separates the *object-prompts* to be evaluated in a contest from the *meta-prompts* used for evolution.
4.  **Auditability:** The state of the population at every stage of the experiment's history is preserved on the filesystem.

---

### The `bootstrap` → `vary` → `evaluate` → `select` Workflow

Let's trace the lifecycle of an evolutionary cycle, starting from a fresh experiment.

#### State 1: After `pcrit bootstrap`

The user runs `pcrit bootstrap my-experiment`. This command ingests the seed prompts, creates top-level links, and **critically, creates `gen-0`**, populating it with any object-prompts found in the manifest. The experiment now has an initial population ready for evolution.

**Directory Structure:**
```
my-experiment/
├── links/
│   ├── refine -> ../pdb/P2.prompt
│   ├── seed -> ../pdb/P1.prompt
│   └── vary -> ../pdb/P3.prompt
├── pdb/
│   ├── P1.prompt   ; The seed :object-prompt
│   ├── P2.prompt   ; A :meta-prompt
│   └── P3.prompt   ; Another :meta-prompt
│   └── pdb.counter ; Contains "3"
├── seeds/
│   ├── refine.txt
│   ├── seed.txt
│   └── vary.txt
├── bootstrap.edn
└── generations/
    └── gen-000/
        └── population/
            └── P1.prompt -> ../../../pdb/P1.prompt
```

#### State 2: After `pcrit vary`

The user runs `vary` to "breed" the initial population, creating new candidates. This command **mutates the current generation in-place**.

**User Command:**
```bash
pcrit vary my-experiment/
```
**Computation / Actions:**
1.  The `vary` command identifies the latest generation, `gen-000`.
2.  It applies meta-prompts to the population members of `gen-000`, generating new object-prompts, say `P4` and `P5`.
3.  It adds symlinks for the new offspring (`P4`, `P5`) into the **existing `gen-000/population/` directory**.

**Directory Structure:**
```
my-experiment/
└── generations/
    └── gen-000/
        └── population/
            ├── P1.prompt -> ../../../pdb/P1.prompt
            ├── P4.prompt -> ../../../pdb/P4.prompt
            └── P5.prompt -> ../../../pdb/P5.prompt
```

#### State 3: After `pcrit evaluate`

The user evaluates the now-expanded population of Generation 0.

**User Command:**
```bash
pcrit evaluate my-experiment/ \
  --generation 0 \
  --name "initial-web-cleanup" \
  --inputs path/to/web-articles/
```

**Computation / Actions:**
1.  The `evaluate` command resolves the path to `my-experiment/generations/gen-000/`.
2.  It creates a contest directory: `.../gen-000/contests/initial-web-cleanup/`.
3.  It creates the `failter-spec/` subdirectory and populates it with links to the three prompts currently in `gen-000/population/`.
4.  It shells out to `failter` to execute the contest and captures the `report.csv`.

**Final Directory Structure of the Contest:**
```
my-experiment/
└── generations/
    └── gen-000/
        ├── population/
        │   └── ... (unchanged)
        └── contests/
            └── initial-web-cleanup/
                ├── failter-spec/
                │   └── ... (as prepared above)
                ├── report.csv          ; <-- Raw output from Failter
                └── contest-metadata.edn   ; <-- Auditable record of the pcrit run
```

#### State 4: After `pcrit select`

The user now runs the `select` command to winnow the population. This is the command that **creates the next generation**.

**User Command:**
```bash
pcrit select my-experiment/ --from-contest "initial-web-cleanup"
```

**Computation / Actions:**
1.  The `select` command reads `report.csv` from the specified contest.
2.  It applies a selection strategy (e.g., "keep `P1` and `P4`").
3.  It calls `pcrit.pop/create-new-generation!`, passing it the list of surviving prompt records. This creates a new generation directory (`gen-001`) containing links to only the survivors.

**Directory Structure After Selection:**
```my-experiment/
└── generations/
    ├── gen-000/
    │   └── ... (unchanged, now a historical record)
    └── gen-001/
        └── population/
            ├── P1.prompt -> ../../../pdb/P1.prompt
            └── P4.prompt -> ../../../pdb/P4.prompt
```
This completes one full cycle. The experiment now has a new generation, `gen-001`, containing the fittest members of the previous generation, ready for the next `vary` or `evaluate` command.
