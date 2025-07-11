On the question of black-box probing to find LLM "resonances":

We seek to find how modifying prompts changes their performance. The LLM's web
of correlations strongly influences this effect. Is it fruitful to study the
prompts that excite certain behaviors in the model?

This question is motivated by analogy with physics, where by understanding how a
physical object responds to certain stimuli, we can deduce things about its
structure and predict how it will behave. (For example, earthquakes give us
insights into how the earth is put together, and let us map its density
profile.)

The linearity assumption collapses here, and with it goes the elegant machinery
of impulse response analysis. The model's response to "A + B" isn't f(A) + f(B),
so we can't decompose complex prompts into superpositions of simple ones.

But maybe we can find useful analogs by thinking about what an impulse function
really does - it's a probe that excites all frequencies equally, revealing the
system's natural resonances. In prompt space, what would have that quality?

**Minimal semantic seeds**: Single words or very short phrases that are
maximally ambiguous - they could connect to many different correlation clusters.
Words like "consider," "imagine," "suppose" might act as broad-spectrum
activators, revealing which conceptual regions the model defaults to.

**Contradiction pairs**: Prompts that simultaneously pull toward incompatible
attractors - "Be creative but follow strict rules" or "Explain simply but in
great detail." The model's resolution of these tensions reveals the relative
strength of different correlation patterns.

**Meta-prompts**: Prompts about prompting itself - "What kind of text would make
you respond differently?" These might reveal something about the system's
self-awareness of its own response patterns.

**Random walks**: Nonsensical but grammatically valid text that activates
syntactic patterns without strong semantic biases. The model's attempt to make
sense of this reveals its baseline interpretive tendencies.

The non-linearity means we're not doing frequency analysis but something more
like... topology mapping? Exploring the landscape of attractors and basin
boundaries in a space where the terrain itself shifts based on how you approach
it.
