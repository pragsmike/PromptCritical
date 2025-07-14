### **To My Successor: Onboarding for PromptCritical v0.4 Development**

**File:** `copilot/onboard-15.md`

Welcome back to the PromptCritical project. This document supersedes `onboard-14.md`. The primary objective of the last session was to execute Task 1 of the v0.4 milestone: refactoring the test suite by consolidating duplicated setup logic into a shared helper namespace. This critical housekeeping work is now complete. The test suite is significantly cleaner, more robust, and easier to maintain.

Your objective is to complete the `v0.4` milestone by implementing the advanced features planned: more sophisticated selection and mutation operators for the evolutionary engine.

#### 1. Prime Directives and Rules of Collaboration

These principles are the foundation of our workflow. They have proven essential for maintaining velocity and preventing regressions.

*   **A Passing Test Suite is the Only Definition of Done:** We do not proceed until all tests pass. A failing test is not an obstacle; it is a precise, actionable bug report that must be addressed before any new work is started.
*   **A Test Helper Creates Preconditions, Not Results:** This is a lesson we learned the hard way. A helper function's sole job is to create the *minimal valid state* required for the function-under-test to run. It must not create the state that would exist *after* the function has run.
*   **Test the Test and the Code:** When a function's contract changes, its test must also change. The test is part of the specification. The bug we found in `expdir.core-test` (where it was still asserting the old, incorrect behavior) is a perfect example of this.
*   **Respect Architectural Layers:** Low-level, shared components (like `test-helper`) must never depend on high-level, orchestrating components (like `command`). Doing so creates circular dependencies that the build tool will correctly reject.
*   **Isolate, Verify, Implement:** Before relying on a feature from an external library or another component, write a minimal, isolated test to confirm it behaves exactly as you expect.
*   **Test Isolation is Paramount:** Every distinct test case **must** be in its own `deftest` block to ensure a clean state for every run.

#### 2. Hard-Won Lessons & How to Avoid My Mistakes

I made several significant errors during the test refactoring. Understanding them is your best defense against repeating them.

*   **Mistake: The Circular Dependency.** This was a major architectural blunder.
    *   **What Happened:** I created helper functions in the `test-helper` component that called functions from the `command` component (specifically, `cmd/bootstrap!`). However, `command`'s tests already depended on `test-helper`, creating a `command -> test-helper -> command` cycle that the Polylith tooling correctly identified and blocked.
    *   **Lesson:** This was a failure to respect component layers. The fix was to recognize that these were not generic helpers; they were helpers *for testing commands*. Moving them into the `command` component's own test scope (`pcrit.command.test-helper`) immediately broke the cycle and made the architecture sound again.

*   **Mistake: The Flawed Helper Regression.** This was deeply embarrassing.
    *   **What Happened:** In the process of creating the new helpers, I repeated the exact mistake `onboard-13.md` warned about: I created a helper that built the `generations/` directory, which is an invalid precondition for the `bootstrap!` command. This caused a cascade of test failures.
    *   **Lesson:** The rule "A Test Helper Creates Preconditions, Not Results" cannot be overstated. The bug was only truly fixed when I corrected `expdir.core` itself to never prematurely create that directory, and then fixed the `expdir` test to verify the new, correct behavior.

#### 3. Current State & Unwritten Insights

*   **Test Helpers are now DRY and Correctly Scoped:** The new `pcrit.command.test-helper` namespace provides a clean, reliable way to set up test experiments. The rest of the command tests (`vary_test`, `cli.main_test`, etc.) have been successfully refactored to use it.
*   **Debug from the Bottom Up:** Our most effective debugging technique has been to trust the test output and start at the lowest-level failure. The `bootstrap!` failures were not a problem in the `cli` or `evolve` tests; they were a problem in the `expdir` component and the test helpers. Fixing the problem at its root cause is always the right approach.
*   **`make test` is the Gatekeeper:** The `make test` command is our contract. Running it is the first and last step of any task.

#### 4. Next Steps: The Road to v0.4

Your mission is to complete the remaining tasks for the v0.4 milestone. The test suite is now robust, so you can proceed with confidence.

##### **Task 1: (Highest Priority) Implement Advanced Selection Operators**

The current `top-N` selection policy is too greedy. We will now implement Tournament Selection.

1.  **Modify `pcrit.command.select`:**
    *   In the `parse-policy` function, add a new clause to recognize the `"tournament-k=N"` string format (e.g., `"tournament-k=4"`). It should parse this into a map like `{:type :tournament, :k 4}`.
    *   Implement the new `(defmethod apply-selection-policy :tournament ...)` multimethod.
2.  **Tournament Logic:**
    *   The `:tournament` method should receive the full list of scored prompts from the report.
    *   It should loop N times, where N is the original population size.
    *   In each iteration, it should:
        *   Select `k` individuals *at random* from the full list.
        *   Identify the one with the highest `:score` from that small group.
        *   Add that winner to the list of survivors.
    *   This process naturally allows for duplicates in the new generation and gives lower-scoring prompts a chance to survive, preserving diversity.
3.  **Create a New Test:**
    *   In `select_test.clj`, add a new `deftest` for tournament selection.
    *   Create a predictable report where, for example, `P10` has a low score.
    *   Run tournament selection multiple times. Assert that it's possible for `P10` to be selected as a survivor, which would be impossible with `top-N=5`.

##### **Task 2: Implement Advanced Mutation Operators**

The `vary` command currently has only one hardcoded strategy. We will refactor it to be pluggable and add a "crossover" operator.

1.  **Refactor `pcrit.command.vary`:**
    *   Turn the `gen-offspring` function into a `defmulti` that dispatches on a strategy keyword (e.g., `:refine`, `:crossover`).
    *   The dispatch function will look up the active strategy from the `:vary` section of `evolution-parameters.edn`.
2.  **Implement Crossover:**
    *   Create a new meta-prompt, e.g., `crossover-meta-prompt.txt`: `"Combine the best elements of the following two prompts to create a new, superior prompt that achieves the same goal. PROMPT A: {{OBJECT_PROMPT_A}} PROMPT B: {{OBJECT_PROMPT_B}}"`
    *   Implement the `(defmethod gen-offspring :crossover ...)` method. It should select two high-performing parents from the population, format them into the new meta-prompt, and call the LLM.
    *   Ensure the resulting offspring prompt record lists **both** parents in its `:parents` list.
3.  **Create a New Test:**
    *   In `vary_test.clj`, add a new `deftest` for the crossover strategy.
    *   Verify that when this strategy is used, the generated offspring correctly lists two parent IDs in its header.

When you are finished, please update this document, renaming it to `onboard-16.md`. Good luck.
