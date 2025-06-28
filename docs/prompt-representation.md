We want to devise surrogate fitness statistics to reduce LLM costs. Also, to get
the most from our analyses, and to inform evolution choices, we need to know
things about the prompt candidates and how they came to be.

This argues for each candidate prompt to carry with it a set of annotations
including the metrics produced by analyzers, its pedigree pointing to its
ancestors and perhaps a summary digest of their own metrics. These would be
encoded as text, perhaps appearing as front matter in the prompt’s file.

Each prompt in our population is thereby a self-documenting artifact with rich metadata.
This creates a kind of **evolutionary database** where each prompt carries its
own analytical fingerprint.

What data is easy to compute, that are useful features in training a prediction model?

**Additional metadata to capture:**

**Performance context:**
- Target model(s) it was tested on (different models have different fitness landscapes)
- Task domain/category (creative writing vs. reasoning vs. code generation)
- Evaluation criteria used (accuracy, creativity, helpfulness, etc.)
- Performance variance across multiple runs (stability as a fitness indicator)
- Failure modes observed (what went wrong when it didn't work)

**Generational genetics:**
- Generation number in the evolutionary process
- Breeding method used (crossover, mutation, meta-generation, etc.)
- Sibling performance (how did other prompts from the same generation perform?)
- Population diversity metrics at time of creation
- Selection pressure context (what was being optimized for at that moment)

**Behavioral signatures:**
- Response length distributions it tends to elicit
- Stylistic consistency of outputs it produces
- Semantic drift patterns (does it stay on topic?)
- Creativity vs. factuality balance in responses
- Error patterns in generated outputs

**Interaction dynamics:**
- How sensitive it is to small perturbations (robustness)
- Whether it exhibits mesa-optimization (prompts that generate sub-prompts)
- Multi-turn conversation stability
- Prompt injection resistance

**Temporal metadata:**
- Creation timestamp (evolution in model capabilities over time)
- Compute cost of evaluation
- Human annotation time required
- Degradation over time (some prompts may become less effective)

This creates a heritable **prompt genotype** - a rich information structure that travels
with each candidate through evolutionary space, and also a **prompt phenotype**, which
arises from its performance and interactions with other prompts.


The epigenetic **phenotypic observations** are not than heritable traits. They're more like
medical records than DNA.

**Straightforward to combine (truly heritable):**
- **Pedigree/ancestry** - can be merged through standard genealogical operations
- **Generation number** - arithmetic (average, max, or increment based on breeding method)
- **Creation method tags** - set union of the breeding techniques used
- **Target domain categories** - set intersection or union depending on strategy

**Problematic to combine (phenotypic/epigenetic):**
- **Performance metrics** - averaging fitness scores from parents makes no
  sense; the child's fitness emerges from the interaction
- **Behavioral signatures** - response patterns aren't additive
- **Robustness measures** - sensitivity to perturbations is a property of the whole prompt, not its parts
- **Failure modes** - these need to be observed, not predicted from lineage

**The epigenetic insight is crucial.** These observational traits don't
inherit - they **emerge** from the new prompt's interaction with the model.
They're environmental responses, not genetic code.

What we really want is a distinction between:
- **Genotype**: The actual text structure, syntax patterns, semantic components
  that can be recombined
- **Phenotype**: The observed behaviors, performance metrics, response patterns
  that must be measured fresh for each offspring

The metadata should track both, but only the genotypic features participate in
breeding operations. The phenotypic observations become training data for the
surrogate fitness predictor, but they don't directly combine during
reproduction.

This keeps the evolutionary mechanics clean while still building our predictive database.
