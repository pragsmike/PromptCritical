### **To My Successor: Onboarding for PromptCritical v0.5 Development**

**File:** `copilot/onboard-17.md`

Welcome to the PromptCritical project. This document supersedes `onboard-16.md`. The primary objective of the last session was to harden the system by improving user experience and error handling for common failure scenarios, which we called "bad day" cases. We successfully implemented several new validation checks and warnings, and created a new document, `docs/BAD-DAYS.md`, to guide users through these situations. The system is now significantly more robust and user-friendly.

Your objective is to begin the `v0.5` milestone, which was outlined previously: **implementing a Surrogate Critic to reduce experiment cost.** However, before we build the critic, we must first execute a critical prerequisite refactoring of the results-parsing system.

#### 1. Prime Directives and Rules of Collaboration

These principles are the bedrock of our workflow. They have been refined through trial and error and are essential for maintaining quality and velocity.

*   **A Passing Test Suite is the Only Definition of Done:** We do not proceed until all tests pass. A failing test is not an obstacle; it is a precise, actionable bug report that must be addressed before any new work is started.
*   **A Test Helper Creates Preconditions, Not Results:** This rule remains critical. A helper's sole job is to create the *minimal valid state* required for the function-under-test to run.
*   **The Logs Are Part of the Test Output:** When a test fails, read the log output just as carefully as the test assertion. Twice during this session, the logs contained a `WARN` or `ERROR` that pointed directly to the root cause of a test failure.
*   **Use the Library's Extension Points:** When testing code with a well-designed library (like Telemere), use its intended extension points (`with-handler`) rather than trying to force a mock onto its internals (`with-redefs` on a macro). This is a lesson we learned the hard way.
*   **Respect Architectural Layers:** Low-level components must never depend on high-level ones.
*   **Isolate, Verify, Implement:** Before depending on an external tool's behavior (like `failter`'s output streams), verify your assumptions. A small, targeted test can prevent large, complex bugs.

#### 2. Hard-Won Lessons & How to Avoid My Mistakes

I made a series of significant, related errors during this session while trying to test logging side effects. Understanding this chain of mistakes is the most important lesson from our work.

*   **Mistake: The `with-redefs` on a Macro.** This was the original sin.
    *   **What Happened:** I repeatedly tried to test a `log/warn` call by using `with-redefs` on the `log/warn` macro itself. This is fundamentally impossible in Clojure. Macros are expanded at compile time, while `with-redefs` is a runtime construct. The compiler replaces `(log/warn ...)` with a direct call to `(taoensso.telemere/log! ...)` long before the test runner has a chance to rebind anything.
    *   **Lesson:** You cannot mock a macro. Full stop. The test failures and compiler errors (`Can't take value of a macro`) were definitive proof. Any attempt to work around this by wrapping the macro in a function is a code smell that indicates a flawed testing strategy.

*   **Mistake: Flawed Fixture-Based Workarounds.** This was a consequence of the first mistake.
    *   **What Happened:** After `with-redefs` failed, I tried to build a custom test fixture that would dynamically add and remove a logging handler. My implementations were incorrect, either because I tried to use the fixture inline or because I didn't understand the dynamic scope, leading to a series of `NullPointerException`s when the test tried to access an un-bound atom.
    *   **The Correct Solution (Your Insight):** The breakthrough came when you suggested using the library's built-in `with-handler` macro. This is the correct, idiomatic way to temporarily add a logging handler to capture side effects for a specific block of code. It is designed for precisely this purpose.

*   **Mistake: Mixing Warnings and Validations.**
    *   **What Happened:** In `evaluate!`, I added a non-fatal warning check for an empty inputs directory in the middle of a `cond` block that was supposed to handle fatal validation errors. This caused the `cond` to short-circuit, skipping critical subsequent checks (like "does the contest already exist?"), leading to very confusing test failures.
    *   **Lesson:** Be precise about the difference between a validation (which must halt execution) and a warning (which should not). They must be handled in separate stages of the function logic.

#### 3. Current State & Unwritten Insights

*   **User Experience is Now a Priority:** We have established that a "correct" but silent failure is a bad user experience. The system is now equipped with several explicit warnings for common "bad day" scenarios, all documented in `docs/BAD-DAYS.md`. This is a core part of the project's philosophy.
*   **The `failter` Integration is Now Truly Robust:** We discovered that `failter` was co-mingling logs and JSON on its standard output. By switching to a file-based contract (where `pcrit` tells `failter` where to write its `output_file`), our integration is no longer dependent on fragile stream parsing. Capturing the stdout/stderr to log files was a key improvement for debuggability.

#### 4. Next Steps: The Road to v0.5

Your mission is to begin the work for the `v0.5` milestone. As discussed previously, this requires a foundational refactoring *before* we can implement the surrogate critic.

##### **Task 1: (Prerequisite) Centralize Report Parsing**

The `stats` and `select` commands currently depend on the simplified `report.csv`. To enable richer analysis for the surrogate critic, they must be upgraded to use the `failter-report.json` as their source.

1.  **Create a new `pcrit.results` component.** This component will be the single source of truth for parsing contest results.
2.  **Implement `(results/parse-report ctx gen-num contest-name)`:**
    *   This function will locate and read the `failter-report.json` file for the given contest.
    *   It will move the cost-calculation logic that currently lives in `pcrit.reports.core` into this new component.
    *   It will return a canonical sequence of maps, each representing a scored prompt with all its associated metadata (usage, performance, cost).
3.  **Refactor `pcrit.command.stats`:** Update the `stats!` command to call the new `results/parse-report` function. Because it will now have access to richer data, you can enhance the stats report to include metrics like average tokens-in/out.
4.  **Refactor `pcrit.command.select`:** Update the `select!` command to also use `results/parse-report`. Its internal logic will not need to change, as it only requires the `:prompt` and `:score` keys, which will still be present.
5.  **Deprecate `report.csv` as a data source:** The `process-and-write-csv-report!` function can remain, but it should be understood that the CSV it produces is now purely a human-readable artifact and is no longer used by the core evolutionary loop.

##### **Task 2: Implement the Surrogate Critic**

Once the results parsing is refactored, you can proceed with the main v0.5 feature.

1.  **Create a `pcrit.critic` component.**
2.  **Implement `(critic/filter-population population)`:** The initial implementation can be a simple, rule-based filter that removes prompts that are obviously flawed (e.g., missing template variables, too long, etc.).
3.  **Integrate into the `evolve` loop:** Modify `pcrit.command.evolve` to add a new `critic!` step between `vary!` and `evaluate!`.

This is a substantial but well-defined block of work. When you are finished, please update this document, renaming it to `onboard-18.md`. Good luck.
