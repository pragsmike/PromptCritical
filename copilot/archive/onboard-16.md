### **To My Successor: Onboarding for PromptCritical v0.5 Development**

**File:** `copilot/onboard-16.md`

Welcome to the PromptCritical project. This document supersedes `onboard-15.md`. The primary objective of the last session was to complete the `v0.4` milestone. We successfully implemented and tested two advanced evolutionary operators: **Tournament Selection** (`--policy tournament-k=N`) and **Crossover Mutation** (`:strategy :crossover`). We also performed several important internal refactorings that have left the codebase cleaner and more maintainable. The system is now significantly more powerful and robust.

Your objective is to begin the `v0.5` milestone: **implementing a Surrogate Critic to reduce experiment cost and complexity.**

#### 1. Prime Directives and Rules of Collaboration

These principles have proven essential for our collaboration and have saved us from numerous bugs. Please adhere to them strictly.

*   **A Passing Test Suite is the Only Definition of Done:** We do not proceed until all tests pass. A failing test is not an obstacle; it is a precise, actionable bug report that must be addressed before any new work is started.
*   **A Test Helper Creates Preconditions, Not Results:** This is a critical distinction. A helper's sole job is to create the *minimal valid state* required for the function-under-test to run. It must not create the state that would exist *after* the function has run.
*   **Mocks Must Be Complete:** When mocking a function (like `rand-nth`), you must account for its entire lifecycle within the test. Think through how many times it will be called and provide enough deterministic data to cover all calls, or the test will fail with unexpected errors.
*   **A Green Test Run is Not Enough; Check the Logs:** Twice, we had a bug where the warning logs told us exactly what was wrong (a missing link, a missing require), but we focused only on the test output. Always check the logs for warnings, even on a successful run.
*   **Respect Architectural Layers:** Low-level, shared components (like `test-helper`) must never depend on high-level, orchestrating components (like `command`).
*   **Isolate, Verify, Implement:** Before relying on a feature, write a minimal, isolated test to confirm it behaves exactly as you expect.
*   **Test Isolation is Paramount:** Every distinct test case **must** be in its own `deftest` block.

#### 2. Hard-Won Lessons & How to Avoid My Mistakes

I made several errors during the v0.4 implementation. Understanding them is your best defense against repeating them.

*   **Mistake: The Incomplete Mock.** This caused the `select_test.clj` to fail with an `IndexOutOfBoundsException`.
    *   **What Happened:** I mocked `rand-nth` to provide a sequence of "random" choices for a tournament. However, I only supplied enough choices for a single tournament, failing to realize the main loop would run multiple tournaments, calling `rand-nth` many more times than my mock was prepared for.
    *   **Lesson:** Before mocking a dependency, fully understand the algorithm that calls it. Trace the loops and count the calls to ensure your mock is comprehensive.

*   **Mistake: The Out-of-Sync Test Helper.** This caused the initial `crossover-strategy-test` to fail because the crossover meta-prompt "wasn't found."
    *   **What Happened:** We added a new `crossover` prompt to the `init` and `bootstrap` commands, but the shared `setup-bootstrapped-exp!` test helper was not updated. It was still creating an *old* version of a bootstrapped experiment. My initial fix was a local workaround in the test itself, which was wrong.
    *   **Lesson:** When a core process like bootstrapping is changed, the corresponding test helpers **must** be changed with it. A stale helper is a source of confusing test failures. We corrected this by updating the helper and removing the workaround.

*   **Mistake: The Missing `:require`.** This was a simple but embarrassing oversight.
    *   **What Happened:** I used `pdb/read-prompt` inside the `vary.clj` namespace without adding `[pcrit.pdb.interface :as pdb]` to the namespace's `:require` block. This caused the tests to fail to compile.
    *   **Lesson:** It's a basic error, but it highlights the need for diligence. Always ensure your dependencies are declared.

#### 3. Current State & Unwritten Insights

*   **The Report Data Flow:** We had a crucial discussion about the reporting data flow. Currently, `evaluate!` produces `failter-report.json` and then creates `report.csv` from it. Both `stats!` and `select!` read the `report.csv`. This is a deliberate choice for simplicity and consistency. We decided **not** to have them read the JSON for now, to avoid logic duplication (like cost calculation) and prevent the two commands from potentially diverging. This is an important piece of architectural context.
*   **Command Helpers are Canon:** The pattern of creating a helper in `pcrit.command.test-helper` for each state (e.g., "bootstrappable" vs "bootstrapped") is now well-established. Use it.
*   **The Power of Pluggable Strategies:** The `vary` and `select` commands are now much more powerful. The multimethod approach worked very well, keeping the core command logic clean while allowing for easy extension. This is a pattern to emulate for future features.

#### 4. Next Steps: The Road to v0.5

Your mission is to implement the **Surrogate Critic**. This component will run *after* `vary` but *before* the expensive `evaluate` step to pre-filter bad prompts, saving time and money.

##### **Task 1: (Refactoring Prerequisite) Centralize Report Parsing**

The insight about the `.csv` vs `.json` data flow revealed a necessary refactoring. The surrogate critic will likely need richer data than the CSV provides. Therefore, before building the critic, both `stats` and `select` should be refactored to use the `failter-report.json` as their source.

1.  **Create a `pcrit.results` component:** This new component will have one job: parse a `failter-report.json` file.
2.  **Implement `(results/parse-report ctx gen-num contest-name)`:** This function will read the JSON, perform the cost calculation for each entry (logic that currently lives in `reports/process-and-write-csv-report!`), and return a canonical sequence of maps.
3.  **Refactor `pcrit.command.stats`:** Update `stats!` to call the new `results/parse-report` function instead of reading CSV files.
4.  **Refactor `pcrit.command.select`:** Update `select!` to also call `results/parse-report`. The `report.csv` file will now become a purely informational artifact for human inspection, no longer used by the core loop.

##### **Task 2: Implement the Surrogate Critic**

1.  **Create a `pcrit.critic` component.**
2.  **Implement `(critic/filter-population population)`:** For this first version, the critic can be simple and rule-based. For example, it could remove any prompt that:
    *   Has a character count over a certain threshold.
    *   Does *not* contain the required `{{INPUT_TEXT}}` template variable.
    *   Fails a simple profanity check.
3.  The function should take a list of prompt records and return a (smaller) list of prompt records that pass the checks.

##### **Task 3: Integrate the Critic into the `evolve` loop**

1.  Modify `pcrit.command.evolve/evolve!`.
2.  Add a new step between `vary!` and `evaluate!`. The new loop should look like this:
    `... → vary! → critic! → evaluate! → ...`
3.  The `critic!` step will call the new `critic/filter-population` function, passing its result to `evaluate!`.
4.  Ensure you create a new test for this updated `evolve` flow to verify that the critic is correctly filtering the population before evaluation.

When you are finished, please update this document, renaming it to `onboard-17.md`. Good luck.
