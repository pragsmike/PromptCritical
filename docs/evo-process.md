The unit of work is the experiment.

An experiment starts with bootstrapping a population.
To do this, it uses a set of prompts:
   * the seed prompt, which is the first individual in the population
   * a set of meta-prompts, which produce one or more new prompts from a given one.
     These are the initial set of evolution operators.
     * refine a given prompt to produce another
     * vary to produce one or more variants of a given prompt

An experiment specification is a directory containing
   * the seed prompt
   * the metaprompts
   * a configuration file that gives the parameter settings that control the
     evolution algorithm
We'll call this directory the experiment specification directory, the experiment directory,
or simply the experiment.

NOTE: The `failter` tool also uses the term "experiment" to refer to a set of trials
that it runs.  Those are actually sub-experiments of our evolution experiment.
We'll use the term *contest* to refer to a `failter` experiment.

All the prompts are actually prompt templates (see Prompt Templating).
We must supply values to fill in their fields before we can submit them to an LLM.

The seed prompt and the metaprompts originally come from a pool of "raw" prompts in
the experiment specification directory.
Before they are actually used in the experiment, they are ingested into the prompt database
just like any other prompt in the population.

The `ingest` operation assigns each prompt an id that is used to track ancestry
of their descendants, and adds some metadata describing properties of the
prompt.
   * `template-field-names`
   * `character-count`
   * `word-count`

