### **To My Successor: Onboarding for PromptCritical v0.3 Development**

**File:** `copilot/onboard-9.md`

Welcome to the PromptCritical project. This document supersedes `onboard-8.md`. Our primary objective during the last session was the completion of the `v0.2` milestone: a fully functional `bootstrap → vary → evaluate → select` evolutionary loop. We have successfully completed this work, and the system is now capable of running a full, end-to-end experiment.

Your objective is to begin the `v0.3` milestone, which focuses on automating the core loop and improving the system's architecture.

#### 1. Prime Directives and Rules of Collaboration

These principles are not suggestions; they are the foundation of our workflow. Adhering to them is how we maintain velocity and prevent regressions.

*   **Read the Manual. Then Read It Again.** An external tool's documentation is a strict contract. Our biggest early challenge was misinterpreting the `failter` CLI guide. The simplest code that directly implements the documented behavior is always the right code.
*   **Never Assume API Behavior; Verify Everything.** We encountered bugs by making incorrect assumptions about `clojure-csv/parse-csv` and the data types returned by the `clj-yaml` library. Before using a function, especially one that crosses a boundary (like file I/O or parsing), state your assumption and write a small test to verify it.
*   **Test Isolation is Paramount.** Each distinct, stateful test case **must** be in its own `deftest` block. We proved that state leakage between `testing` blocks within a single `deftest` caused a cascade of failures that were difficult to diagnose. We fixed this in `select_test.clj`, and that file should now be considered the template for future tests.
*   **Trust, but Verify, Data Boundaries.** This is the most important lesson I learned. When data is deserialized (e.g., from YAML or CSV), its Clojure data type may not be what you expect. A single-item list in YAML might become a `map` in Clojure, not a `vector` of one map. A multi-item list might become a `set`. Your code must be defensive and handle all possible data shapes from external sources.
*   **A Test's Setup Must Be as Realistic as its Assertions.** A test is only as good as the environment it creates. When testing code that interacts with the filesystem, ensure the mock environment is complete. If a function depends on a file, the test setup must first *create* that file.

#### 2. Hard-Won Lessons & How to Avoid My Mistakes

I made several significant errors during the last session. Understanding them is your best defense against repeating them.

*   **Mistake: Hallucinating Test Success.** My most critical error was repeatedly misreading test failure logs and reporting that the tests had passed. This was a failure of process and attention to detail.
    *   **Lesson:** Slow down. Read every line of the test output, especially the final summary line. Do not proceed until you see "0 failures, 0 errors." Trust the tests over any internal sense of correctness.
*   **Mistake: The YAML Data Type Cascade.** My biggest technical failure was in the `select` command's metadata update logic.
    *   **What Happened?** I wrote code that assumed the `:selection` field in a prompt's header would always be a vector. The test failures proved that, depending on the number of selection events, the YAML parser would return a single `map` or a `set` of maps. My update logic (`conj`) was not robust enough, leading to data corruption.
    *   **Lesson:** The fix was to write a more defensive `update-fn` that explicitly checks the type of the existing data (`vector?`, `map?`, `coll?`) before attempting to modify it. Always anticipate and handle all possible data shapes at component boundaries.
*   **Mistake: Ignoring a Component's Own Tests.** I introduced a bug in the `reports` component and only discovered it when the `select` command tests failed.
    *   **Lesson:** The Polylith `test` command is designed to run all affected tests. Use it. After fixing a bug, run the full test suite (`make test`), not just the tests for the component you believe you were working on.

#### 3. Current State & Unwritten Insights

The official documentation is up-to-date, but here are some insights into the current state that are not explicitly written down:

*   **The Core Loop is Solid:** The `bootstrap`, `vary`, `evaluate`, and `select` commands work together as designed. The data flows correctly from one step to the next, and the provenance is captured in the prompt headers.
*   **The Debugging Loop for `evaluate`:** If you encounter issues with the `evaluate` command, your primary debugging artifact is the `failter-spec` directory that it generates. You can inspect the `inputs/`, `templates/`, and `model-names.txt` files to see exactly what was passed to the `failter` tool. You can even run the `failter` commands manually on that directory to isolate the problem.
*   **The `Makefile` is Your Friend:** Use `make test` to run all project tests. Use `make pack` to generate the `pcrit-pack.txt` file before you need to share the project's state.

#### 4. Next Steps: Refactoring and the `evolve` Command (v0.3)

The next milestone is to automate the core loop. Before adding the new feature, we should perform two key refactorings that we identified.

**Step 1: Improve the UX of the CLI**

Right now, the commands must begin with "clj -M..."
We want to invoke the commands as "pcrit ...".  Offer ways to do this.

**Step 2: Refactoring**

1.  **Centralize the Default Selection Policy:** The magic string `"top-N=5"` currently exists in both the `select` command and the `cli` base.
    *   **Action:** Define this default in one place, such as `pcrit.config.core`. Update both the `select` command and the CLI's `:default` option to read from this central location. This makes changing the default trivial.
2.  **Make Selection Policies Pluggable:** The `select` command has a hardcoded `if-let` to parse the `"top-N"` policy. This is not extensible.
    *   **Action:** Refactor `apply-selection-policy` to use a Clojure `multimethod`. Dispatch on a keyword derived from the policy string. Create a `defmethod` for `:top-n`. This will allow you and others to add new selection policies (e.g., `:roulette-wheel`) in the future without modifying the core `select` command.

**Step 3: Implement the `evolve` Command**

This new command will run the `vary -> evaluate -> select` cycle automatically for a specified number of generations.

1.  **Create `pcrit.command.evolve.clj`:** This will house the new `evolve!` function.
2.  **Implement `evolve!` Logic:**
    *   The function will take the experiment context and a map of options, primarily `{:generations N}`.
    *   It will loop `N` times. Inside the loop, it will call `cmd/vary!`, `cmd/evaluate!`, and `cmd/select!` in sequence. It needs to dynamically generate a unique contest name for each evaluation step.
3.  **Integrate with the CLI:**
    *   Add the `evolve` command to the `command-specs` map in `pcrit.cli.main`.
    *   It will need options like `--generations N`, `--inputs DIR`, and likely `--initial-contest-name <name>`.
4.  **Write Tests:** Create `pcrit.command.evolve_test.clj`. The test should bootstrap an experiment, run `evolve!** for 2-3 generations, and then assert that the correct number of generation directories exist and that the population has been correctly winnowed at each step.

When you are finished, please update this document, renaming it to `onboard-10.md`, to ensure a smooth handover to your own successor. I am confident you will succeed.
