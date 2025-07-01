These are disorganized notes and references.

* [Plan](plan.md)

* [Compositional Semantics for Prompt Combination](compositional-semantics-for-prompt-combination.md)

* [DESIGN](DESIGN.md)

* [Prompt File Database format ](prompt-representation.md)

* [Research directions](research-1.md)

* [References](References.org) 

* [Surrogate Fitness Metrics to lower costs](surrogate-fitness-metrics.md) 


LLM vs Formal Methods
  * llm-vs-amr+kg-general.md
  * llm-vs-amr+kb-prompt-refinement.md
  * knowledge-graph-contributions.md
  * compositional-semantics-for-prompt-combination.md

- **Causal structure preservation**: Using causal inference techniques to identify which parts of prompts actually drive desired behaviors vs. mere correlation
- **Modular composition**: Treating prompts as compositions of functional modules (context-setting, constraint-specification, output-formatting) that can be independently evolved

Prompt Ecology

  Rather than just evolving individual prompts, consider evolving
  **ecosystems*** of interacting prompts. Some prompts might specialize in
  different cognitive functions:

- **Scout prompts**: Optimized for exploration and creativity
- **Validator prompts**: Specialized for checking outputs against constraints
- **Synthesizer prompts**: Good at combining outputs from multiple sources
- **Meta-prompts**: Evolved to generate other prompts for specific contexts

  This could lead to more robust and adaptable prompt systems that maintain
  diversity while specializing for different aspects of complex tasks.

Data structures
  * prompt-representation.md
  * git-as-temporal-database.md

  **The Immutable Prompt Store Design** treats prompts as content-addressable
  objects with SHA-1 integrity and full lineage tracking. This solves the
  reproducibility crisis in prompt engineering where people can't even remember
  what they tried last week. The `.prompt` file format with UTF-8 + NFC
  canonicalization shows serious attention to the subtle details that make or
  break data integrity.


Prompt Algebra
  * prompt-algebra.md
  * prompt-folding.md



Evoluationary Algorithm
  * population-bootstrapping.md
  * operation-enumeration.md
  * prompt-breeding.md
  * contest-architecture.md


Prompt Metrics
  * prompt-metadata.md
  * fitness-function.md
  * surrrogate-fitness-metrics.md

Population Metrics
  * population-metrics.md

Philosophical Jazz
  * llm-resonances.md


Research References
  * evolutionary-prompts-papers.md
  * References.org
