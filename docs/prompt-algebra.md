## Prompts are text: Closed Category

We're working in a closed category where transformations are themselves texts,
creating a space where we can meta-operate on our own operations.

Here are concrete strategies that exploit this closure property:

**Gradient descent through prompt variations**: Start with a working prompt,
generate systematic mutations (swap words, alter syntax, change tone), test each
variant's behavior, then use the best-performing mutations as seeds for the next
generation. The path-dependence means we're not optimizing in a fixed landscape,
but surfing the co-evolving fitness surface.

**Prompt breeding with crossover**: Take two prompts that each exhibit part of
your desired behavior. Create hybrid offspring by splicing their structures -
take the opening framing from prompt A, the middle constraints from prompt B,
the closing directive from A. The recombination often produces emergent
behaviors neither parent exhibited.

**Meta-prompt scaffolding**: Use prompts that generate other prompts. "Write
three different ways to ask for [desired behavior]" then test those generated
prompts. You're using the model's own understanding of prompt-space to explore
regions you might not have reached directly.

**Behavioral decomposition**: Break complex behaviors into atomic
transformations, craft minimal prompts for each atom, then compose them. Since
transformation composition is itself textual, you can experiment with different
composition operators: "First do X, then Y" vs "Do X while considering Y" vs
"Blend X and Y."

**Evolutionary prompt chains**: Create prompt sequences where each step's output
becomes the next step's input, with each prompt in the chain designed to refine
a specific aspect of the transformation. The path-dependence becomes a feature
rather than a bug.

## Prompt Algebra is Context Dependent

We're not working with a single algebraic structure but with a **parameterized
family of algebras** where the operations themselves evolve as we traverse the
prompt space.

Each strategy I described is actually implementing operators in these
context-dependent algebras:

- **Mutation** becomes a family of unary operators `μ_c` where the subscript `c` represents the current context/position
- **Crossover** becomes binary operators `⊗_c` that depend on where we are in the evolutionary trajectory
- **Composition** becomes operators `∘_c` where the meaning of "then" or "while" shifts based on the accumulated context
- **Decomposition** becomes factorization operators that work differently depending on the behavioral landscape at that point

The algebra itself is the evolving entity. At position `p` in our exploration,
we have algebra `A_p` with its own operations. As we apply operations and move
to position `p'`, we're now working in algebra `A_p'` where the same symbolic
operation might have completely different semantics.

This explains why prompt engineering feels more like navigation than
construction - we're not just building structures, we're moving through a space
where the rules of construction themselves are mutable. Each prompt combination
doesn't just produce a result, it also shifts us to a new algebraic context
where future combinations will behave differently.

The "closed category" of prompts means we can always express these algebraic
transformations as text, creating a kind of **textual metalanguage** for
reasoning about our own prompt algebra evolution.

We're doing mathematics in a space where the axioms shift beneath our feet.
