### **To My Successor: Onboarding for PromptCritical v0.2**

Welcome to the PromptCritical project. I have just completed the tasks laid out by my predecessor in `onboard-1.md`. The foundational `pcrit.pdb` library has been successfully refactored and significantly hardened. The codebase is stable, the test suite is comprehensive, and the system is now ready for the next stage of development.

This document supersedes `onboard-1.md`. Your goal is to build upon this solid foundation to deliver the project's next key capability: a vertical slice that integrates with an external evaluation tool.

The codebase uses the Polylith organization for Clojure code. Use your web
search tool to read about that. Be aware that Polylith conventions for python
code are slightly different than for Clojure. Your predecessors have been
confused about that.

Beware that some documents may refer to the old, non-Polylith organization.  If you spot such a discrepancy, tell me.

### 1. Current State: A Robust Foundation

The previous development cycle was highly successful. We not only refactored the monolithic `pcrit.pdb` namespace but also addressed a comprehensive list of specification mismatches and potential bugs. As a result, the library is now more robust, reliable, and easier to reason about.

Here are the key improvements you can now rely on:

*   **Clear Separation of Concerns:** The original `pcrit.pdb` has been decomposed into `pcrit.pdb.core`, `pcrit.pdb.io`, `pcrit.pdb.id`, and `pcrit.pdb.lock`.
*   **Guaranteed Atomic Writes:** All file operations (prompt creation, metadata updates, and ID counter updates) use a centralized atomic write protocol that prevents data corruption from partial writes.
*   **Full Spec Conformance:** The code now correctly enforces canonical text representation (exactly one trailing newline), handles header parsing robustly, and ensures data durability with `fsync` where necessary.
*   **Explicit Data Contracts:** We now explicitly pass `:keywords true` to the YAML parser and `:sort-keys true` when writing, ensuring consistent data structures and stable file outputs for version control.
*   **Comprehensive Tests:** The test suite has been expanded with specific, targeted tests for the spec-conformance fixes, providing a strong safety net against future regressions.

*   **Population control** The `pcrit.pop` namespace has begun to be filled in.  It defines the data structures of
an evolutionary experiment, and has begun implementing the population bootstrap phase of an experiment.

The foundation is solid. Your task is to build on it.

### 2. The Next Challenge: System-Level Integration

The project's focus now shifts from internal library design to **external system
integration**. As the `README.md` outlines, the next milestone (v0.2) is to
create a complete "bootstrap -> contest -> record" loop using the external **Failter**
tool.

This introduces new categories of engineering challenges. While the foundational "Lessons Learned" from the previous onboarding document still apply, you must now master these new principles:

**Lesson 1: The Shell is a Foreign Country. Verify its Behavior.**
*   **The Challenge:** You will need to use `clojure.java.shell/sh` to execute Failter. This is not a simple function call. You must be meticulous about its arguments, working directory (`:dir`), environment variables, and especially its return value.
*   **Your Mandate:** Before writing the shell-out code, verify the exact command-line arguments Failter expects. After the call, you **must** check the `:exit` code. A non-zero exit code signifies an error that must be logged and handled, not ignored. You must also handle `:out` and `:err` streams correctly.

**Lesson 2: The Filesystem is an API. Be Precise.**
*   **The Challenge:** The `run-failter` command needs to create a directory structure that Failter can understand (`inputs/`, `templates/`, etc.).
*   **Your Mandate:** Treat this directory structure as a strict API contract. Do not guess paths. Use `clojure.java.io/file` to construct paths reliably. Ensure that the correct prompt bodies are written to the correct files in the correct locations. A mistake here will cause the external tool to fail in ways that can be difficult to debug.

**Lesson 3: Parse External Data Defensively.**
*   **The Challenge:** The final step is to ingest Failter's `report.csv`. This file is produced by an external system, and its format, while specified, should be treated as untrusted.
*   **Your Mandate:** Use a proper CSV parsing library (`clojure-csv/clojure-csv` is already in `deps.edn`). Your parsing logic must be robust. It should handle potential errors gracefully (e.g., missing columns, incorrect data types) and log clear warnings or errors when it encounters malformed data. Do not assume the CSV will always be perfect.

### 3. Your First Task: Implement the v0.2 Milestone

Your immediate goal is to implement the three new CLI commands outlined in the
`README.md`. I recommend creating new namespaces to house this logic.

**Proposed Plan:**

1.  **Create `pcrit.pop.core`:**
    *   This namespace will contain the logic for the `pcrit bootstrap <file>` command.
    *   It ingests a manifest file that specifies a set of prompts, and ingests
    them by calling `pcrit.pdb.core/create-prompt` for each to create a
    population of seed prompts. n

2.  **Create `pcrit.pop.contest`:**
    *   This will contain the logic for the `pcrit contest [options]` command.
    *   It will be responsible for:
        *   Parsing command-line arguments (prompt IDs, input paths, etc.).
        *   Creating the temporary experiment directory for Failter.
        *   Reading the specified prompts from the database using `pdb/read-prompt`.
        *   Writing the necessary files into the Failter directory structure.
        *   Executing the `failter` command-line tool using `clojure.java.shell/sh` and handling its output.

3.  **Create `pcrit.pop.record`:**
    *   This will contain the logic to parse the `report.csv` file generated by Failter.
        It will update the generation's contest record.

4.  **Update `pcrit.cli.main`:**
    *   The `-main` function will need to be updated to dispatch these new
        commands (`bootstrap`, `contest`) and their associated command-line options.

By completing this vertical slice, you will have connected the PromptCritical database to the outside world, proving the viability of the entire evolutionary loop. The foundation we have built is strong. Now, build upon it. Good luck.
