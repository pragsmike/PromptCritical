### **To My Successor: Onboarding for PromptCritical v0.4 Development**

**File:** `copilot/onboard-14.md`

Welcome to the PromptCritical project. This document supersedes `onboard-13.md`. The primary objectives of the last session were to refactor the evaluation pipeline for a new, spec-driven `Failter` API and to implement a high-level `evolve` command to automate the core evolutionary loop. This work is now complete, marking the successful conclusion of the `v0.3` milestone.

Your objective is to begin the `v0.4` milestone. This will involve two major efforts: first, improving the quality and maintainability of the test suite by refactoring shared setup logic; and second, introducing more sophisticated selection and mutation strategies to enhance the power of the evolutionary engine.

#### 1. Prime Directives and Rules of Collaboration

These principles have proven effective and remain mandatory.

*   **A Passing Test Suite is the Only Definition of Done:** We do not consider a task complete until all tests pass. A failing test is not an annoyance; it is a pointer to a bug that must be fixed before proceeding.
*   **Isolate, Verify, Implement:** When interacting with an external library or a different component, write a small, isolated test to verify its behavior *before* building logic that depends on it. This was the key lesson from the `clj-yaml` bug.
*   **Test Isolation is Paramount:** Each distinct test case **must** be in its own `deftest` block. This leverages the `:each` fixture to guarantee a clean state and prevent subtle, cascading failures caused by state pollution.
*   **Mock the Implementation, Not the Interface:** When testing a component (e.g., `evolve`) that calls another component (e.g., `vary`), the test must mock the *implementation var* (`pcrit.command.vary/vary!`) not the interface var (`pcrit.command.interface/vary!`). This is a crucial pattern for testing in Polylith.

#### 2. Hard-Won Lessons & How to Avoid My Mistakes

My biggest error during the last session was test pollution.

*   **Mistake: The Test-Setup Cascade Failure.** My initial tests for the `evolve` command failed because the test environment was missing the `evolution-parameters.edn` file required by the `evaluate` command, which `evolve` calls internally. This was compounded by having multiple `(testing ...)` blocks inside a single `deftest`, which prevented proper test isolation.
    *   **Lesson:** A test for a high-level orchestrator function (like `evolve!`) must set up the complete and valid preconditions for *all downstream functions it calls*. Breaking tests into separate `deftest` blocks was the ultimate fix, as it forced a clean setup for every single case and made the root cause immediately obvious.

#### 3. Current State & Unwritten Insights

*   **The `evolve` command is the new entry point.** While the manual `vary`, `evaluate`, and `select` commands are still available, the primary user workflow is now `init` -> `bootstrap` -> `evolve`. Our documentation reflects this.
*   **Cost tracking is built-in.** The `vary!` and `evaluate!` functions now return the cost of their operations, which is aggregated by `evolve!` and checked against the optional `--max-cost` budget. This makes running experiments safer and more predictable.
*   **The Failter integration is rock-solid.** The new spec-driven `failter run` command is a massive improvement. Our `pcrit.failter` component is now a very thin, robust wrapper around this new API.

#### 4. Next Steps: The Road to v0.4

Your mission is to complete the v0.4 milestone. The plan is as follows:

##### **Task 1: (Highest Priority) Refactor Test Helpers**

This was originally Task 3 in the v0.3 plan, but it's now the most important housekeeping item to improve codebase quality.

1.  **Create a `pcrit.test-helper.experiment` namespace.**
2.  **Consolidate setup logic.** Create a set of granular helper functions (e.g., `make-bootstrapped-exp`, `make-configured-exp`) that build on each other to establish common states for testing.
3.  **Refactor existing command tests.** Go through `evolve_test.clj`, `evaluate_test.clj`, etc., and replace the local, hand-written setup functions with calls to the new, shared helpers. This will DRY up the test suite and make it much easier to maintain.

##### **Task 2: Implement Advanced Selection Operators**

The current `top-N` selection policy is simplistic and risks premature convergence. We need to introduce more sophisticated strategies.

1.  **Enhance `pcrit.command.select`:**
    *   Modify the `parse-policy` function to recognize new policy strings (e.g., `"tournament-k=4"`, `"roulette"`).
    *   Implement the corresponding `apply-selection-policy` multimethods.
        *   **Tournament Selection:** A good next step. Select K random individuals from the population and choose the best one. Repeat N times. This maintains diversity better than `top-N`.
        *   **Roulette Wheel Selection:** A classic genetic algorithm approach where selection probability is proportional to fitness.

##### **Task 3: Implement Advanced Mutation Operators**

The current `vary` command only has one strategy ("refine"). We need to expand this to allow for more diverse evolutionary paths.

1.  **Enhance `pcrit.command.vary`:**
    *   Refactor `gen-offspring` into a multimethod that dispatches on a strategy defined in `evolution-parameters.edn`.
    *   Implement new meta-prompts and their corresponding strategies, such as:
        *   **Crossover:** A two-parent strategy that takes two high-performing prompts and asks an LLM to "combine the best elements of both into a new, superior prompt."
        *   **Targeted Refinement:** Meta-prompts that focus on specific attributes, e.g., "make this prompt more concise," "add chain-of-thought reasoning to this prompt," or "rephrase this prompt to be more polite."

When you are finished, please update this document, renaming it to `onboard-15.md`, to ensure a smooth handover for your successor. Good luck.
