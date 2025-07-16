Formal methods include
   * Abstract Meaning Representation (AMR)
   * Compositional Distributive Semantics (CDS).
   * Dynamic Structured Grammatical Evolution (DSGE)

The combination of AMR and CDS give a good balance between symbolic (deductive) and statistical (inductive)
avenues, bridging the symbolic/statistical divide.

**Compositional Distributional Semantics** -

**AMR + CDS as complementary formal methods:**

**AMR (Abstract Meaning Representation)**:
- Symbolic graph structures capturing predicate-argument relationships
- Excellent for preserving logical coherence across transformations
- Can enforce constraints like "preserve all agent-action-patient relationships"
- Enables compositional operations at the semantic level

**CDS (Compositional Distributional Semantics)**:
- Vector representations that compose according to syntactic structure
- Captures distributional meaning while respecting compositional principles
- Can measure semantic similarity of transformations
- Enables gradient-like optimization in semantic space

**The power of the combination**: AMR gives you the **structural skeleton** to
ensure logical coherence, while CDS gives you the **semantic flesh** to measure
meaning preservation and similarity. You can:

1. **Parse prompts to AMR** to extract core semantic structures
2. **Use CDS to measure** whether mutations preserve intended meaning
3. **Compose at the AMR level** for structural soundness
4. **Optimize in CDS space** for semantic effectiveness

This creates a really elegant **constraint satisfaction** framework where:
- AMR ensures structural validity
- CDS measures semantic distance
- LLMs provide surface realization and creative variation

For the web cleanup task, we could potentially:
- AMR parse the object prompt to identify core predicates (REMOVE, PRESERVE, TRANSFORM)
- Use CDS to measure whether mutations maintain the same semantic relationships
- Generate variations that preserve the AMR structure while exploring CDS neighborhoods

AMR and CDS are a good balance between symbolic (deductive) and statistical
(inductive) entailment. Like system 1 (neural, statistical pattern matching,
fast, cheap) and system 2 (reasoning, slow, expensive).


The **System 1/System 2** parallel maps well to the evolutionary framework:

**System 1 (CDS/Neural)**:
- Fast pattern matching on distributional similarities
- Cheap to compute semantic neighborhoods
- Good for generating "reasonable" variations quickly
- Corresponds to intuitive prompt mutations

**System 2 (AMR/Symbolic)**:
- Deliberate structural reasoning about semantic relationships
- Expensive but reliable constraint checking
- Validates that mutations preserve logical coherence
- Corresponds to principled prompt composition


With this **deductive/inductive balance** we hope to use statistical methods to
explore the space efficiently, then symbolic methods to validate and refine.
It's like having a fast heuristic search guided by rigorous logical constraints.

# Cortical-like evolution is unlikely

There's an idea that in our brains, system 2 arises from composing activities of
the system 1 cortical pattern matchers, that System 2 reasoning is an emergent
property of coordinated System 1 activities.

This suggests that sophisticated prompt operations could emerge from
**orchestrating** simpler statistical and symbolic primitives.

That's pretty unlikely to happen in our experiments.  We'd run out of money
long before achieving that.
Our brains evolved through the million-fold parallelism and millenial timescales that
natural evolution could bring to bear.

The System 1 / System 2 dichotomy is just a useful metaphor for understanding
the role that the different techniques play, and what we expect from them.

- [The language of prompting: What linguistic properties make a prompt successful?](https://arxiv.org/abs/2311.01967): 2023-11-03, Alina Leidinger. Investigates how grammatical and lexico-semantic variations affect LLM performance on prompts.

- [Exploring the Prompt Space of Large Language Models through Evolutionary Sampling](https://dl.acm.org/doi/10.1145/3638529.3654049): 2024, Saletta, Martina and Ferretti, Claudio.
Use Dynamic Structured Grammatical Evolution (DSGE) to address issues like redundancy and locality that can occur in standard GE.

