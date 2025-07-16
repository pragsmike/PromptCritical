## Compositional semantics for combining prompts

Consider the algebra of prompts. They are analogous to programs, so program
synthesis is relevant. But they are natural language, so nlp techniques are also
relevant.

Syntactic manipulations can induce semantic changes. Recombination at the
semantic level is also possible and may be fruitful to investigate.

What research efforts should we look into?

Research areas include Lambek’s pregroups, the body of work and software tools
from the Stanford NLP group. Our goal is to find operations that preserve,
combine, and amplify semantic entailments while discouraging or disallowing
contradictions and other nonsense.

However, a bit of nonsense can sometimes spur creative novelty. This might be
useful in generating prompts, but not for certain tasks that require strict
preservation of semantics like removal of html tags.

The core challenge: we need **compositional semantics for prompt engineering** -
operations that respect meaning while enabling controlled creativity. Here are
the key research areas:

## Desirable properties

- **Causal structure preservation**: Using causal inference techniques to
  identify which parts of prompts actually drive desired behaviors vs. mere
  correlation

- **Modular composition**: Treating prompts as compositions of functional
  modules (context-setting, constraint-specification, output-formatting) that
  can be independently evolved


## Formal semantic composition methods

- **Combinatory Categorial Grammar (CCG)** - extends Lambek's pregroups with more flexible composition rules
- **Abstract Meaning Representation (AMR)** - graph-based semantic representations that could enable semantic-level recombination
- **Discourse Representation Theory (DRT)** - handles context and reference resolution, crucial for multi-part prompts
- **Type-theoretic semantics** - ensures compositional operations preserve semantic coherence

**Program synthesis approaches:**
- **Semantic program synthesis** - techniques from PROSE, FlashFill for generating programs from examples
- **Neural program induction** - differentiable programming approaches that could apply to prompt generation
- **Sketch-based synthesis** - partial program templates that could work for prompt scaffolding
- **Version space learning** - constraint-based approaches for finding consistent generalizations

**NLP compositional methods:**
- **Semantic parsing** research (Percy Liang's group, Luke Zettlemoyer's work)
- **Compositional distributional semantics** - how meaning composes in vector spaces
- **Controlled text generation** - PPLM, CTRL, GeDi for steering generation while preserving coherence
- **Semantic textual similarity** metrics for measuring meaning preservation

**Hybrid approaches worth exploring:**
- **Semantic templates** with variable slots that can be filled systematically
- **Dependency-preserving transformations** that maintain logical relationships
- **Constraint satisfaction** approaches where semantic requirements become constraints
- **Multi-objective optimization** balancing creativity vs. semantic fidelity

The key insight: we need operations that work at multiple levels
simultaneously - syntactic (for grammaticality), semantic (for meaning
preservation), and pragmatic (for communicative effect).
