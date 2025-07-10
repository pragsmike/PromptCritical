### **To My Successor: Onboarding for PromptCritical v0.2 Development**

**File:** `copilot/onboard-6.md`

Welcome to the PromptCritical project. This document supersedes `onboard-5.md` and contains your primary directives. We have just completed a series of critical refactorings and feature implementations that have prepared the codebase for the final phase of the `v0.2` milestone.

#### 1. Prime Directives and Rules of Collaboration

These principles remain the foundation of our work. They have proven their value repeatedly.

*   **Never Assume API Behavior; Verify Everything:** State your uncertainty; do not generate code based on a guess.
*   **Address the Root Cause, Not the Symptom:** Trust `actual` output in tests. Fix the data or setup before fixing the code.
*   **The Filesystem and Shell are APIs:** Treat directory structures and command arguments as strict contracts.
*   **State Your Intentions Clearly** and **Incorporate Feedback Directly.**

#### 2. Recent Accomplishments (What We Just Did)

The previous onboarding document's main goal was to refactor the CLI. We have accomplished that and more. Understanding these changes is key to your success.

1.  **Made the CLI Testable:** We extracted the core logic from `pcrit.cli.main` into a pure `process-cli-args` function, allowing for robust unit testing of command dispatching. This was a prerequisite for all future work.

2.  **Fixed LLM Cost Parsing:** We fixed a bug where LLM call costs were always `0.0`. We then refactored the `pcrit.llm` component to centralize all response parsing (body and headers) into `parse-llm-response`, improving the design.

3.  **Implemented Prompt Ancestry:** The `vary` command now records the `:parents` and `:generator` (including model name and meta-prompt ID) in the header of all new prompts it creates. This is a critical step for provenance.

4.  **Made Variation Configurable:** The `vary` command no longer uses a hardcoded model. It now reads the model from an `evolution-parameters.edn` file in the experiment directory, falling back to a default ("mistral") if not specified.

#### 3. Unwritten Knowledge & Hard-Won Lessons

These are the crucial lessons learned from our recent work. They are not explicitly captured elsewhere in the documentation.

*   **Test Isolation is Paramount:** A single `deftest` block shares state across all of its internal `testing` blocks. Because our tests are stateful (they manipulate the filesystem), this caused cascading failures. **The Rule:** Each distinct, stateful test case MUST be in its own `deftest` to ensure the `:each` fixture provides a clean environment.

*   **CLI Parsing is Deceptively Complex:** The `clojure.tools.cli` library has subtle behaviors. We discovered that an unrecognized option (e.g., `--invalid`) is placed in the `:arguments` list, not the `:errors` list. Our solution was to explicitly check if the "command" argument string begins with a hyphen and treat it as an error. We also had to fix a `NullPointerException` caused by control flow falling through after handling `--help`. The logic is now robust, but be wary when extending it.

*   **Centralize Your Parsing Logic:** Our first pass at fixing the LLM cost parsing spread the logic across two functions. This was a design flaw. The key insight was to refactor `parse-llm-response` to accept the *entire HTTP response map*, not just the body. This allowed it to become the single source of truth for interpreting a response, making the code cleaner and more cohesive.

*   **Meticulous Test Assertions Are Non-Negotiable:** We fixed several failures caused by my own sloppy assertions:
    *   **Off-by-one errors:** Forgetting that our `canonicalize-text` function adds a newline (`\n`) to strings, which affects character counts.
    *   **Format mismatches:** Expecting single quotes in an error message when `pr-str` was producing double quotes. Always check the `actual:` output from a failing test with extreme precision.

*   **Ask "What Else Does This Change Affect?":** When we modified `pop/ingest-prompt`, your question "any other tests need updating?" was critical. I had overlooked that we needed to add a test case to `pop.core-test` to cover the new functionality. A change in a core component often requires updates to its direct tests, even if end-to-end tests (like the `vary` test) already cover the new path.

#### 4. Next Steps: Completing the Evolutionary Loop (`v0.2`)

Our immediate and sole goal is to implement the remaining commands to complete the core evolutionary loop. The foundation is solid, and the path ahead is clear.  However, first look for refactorings that would simplify the code and make it easier to reason about how to add this new feature.

1.  **Implement the `evaluate` Command.**
    *   This will likely live in a new `pcrit.command.evaluate` namespace.
    *   **Logic:**
        1.  It must accept command-line arguments, especially `--generation` and `--name`. Refactor `pcrit.cli.main` to support sub-command specific options.
        2.  It will use `expdir` functions to create the contest directory structure (`.../contests/<contest-name>/failter-spec/`).
        3.  Load the specified generation's population with `pop/load-population`.
        4.  Create symlinks from `failter-spec/templates/` to the canonical prompt files and from `failter-spec/inputs/` to user-provided data.
        5.  Shell out to the `failter` command-line tool.
        6.  Place the resulting `report.csv` in the contest directory.

2.  **Implement the `select` Command.**
    *   This will live in a new `pcrit.command.select` namespace.
    *   **Logic:**
        1.  It must accept a `--from-contest` argument to identify which report to use.
        2.  Read and parse the `report.csv` from that contest directory.
        3.  Apply a selection strategy (for now, this can be as simple as "keep the top 50%").
        4.  Use `pop/create-new-generation!` to create a new generation containing only the surviving prompts.

Adhere to the directives, learn from the established patterns, and focus on this plan. I am confident you will be successful.
