# Handling "Bad Day" Scenarios

PromptCritical is designed to be a robust, data-driven tool. However, experiments can sometimes go wrong due to misconfiguration, unexpected external behavior, or flawed evolutionary strategies. This document outlines common "bad day" scenarios and explains how PromptCritical helps you diagnose and recover from them.

## 1. `failter` Command Fails

**The Bad Day:** You run `pcrit evaluate` and it immediately fails with a non-zero exit code.

**How PromptCritical Helps:** When the external `failter` tool fails, PromptCritical will:
1.  Print an error message indicating that the `failter` process failed.
2.  **Print the complete `stderr` output from `failter` directly to your console.** This is the most critical diagnostic information.
3.  Store both the `stdout` and `stderr` streams in the contest directory (as `failter.stdout.log` and `failter.stderr.log`) for later inspection.

**Example `stderr` from `failter`:**
```
2025-07-14 19:40:57.124 ERROR failter.run | Invalid specification: Spec file is missing required key: :judge_model
```
This tells you exactly what is wrong with the generated `spec.yml` file, pointing to a missing `:judge-model` in your experiment's configuration.

## 2. Evaluation Succeeds but Nothing Happens

**The Bad Day:** You run `pcrit evaluate`, it completes successfully with no errors and zero cost, but when you run `pcrit stats`, you see zero prompts were evaluated and no scores were recorded.

**How PromptCritical Helps:** This usually means your inputs were not configured correctly. Before starting an evaluation, PromptCritical performs several checks and will print warnings if it detects a potential problem:

*   **Warning: Empty Inputs Directory**
    ```
    WARN pcrit.command.evaluate | Inputs directory is empty: /path/to/your/texts
    ```
    This warning indicates that the `--inputs` directory you provided contains no files. The contest will run, but `failter` will have no documents to evaluate the prompts against, resulting in an empty report.

## 3. Selection Produces an Empty Generation

**The Bad Day:** You run `pcrit select` (or it's run for you by `evolve`), and it completes without error. However, the *next* `vary` step fails with an error about not being able to find any parents.

**How PromptCritical Helps:** This happens when the selection policy results in zero survivors. This could be because all prompts scored very poorly or failed to run entirely. `pcrit` now detects this and provides a clear warning:

*   **Warning: Zero Survivors**
    ```
    WARN pcrit.command.select | Selection resulted in zero survivors. The next generation will be empty.
    ```
    This message alerts you that the evolutionary line has died out. You will need to investigate the contest report (`report.csv`) to understand why all prompts failed and potentially adjust your selection policy or prompts.

## 4. Misspelled or Unsupported Model Name

**The Bad Day:** You edit `evolution-parameters.edn` to try a new model, but you make a typo (e.g., `"openai/gpt-4o-minii"`). The `evaluate` or `vary` command then fails with a cryptic error message from the underlying LLM API.

**How PromptCritical Helps:** To help you catch this early, `pcrit` checks the model names in your configuration against its internal price table of known, supported models. If it finds a name it doesn't recognize, it will print a warning:

*   **Warning: Unknown Model Name**
    ```
    WARN pcrit.command.evaluate | Model 'openai/gpt-4o-minii' is not a known model in PromptCritical's price table. It may not be supported.
    ```
    This doesn't stop the command from running (as you may have a custom LiteLLM proxy), but it gives you an immediate hint that you should double-check your spelling and configuration.

NOTE: The price table is actually configured in a library that `pcrit` uses, `pcrit-llm`.
