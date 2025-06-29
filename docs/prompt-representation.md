# Prompt file format
## Attached Metadata in YAML front matter

Here we are concerned with the format of the files used to store the prompts and
their metadata, and not with the precise items of metadata that may be stored.

The prompt text is kept in a text file, with metadata about the prompt
encoded in YAML front matter, enclosed between lines of dashes.
We'll also call this front matter the header.

Each prompt in our population is thereby a self-documenting artifact with rich
metadata.

The metadata is not sent to the LLMs.  Only the prompt itself, the body of the file,
is ever seen by an LLM when it acts on the prompt.

When prompts are generated, some initial metadata is added as front matter.
This includes
   * id's of ancestor prompts if any
   * the algorithm or model that generated it
   * the id of the prompt that generated it, if any
   * creation timestamp
   * hash of the body text (see below)

In general, prompts are not modified after creation. The file body, containing the
prompt text, is immutable. If a prompt is mutated, a new prompt file will be
created to hold the mutated prompt, along with the new prompt's metadata.

Once prompt text has been written into a file, it is immutable.  The *text* is never changed,
but metadata is routinely added to the front matter.
Prompts are often analyzed to compute metrics (entropy, komogorov complexity, and the like)
and the computed metrics are added as metadata field in the front matter.

One of the metadata fields is a hash of just the prompt text, computed without any of the metadata.
This ensures that corruption of the text will be detected. This policy preserves
the pedigree of the prompts descended from it.

The hash is computed over the prompt text starting with first nonblank line after front matter.

The metadata in the prompt file header only describes properties of the prompt itself.

Details of its interactions with other processes, such as scores from contests,
should be recorded in records of those contests.
NOTE: We might relax this if it becomes useful to do so.

## Prompt file example

```
---
id: 323
generator: human
generated-at: '2022-08-17'
md5-hash: 7fd8e8e70235bc6fd5c164389fa81fd8
---

Find more precise way to state this instruction:
Discard all HTML tags.

```
