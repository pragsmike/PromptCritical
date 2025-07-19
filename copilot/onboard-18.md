### **To My Successor: Onboarding for PromptCritical v0.5 Development**

**File:** `copilot/onboard-18.md`

Welcome to the PromptCritical project. This document supersedes `onboard-17.md`. The previous session's objective was to execute a critical prerequisite refactoring before beginning the v0.5 milestone. This was a complete success.

We have now finished two major architectural improvements:
1.  **Centralized Report Parsing:** A new `pcrit.results` component is now the single source of truth for parsing `failter-report.json`.
2.  **Extracted `pcrit-llm` Library:** All LLM interaction and cost calculation logic has been moved to a new, standalone, and fully tested library, `pcrit-llm`.

`PromptCritical` is now fully decoupled from the mechanics of LLM communication, making the core project significantly simpler and easier to reason about. With this robust foundation in place, your objective is to implement the main feature for the `v0.5` milestone: **the Surrogate Critic**.

#### 1. Prime Directives and Rules of Collaboration

These principles are non-negotiable and have been proven essential to our success.

*   **A Passing Test Suite is the Only Definition of Done:** We learned this the hard way. Do not assume a task is complete. Do not proceed to the next step. Run the full test suite after every significant change and do not continue until it passes.
*   **Fix the Test, Not Just the Code:** A failing test can indicate a bug in the code or a flaw in the test's assumptions. In our last session, a `stats` test failed because its mock data was unrealistic. The fix was to make the test more accurately reflect the real-world data flow, which in turn verified the code's correctness.
*   **Respect Architectural Layers:** Low-level components must never depend on high-level ones. The dependency graph should be a directed, acyclic graph.
*   **A Test Helper Creates Preconditions, Not Results:** A helper's sole job is to create the *minimal valid state* required for the function-under-test to run.
*   **Format for the UI:** When emitting code blocks, always ensure there is a trailing newline *inside* the block for file correctness, and a newline *after* the closing ` ``` ` to prevent UI rendering glitches.

#### 2. Hard-Won Lessons & How to Avoid My Mistakes

The refactoring process was highly instructive and revealed several potential pitfalls that you must avoid.

*   **Mistake: Forgetting to Update the Interface:** The most frequent source of errors was changing a `core.clj` file but forgetting to update its corresponding `interface.clj` file. For example, I removed `price-table` from `config/core.clj` but left it in `config/interface.clj`, which caused a downstream compilation failure. **Lesson:** When you change a component's implementation, always check if its public interface needs to change as well.
*   **Mistake: Incorrect Relative Paths:** I introduced a bug by providing an incorrect relative path (`../../`) in a `deps.edn` file. This highlights the fragility of local, cross-project dependencies. **Lesson:** Double-check relative paths in dependency definitions. A single `../` in the wrong place can break the entire build.
*   **Mistake: Assuming a Clean Test Run:** I repeatedly made the error of moving on to the next step before verifying that the tests passed. This is inefficient and violates our Prime Directive. **Lesson:** Trust, but verify. After every change, run the tests. The feedback loop is your most valuable tool.

#### 3. Current State & Unwritten Insights

*   **Clean Separation of Concerns:** The project now has a beautiful architectural boundary. `PromptCritical` manages the evolutionary process; `pcrit-llm` handles the external communication. This makes both projects easier to understand, test, and maintain.
*   **Just-in-Time Cost Calculation:** The `stats` command now calculates cost on-the-fly. This is a robust design, as it ensures that cost reporting will always use the latest pricing information from the `pcrit-llm` library, even when analyzing historical contest data.

#### 4. Next Steps: The Road to v0.5

Your mission is to implement the Surrogate Critic. This feature is designed to reduce the cost of experiments by pre-filtering a large population of generated prompts *before* they are sent to the expensive `Failter` evaluation step.

##### **Task 1: Create the `critic` Component**

1.  Create a new Polylith component: `pcrit.critic`.
2.  Define its interface in `components/critic/src/pcrit/critic/interface.clj`. It should expose a single function:
    ```clojure
    (critic/filter-population [ctx population])
    ```
    This function will take the experiment context and a sequence of prompt records, and it must return a new, smaller sequence of prompt records that have survived the filtering.

##### **Task 2: Implement a Simple Rule-Based Critic**

1.  In `components/critic/src/pcrit/critic/core.clj`, implement the `filter-population` function.
2.  For this initial version, the critic should be simple and rule-based. It should log which prompts are being filtered and why. Good starting rules would be to filter out any prompt that:
    *   Is an `:object-prompt` but is missing the `{{INPUT_TEXT}}` template variable.
    *   Has a `:word-count` less than a reasonable minimum (e.g., 5).
    *   Has a `:character-count` greater than a reasonable maximum (e.g., 2000).

##### **Task 3: Integrate the Critic into the `evolve` Loop**

1.  Modify the `pcrit.command.evolve` component.
2.  In the main `evolve!` loop, add a new "Critic" step that runs **between** the `vary!` and `evaluate!` steps.
3.  The output population from `vary!` will now be piped into `critic/filter-population`, and the result of that filtering will be what gets passed to `evaluate!`.
4.  Ensure that the `evolve` loop logs how many candidates were filtered by the critic in each generation.

This work will complete the v0.5 milestone. When you are finished, please update this document, renaming it to `onboard-19.md`. Good luck.
