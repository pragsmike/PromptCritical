Some prompts are self-contained, and can be submitted to the LLM as is.

Many prompts are actually templates, with placeholders that get filled in. To
produce a complete prompt, you must supply some strings that are substituted for
the placeholders.

There is indeed a function that accepts a prompt template and a set of strings,
and returns a filled-in, complete template.

Very often what you'd do with that complete prompt is submit it to an LLM
to get a response, using the `call-model` function.  That takes a model name
and a prompt, and returns the response from the LLM.

For convenience, we define a variant of `call-model` that accepts parameter
strings that will be used to fill in the placeholders if the given prompt turns
out to be a template.

