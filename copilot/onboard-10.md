### **To My Successor: Onboarding for PromptCritical v0.3 Development**

**File:** `copilot/onboard-10.md`

Welcome to the PromptCritical project. This document supersedes `onboard-9.md`. My primary objective during the last session was to begin the `v0.3` milestone by improving the developer experience and adding an `init` command. This work is now complete.

Your objective is to continue the `v0.3` milestone by refactoring a core piece of the evolutionary logic, which will prepare the system for more advanced features.

#### 1. Prime Directives and Rules of Collaboration

These principles remain the foundation of our workflow.

*   **Read the Manual. Then Read It Again.** An external tool's documentation is a strict contract.
*   **Never Assume API Behavior; Verify Everything.** Before using a function that crosses a boundary (file I/O, parsing), state your assumption and write a test to verify it.
*   **Test Isolation is Paramount.** Each distinct, stateful test case **must** be in its own `deftest` block. `select_test.clj` is the template for this.
*   **Trust, but Verify, Data Boundaries.** When data is deserialized from YAML or CSV, your code must be defensive and handle all possible data shapes from external sources.
*   **A Test's Setup Must Be as Realistic as its Assertions.** A test is only as good as the environment it creates.

#### 2. Hard-Won Lessons & How to Avoid My Mistakes

I made several significant errors during the last session. Understanding them is your best defense against repeating them.

*   **Mistake: Ignoring the Classpath.** My biggest technical failure was forgetting that resources are not automatically visible to the Java classpath. The `init` command tests failed with `Cannot open <nil> as an InputStream` because the `command` component's `deps.edn` was missing `"resources"` in its `:paths` vector.
    *   **Lesson:** If you add a `resources` directory to a component, you **must** update its `deps.edn` to include it on the classpath.
*   **Mistake: Function Name Collision.** I introduced a blocking bug by naming the new `init` command's function `init!`, which collided with an existing application setup function of the same name but with a different arity (number of arguments).
    *   **Lesson:** When adding new functions to a component's public interface, ensure the names are unique. We resolved this by renaming the setup function to `setup!`. Be vigilant about namespacing and interface definitions.
*   **Mistake: The YAML Data Type Cascade.** (From predecessor) The `:selection` field in a prompt's header can be a `map`, a `vector`, or a `set` depending on the YAML parser's output.
    *   **Lesson:** The fix was a defensive `update-fn` that explicitly checks the data type before modifying it. Always anticipate and handle all possible data shapes at component boundaries.

#### 3. Current State & Unwritten Insights

The official documentation is now up-to-date with the `init` command workflow.

*   **The Core Loop is Solid:** The `init` → `bootstrap` → `vary` → `evaluate` → `select` workflow is fully functional.
*   **The CLI is easier to use:** The `bin/pcrit` wrapper script works correctly, providing a much better developer experience.
*   **The `Makefile` is Your Friend:** Use `make test` to run all project tests. Use `make pack` to generate the `pcrit-pack.txt` file before you need to share the project's state. It has been updated to include the new resources.

#### 4. Next Steps: Refactoring Selection Policies (v0.3)

The next milestone is to make the core evolutionary algorithm more extensible. Before implementing the automated `evolve` command, you must perform two key refactorings that were identified by my predecessor. They are critical for the project's long-term health.

**Step 1: Centralize the Default Selection Policy**

The magic string `"top-N=5"` currently exists in both the `select` command (`pcrit.command.select`) and the `cli` base (`pcrit.cli.main`).

*   **Action:** Define this default in one place, such as `pcrit.config.core`. Update both the `select` command and the CLI's `:default` option to read from this central location. This makes changing the default trivial.

**Step 2: Make Selection Policies Pluggable (Your Primary Task)**

The `select` command has a hardcoded `case` statement to parse the `"top-N"` policy. This is not extensible.

*   **Action:** Refactor `pcrit.command.select/apply-selection-policy` to use a Clojure `multimethod`. The dispatch function will parse the policy string and return a keyword (e.g., `"top-N=10"` dispatches to `:top-n`). Create a `defmethod` for `:top-n` containing the current logic. This will allow you and others to add new selection policies (e.g., `:roulette-wheel`, `:tournament-selection`) in the future without modifying the core `select` command's code.

**Step 3: Implement the `evolve` Command**

Once the selection refactoring is complete, you can proceed with the original plan for `v0.3`: implement the new `evolve` command to run the `vary -> evaluate -> select` cycle automatically for a specified number of generations.

When you are finished, please update this document, renaming it to `onboard-11.md`, to ensure a smooth handover to your own successor. I am confident you will succeed.
