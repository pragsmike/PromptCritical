Some prompts are self-contained, and can be submitted to the LLM as is.

Many prompts are actually templates, with placeholders that get filled in. To
produce a complete prompt, you must supply some strings that are substituted for
the placeholders.  We call these placeholders *fields*.

The placeholder is encoded as the field name, kebab-case in all caps, surrounded by pairs of curly braces
like this `{{FIELD-NAME}}`.

There is a function `llm.templater/expand` that accepts a prompt template and a set of strings,
and returns a filled-in, complete template.
Note that the `llm` component is unaware of prompt metadata and doesn't use or change it.

Very often what you'd do with that complete prompt is submit it to an LLM
to get a response, using the `call-model` function.  That takes a model name
and a prompt, and returns the response from the LLM.

For convenience, we define a variant of that named `call-model-template` that accepts parameter
strings that will be used to fill in the placeholders if the given prompt turns
out to be a template.

When considering how to apply a prompt, we need to know what fields
it has so we can supply values to fill them in.

We can analyze the prompt to find the field placeholders. To save work later,
when the template is ingested into the store we write this information to
metadata fields:
   * `template-field-names`

If that metadata field is missing or its value is empty, the prompt is already a proper prompt
and need not be filled out.
