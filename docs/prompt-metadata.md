# Prompt Metadata
## Motivation for metadata choices

We want to devise surrogate fitness statistics to reduce LLM costs. Also, to get
the most from our analyses, and to inform evolution choices, we need to know
things about the prompt candidates and how they came to be.

This is why each candidate prompt carries with it a set of annotations (metadata)
including the metrics produced by analyzers, its pedigree pointing to its
parent(s) (immediate ancestors) and a summary digest of its own metrics.

As described above, this is encoded as YAML text, appearing as front matter in
the prompt’s file.

This creates a kind of **evolutionary database** where each prompt carries its
own analytical fingerprint.

What data is easy to compute, that are useful features in training a prediction model?

## Metadata captured:

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
- Degradation over time (some prompts may become less effective**

**Measures that capture intrinsic properties of the text
  Some measures that capture the "meta-linguistic awareness" of prompts:

- **Reflexivity indicators**: How well does the prompt anticipate and guide the model's own reasoning process?
- **Constraint specificity**: Measures of how precisely the prompt constrains the solution space without over-constraining
- **Contextual anchoring**: How well the prompt establishes stable reference points for the model's reasoning



# Genotype vs. Phenotype

Some of the metadata items are heritable by offspring prompts.
Others are observed measurements of the individual and are not passed down to offspring.
We call these subsets the *genotype* and the *phenotype*, respectively.
We keep these separate in the metadata structure.

This keeps the evolutionary mechanics clean while still building our predictive database.
This creates a heritable **prompt genotype** - a rich information structure that travels
with each candidate through evolutionary space, and also a **prompt phenotype**, which
arises from its performance and interactions with other prompts.


- **Genotype**: The actual text structure, syntax patterns, semantic components
  that can be recombined
- **Phenotype**: The observed behaviors, performance metrics, response patterns
  that must be measured fresh for each offspring


## Genotype

The genotypic features participate in breeding operations.

** Truly heritable features:**
- **Pedigree/ancestry** - can be merged through standard genealogical operations
- **Generation number** - arithmetic (average, max, or increment based on breeding method)
- **Creation method tags** - set union of the breeding techniques used
- **Target domain categories** - set intersection or union depending on strategy

## Phenotype

The epigenetic **phenotypic observations** are not than heritable traits. They're more like
medical records than DNA.  It doesn't make sense to combine these when producing offspring.
They're environmental responses, not genetic code.

These observational traits don't inherit - they **emerge** from the prompt's
interaction with the model.

The phenotypic observations become training data for the surrogate fitness
predictor, but they don't directly combine during reproduction.

** phenotypic/epigenetic features:**
- **Performance metrics** - averaging fitness scores from parents makes no
  sense; the child's fitness emerges from the interaction
- **Behavioral signatures** - response patterns aren't additive
- **Robustness measures** - sensitivity to perturbations is a property of the whole prompt, not its parts
- **Failure modes** - these need to be observed, not predicted from lineage

