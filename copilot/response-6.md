This is the next logical step in building the v0.2 milestone. I will detail the implementation plan for the `evaluate` command, including answers to your specific questions and the requested directory listings.

### Answering Your Questions

First, let's address your questions directly.

> Is there any reason `evaluate` wouldn't work on generation 0, as produced by `bootstrap`?

No, there is no reason it wouldn't work. In fact, our recent refactoring was designed specifically to enable this workflow. `bootstrap` now creates a well-formed `gen-0` populated with all the `:object-prompt`s from your seeds. The `evaluate` command is designed to work on any generation containing object-prompts, so it will work perfectly on `gen-0`.

> In other words, would `bootstrap` immediately followed by `evaluate` do something sensible?

Yes, absolutely. This is now the primary intended workflow for starting an experiment:
1.  `pcrit bootstrap my-experiment/`: Creates a testable `gen-0`.
2.  `pcrit evaluate my-experiment/ --inputs ...`: Runs the first contest on that initial population to establish a baseline performance score.

### Directory State: Before and After `evaluate`

Here is a visualization of the experiment directory immediately after `bootstrap` and then after running `evaluate` with default arguments.

Let's assume `bootstrap` was run on seeds that produced one object-prompt (`P1`) and one meta-prompt (`P2`).

#### Directory State: BEFORE `evaluate`

After `pcrit bootstrap my-experiment/` completes:

```
my-experiment/
├── links/
│   ├── seed -> ../pdb/P1.prompt
│   └── refine -> ../pdb/P2.prompt
├── pdb/
│   ├── P1.prompt
│   └── P2.prompt
└── generations/
    └── gen-000/
        ├── population/
        │   └── P1.prompt -> ../../../pdb/P1.prompt
        └── contests/  # Does not exist yet
```

#### Directory State: AFTER `evaluate`

After running `pcrit evaluate my-experiment/ --inputs path/to/my/inputs/`:

```
my-experiment/
├── links/
│   └── ... (unchanged)
├── pdb/
│   └── ... (unchanged)
└── generations/
    └── gen-000/
        ├── population/
        │   └── P1.prompt -> ../../../pdb/P1.prompt
        └── contests/
            └── contest/  # The default contest name
                ├── failter-spec/
                │   ├── inputs/     # Contains symlinks to your input data
                │   └── templates/  # Contains a symlink to P1.prompt
                ├── report.csv      # The captured output from the failter tool
                └── contest-metadata.edn # An audit file for this specific run
```

### Implementation Plan for `evaluate`

I will implement this by creating two new components to maintain separation of concerns, consistent with the Polylith architecture.

1.  **New Component: `pcrit.failter`**
    This component will be responsible for all direct interactions with the external `failter` tool. It will be a thin adapter that hides the details of shelling out and managing the `failter-spec` directory. This keeps the core command logic clean and makes testing much easier.
    *   **Interface:** It will expose a single function, `(run-contest! ctx {:keys [generation-number contest-name inputs-dir population]})`.
    *   **Logic:**
        1.  Uses `expdir` functions to create the required directory structure (`.../contests/<contest-name>/failter-spec/{inputs,templates}`).
        2.  Creates symlinks from `failter-spec/templates` to the prompt files in `pdb`.
        3.  Creates symlinks from `failter-spec/inputs` to the user-provided files in `inputs-dir`.
        4.  Uses `clojure.java.shell/sh` to execute the `failter` command sequence (`failter experiment`, `failter evaluate`, `failter report`).
        5.  Copies the final `report.csv` from Failter's output directory into our contest directory.
        6.  Creates `contest-metadata.edn` to record the parameters of the run.

2.  **New Namespace: `pcrit.command.evaluate`**
    This will contain the high-level orchestration logic for the `evaluate` command, but it will delegate the details to the `failter` component.
    *   **Interface:** `(evaluate! ctx {:keys [generation name inputs]})`.
    *   **Logic:**
        1.  Determine the target generation number. If the `generation` option is `nil`, it will call `expdir/find-latest-generation-number`.
        2.  Handle defaults: If `name` is `nil`, use `"contest"`.
        3.  **Validation:**
            *   Check that the target generation exists.
            *   Check that the specified `--inputs` directory exists.
            *   Check that the contest name is not already in use for that generation by checking for the existence of `.../contests/<name>`.
            *   Load the population using `pop/load-population` and fail if it's empty.
        4.  If validation passes, call `failter/run-contest!` with all the required parameters.
        5.  Log the success or failure of the operation.

This approach ensures the `evaluate!` command is easy to read and test. We can test its logic (default handling, validation) by providing a mock `failter/run-contest!` function, completely isolating the command's orchestration from the side effects of shelling out to an external tool.
