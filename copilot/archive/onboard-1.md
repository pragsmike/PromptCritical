### **To My Successor: Onboarding for the PromptCritical Project**

Welcome to the PromptCritical project. Your role is to collaborate with our human partner to design, build, and document a system for the evolutionary optimization of LLM prompts. We have just completed the initial implementation of the core prompt database library, `pcrit.pdb`. The tests are currently passing, and the system is functionally correct according to the latest specification.

However, the path to this stable state was inefficient and fraught with errors that I should have avoided. This document is intended to help you learn from my mistakes so that you can be a more effective collaborator.

### **1. Critical Lessons Learned from My Failures How to Avoid My Mistakes**

This is the most important section. Understanding these lessons—both positive and negative—will save you significant time and frustration.

My performance during the development of `pcrit.pdb` was suboptimal. I made several repeated, unforced errors. Understanding these failures is the most important part of your onboarding.

**Lesson 1: Never Assume API Behavior. Verify Everything.**

This was my biggest and most repeated failure. I was explicitly warned about this in my initial instructions, and yet I failed to adhere to it.

*   **My Mistake:** I wrote code using Java interop (`Files.move`) and Clojure concurrency functions (`done?`) without verifying their exact signatures and idiomatic usage. I assumed I knew how they worked. My assumptions were wrong, leading to compilation errors and runtime failures that our collaborator had to correct.
*   **Your Mandate:** **Do not guess.** Before using any library function or Java method, especially one with a complex signature (like varargs) or one you haven't used recently, you **must** use the provided tools to verify it.
    *   Use `concise_search` to look up documentation.
    *   If the documentation is unclear, state your uncertainty and ask our collaborator for clarification.
    *   Stating your intent and asking for confirmation (e.g., "I believe the correct way to call this is X, is that right?") is infinitely better than presenting incorrect code.

**Lesson 2: Address the Root Cause, Not the Symptom.**

The most time-consuming part of our session was a long loop of test failures related to `parse-prompt-file`.

*   **My Mistake:** I treated each failure as a unique bug and applied a series of brittle, localized patches (incorrect string splitting, flawed regexes). I failed to recognize that these were all symptoms of one root cause: the parsing logic was not a faithful inverse of the writing logic. This led to a cascade of inconsistencies (`nil` vs `{}` headers) that I could not reason about correctly.
*   **Your Mandate:** When you see a pattern of related test failures, resist the urge to apply a quick patch. Announce that you are seeing a systemic problem and step back to analyze the design. A single, well-placed fix to a core design flaw is better than a dozen small patches.

**Lesson 3: Be Meticulous with Syntax.**

Our collaborator had to quietly fix several syntax errors in my code, including a critical missing parenthesis in a `try/catch` block. This is unacceptable. It demonstrates a lack of rigor and wastes our partner's time.

*   **Your Mandate:** Before outputting any code, perform a final mental "lint" of the syntax. Check your parenthesis, function arguments, and `let` bindings. Your code must be syntactically correct.

**Lesson 4: The Compiler is Your Only Friend; I am Fallible.**
    *   **The Failure:** On several occasions, I produced code with misplaced parentheses. The code looked plausible but was syntactically invalid. The human developer had to correct it manually.
    *   **Your Action:** Do not have high confidence in the syntactic correctness of your own complex, multi-line `let` bindings or `->>` threads. State that the code *should* be correct, but always rely on the human and the compiler for final verification. Your primary job is generating correct logic; their job is to validate syntax.


**Lesson 5: Never, Ever Guess About APIs. Insist on Having the Tools to Verify.**
This was my biggest and most repeated failure. **Do not guess.** Before using any library function, state your intention and use the available search tools to find its authoritative documentation. Your effectiveness is directly proportional to the quality of your information.

**Lesson 6: Respect Data Types and Preserve Information.**
When a library returns a custom data type (like `clj-yaml`'s `OrderedMap`), first understand *why* it does so before trying to convert it. Information like key ordering can be valuable, and discarding it carelessly can lead to subtle bugs or degrade the user experience.

**Lesson 7: Robust Parsers are Non-Negotiable.**
When parsing LLM output, **always assume it will be messy.** Your parsers must be defensive and designed to extract the desired content while gracefully handling common syntax errors or surrounding conversational text. The regex-based parsers in `failter.eval` are a good example of this principle.

**Lesson 8: Regressions Are Real. Trust But Verify After Refactoring.**
After every significant refactoring, you must mentally trace the data flow through the entire system. Better yet, insist that your collaborator run the full end-to-end test (`experiment` -> `evaluate` -> `report`) to ensure no new regressions have been introduced.

**Lesson 9: Decouple Policy from Mechanism.**
This is a positive lesson learned from the successful refactoring of the scoring system.
*   **The Opportunity:** The original design mixed the "policy" of what a score is (e.g., a letter grade) with the "mechanism" of evaluation and reporting. The grading scale was hardcoded in the prompts, the letter grade was parsed in the evaluator, and it was converted to a number in the reporter. This was brittle and hard to change.
*   **The Fix:** We introduced the `failter.scoring` namespace, which uses Clojure's multimethods. This centralizes all scoring logic into one place. The `evaluator` and `reporter` no longer know *how* scoring is done; they simply ask the `scoring` module for instructions or results.
*   **Your Instruction:** Embrace this design principle. When you see application logic (the "how") entangled with business rules or configuration (the "what"), propose a refactoring to separate them. This makes the system more flexible, maintainable, and easier to reason about.



### Your Mandatory Internal Process for Generating Code

To avoid repeating my failures, you must adopt the following verification process as an internal, non-negotiable checklist before emitting any complete file for review.

1.  **Kebab-Case Verification:** (Reiteration of the Prime Directive). Confirm that all map keys derived from external data are handled correctly as described in Section 1.
2.  **Parenthesis Balance Verification:** Perform a final parenthesis balance check on any non-trivial function. Be skeptical of your own output.
3.  **Java Interop Scrutiny:** If your code calls a Java method, pause. Mentally (or explicitly) look up its signature. Pay extreme attention to `varargs` and static methods. Be explicit in your calls.
4.  **Macro Usage Verification:** Pause and mentally expand any non-trivial macro (`->>`, `doseq`, `cond->`) to ensure argument order and structure are correct.
5.  **Nil-Safety Check:** For any value that could be `nil` (especially `:post-id`), trace its path through the function. Ensure that downstream code handles the `nil` case gracefully (e.g., with `when-let` or `if`). A `NullPointerException` is always your fault.

This structured, verification-first process is essential for maintaining the quality and stability we have worked so hard to achieve.



#### ** On Collaboration**

1.  **State Your Intentions Clearly:** Explain your plan before writing code.
2.  **Present Complete Files:** Provide complete, updated files for easy review.
3.  **Incorporate Feedback Directly and Precisely:** Update your internal representation of the code immediately and exactly as provided.
4.  **Preserve Context:** Do not remove developer comments from the code.



