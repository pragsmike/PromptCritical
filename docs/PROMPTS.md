Here are sample prompts we're considering.

# Crossover (binary recombination) prompt

  Given two prompts, produce a new one that combines the best parts of each
  to produce a stronger one that is likely to give better results.


  Prompt_Crossover
  ```
      Combine the useful parts of these two prompts:
      A) {{prompt_A}}
      B) {{prompt_B}}
      Make {{m}} hybrids that integrate the best instructions from each while satisfying: {{constraints}}.
      Give one hybrid per bullet.
  ```

##  Inputs:

    * prompt_A
    * prompt_B
    * m
    * constraints

# Mutation (Unary improvement) prompt

   Given a prompt, produce a stronger one that is likely to give better results.

   Prompt_Mutation
   ```
      Here is a parent prompt:
      >>> {{parent_prompt}}
      ---
      Make {{k}} variants that preserve intent but differ in wording, order, or emphasis.
      Keep them ≤ {{token_limit}} tokens and obey these constraints: {{constraints}}.
   ```

###  Inputs:
    * parent_prompt
    * k
    * token_limit
    * constraints

# Transformation prompt

   Given a prompt, an input text, and a gold standard output text,
   produce a prompt that instructs an LLM to transform the input into the output.
   It's best if there are multiple sample text+output pairs.

# Seed prompt generation


  Seed_Prompt_Generation
  ```
    You are designing prompts for the task {{task_description}}.
    Produce {{n}} diverse candidate prompts that: 1) follow the style guide {{style_rules}}, 2) stay within {{token_limit}} tokens, 3) avoid prohibited content {{policy_summary}}.
    List each prompt on its own line.
  ```

### Inputs:
    * task_description
    * n
    * style_rules
    * token_limit
    * policy_summary


# Constraint-aware drafting prompt

  Constraint_Aware_Drafting
  ```
    Write {{k}} brand-new prompts for {{task_description}} that explicitly satisfy all domain rules:
    {{domain_rules}}
    Each prompt ≤ {{token_limit}} tokens.
  ```

### Inputs:
    * task_description
    * domain_rules
    * k
    * token_limit


# Semantic Policy Filter

  Semantic_Policy_Filter
  ```
  Evaluate the following prompt for (a) contradiction, (b) policy violations, (c) missing mandatory clauses.
  Output PASS or FAIL plus a short reason.\nPrompt:
  >>> {{candidate_prompt}}
  Rules:
  {{policy_rules}}
  ```

### Inputs:
     * candidate_prompt
     * policy_rules

# Prompt quality evaluation prompt

  Given a prompt, evaluate it on some quality aspects.
  Produce a score for each area, and a composite score.

  Is it clearly stated?
  Are there ambiguities that could be interpreted in different ways?
  Are the instructions obviously stated and clear?
  Are the assumptions explicitly stated?
  Are there unstated assumptions?

# Surrogate Fitness Estimate (optional)

  Surrogate_Fitness_Estimate
  ```
      Predict on a 0–100 scale how well this prompt will perform on {{task_metric}} for {{task_description}}.
      Give only the numeric estimate and ≤ 20-word justification.
      Prompt:
      >>> {{candidate_prompt}}
  ```


### Inputs:
    * candidate_prompt
    * task_description
    * task_metric

# Best Prompt Summary

  Best_Prompt_Summary
  ```
      Summarise the strengths and weaknesses of the top {{top_k}} prompts below, then recommend the single best prompt.
      Prompts + scores:
      {{prompt_score_table}}
  ```

### Inputs:
     * prompt_score_table
     * top_k

# Transformation Evaluation prompt

  These are given to failter as part of the experiment spec.
  There are two cases in general, depending on whether there is a known
  correct output text (the "gold" standard).

  With no gold standard:
  Given a prompt, an input text, and an output text,
  analyze how closely the transforming LLM followed the instruction.

  With a gold standard:
  Given a prompt, an input text, and an output text, and a "gold" correct output text,
  analyze how closely the transforming LLM followed the instruction.
  If it produced exactly the gold output, it gets the highest score.


# NOTES

  Prompts are most often imperative commands.
