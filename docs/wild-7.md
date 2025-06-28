You're absolutely right to question this - most of those genome components are **phenotypic observations** rather than heritable traits. They're more like medical records than DNA.

**Straightforward to combine (truly heritable):**
- **Pedigree/ancestry** - can be merged through standard genealogical operations
- **Generation number** - arithmetic (average, max, or increment based on breeding method)
- **Creation method tags** - set union of the breeding techniques used
- **Target domain categories** - set intersection or union depending on strategy

**Problematic to combine (phenotypic/epigenetic):**
- **Performance metrics** - averaging fitness scores from parents makes no sense; the child's fitness emerges from the interaction
- **Behavioral signatures** - response patterns aren't additive
- **Robustness measures** - sensitivity to perturbations is a property of the whole prompt, not its parts
- **Failure modes** - these need to be observed, not predicted from lineage

**The epigenetic insight is crucial.** These observational traits don't inherit - they **emerge** from the new prompt's interaction with the model. They're environmental responses, not genetic code.

What we really want is a distinction between:
- **Genotype**: The actual text structure, syntax patterns, semantic components that can be recombined
- **Phenotype**: The observed behaviors, performance metrics, response patterns that must be measured fresh for each offspring

The metadata should track both, but only the genotypic features participate in breeding operations. The phenotypic observations become training data for the surrogate fitness predictor, but they don't directly combine during reproduction.

This keeps the evolutionary mechanics clean while still building our predictive database.
