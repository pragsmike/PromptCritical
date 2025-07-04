# Evolution Experiment

The unit of work is the experiment.

The specification of the experiment and its state all reside under a single
directory, known as the experiment directory.

NOTE: The `failter` tool also uses the term "experiment" to refer to a set of trials
that it runs.  Those are actually sub-experiments of our evolution experiment.
We'll use the term *contest* to refer to a `failter` experiment.

## Experiment directory structure

The specification and current state of an experiment reside under a directory.
We'll call this directory the experiment specification directory, the experiment
directory, or simply the experiment.

An experiment specification is a directory containing
   * `seeds`
      * the seed prompt
      * the initial metaprompts
   * `links`
   * `pdb` prompt store directory
   * `bootstrap.edn` manifest
   * `evolution-parameters.edn`

The `prompts` directory holds the initial seed prompt and the initial mutators,
in raw form, without any metadata.  During bootstrapping, they are ingested into
the prompt store `pdb`.

The `links` directory holds symlinks to where the seed prompts ended up in the `pdb` store.
They are given names as specified in the `bootstrap.edn` file.

The evolution parameter file gives the parameter settings that control the evolution algorithm.

## Bootstrapping

An experiment starts with bootstrapping a population.
In the bootstrap step, it sets up generation zero from the bootstrap manifest specification.
Then it carries out one round of evolution by applying the specified mutation meta-prompts
to a seed prompt.  This produces generation one.

The bootstrap sequence uses a set of prompts, specified in the manifest:
   * the seed prompt, which is the first individual in the population
   * a set of meta-prompts, which produce one or more new prompts from a given one.
     These are the initial set of evolution operators.
     * refine a given prompt to produce another
     * vary to produce one or more variants of a given prompt

The seed prompt and the metaprompts originally come from a pool of "raw" prompts
in the `seeds` directory. Before they are actually used in the
experiment, they are ingested into the prompt database just like any other
prompt in the population.

The `ingest` operation assigns each prompt an id that is used to track ancestry
of their descendants, and adds some metadata describing properties of the
prompt.
   * `template-field-names`
   * `character-count`
   * `word-count`

All the prompts are actually prompt templates (see Prompt Templating).
We must supply values to fill in their fields before we can submit them to an LLM.
The names of the fields that must be supplied are found in the `template-field-names`
key in the metadata.

## Evaluation and Selection

In this first increment of development, we are restricting ourselves to very
simple methods for the `winnow-and-breed` operation on the population.

1. Failter evaluation:
   - Package all 5 prompts for Failter
   - Run on blog post corpus
   - Ingest fitness scores back to prompt files

2. Simple selection + mutation:
   - Eliminate worst performer
   - Mutate best performer → new prompt
   - Population: [4 survivors + 1 new mutant]

