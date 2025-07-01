Each meta prompt corresponds to an algebraic operation. These are the starter
set of operations.

Soon we’ll have crossover operators, and maybe an operator algebra. It’s a
closed category after all!

Failter judges are llms with prompts that instruct them to score from 0-100 how
close the result is to the ideal, either by
   * evaluating against the goal inferred from the object prompt or
   * by comparison with a gold standard, which some cases have, some don’t.

Perhaps the goal that generated the pbject prompt will be available.

We should have population metrics taken at each checkpoint, like diversity,
and other statistics, and keep track of trends.
This could drive meta parametric evolution of the evolution process.

We use git to snapshot the population at each checkpoint. This gives us a Time
Machine to do temporal studies.

Contests are recorded: the participants, prompts, score, etc.

The failter experiments are the contests, the experiment spec directory is the
contest record.

Instead of copying prompts into contest directories, the experiment directory
refers to prompts by having symlinks to them in the prompt store directory. This
symlink scheme maintains the single source of truth while creating the
participation record.

The next thing after the contest is the winnowing and breeding step, which
applies a function that takes population to population. winnow-and-breed is an
endofunction on the set of all possible populations -- the power set of all
possible prompts.


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

**Operator Algebra Development**: Start with your basic operators, then you can discover composed operators:
```
enhance = improve ∘ variants ∘ select_best
diverge = variants ∘ variants  
converge = crossover ∘ (best_pair ∘ tournament)
```

**Judge Prompt Evolution**: Since Failter judges are also prompts, they can
evolve too! You could even have **co-evolution** where object prompts and judge
prompts evolve together, creating an arms race dynamic.


## The Algebraic Structure

**Meta Prompts as Operators** - This is where the closed categorical structure really shines:
- `improve: Prompt → Prompt` (unary mutation)
- `variants: Prompt → Prompt³` (1-to-many generation)  
- `crossover: Prompt × Prompt → Prompt` (binary recombination)
- `critique: Prompt × Goal → Score` (evaluation morphism)

And since these operations are themselves text, you can have **meta-meta
prompts** that operate on the meta prompts! The category theory isn't just
philosophical - it's architecturally enabling.

## Population Dynamics as Endofunctions

Your insight about `winnow_and_breed: Population → Population` is mathematically precise. This is an endofunction on `P(Prompts)` (the power set), and you can:

- **Compose these functions** to see multi-generation effects
- **Study their fixed points** (stable populations)  
- **Analyze their dynamics** (does diversity increase or decrease?)
- **Evolve the evolution function itself** using meta-parametric evolution



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
