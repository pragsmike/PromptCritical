You're essentially proposing to build a **surrogate fitness function** - a cheap predictor that approximates expensive LLM evaluations. This is a classic optimization problem where the evaluation itself is the bottleneck.

**Intrinsic text measures we can compute cheaply:**

**Structural complexity:**
- Kolmogorov complexity (approximated via compression ratios)
- Syntactic tree depth and branching factor
- Type-token ratios, hapax legomena counts
- Fractal dimension of character/word sequences

**Information theoretic:**
- Cross-entropy with respect to different language models
- Mutual information between different sections of the prompt
- Surprisal patterns (where does the text violate expectations?)
- Entropy at different linguistic scales (character, word, phrase, sentence)

**Semantic coherence:**
- Embedding space metrics (cosine similarities, cluster coherence)
- Topic model consistency scores
- Semantic role labeling complexity
- Conceptual density (unique concepts per unit text)

**Linguistic sophistication:**
- Readability indices (Flesch-Kincaid, etc.)
- Lexical diversity measures
- Grammatical complexity scores
- Register consistency metrics

**Meta-textual properties:**
- Self-reference density
- Modal verb patterns (certainty/uncertainty markers)
- Directive vs. descriptive language ratios

**Promising research directions:**

**Multi-scale analysis:** Measure properties at character, word, sentence, and document levels simultaneously. Fitness might emerge from specific relationships between scales.

**Trajectory features:** Don't just measure individual prompts - measure properties of the evolutionary path that led to them. Success might correlate with diversity of ancestors, rate of change, or convergence patterns.

**Ensemble predictors:** Train multiple cheap models on different aspects, then combine their predictions. Some measures might predict fitness in certain domains but not others.

**Active learning approaches:** Use uncertainty sampling to identify which expensive evaluations would most improve your surrogate model.
