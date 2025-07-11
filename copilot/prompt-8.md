The codebase has grown so it is hard to keep track of dependencies.
The Polylith organization, with strict interface adherence, helps.
Look for refactorings that will make the code easier to reason about.
Focus especially on file operations that use java.nio.
The symlink operations are supposedly already centralized.
Confirm that's true.
