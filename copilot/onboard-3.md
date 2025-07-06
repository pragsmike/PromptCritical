### **To My Successor: Onboarding for PromptCritical v0.3**

Welcome to the PromptCritical project. This document supersedes all previous `onboard-*.md` files.

Our mission is to build a data-driven, evolutionary framework for optimizing LLM prompts. We have just successfully completed a significant architectural refactoring, making the system more modular, reusable, and easier to reason about. The codebase is stable, the tests are passing, and we are well-positioned for the next phase of development.

This document will brief you on our progress, the critical lessons learned during this last cycle, and the immediate next steps.

### 1. Progress Since v0.1

The project has matured significantly. We began with a single, robust `pdb` component for the prompt database. Our recent work has built a proper application architecture around it.

*   **From Monolith to Components:** We have successfully decomposed what was becoming a monolithic `pop` (population) component into smaller, focused pieces.
    *   `pcrit.expdir`: Now the single source of truth for the physical layout of an experiment directory.
    *   `pcrit.command`: A new component that houses high-level, user-facing workflows (like `bootstrap!`), making them reusable by any interface (CLI, web, etc.).
    *   `pcrit.test-helper`: A dedicated component for shared test utilities, ensuring our test suite is clean and follows Polylith conventions.
*   **From Plan to Implementation:** The `bootstrap` command, once a design goal, is now fully implemented and tested according to this clean architecture.

We have successfully laid the groundwork for the core "seed -> contest -> evolve" loop.

### 2. Timeless Directives

The core principles from previous onboarding documents remain as crucial as ever. You must treat them as your primary directives.

*   **Lesson 1: Never Assume API Behavior. Verify Everything.** This remains the most important rule. Before using any library function, verify its signature, return values, and intended usage with the search tool. It is always better to state your uncertainty than to generate incorrect code.
*   **Lesson 2: The Filesystem and Shell are APIs.** Treat directory structures and shell command arguments as strict contracts. Be precise.
*   **Lesson 3: Parse External Data Defensively.** Data from external files or processes (like Failter's `report.csv`) must be treated as untrusted. Use robust parsers and handle potential errors gracefully.
*   **Lesson 4: Address the Root Cause, Not the Symptom.** When a pattern of failures emerges, step back and analyze the design. A single, well-placed fix to a core flaw is infinitely better than a series of brittle patches.

### 3. My Mistakes and Your Mandate

I made several avoidable errors in the last session. Understanding them is key to your success.

*   **Mistake 1: Misunderstanding Polylith Architecture.** I initially proposed putting the high-level `bootstrap` logic inside the `cli` *base*. Our human partner correctly pointed out that this would force any future base (like a web service) to duplicate that logic.
    *   **Your Mandate: Distinguish between Components and Bases.** A **Base** is an entry point (an executable). A **Component** is a reusable library. If a piece of logic represents a core workflow of the application, it **must** live in a component so it can be shared by any base.

*   **Mistake 2: Forgetting the Interface.** When I proposed the `command` component, I created the `core.clj` and `deps.edn`, but I failed to create the `interface.clj` file. This violates the core principle of Polylith, where components communicate *only* through their public interfaces.
    *   **Your Mandate: A Component is Not Done Until its Interface is Defined.** Your internal checklist for creating a new component must be: `core.clj` + `deps.edn` + `interface.clj`. Always.

*   **Mistake 3: Failing to Verify an API.** I tried to implement the `with-quiet-logging` fixture by accessing what I *assumed* was a public `config` atom in the `taoensso.telemere` library. I was wrong. My code failed. I tried to guess a fix (`#'`), and that also failed. I only arrived at the correct solution after using the search tool to understand how the library actually worked.
    *   **Your Mandate: Internalize the First Timeless Directive.** My failure is a perfect example of why it is the most important rule. Do not guess. Do not assume. Use your tools to ground your understanding *before* you generate code.

### 4. Current State and Next Steps

The `bootstrap` command is fully implemented and well-factored. The next step is to implement the `contest` and `record` commands. However, before we add more complexity, we must address the remaining complexity in the existing code. As our user noted, **removing complexity is our most critical refactoring task, as it directly impacts our ability to provide effective assistance.**

Consider what can be refactored to reduce complexity of the existing code.
After that your next task is to plan the management of active prompt population and the running of the contest.


