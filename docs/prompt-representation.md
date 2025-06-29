# Prompt file format
## Attached Metadata in YAML front matter

Here we are concerned with the format of the files used to store the prompts and
their metadata, and not with the precise items of metadata that may be stored,
other than a set of mandatory initial items described below.

The prompt text is kept in a text file, with metadata about the prompt
encoded in YAML front matter, enclosed between lines of dashes.
We'll also call this front matter the header.

Each prompt in our population is thereby a self-documenting artifact with rich
metadata.

The metadata is not sent to the LLMs.  Only the prompt itself, the body of the file,
is ever seen by an LLM when it acts on the prompt.

## Prompt IDs

Each prompt is assigned an ID the first time it is stored.
The id is a string of the form "Pnnn" where nnn is an integer. It is guaranteed not to
collide with an existing prompt. The implementation will try to keep the
integers as small as possible and still guarantee uniqueness.

NOTE: The code will likely keep a file that holds the next id integer
and will update it atomically when it draws one.

## File naming

Prompt files are written with the extension `.prompt`.

Processors will accept any name with that extension, but
they always produce new files with the prompt ID as the name.

## File encoding

The file is ALWAYS UTF-8, and the files are written without any UTF marker bytes.
Line endings are LF. The parser will tolerate CRLF but will convert it to LF internally.
The hash computation uses LF canonically (see below).

When any of the processors writes a prompt file, it applies this canonicalization:

   * Line endings (CR, LF, or CRLF) are converted to LF.
   * If there is no LF at the end of the last line, one is added.


## Initial metadata

When prompts are generated, some initial metadata is added as front matter.
This always includes these keys:
   * `prompt-id` a string of the form "Pnnn" where nnn is an integer starting at 1
   * `created-at` timestamp
   * `sha1-hash` of the body text (see Hashing Algorithm, below)

If any of these fields is missing, processing will still proceed, but the
processor will print a warning, and it will rewrite the file with correct
initial metadata. This allows prompts to be quickly added by hand for testing.

A CLI utility will also be provided that will rewrite such a file with correct
metadata.

The initial metadata may also include these keys:
   * id's of ancestor prompts if any, as a YAML list
   * the algorithm that generated it (freeform)
   * the model name and id of the meta-prompt that generated it, if any

As of now, these fields are free-form except
  * Timestamps are ISO-8601, eg "2022-08-17T14:37:22Z"
  * the hash is a 40-character hexadecimal string (see below)

## Prompt text (body) never changes

In general, prompt text is not modified after creation, though metadata can be.
The file body, containing the prompt text, is treated as immutable.
However, metadata is routinely added to the front matter,
and the entire file is rewritten with only the metadata changed
and the prompt text unchanged.

If a prompt is mutated, a new prompt file will be created to hold the mutated offspring
prompt, along with the new prompt's metadata.

Once prompt text has been written into a file, it is immutable.  The *text* is never changed,
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

## Hashing algorithm

The prompt body text (NOT the YAML metadata) is protected against corruption by
a hash code that is stored in the metadata.

SHA-1 produces a 160-bit (20-byte) hash value, represented as a 40-character hexadecimal string.
These are written using lowercase characters, but the parser will also accept uppercase.

The hash is computed over the prompt text as if it were extracted from the prompt file as follows:
   * The text is ALWAYS UTF-8
   * the starting point is the beginning of the first nonblank line after the YAML ending separator
   * Line endings (CR, LF, or CRLF) are converted to LF.
   * If there is no LF at the end of the last line, one is added.
   * The ending point is the end of the file, after the last newline.

This canonicalization happens to be what the file writer does when it writes the file.

## Prompt file example

```
---
id: "P323"
generator: "human"
generated-at: "2022-08-17T14:37:22Z"
sha1-hash: "7fd8e8e70235bc6fd5c1"
---

Find more precise way to state this instruction:
Discard all HTML tags.

```




