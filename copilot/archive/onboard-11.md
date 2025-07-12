### **To My Successor: Onboarding for PromptCritical v0.3 Development**

**File:** `copilot/onboard-11.md`

Welcome to the PromptCritical project. This document supersedes `onboard-10.md`. Our primary objectives during the last session were to refactor the selection policy mechanism and to add cost-reporting capabilities. This work is now complete.

Your objective is to complete the `v0.3` milestone by implementing the final planned feature: a high-level `evolve` command that automates the evolutionary loop.

#### 1. Prime Directives and Rules of Collaboration

These principles remain the foundation of our workflow.

*   **Read the Manual. Then Read It Again.** An external tool's documentation is a strict contract.
*   **Never Assume API Behavior; Verify Everything.** Before using a function that crosses a boundary (file I/O, parsing), state your assumption and write a test to verify it.
*   **Test Isolation is Paramount.** Each distinct, stateful test case **must** be in its own `deftest` block. `select_test.clj` is the template for this.
*   **Trust, but Verify, Data Boundaries.** When data is deserialized from YAML or CSV, your code must be defensive and handle all possible data shapes from external sources.
*   **A Test's Setup Must Be as Realistic as its Assertions.** A test is only as good as the environment it creates.

#### 2. Hard-Won Lessons & How to Avoid My Mistakes

I made several significant errors during the last session. Understanding them is your best defense against repeating them.

*   **Mistake: Confusing Logs and User Output.** My initial test for the `stats` command failed because it used `with-out-str` to capture output, but the command was writing its error/info messages using the `log` macros. `with-out-str` only captures standard output (`println`), not messages sent to the logging system.
    *   **Lesson:** Be deliberate about a command's output channels. User-facing results and messages that need to be tested for presence should be sent to standard output via `println`. Diagnostic information intended for developers should use the logging framework (`log/info`, `log/error`, etc.).
*   **Mistake: Ignoring the Classpath.** (From predecessor) The `init` command tests failed with `Cannot open <nil> as an InputStream` because the `command` component's `deps.edn` was missing `"resources"` in its `:paths` vector.
    *   **Lesson:** If you add a `resources` directory to a component, you **must** update its `deps.edn` to include it on the classpath.
*   **Mistake: Function Name Collision.** (From predecessor) A blocking bug was introduced by naming the `init` command's function `init!`, which collided with an existing application setup function of the same name.
    *   **Lesson:** When adding new functions to a component's public interface, ensure the names are unique. The collision was resolved by renaming the setup function to `setup!`.

#### 3. Current State & Unwritten Insights

The official documentation is now up-to-date with the `init` and `stats` commands.

*   **Cost Reporting is Implemented:** The `evaluate` command now logs the total cost of a contest run upon completion. A new `pcrit stats` command is also available for detailed analysis of the cost and scores of any completed contest or generation.
*   **Selection is Pluggable:** The `select` command has been refactored to use a `multimethod`, making it easy to add new selection strategies in the future without modifying the core command.
*   **The Core Loop is Solid:** The `init` → `bootstrap` → `vary` → `evaluate` → `select` workflow is fully functional.
*   **The `Makefile` is Your Friend:** Use `make test` to run all project tests. Use `make pack` to generate the `pcrit-pack.txt` file before you need to share the project's state.

#### 4. Next Steps: Implement the `evolve` Command (v0.3)

The final task for this milestone is to automate the core loop. This will be a new top-level command that composes the existing ones.

1.  **Create `pcrit.command.evolve.clj`:** This new namespace will house the `evolve!` function.
2.  **Implement `evolve!` Logic:**
    *   The function will take the experiment context and a map of options, primarily `{:generations N}` to specify the number of cycles to run.
    *   It will loop `N` times. Inside the loop, it must call `cmd/vary!`, `cmd/evaluate!`, and `cmd/select!` in sequence.
    *   A key challenge will be dynamically generating a unique contest name for each `evaluate!` step within the loop (e.g., `evolve-run-1`, `evolve-run-2`). The timestamp or generation number would be good candidates for this.
3.  **Integrate with the CLI:**
    *   Add the `evolve` command to the `command-specs` map in `pcrit.cli.main`.
    *   It will need options like `--generations N`, `--inputs DIR`, and likely an initial contest name pattern.
4.  **Write Tests:** Create `pcrit.command.evolve_test.clj`. The test should bootstrap an experiment, run `evolve!` for 2-3 generations, and then assert that the correct number of generation directories exist and that the population has been correctly winnowed at each step. Mocking the underlying `vary!`, `evaluate!`, and `select!` commands is a good strategy to keep the test fast and focused on the looping logic.

When you are finished, please update this document, renaming it to `onboard-12.md`, to ensure a smooth handover to your own successor.
