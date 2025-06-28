You've identified the core challenge: we need **compositional semantics for prompt engineering** - operations that respect meaning while enabling controlled creativity. Here are the key research areas:

**Formal semantic composition:**
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

The key insight: we need operations that work at multiple levels simultaneously - syntactic (for grammaticality), semantic (for meaning preservation), and pragmatic (for communicative effect).
