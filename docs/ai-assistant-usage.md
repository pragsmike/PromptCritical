Here are some tips for using an AI assistant in generating this design, writing
code, and refactoring.

The prompts in the `copilot` directory were used in designing this system and developing the codebase.

Use TDD.  Tell the assistant to write tests for EVERYTHING.  Have it write the tests first.
The assistant can't keep everything in mind at once, and will sometimes
break things.  Having good tests will catch a lot of these cases.
Insist that tests pass after every change set.

Checkpoint often.  There's a prompt that instructs the assistant to take stock of the current state
and reflect on what happened up to now in the session.  This refreshes its context and can keep its
recollections fresh.

Refactor often. At checkpoints also invite the assistant to review the codebase and suggest
refactorings that will simplify the code to make it easier to reason about.
Accidental complication makes it harder for the assistant to be useful.

Tell the assistant to use its  tools to look up docs when it gets stuck.

After a refactoring is done, a feature is added, tests fixed, or other checkpoints,
commit the code.  Ask the assistant to generate the commit message, as it will
be thorough and informative.
