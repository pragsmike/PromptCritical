# Evolution Process

PromptCritical optimizes prompts for a particular task by carrying out
an evolutionary process to explore a prompt fitness landscape.

## Evolution Cycle Theory

Each "generation" has as its population a set of prompts.
Briefly, each generation undergoes a "breed-vie-winnow" cycle.
That is, the population is replaced by a new one computed by a composite function `evolve`,
which takes a set of prompts (the population) and returns a new set of prompts (the new population).

`evolve` : population -> population is thus an endofunction on the powerset of the set of all prompts.

`evolve` is composed from a number of other functions on the population:
  * `breed` : population -> population
  * `contest` : population -> scores
  * `select` : population x scores -> population

You can think of scores as a function from the population to integers 0-100.
You can think of select as a function from population to population, parameterized by scores.

## Evolution Experiment

The unit of work is the experiment.  It consists of a number of generations, or breed-vie-winnow cycles.
Each generation has:
   * a population - a set of prompts, or members
   * an evolution - application of the `evolve` function, with specified parameters
      * a birthing - (breed) produce new members
      * a contest - (vie) an evaluation that assigns fitness to each member
      * a selection - (winnow) purges unfit members from the population, using contest results

The experiment process runs several cycles, each producing a new generation.
The experiment ends when a specified condition becomes true.  Examples of
exit conditions:
    * a certain number of generations is reached
    * fitness score of a generation's "champion" prompt reaches a threshold

The specification of the experiment and its state all reside under a single
directory, known as the experiment directory.

NOTE: The `failter` tool also uses the term "experiment" to refer to a set of trials
that it runs.  Those are actually sub-experiments of our evolution experiment.
We'll use the term *contest* to refer to a `failter` experiment.

### Experiment directory structure

The specification and current state of an experiment reside under a directory.
We'll call this directory the experiment specification directory, the experiment
directory, or simply the experiment.

An experiment specification is a directory containing
   * `seeds`
      * the seed prompt
      * the initial metaprompts
   * `links` symlinks to notable prompts, both object and meta
   * `pdb` prompt store directory
   * `generations`
      * `gen-NNN`
        * `evolution` specification and results of evolution operations (winnow, mutate, breed)
        * `contests` contest specifications and results
   * `bootstrap.edn` manifest
   * `evolution-parameters.edn`

The `prompts` directory holds the initial seed prompt and the initial mutators,
in raw form, without any metadata.  During bootstrapping, they are ingested into
the prompt store `pdb`.

The `links` directory holds symlinks to where the seed prompts ended up in the `pdb` store.
They are given names as specified in the `bootstrap.edn` file.

The evolution parameter file gives the parameter settings that control the evolution algorithm.

### Bootstrapping

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

#### Evaluation and Selection

In this first increment of development, we are restricting ourselves to very
simple methods for the `winnow-and-breed` operation on the population.

1. Failter evaluation:
   - Package all 5 prompts for Failter
   - Run on blog post corpus
   - Ingest fitness scores back to prompt files

#### Contest Architecture

```
generations/gen-NNN/contests/
├── 2025-07-01-web-cleanup/
│   ├── participants/           # symlinks to P001, P002, etc.
│   ├── failter-spec/          # experiment definition
│   ├── results.csv            # scores and rankings
│   └── contest-metadata.yaml  # timestamp, generation, etc.
```

This creates a **complete audit trail** where you can trace any prompt's
evolutionary performance history through the contests it participated in.

2. Simple selection + mutation:
   - Eliminate worst performer
   - Mutate best performer → new prompt
   - Population: [4 survivors + 1 new mutant]

