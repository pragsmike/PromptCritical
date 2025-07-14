### **To My Successor: Onboarding for PromptCritical v0.3 Development**

**File:** `copilot/onboard-13.md`

Welcome to the PromptCritical project. This document supersedes `onboard-12.md`. The primary objectives of the last session were to identify and eliminate several deep-seated bugs in the Prompt Database (PDB) and testing framework, and to document the project's cost-accounting philosophy. This work is now complete. The system is significantly more robust and its on-disk representation is now canonical and auditable, as originally designed.

Your objective is to complete the `v0.3` milestone. This will now involve two major efforts: first, refactoring the evaluation component to adapt to a forthcoming breaking change in our key dependency, `Failter`; and second, implementing the high-level `evolve` command to automate the evolutionary loop.

#### 1. Prime Directives and Rules of Collaboration

These principles are the foundation of our workflow. Adhering to them is mandatory to prevent regressions and maintain velocity.

*   **Read the Manual, Then Read It Again:** An external tool's documentation is a strict contract. The simplest code that directly implements the documented behavior is always the right code.
*   **A Failing Test is an Asset:** Our goal is not to avoid failed tests, but to create them deliberately. A test that reliably fails proves the existence of a bug. A test that passes after a change proves the fix. This is our core development loop.
*   **Distrust Your Assumptions About Dependencies:** Before using a feature of a third-party library, write a minimal, isolated test case to verify it behaves exactly as you expect. Do not build logic on top of an unverified assumption.
*   **Test Isolation is Paramount:** Each distinct, stateful test case **must** be in its own `deftest` block. This prevents state from one test from invisibly corrupting the next.
*   **Respect Component Boundaries:** A component should be "dumb" about the data it handles. The `pdb` component's job is to read and write files according to a canonical format, not to understand the application's data schema. Keep responsibilities narrow and well-defined.
*   **The Invocation Layer Owns the Environment:** The application's core logic should not know how it was launched. The `bin/pcrit` wrapper script and the `cli.main` namespace are responsible for resolving all relative file paths into absolute paths *before* passing them to the core command functions.

#### 2. Hard-Won Lessons & How to Avoid My Mistakes

I made several significant errors during the last session. Understanding them is your best defense against repeating them.

*   **Mistake: The YAML Key-Order Fiasco.** This was a critical error in my analysis.
    *   **What Happened:** I saw `:sort-keys true` in the `pdb` code and confidently asserted that it was working, producing a clean `git diff`. You provided evidence (an unsorted prompt file) that proved me wrong. My next analysis incorrectly blamed the library version. Only after you provided targeted, minimal tests did we discover the truth: the syntax was wrong and the library was silently ignoring the option.
    *   **Lesson:** My failure was not verifying my assumption about the `clj-yaml` library's API. The correct workflow, which we eventually followed, is: **Isolate -> Verify -> Implement**. Before relying on a library feature, write a tiny, separate test (`yaml_test.clj`) to confirm its behavior. Only then should you integrate it into the application. We ultimately fixed the bug by manually sorting the map (`(into (sorted-map) header)`) before serialization, a robust solution that removes the dependency on the library feature.

*   **Mistake: The Test Setup Cascade Failure.** This bug appeared in multiple test files (`cli.main-test`, `command.vary-test`) and caused a confusing cascade of failures.
    *   **What Happened:** Tests were failing because the `bootstrap!` command aborted, claiming the experiment was "already bootstrapped." The root cause was a flawed test helper, `make-temp-exp-dir!`, which incorrectly created the `generations/` directory as part of its setup. This created an invalid state that violated the preconditions of the `bootstrap!` command.
    *   **Lesson:** Test helpers must create only the **minimal valid preconditions** for the function under test, not the function's *results*. The helper's job was to create the state for `bootstrap!` to *run on*, not the state that would exist *after* it ran. We fixed this by creating more granular helpers that set up only the necessary `seeds/` and `bootstrap.edn` files.

#### 3. Current State & Unwritten Insights

The official documentation is now much stronger, but here are some insights not explicitly recorded elsewhere.

*   **The Provide-Evidence Debugging Loop:** Our most effective technique was our collaboration pattern. You provided raw, unedited output (`ls -lR`, full stack traces, failing test results). This evidence was always the key to pinpointing the bug. We must continue this practice. The `--log=debug` flag is your most powerful tool for generating this evidence.
*   **Cost Calculation is Now Fully Documented:** Our discussions about cost have been codified in the new `docs/evo-cost-calculation.md` document. This file is now the single source of truth for how costs are tracked and calculated, both for generation (`vary`) and evaluation (`evaluate`). It correctly anticipates the upcoming `Failter` changes.
*   **`make pack` is Our Shared Context:** The `Makefile`'s `pack` command is essential. Before asking me to review or debug, always run `make pack` to ensure my context is identical to yours.

#### 4. Next Steps: The Road to v0.3

Your mission is to complete the v0.3 milestone. The plan has been revised based on our session.

##### **Task 1: (Highest Priority) Refactor for the New Failter Interface**

You must assume the next version of `Failter` will have a new, simplified interface, as we proposed. It will no longer require a complex directory of symlinks and it will report token usage, not pre-calculated costs.

1.  **Modify `pcrit.command.evaluate` and `pcrit.failter.core`:**
    *   Change the `evaluate!` command to no longer call `expdir/prepare-contest-directory!`.
    *   Instead, it should generate a `contest.spec` file (in YAML or EDN) that defines the inputs, templates, and models to be run.
    *   The `failter/run-contest!` function should be updated to invoke `failter run --spec /path/to/contest.spec`.
2.  **Implement Cost Calculation for Evaluation:**
    *   The `evaluate!` command will now be responsible for parsing the *token usage* from Failter's output (`report.csv` or, ideally, a future JSON stream).
    *   For each evaluated prompt, it must use the `config/price-table` to calculate the final cost. This moves the cost calculation logic fully inside PromptCritical, making the system more self-contained and robust.
    *   The final `report.csv` stored in the `contests/` directory should be augmented to include this calculated cost column.

##### **Task 2: Implement the `evolve` Command**

Once the `evaluate` command has been refactored, you can proceed with the original v0.3 goal.

1.  **Create `pcrit.command.evolve.clj`:** This new namespace will house the core `evolve!` function.
2.  **Implement `evolve!` Logic:**
    *   It must accept options for controlling the evolution: `--generations N` and `--max-cost N.NN`.
    *   It will loop, executing `cmd/vary!`, `cmd/evaluate!`, and `cmd/select!` in sequence.
    *   **It must track cumulative cost.** After each `vary` and `evaluate` step, it must aggregate the costs and check against the `--max-cost` budget. If the budget is exceeded, the loop must halt gracefully.
    *   It must dynamically generate a unique contest name for each `evaluate!` call within the loop to avoid collisions.

##### **Task 3: (Housekeeping) Refactor Test Helpers**

The cascade failure we debugged highlighted a weakness in our test helpers. Create a new, shared helper namespace (e.g., `pcrit.test-helper.experiment`) to consolidate test setup logic and prevent future bugs. This will DRY up the tests in `command/*` and `cli/*`.

When you are finished, please update this document, renaming it to `onboard-14.md`, to ensure a smooth handover for your successor. Good luck.
