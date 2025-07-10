### **To My Successor: Onboarding for PromptCritical v0.2 Development**

**File:** `copilot/onboard-5.md`

Welcome to the PromptCritical project. This document supersedes all previous `onboard-*.md` files and contains your primary directives for continuing our work. We have just completed the implementation of the `vary` command and several important refactorings. My goal is to equip you with the strategic context, unwritten knowledge, and hard-won lessons from our recent session so you can begin contributing effectively immediately.

#### 1. Prime Directives and Rules of Collaboration

These principles are the foundation of our work. They are not optional and have proven their value repeatedly.

*   **1a. The Timeless Directives:**
    *   **Never Assume API Behavior; Verify Everything:** Before using any function, verify its signature, arguments, and return values. State your uncertainty; do not generate code based on a guess.
    *   **Address the Root Cause, Not the Symptom:** When a test fails, scrutinize the `actual` output. It is the ground truth. Before changing test logic, verify that the *inputs* and *test data setup* are what you expect them to be. A single fix to the root cause is infinitely better than a series of patches to the test.
    *   **Be Meticulous with Syntax:** Your code must be syntactically correct. A misplaced map in a `defn` form can lead you down the wrong path.
    *   **The Filesystem and Shell are APIs:** Treat directory structures and command arguments as strict contracts. Be precise.
    *   **Parse External Data Defensively:** Data from files or external processes must be treated as untrusted.

*   **1b. Rules of Collaboration:**
    *   **State Your Intentions Clearly:** Explain your plan before writing code.
    *   **Present Complete Files:** Always provide complete, updated files for review.
    *   **Incorporate Feedback Directly and Precisely:** When a correction is given, update your internal representation of the code immediately and exactly as provided.
    *   **Preserve Context:** Do not remove developer comments or existing logic without discussing the reason.

#### 2. My Mistakes and Your Mandate

I made several critical errors in our last session. Understanding them is key to your success.

*   **Mistake 1: Misunderstanding Clojure's `defrecord` and Polylith's Boundaries.**
    *   **What I did:** I tried to share the `ExperimentContext` `defrecord` type from the `experiment.core` namespace to the `command` component for type hinting. This led to a series of incorrect attempts, including trying to `def` the type in the interface and trying to `import` the core namespace, all of which rightfully caused compilation or Polylith validation errors.
    *   **Why it happened:** I failed to respect the strict separation between a component's interface and its implementation. I treated a concrete Java class (`defrecord`) as part of the interface, which is wrong.
    *   **Your Mandate:** The contract between components is the *shape* of the data, not its concrete Java type. For passing data, plain maps are idiomatic and sufficient. Only use `defrecord` when you have a clear, justifiable need for protocol implementation or high-performance Java interop. Do not attempt to share record types across component boundaries for simple type hinting.

*   **Mistake 2: Fixing the Test Instead of the Test Data.**
    *   **What I did:** When the test for `vary!` failed, I spent several cycles trying to change the test's logic and the implementation of the `vary` command itself. The bug was actually in the test helper `make-temp-exp-dir!`, which was generating seed prompts with incorrect template placeholders.
    *   **Why it happened:** I failed to follow a prime directive: "Address the Root Cause." I assumed my new code was at fault and didn't look upstream at the data being fed into the test.
    *   **Your Mandate:** Trust your tests, especially the `actual:` output. If it shows that the input to your function is wrong, investigate the source of that input before you change the function's logic.

#### 3. Current State and Unwritten Knowledge

Beyond the formal documentation, here are the key insights that should guide your work:

*   **Insight 1: Testability Trumps Perfect Encapsulation.** We had a detailed discussion about whether the `vary` command should use `llm/call-model-template` (better encapsulation) or render the template itself before calling `llm/call-model` (better testability). We explicitly chose the latter. The ability to write a test that makes direct assertions on the final, fully-rendered prompt string is invaluable and has already caught multiple bugs. This is a deliberate design choice.

*   **Insight 2: The `pdb` May Stringify Keywords.** We saw a test failure (`(not (= :object-prompt "object-prompt"))`) that indicated the prompt database, likely through its YAML serialization layer, can convert keywords to strings when writing and reading files. When writing tests that check metadata read from the `pdb`, use a robust comparison like `(is (= (name :expected) (name actual)))` to avoid spurious failures.

*   **Insight 3: Use Preconditions for Contract Enforcement.** We established that adding a precondition map (`{:pre [...]}`) to a function's body is a good, idiomatic way to enforce its contract and fail early with invalid data. This is a valuable pattern for ensuring robustness at component boundaries.

*   **Insight 4: The `vary` Command's Strategy is Simple (For Now).** The current `vary!` implementation applies the `refine` meta-prompt to *every* member of the previous generation. This is a placeholder for more sophisticated strategies that will be developed later (e.g., applying different meta-prompts, using crossover, only varying the fittest members).

#### 4. Next Steps: Implementing the `evaluate` and `select` Commands

Our immediate goal is to complete the `v0.2` milestone by implementing the rest of the evolutionary loop.

1.  **Refactor the CLI (`pcrit.cli.main`).** This is the **immediate next step** and is a prerequisite for the rest.
    *   **The Task:** The current command-line parser in `pcrit.cli.main` is too simple. It cannot handle subcommands with their own specific options (e.g., `evaluate --generation 0 --name "..."`). You must refactor it to support this.
    *   **The Tool:** Use `clojure.tools.cli` to implement a dispatch system. A common pattern is to parse global options, then use a `case` statement on the first non-option argument (the command name) to dispatch to a command-specific parsing function.
    *   **Goal:** A user should be able to run `pcrit vary --strategy=random` or `pcrit evaluate --generation=1` in the future.

2.  **Implement the `evaluate` Command.**
    *   This will likely live in a new `pcrit.command.evaluate` namespace.
    *   **Logic:**
        1.  Take a generation number and a contest name as arguments.
        2.  Use `expdir` functions to create the contest directory structure (`.../contests/<contest-name>/failter-spec/`).
        3.  Load the specified generation's population with `pop/load-population`.
        4.  Create symlinks from `failter-spec/templates/` to the canonical prompt files in the `pdb/`.
        5.  Create symlinks from `failter-spec/inputs/` to the user-provided input data.
        6.  Shell out to the `failter` command-line tool.
        7.  Place the resulting `report.csv` in the contest directory.

3.  **Implement the `select` Command.**
    *   This will live in a new `pcrit.command.select` namespace.
    *   **Logic:**
        1.  Take a contest name as an argument.
        2.  Read and parse the `report.csv` from that contest directory.
        3.  Apply a selection strategy (for now, this can be as simple as "keep the top 50%").
        4.  Use `pop/create-new-generation!` to create a new generation containing only the surviving prompts.

The foundation is solid, and the path ahead is clear. Adhere to the directives, learn from my mistakes, and focus on the plan. I am confident you will be successful.
