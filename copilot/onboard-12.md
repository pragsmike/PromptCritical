### **To My Successor: Onboarding for PromptCritical v0.3 Development**

**File:** `copilot/onboard-12.md`

Welcome to the PromptCritical project. This document supersedes `onboard-11.md`. The primary objectives of the last session were to build a robust `init` command, fix several deep-seated bugs in the CLI and logging systems, and implement a sophisticated cost-accounting mechanism. This work is now complete.

Your objective is to complete the `v0.3` milestone by implementing the final planned feature: a high-level `evolve` command that automates the evolutionary loop, making use of the new cost-accounting system.

#### 1. Prime Directives and Rules of Collaboration

These principles are the foundation of our workflow. They are not suggestions; they are rules that prevent regressions and maintain velocity.

*   **Read the Manual. Then Read It Again:** An external tool's documentation (like `clojure.tools.cli` or `clj-yaml`) is a strict contract. The simplest code that directly implements the documented behavior is always the right code.
*   **Never Assume API Behavior; Verify Everything:** Before using a function that crosses a boundary (file I/O, parsing, network calls), state your assumption and write a small test to verify it.
*   **Test Isolation is Paramount:** Each distinct, stateful test case **must** be in its own `deftest` block. This prevents state from one test from invisibly corrupting the next.
*   **Respect Component Boundaries:** A component should be "dumb" about the data it handles. The `pdb` component's job is to read and write files, not to understand the application's data schema. The `llm` component's job is to talk to an API, not to know how its output will be used in a selection strategy. Keep responsibilities narrow and well-defined.
*   **The Invocation Layer Owns the Environment:** The application's core logic should not know or care how it was launched. The `bin/pcrit` wrapper script is responsible for setting up the execution context (like the working directory). The `cli.main` namespace is responsible for interpreting that context. The core command functions should receive unambiguous, absolute paths.
*   **Distinguish User Output from Developer Logs:** User-facing results, confirmation messages, and testable error conditions should be printed to standard output with `println`. Diagnostic information, warnings, and internal errors useful for debugging should use the logging framework (`log/info`, `log/error`, etc.).

#### 2. Hard-Won Lessons & How to Avoid My Mistakes

I made several significant and repeated errors during the last session. Understanding them is your best defense against repeating them.

*   **Mistake: The Current Working Directory (CWD) Bug.** This was my most persistent failure.
    *   **What Happened:** I struggled to make the `pcrit` script run commands on relative paths (e.g., `pcrit init work`) correctly. My first attempts to use `-Duser.dir` were flawed and led to dangerous, inconsistent behavior where the application logged one path but wrote to another.
    *   **Lesson:** The JVM's working directory is not a reliable mechanism for this kind of application. The correct, robust pattern we implemented is **explicit context passing**. The wrapper script captures the user's true CWD and passes it as a special argument (`--pcrit-user-cwd`). The `cli.main` namespace is responsible for using this value to resolve all relative file paths into absolute paths *before* passing them to the core command functions. This is the only way to guarantee correct behavior.

*   **Mistake: Brittle CLI Parsing Logic.** I submitted multiple versions of `cli.main` that failed with obscure errors like `Unknown option: "--log"` and `AssertionError ... distinct?*`.
    *   **What Happened:** I did not fully understand how `clojure.tools.cli` handles multiple parsing passes for global and command-specific options. My logic failed to correctly combine the option specifications, leading to the parser rejecting valid flags.
    *   **Lesson:** The final, correct implementation in `cli.main` performs two passes. The key is that the *second* pass must be given a list of *all valid options* for that context (the command's own options merged with the global options). Study the `process-cli-args` function carefully; it is now correct and robust.

*   **Mistake: The Keyword vs. String Bug.** A test for the `vary` command failed because it expected a keyword (`:openai`) from a prompt header but received a string (`"openai"`).
    *   **What Happened:** My first instinct was to "fix" this by hacking the `pdb` component to transform the data on read. This was wrong. It violated the component's responsibility.
    *   **Lesson:** The YAML serialization process naturally turns keywords into strings. The entity that reads the data from disk and uses it (in this case, the test) is responsible for handling the data in the format it *actually exists in*. We correctly fixed the test to expect a string. Do not add application-specific logic to the persistence layer.

#### 3. Current State & Unwritten Insights

The official documentation is up-to-date. Here are some insights that are not explicitly recorded elsewhere.

*   **The Provide-Evidence Debugging Loop:** The most effective technique we used was for you to provide the raw, unedited output of a failing command (`ls -lR`, full stack traces, log files). This evidence was always the key to pinpointing the bug. We should continue this practice. The `--log=debug` flag is your most powerful tool for generating this evidence.
*   **`make pack` is Our Shared Context:** The `Makefile`'s `pack` command is essential. It assembles all relevant source code, documentation, and resources into a single file. Before asking me to review or debug, always run `make pack` to ensure my context is identical to yours.
*   **The `init` command is the new entry point.** Always start a new test or experiment with `pcrit init ...` followed by `pcrit bootstrap ...`.

#### 4. The Cost-Accounting Philosophy

This is a critical architectural decision we just made, and understanding the "why" is essential.

*   **The Principle:** Store immutable facts inside the prompt; compute derived views (like money) outside of it.
*   **Why:** The price of an LLM call in USD is not an immutable fact; it changes over time. Storing `cost: 0.0015` in a prompt header is a bug waiting to happen. In a year, that value will be meaningless without knowing the price table from that day.
*   **Our Implementation:**
    1.  We store the **immutable facts**: `:model`, `:provider`, `:token-in`, `:token-out`, and `:duration-ms`. These will be true forever.
    2.  As a pragmatic compromise for immediate user feedback, we also store `:cost-usd-snapshot`. The name explicitly states its nature: it's a snapshot of the cost in USD at the moment of creation.
    3.  For true, long-term historical analysis, any reporting tool we build *must* use the token counts and a historical price table to calculate cost, not the snapshot value.
*   **Risk of Double-Counting:** Be aware that a prompt's cost metadata only reflects the cost of *its own generation step*. The total cost to arrive at a prompt is the sum of its own cost plus the costs of all its ancestors. When building a future analysis tool (like a web dashboard), a naive `SUM(cost)` over all prompts in a generation will be wrong. You must perform a **graph traversal (like a DFS) of the prompt ancestry DAG with a visited set** to correctly sum the costs without double-counting shared parents.

#### 5. Next Steps: Implement the `evolve` Command (v0.3)

With all prerequisites now in place, your task is to implement the automated `evolve` command.

1.  **Create `pcrit.command.evolve.clj`:** This new namespace will house the core `evolve!` function.
2.  **Implement `evolve!` Logic:**
    *   It must accept options for controlling the evolution, primarily `--generations N` and `--max-cost N.NN`.
    *   It will loop, executing `cmd/vary!`, `cmd/evaluate!`, and `cmd/select!` in sequence.
    *   **It must track cumulative cost.** After each `vary` and `evaluate` step, it must aggregate the costs and check against the `--max-cost` budget. If the budget is exceeded, the loop must halt gracefully. You will need to parse the `report.csv` from the `evaluate` step to get its cost. The `vary` command will need to be updated to return the total cost of the variations it created.
    *   It must dynamically generate a unique contest name for each `evaluate!` call within the loop to avoid collisions. A pattern like `evolve-gen-1-run-<timestamp>` would be effective.
3.  **Integrate with the CLI:** Wire the new command into `pcrit.cli.main` with the necessary options.
4.  **Write Tests:** Create `pcrit.command.evolve_test.clj`. The test should focus on the looping and budget-checking logic. Mocking the underlying commands (`vary!`, `evaluate!`, `select!`) will be the most effective strategy to keep the test fast and reliable.

When you are finished, please update this document, renaming it to `onboard-13.md`, to ensure a smooth handover. Good luck.
