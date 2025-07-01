Formal methods include
   * Abstract Meaning Representation (AMR)
   * Compositional Distributive Semantics (CDS).

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

**The power of the combination**: AMR gives you the **structural skeleton** to ensure logical coherence, while CDS gives you the **semantic flesh** to measure meaning preservation and similarity. You can:

1. **Parse prompts to AMR** to extract core semantic structures
2. **Use CDS to measure** whether mutations preserve intended meaning
3. **Compose at the AMR level** for structural soundness
4. **Optimize in CDS space** for semantic effectiveness

This creates a really elegant **constraint satisfaction** framework where:
- AMR ensures structural validity
- CDS measures semantic distance
- LLMs provide surface realization and creative variation

For your web cleanup task, you could potentially:
- AMR parse the object prompt to identify core predicates (REMOVE, PRESERVE, TRANSFORM)
- Use CDS to measure whether mutations maintain the same semantic relationships
- Generate variations that preserve the AMR structure while exploring CDS neighborhoods

- **Causal structure preservation**: Using causal inference techniques to identify which parts of prompts actually drive desired behaviors vs. mere correlation
- **Modular composition**: Treating prompts as compositions of functional modules (context-setting, constraint-specification, output-formatting) that can be independently evolved

