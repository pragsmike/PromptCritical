# Metadata kept in prompt files

The prompt text is kept in a text file, with metadata about the prompt
encoded in YAML front matter, enclosed between lines of dashes.
We'll also call this front matter the header.

Each prompt in our population is thereby a self-documenting artifact with rich
metadata.

The metadata is not sent to the LLMs.  Only the prompt itself, the body of the file,
is ever seen by an LLM when it acts on the prompt.

When prompts are generated, some initial metadata is added as front matter.
This includes
   * id's of ancestor prompts if any
   * the algorithm or model that generated it
   * the id of the prompt that generated it, if any
   * creation timestamp
   * hash of the body text (see below)

In general, prompts are not modified after creation. The file body, containing the
prompt text, is immutable. If a prompt is mutated, a new prompt file will be
created to hold the mutated prompt, along with the new prompt's metadata.

Once prompt text has been written into a file, it is immutable.  The *text* is never changed,
but metadata is routinely added to the front matter.
Prompts are often analyzed to compute metrics (entropy, komogorov complexity, and the like)
and the computed metrics are added as metadata field in the front matter.

One of the metadata fields is a hash of just the prompt text, computed without any of the metadata.
This ensures that corruption of the text will be detected. This policy preserves
the pedigree of the prompts descended from it.

The hash is computed over the prompt text starting with first nonblank line after front matter.

The metadata in the prompt file header only describes properties of the prompt itself.

Details of its interactions with other processes, such as scores from contests,
should be recorded in records of those contests.
NOTE: We might relax this if it becomes useful to do so.

## Motivation for metadata choices

We want to devise surrogate fitness statistics to reduce LLM costs. Also, to get
the most from our analyses, and to inform evolution choices, we need to know
things about the prompt candidates and how they came to be.

This is why each candidate prompt carries with it a set of annotations (metadata)
including the metrics produced by analyzers, its pedigree pointing to its
ancestors and a summary digest of its own metrics.

As described above, this is encoded as YAML text, appearing as front matter in
the prompt’s file.

This creates a kind of **evolutionary database** where each prompt carries its
own analytical fingerprint.

What data is easy to compute, that are useful features in training a prediction model?

## Additional metadata to capture:

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
