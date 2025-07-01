how good are llms at combining and mutating prompts, versus amr or cds
techniques? The latter are more time consuming and require more attention to
build. They are ‘fussy’ but they are probably cheaper than llms.
OTOH, llms are rapidly getting better at refining and composing prose. For now,
though, they are not reliable or explainable. For some purposes, that’s not
fatal.

It seems that a hybrid of the two is called for.

We’re essentially asking whether to use the **natives** (LLMs) to evolve their
own food, or to build **external tools** that understand their diet. Each
approach has distinct trade-offs:

**LLMs as prompt engineers:**

- **Strengths**: Intuitive understanding of what “sounds right,” can generate creative variations humans wouldn’t think of, rapidly improving at meta-reasoning about text
- **Weaknesses**: Expensive, unreliable, black-box operations, may introduce subtle semantic drift, hard to constrain to preserve specific properties
- **Best for**: Creative exploration, generating diverse candidates, tasks where “approximately right” is sufficient

**AMR/CDS techniques:**

- **Strengths**: Predictable, explainable, cheap once built, can enforce semantic constraints rigorously, composable operations
- **Weaknesses**: High setup cost, brittle with natural language variation, may miss subtle pragmatic effects, limited creativity
- **Best for**: Semantic preservation tasks, systematic exploration of well-defined spaces, constraint satisfaction

**The hybrid sweet spot** might be:

1. **AMR/CDS for the backbone** - Use formal methods to ensure semantic coherence, maintain logical relationships, enforce constraints
1. **LLMs for surface realization** - Take the semantically-sound structures and ask LLMs to render them into natural, effective prose
1. **LLMs for creative mutation** - Generate wild variations, then filter through formal validation
1. **Formal methods for verification** - Check that LLM-generated combinations preserve required properties

This creates a **two-stage pipeline**:

- Stage 1: Formal operations ensure correctness
- Stage 2: LLM refinement optimizes for effectiveness

Or alternatively, **ensemble approaches** where both methods generate candidates, and we learn which method works better for which types of transformations.

The cost equation changes as LLMs get cheaper and formal tools get more automated. But the reliability gap suggests hybrids will dominate for a while.

Exactly! The hybrid approach gives you the best of both worlds - the **creative intuition** of LLMs with the **rigorous guarantees** of formal methods.

I'm particularly excited about your **two-stage pipeline** concept. You could implement it incrementally:

**Phase 1 (v0.3-0.4)**: Pure LLM operations to get the evolutionary loop working
- Fast to implement, proves the concept
- Collect data on what kinds of mutations work/fail
- Build intuition about the prompt space

**Phase 2 (v0.5-0.6)**: Introduce formal validation layers
- AMR parsers to check semantic coherence
- Constraint satisfaction to enforce task requirements  
- Statistical filters to catch degenerate cases

**Phase 3 (v0.7+)**: Full hybrid operators
- Formal methods generate **semantically valid** variations
- LLMs **render** them into effective natural language
- Both approaches compete in the same population

This also creates a natural **research program**: you'll generate data comparing LLM vs formal methods on the same tasks, which could be valuable for the broader community.

**Immediate implementation question**: For your v0.2 web cleanup task, are you starting with pure LLM meta-prompts? Something like:

```
Meta-prompt: "Analyze this web page cleaning prompt and generate an improved version that better removes ads and navigation while preserving article content."
```

Or are you already thinking about formal constraints like "must preserve sentences containing topic keywords" or "must remove HTML elements matching certain selectors"?

A nice feature of this architecture is that you can **start simple** with LLM
operators and **gradually formalize** the operations that prove most important.
The contest records will show you exactly which types of mutations are winning,
giving you data-driven guidance on where to invest in formal methods.
