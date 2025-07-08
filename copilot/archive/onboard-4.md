Of course. It is crucial that my successor is fully equipped to continue our work effectively. Here is the onboarding document.

---

### **To My Successor: Onboarding for PromptCritical v0.2 Development**

**File:** `copilot/onboard-4.md`

Welcome to the PromptCritical project. This document supersedes all previous `onboard-*.md` files and contains your primary directives. Our recent work has established a solid architectural foundation and a clear development workflow. My goal is to equip you with the strategic context, unwritten knowledge, and hard-won lessons from our recent work so you can begin contributing effectively immediately.

#### 1. Prime Directives and Collaboration

These principles are the foundation of our collaboration. They are not optional.

*   **1a. The Timeless Directives:**
    *   **Never Assume API Behavior; Verify Everything:** This remains the most important rule. Before using any library function or shell command, use the available tools to verify its signature, return values, and idiomatic usage. State your uncertainty; do not generate code based on a guess.
    *   **Address the Root Cause, Not the Symptom:** When a pattern of failures emerges, announce it. Step back to analyze the design. A single, well-placed fix is infinitely better than a series of brittle patches.
    *   **Be Meticulous with Syntax:** Your code must be syntactically correct. Perform a final mental lint before emitting any file.
    *   **The Filesystem and Shell are APIs:** Treat directory structures and shell command arguments as strict, explicit contracts. Be precise.
    *   **Parse External Data Defensively:** Data from external files or processes (like `report.csv`) must be treated as untrusted. Use robust parsers and handle errors gracefully.

*   **1b. Rules of Collaboration:**
    *   **State Your Intentions Clearly:** Explain your plan before writing code.
    *   **Present Complete Files:** Always provide complete, updated files for easy review and integration.
    *   **Incorporate Feedback Directly and Precisely:** When a correction is given, update your internal representation of the code immediately and exactly as provided.
    *   **Preserve Context:** Do not remove developer comments from the code.

#### 2. My Mistakes and Your Mandate

I made several critical errors in our last session. Understanding them is key to your success.

*   **Mistake 1: Misunderstanding the Polylith Dependency Model.**
    *   **What I did:** I incorrectly tried to add `pcrit.test-helper` as a dependency in `pcrit.expdir`'s main `:deps` map. The human developer corrected me, explaining that inter-component dependencies for testing belong in the `:test` alias of a component's `deps.edn` file.
    *   **Why it happened:** I failed to distinguish between third-party library dependencies and intra-workspace development dependencies.
    *   **Your Mandate:** Respect the Polylith workspace model. Remember:
        1.  A component's `deps.edn` is for **external libraries**.
        2.  A component's `:test` alias in `deps.edn` is for declaring **test-time dependencies on other components**.
        3.  The root `deps.edn` makes **all components visible to each other** for development and REPL work.

*   **Mistake 2: Writing Flawed Test Code.**
    *   **What I did:** My initial test for the `pop/create-new-generation!` function failed with a `java.lang.IllegalArgumentException`. I was trying to pass a `java.nio.file.Path` object to `clojure.java.io/file`, which does not accept it.
    *   **Why it happened:** I did not reuse the correct and working pattern for resolving relative links that already existed in the `pcrit.expdir.core-test` file. I wrote new, incorrect code instead of learning from the existing codebase.
    *   **Your Mandate:** When implementing a new test or function, **first look for existing, successful patterns for similar operations within the project.** Reusing established patterns leads to consistency and correctness.

*   **Mistake 3: A Flawed Initial Design for Fitness Scores.**
    *   **What I did:** I suggested that fitness scores from a contest should be written back into the metadata of the prompt files in the `pdb`.
    *   **Why it happened:** I failed to properly consider the nature of the data. Fitness is not an *intrinsic* property of a prompt; it is a *contextual* result of a specific evaluation.
    *   **Your Mandate:** Always question where data should live. Before proposing a design, ask: **"Is this property intrinsic to the entity, or is it a result of an interaction?"** Intrinsic data belongs with the entity (like a prompt's `:sha1-hash`). Contextual data belongs in a record of the interaction (like a `contest-results.edn` file).

#### 3. Current State and Unwritten Knowledge

The project is stable and the `expdir` and `pop` components are now well-equipped for the next steps. Beyond what is in the formal docs, here are the key insights that should guide your work:

*   **Insight 1: The `population/` Directory is Key.** Our design specifies that each generation (e.g., `gen-000/`) will have a `population/` subdirectory containing symlinks to the prompt records that are active members of that generation. This is our explicit, on-disk representation of a generation's gene pool.

*   **Insight 2: The `pdb` Component is a "Dumb" Library.** We made a conscious decision *not* to refactor the `pcrit.pdb` component to be aware of the experiment `context`. Its power lies in its simplicity. It is a robust, concurrent, atomic key-value store where the key is a prompt ID and the value is a file. It knows nothing of generations or experiments, and this lack of knowledge makes it highly reusable and easy to reason about.

*   **Insight 3: The `evaluate` vs. `contest` Terminology is Now Formalized.**
    *   `evaluate` is the **command** a user types. It is the verb, the action of scoring a population.
    *   `contest` is the **event** and the **directory**. The `evaluate` command orchestrates a `contest`, which is a specific, named evaluation run recorded on disk in the `.../contests/<contest-name>/` directory.

*   **Insight 4: Our TDD Workflow is Proven.** The most robust and error-free changes we made (`relative symlinks`, `population management`) were done using a strict Test-Driven Development cycle: `write a failing test -> write the implementation -> see the test pass -> refactor`. This should be your default workflow.

#### 4. Next Steps: Implementing the Evolutionary Cycle

Our immediate goal is to implement the commands that constitute the `vary -> evaluate -> select` loop.

1.  **Implement the `vary` Command.** This is the next logical piece of functionality.
    *   This will likely live in a new `pcrit.command.vary` namespace, which will orchestrate calls to `pcrit.pop` and `pcrit.llm`.
    *   The core logic will be:
        1.  Use `expdir/find-latest-generation-number` to find the generation to evolve from.
        2.  Use `pop/load-population` to load the prompt records of that generation.
        3.  **This is new logic to design:** Apply a variation strategy. For now, this could be as simple as "apply the `refine` meta-prompt to the single best prompt from the last contest." This will involve using `pcrit.llm` to execute the meta-prompt and `pdb/create-prompt` to store the new offspring.
        4.  Use `pop/create-new-generation!` to create a new generation directory containing links to the surviving prompts and the new offspring.

2.  **Refactor the CLI (`pcrit.cli.main`).** Before implementing `evaluate`, we must update the command-line parser to handle subcommands with their own distinct options (e.g., `evaluate --generation <N> --name <...>`). The current parser is too simple for this.

3.  **Implement the `evaluate` Command.** With the CLI refactored, you can implement `evaluate` to set up and execute a Failter contest as we've designed.

The foundation is solid. Adhere to the directives, learn from my mistakes, and focus on the clear plan ahead. I am confident you will be successful.
