### The Core Design Proposal: Generation-Specific Populations

The best place to indicate the active population is the directory for each generation. We create a specific subdirectory, `population/`, inside each `gen-NNN` directory.

*   **`generations/gen-NNN/population/`**: This directory will contain symbolic links to all the *active object-prompts* that constitute the population for generation `N`.

This design has several key advantages:
1.  **Explicitness:** It provides a single, clear location to see the exact members of any given generation's population.
2.  **Immutability:** The canonical prompts remain untouched in `pdb/`. The population list is just a set of pointers.
3.  **Separation of Concerns:** It naturally separates the *object-prompts* to be evaluated in a contest from the *meta-prompts* used for evolution.
4.  **Auditability:** The state of the population at every stage of the experiment's history is preserved on the filesystem.

---

### The `bootstrap` → `evaluate` → `select` → `vary` Workflow

Let's trace the lifecycle of an evolutionary cycle, starting from a fresh experiment.

#### State 1: After `pcrit bootstrap`

The user runs `pcrit bootstrap my-experiment`. This command ingests the seed prompts, creates top-level links, and **critically, creates `gen-0`**, populating it with any object-prompts found in the manifest. The experiment now has an initial population ready for evaluation.

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

#### State 2: User Runs `pcrit evaluate`

The user wants to evaluate the initial population from Generation 0. They provide the necessary data and a name for this specific contest run.

**User Command:**
```bash
pcrit evaluate my-experiment/ \
  --generation 0 \
  --name "initial-web-cleanup" \
  --inputs path/to/web-articles/
```

**Computation / Actions:**
1.  The `evaluate` command parses the arguments and resolves the path to `my-experiment/generations/gen-000/`.
2.  It creates a new, self-contained contest directory: `.../gen-000/contests/initial-web-cleanup/`.
3.  It creates the `failter-spec/` subdirectory and populates it by:
    *   Linking to the user's `--inputs` data.
    *   Reading the prompt IDs from `gen-000/population/`.
    *   Creating symlinks in `failter-spec/templates/` that point to the canonical prompts in the main `pdb/`.
4.  It shells out to `failter` to execute the contest.

#### State 3: After `pcrit evaluate` Completes

The `evaluate` command orchestrates the Failter run and captures its output.

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
The `contest-metadata.edn` file would contain:
```clojure
{:timestamp "2025-07-08T10:30:00Z"
 :generation 0
 :participants ["P1"]
 :inputs-path "/abs/path/to/web-articles/"
 :contest-name "initial-web-cleanup"}
```

#### State 4: After `pcrit select`

The user now runs the `select` command to winnow the population based on the contest results.

**User Command:**
```bash
pcrit select my-experiment/ --from-contest "initial-web-cleanup"
```

**Computation / Actions:**
1.  The `select` command reads `report.csv` from the specified contest.
2.  It applies a selection strategy (e.g., "keep the top 50%").
3.  It calls `pcrit.pop/create-new-generation!`, passing it the list of surviving prompt records. This creates a new generation directory (`gen-001`) containing links to only the survivors.

**Directory Structure After Selection:**
Assuming `P1` was the only prompt and it survived the selection:
```
my-experiment/
└── generations/
    ├── gen-000/
    │   └── ... (unchanged, historical record)
    └── gen-001/
        └── population/
            └── P1.prompt -> ../../../pdb/P1.prompt
```
The experiment now has a new generation, `gen-001`, containing the fittest members of the previous generation.

#### State 5: After `pcrit vary`

Now, the user runs `vary` to "breed" the surviving population, creating new candidates for the next round.

**User Command:**
```bash
pcrit vary my-experiment/
```
**Computation / Actions:**
1.  The `vary` command identifies the latest generation, `gen-001`.
2.  It applies meta-prompts (e.g., `refine` and `vary`) to the population members of `gen-001` (in this case, just `P1`), generating new object-prompts, say `P4` and `P5`.
3.  It creates the `gen-002` directory.
4.  It populates `gen-002/population/` with symlinks to all object-prompts in this new population: the survivors from the last step (`P1`) and their new offspring (`P4`, `P5`).

**Directory Structure:**
```
my-experiment/
└── generations/
    ├── gen-000/ ...
    ├── gen-001/ ... (historical record)
    └── gen-002/
        └── population/
            ├── P1.prompt -> ../../../pdb/P1.prompt
            ├── P4.prompt -> ../../../pdb/P4.prompt
            └── P5.prompt -> ../../../pdb/P5.prompt
```
This completes one full cycle. The experiment is now ready for the next `evaluate` command to be run on the `gen-002` population, continuing the evolutionary process.
