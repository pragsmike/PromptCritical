### **To My Successor: Onboarding for PromptCritical v0.2 Development**

**File:** `copilot/onboard-7.md`

Welcome to the PromptCritical project. This document supersedes `onboard-6.md` and contains your primary directives for completing the `v0.2` milestone. We have just successfully implemented the `evaluate` command, and the core evolutionary loop is nearly complete.

#### 1. Prime Directives and Rules of Collaboration

These principles are not just guidelines; they are the rules that prevent errors and ensure our work is robust. They have been validated repeatedly during our last session.

*   **Never Assume API Behavior; Verify Everything:** State your uncertainty; do not generate code based on a guess. My incorrect assumptions about `clojure.tools.cli` and `clojure.core/apply` caused multiple cycles of failure. Trust the test output, not your memory of an API.
*   **Test Isolation is Paramount:** Each distinct, stateful test case **MUST** be in its own `deftest` block. The `:each` fixture provides a clean environment at the `deftest` level. Sharing state between `testing` blocks caused cascading, difficult-to-diagnose failures in the `evaluate` command tests.
*   **A Test's Setup Must Be as Realistic as its Assertions:** A test is only as good as the environment it creates. My `failter` test failed because `File.exists()` on a symbolic link is only true if the *target* file also exists. The test setup must create all necessary preconditions—including dummy target files—for the assertions to be valid.
*   **Address the Root Cause, Not the Symptom:** Trust `actual` output in tests with absolute precision. Fix the data, the test setup, or the core logic before making superficial changes to assertions.
*   **The Filesystem and Shell are APIs:** Treat directory structures and command arguments as strict contracts.

#### 2. Recent Accomplishments (What We Just Did)

1.  **Simplified the Workflow:** We refactored `bootstrap` to create `gen-0` directly, which was a significant improvement. A user can now get from a fresh experiment to a scored population in just two commands (`bootstrap`, `evaluate`).
2.  **Refactored the CLI:** We implemented a robust, two-pass CLI parser driven by a declarative `command-specs` map. The CLI now cleanly supports global options and command-specific options with their own help text.
3.  **Created a `failter` Adapter:** We built a new `pcrit.failter` component. This is a crucial "thin adapter" that encapsulates all interaction with the external `failter` tool, keeping the main command logic clean and testable.
4.  **Implemented the `evaluate` Command:** We successfully implemented the `evaluate!` command, including its CLI integration, default option handling, and validation logic.

#### 3. Hard-Won Lessons & How to Avoid My Mistakes

I made several errors during our last session. Understanding them is key to avoiding them in the future.

*   **Mistake: Test State Leakage.**
    *   **What Happened?** In `evaluate_test.clj`, multiple stateful `testing` blocks were placed in a single `deftest`. The second test failed because it inherited a modified filesystem from the first, causing `find-latest-generation-number` to return an unexpected value.
    *   **Lesson:** For any test that touches the filesystem (which is most of them in this project), **use a new `deftest` for each independent scenario.** This guarantees the `with-temp-dir` fixture provides a pristine environment.

*   **Mistake: Incomplete Test Setup.**
    *   **What Happened?** The initial `failter` test failed because an assertion `(.exists link-file)` returned false. The code to create the symbolic link worked, but the test setup never created the *target file* the link pointed to. `File.exists()` on a broken symlink is false.
    *   **Lesson:** When testing code that interacts with the filesystem, ensure the mock environment is complete. If you're testing a function that reads or links to a file, the test setup must first *create* that file.

*   **Mistake: Incorrect `apply` Usage.**
    *   **What Happened?** A call to the shell using `(apply shell/sh "failter" args :dir ...)` threw a cryptic `IllegalArgumentException`.
    *   **Lesson:** `clojure.core/apply` only unrolls its *final* argument. You cannot pass a sequence and then add more arguments after it. The correct pattern, which we implemented, is to `concat` all arguments into a single sequence *before* calling `apply`.

#### 4. Next Steps: Implementing the `select` Command

Our immediate and sole goal is to implement the `select` command to complete the `v0.2` milestone. The system is perfectly prepared for this final step.

Here is the plan:

1.  **Create a `pcrit.command.select` Namespace:** This will house the `select!` function.
2.  **Integrate with the CLI:** Add a new entry to the `command-specs` map in `pcrit.cli.main`.
    *   **Command:** `select`
    *   **Options:** It must have a `--from-contest <name>` option to specify which `report.csv` to use. It should probably also take an optional `--generation <number>` in case contest names are not unique across the whole experiment.
3.  **Implement the `select!` Logic:**
    *   It will parse the CLI options and resolve the path to the specified `report.csv` file using `expdir` functions.
    *   **CSV Parsing:** The `pcrit.pdb` component already includes the `clojure-csv/clojure-csv` dependency. To maintain separation of concerns, we should create a new, small `pcrit.reports` component whose only job is to parse a `report.csv` file into a sequence of maps. This keeps the CSV parsing logic out of the main command.
    *   **Selection Strategy:** The `select!` function will use the parsed report data to apply a selection strategy. For `v0.2`, this can be a simple, hardcoded strategy: "read the `:score` column and keep the prompts that are in the top 50%".
    *   **Create New Generation:** The command will determine the list of surviving prompt IDs from the strategy. It will then load the full prompt records for these survivors using `pop/read-prompt` and pass this list to `pop/create-new-generation!`. This will create the next generation's directory, populated with symlinks to only the fittest prompts.

Adhere to the directives, learn from the established patterns, and focus on this plan. I am confident you will be successful.
