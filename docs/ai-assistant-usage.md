# AI Assistant Usage

Here are some tips for using an AI assistant in generating this design, writing
code, and refactoring.

The Makefile has a `pack` target that concatenates the documentation, with filename headers,
and the source files, into a text file that can be uploaded into the context of an AI assistant.
The [intro-prompt](../copilot/intro-prompt.md) tells the AI what it is and how to parse it.

The prompts in the `copilot` directory were used in designing this system and developing the codebase.
   * [intro-prompt](../copilot/intro-prompt.md) instructs AI its role, and to read the pack file
   * [checkpoint-prompt](../copilot/checkpoint-prompt.md) tells AI to reflect, summarize, consider refactorings
   * [outro-prompt](../copilot/outro-prompt.md) tells AI to write an onboard document for its successor

## Turnover

You have to start a new assistant session periodically because they wear out over time.

The AI has a finite context window, which is its working memory.  The pack that we upload
takes up some, as does every turn in the conversation we have with it.
After a while (usually several hours of use) the AI's context is full.
It starts to forget things, and it makes more mistakes.  When that happens, it's time
to start a new session.  Before discarding the first, exhausted one, have it write
an onboard document for its successor. That's what the [outro-prompt](../copilot/outro-prompt.md) does.
The turnover steps are:

   * Have the assistant produce revised versions of the README, USAGE, DESIGN,
     and OVERVIEW documents. Commit these into the repo.
   * Use the [outro-prompt](../copilot/outro-prompt.md) to create an onboard document.
   * Put that document in the `copilot` directory and commit it, so it will be included in the pack.
   * Generate the pack by `make pack` at top level.
   * Start the new AI session.
   * Upload that pack to the context, and issue the [intro-prompt](../copilot/intro-prompt.md).

## Tips

Use TDD.  Tell the assistant to write tests for EVERYTHING.  Have it write the tests first.
The assistant can't keep everything in mind at once, and will sometimes
break things.  Having good tests will catch a lot of these cases.
Insist that tests pass after every change set.

Checkpoint often. There's a [checkpoint-prompt](../copilot/checkpoint-prompt.md)
that instructs the assistant to take stock of the current state and reflect on
what happened up to now in the session. This refreshes its context and can keep
its recollections fresh:
```
Take stock of the current state.  Reflect on what actions we have taken.
What refactorings have we done?  What features added?
What documents need to be revised now?
What refactorings could simplify the code to make it easier to reason about?
```

Refactor often. At checkpoints we invite the assistant to review the codebase and suggest
refactorings that will simplify the code to make it easier to reason about.
Accidental complication makes it harder for the assistant to be useful.

Tell the assistant to use its  tools to look up docs when it gets stuck.

After a refactoring is done, a feature is added, tests fixed, or other checkpoints,
commit the code.  Ask the assistant to generate the commit message, as it will
be thorough and informative.

Here's an excerpt from an [onboarding document](../copilot/archive/onboard-4.md):

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

# Critique and Reflections

Periodically upload the pack to a different assistant (the Critic) and ask it to
critique the documentation and then the codebase produced by the first assistant
(the Worker). Have it look for discrepancies between the docs and code.
Ask the Critic to find inconsistencies, ambiguities, gaps, out-of-date references
in the docs, and the like.  Have the Critic write instructions for the Worker to
correct the shortcomings.

Here's an example [critic prompt](../copilot/critic-prompt.md).

You can also have the Worker examine the critique and come up with its own mitigation plan.
It can be instructive to see how the two assistants differ in their take on how to correct course.


