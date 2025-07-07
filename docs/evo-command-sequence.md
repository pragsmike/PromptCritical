### The Core Design Proposal: Generation-Specific Populations

The best place to indicate the active population is the directory for each generation.
We create a specific subdirectory, `population/`, inside each `gen-NNN` directory.

*   **`generations/gen-NNN/population/`**: This directory will contain symbolic links to all the *active object-prompts* that constitute the population for generation `N`.

This design has several key advantages:
1.  **Explicitness:** It provides a single, clear location to see the exact members of any given generation's population.
2.  **Immutability:** The canonical prompts remain untouched in `pdb/`. The population list is just a set of pointers.
3.  **Separation of Concerns:** It naturally separates the *object-prompts* to be evaluated in a contest from the *meta-prompts* used for evolution, as only the former would be linked into the `population/` directory.
4.  **Auditability:** The state of the population at every stage of the experiment's history is preserved on the filesystem.

---

### The Step-by-Step Workflow: From Bootstrap to Evaluate

Let's trace the lifecycle of an experiment, starting from a bootstrapped state and proceeding through the first contest.

#### State 1: After `pcrit bootstrap`

The user has run `pcrit bootstrap my-experiment`. This command, as implemented, ingests the seed prompts and creates the top-level links. **Crucially, no generations have been created yet.** The `generations/` directory is empty.

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
└── generations/    ; Exists, but is empty
```

#### State 2: After a (Future) `pcrit vary`

To have a population to test, we must first run an evolution step. Let's assume a future `pcrit evolve` command is run. Its job is to create the next generation. For the very first run, it would:

1.  Identify the initial population (just the `seed` prompt, `P1`).
2.  Apply the `refine` (`P2`) and `vary` (`P3`) meta-prompts to `P1`, generating new object-prompts, say `P4` and `P5`.
3.  Create the `gen-000` directory to represent this new generation.
4.  Create the `gen-000/population/` directory and populate it with symlinks to *all* the object-prompts that are part of this generation: the original seed and its new offspring.

**Directory Structure:**
```
my-experiment/
├── links/
│   └── ... (unchanged)
├── pdb/
│   ├── P1.prompt
│   ├── P2.prompt
│   ├── P3.prompt
│   ├── P4.prompt   ; Offspring of P1 + P2
│   └── P5.prompt   ; Offspring of P1 + P3
│   └── pdb.counter ; Contains "5"
├── seeds/
│   └── ... (unchanged)
├── bootstrap.edn
└── generations/
    └── gen-000/
        └── population/
            ├── P1.prompt -> ../../../pdb/P1.prompt
            ├── P4.prompt -> ../../../pdb/P4.prompt
            └── P5.prompt -> ../../../pdb/P5.prompt
```
Now we have an "active population" for Generation 0 and are ready to run a contest.

#### State 3: User Runs `pcrit evaluate`

The user wants to evaluate the population of Generation 0. They provide the necessary data and a name for this specific contest run.

**User Command:**
```bash
pcrit contest my-experiment/ \
  --generation 0 \
  --name "initial-web-cleanup" \
  --inputs path/to/web-articles/ \
  --ground-truth path/to/cleaned-articles/ \
  --models-file path/to/models.txt
```

**Computation / Actions:**
1.  The `contest` command parses the arguments. It resolves the generation to the path `my-experiment/generations/gen-000/`.
2.  It creates a new, self-contained directory for this contest run: `my-experiment/generations/gen-000/contests/initial-web-cleanup/`.
3.  It creates the `failter-spec/` subdirectory inside the new contest directory.
4.  **Prepare `failter-spec`:**
    *   It creates symlinks inside `failter-spec/inputs/` pointing to the user's data files.
    *   It does the same for `failter-spec/ground_truth/`.
    *   It copies the user's `models.txt` file into `failter-spec/`.
    *   It reads the contents of `my-experiment/generations/gen-000/population/`.
    *   For each symlink found there (`P1.prompt`, `P4.prompt`, `P5.prompt`), it creates a *new* symlink inside `failter-spec/templates/`, pointing to the canonical prompt in the main `pdb/`.

**Directory Structure (During Contest):**
```
my-experiment/
└── generations/
    └── gen-000/
        ├── population/
        │   └── ... (unchanged)
        └── contests/
            └── initial-web-cleanup/
                └── failter-spec/
                    ├── inputs/
                    │   └── article1.md -> /abs/path/to/web-articles/article1.md
                    ├── ground_truth/
                    │   └── article1.md -> /abs/path/to/cleaned-articles/article1.md
                    ├── templates/
                    │   ├── P1.prompt -> ../../../../../pdb/P1.prompt
                    │   ├── P4.prompt -> ../../../../../pdb/P4.prompt
                    │   └── P5.prompt -> ../../../../../pdb/P5.prompt
                    └── model-names.txt
```

#### State 4: After `pcrit evaluate` Completes

The `contest` command now shells out to `failter`.

**Computation / Actions:**
1.  `failter` runs using the prepared `failter-spec/` directory.
2.  `failter` produces its output, which the `pcrit contest` command ensures is placed into the contest directory as `results.csv`.
3.  The `pcrit contest` command also writes its own metadata file, `contest-metadata.edn`, to the contest directory, recording the parameters of the run for perfect auditability.

**Final Directory Structure:**
```
my-experiment/
└── generations/
    └── gen-000/
        ├── population/
        │   └── ... (unchanged)
        └── contests/
            └── initial-web-cleanup/
                ├── failter-spec/
                │   └── ... (as above)
                ├── results.csv          ; <-- Output from Failter
                └── contest-metadata.edn   ; <-- Record of the pcrit run
```
The `contest-metadata.edn` file would contain:
```clojure
{:timestamp "2025-07-06T10:30:00Z"
 :generation 0
 :participants ["P1" "P4" "P5"]
 :inputs-path "/abs/path/to/web-articles/"
 :contest-name "initial-web-cleanup"}
```

This completes the `contest` step. The next command, `select`, would then read `results.csv` and update the experiment's history, likely by adding fitness scores to the metadata of the prompt files in `pdb/`.
