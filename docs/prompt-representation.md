# Prompt file format

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

Each prompt is assigned an `id` the first time it is stored.
The id is a string of the form "Pnnn" where nnn is an integer. It is guaranteed not to
collide with an existing prompt. The implementation will try to keep the
integers as small as possible and still guarantee uniqueness.

NOTE: The code will likely keep a file that holds the next id integer
and will update it atomically when it draws one.

## File naming and directory organization

Prompt files are written with the extension `.prompt`.

Processors will accept any name with that extension, but
they always produce new files with the prompt `id` as the name.

All prompt files are stored under a single directory whose name is specified
to the tool by the experimenter at runtime.

For now, we store all the files in one directory.
In the future, we may shard them across a set of subdirectories to shorten
lookup time by the filesystem.


## File encoding

The file is ALWAYS UTF-8, and the files are written without any UTF-8 BOM bytes.
Line endings are LF. The parser will tolerate CRLF but will convert it to LF internally.
The hash computation uses LF canonically (see below).

When any of the processors writes a prompt file, it applies this canonicalization:

   * Line endings (CR, LF, or CRLF) are converted to LF.
   * If there is no LF at the end of the last line, one is added.


## Attached Metadata in YAML front matter

Here we describe the metadata stored with the file when it is created. Other
processors, such as analyzers, will later add other metadata, but we are not
concerned with those details here, other than to state that the front matter
will always be parseable as YAML.

### Front matter encoding

Placement: The YAML front matter must be the very first thing in the file.

Delimiters: It is enclosed between lines containing only three dashes (---). The
block starts with --- on the first line and ends with another --- .
NOTE: Some YAML parsers expect the end marker to be three dots ... but we disallow that.
The end marker MUST be three dashes on a line by themselves with no whitespace anywhere.

Content: The content between the delimiters must be valid YAML. This typically
includes key-value pairs, arrays, and sometimes multiline strings.

NOTE: The YAML will ALWAYS be UTF-8.  The file writer will ALWAYS normalize
it before writing the file, as described below.  Validators should expect
normalized text.

Because the front matter occurs at the very start of the file, the
text of the prompt need not be escaped.  It doesn't matter if
the prompt contains --- because the YAML parser never sees it.

### Initial and Evolutionary Metadata

When prompts are generated, some initial metadata is added as front matter.
This always includes these keys:
   * `spec-version`: currently always "1"
   * `id`: a string of the form "Pnnn" where nnn is an integer starting at 1
   * `created-at`: timestamp
   * `sha1-hash`: of the body text (see Hashing Algorithm, below)

As prompts evolve, the following keys are added to their headers to record
their full lineage directly within the prompt artifact itself.

*   `parents`: (list of strings) The IDs of the direct parent prompts.
*   `generator`: (map) A description of the `vary` operation that produced the prompt. Added by the `vary` command.
    ```yaml
    # Example generator block
    generator:
      model: "gpt-4-turbo"
      meta-prompt: "P2"
      vary-run: "2025-07-11T12:00:00Z"
    ```
*   `selection`: (map) A description of the `select` operation that confirmed the prompt as a "survivor". Added by the `select` command.
    ```yaml
    # Example selection block
    selection:
      policy: "top-k"
      contest-name: "web-cleanup-v3"
      select-run: "2025-07-11T13:00:00Z"
    ```

Other ancestry information MAY be added by further research. The depth of the ancestry chain is not
bounded by this document.

As of now, otherwise the metadata fields are free-form except:
  * Timestamps are ISO-8601 Zulu zone (Z suffix), eg "2022-08-17T14:37:22Z"
  * the hash is a 40-character hexadecimal string (see below)

The sha1-hash is added automatically when the file is first written.
Other processors may add other hashes under their own keys, such as `sha256-hash`.

## Prompt text (body) never changes

In general, prompt text is not modified after creation, though metadata can be.
The file body, containing the prompt text, is treated as immutable.
However, metadata is routinely added to the front matter,
and the entire file is rewritten with only the metadata changed
and the prompt text unchanged.

If a prompt is mutated, a new prompt file will be created to hold the mutated offspring
prompt, along with the new prompt's metadata.

Once prompt text has been written into a file, it is immutable. The *text* is never changed.
Prompts are often analyzed to compute metrics (entropy, kolmogorov complexity, and the like)
and the computed metrics are added as metadata field in the front matter.

One of the metadata fields is a hash of just the prompt text, computed without any of the metadata.
This ensures that corruption of the text will be detected. This policy preserves
the pedigree of the prompts descended from it.

Details of its interactions with other processes, such as scores from contests,
should be recorded in records of those contests, not in the prompt header itself.

## Hashing algorithm

The prompt body text (NOT the YAML metadata) is protected against corruption by
a hash code that is stored in the metadata.

Again: NONE of the YAML participates in the hashing computation.

SHA-1 produces a 160-bit (20-byte) hash value, represented as a 40-character hexadecimal string.
These are written using lowercase characters, but the parser will also accept uppercase.
NOTE: The purpose of the hash is to detect corruption, NOT to uniquely identify the prompt.
It is OK if two prompts have the same hash. It's very likely that they're the same text,
but it doesn't matter if they don't due to (unlikely) hash collision.

The hash is computed over the prompt text as if it were extracted from the prompt file as follows:
   * The starting point is the beginning of the first nonblank line after the YAML ending separator.
   * The text is encoded as UTF-8, and normalized by NFC.
   * Line endings (CR, LF, or CRLF) are converted to LF.
   * If there is no LF at the end of the last line, a LF is appended.

This canonicalization happens to be what the file writer does when it writes the file.

## Concurrency – `.lock` file protocol

When multiple processes might update a prompt’s metadata concurrently, they coordinate through a per-prompt lock file.
The lock file lives **next to** the prompt file and has the suffix `.lock` (e.g., `P123.prompt.lock`).
Creating the lock file with the *exclusive-create* flag (`O_CREAT | O_EXCL` on POSIX) is atomic on local filesystems and therefore sufficient for mutual exclusion in normal operation.

### Step-by-step procedure

1. **Acquire lock**
2. **Read current prompt file**
3. **Write updated version to temporary file**
4. **Atomic replace**
5. **Release lock**

*(Procedure details omitted for brevity; see full documentation for recovery rules and platform notes.)*

## Prompt file example

```
---
id: "P323"
created-at: "2025-07-11T12:00:00Z"
sha1-hash: "7fd8e8e70235bc6fd5c17fd8e8e70235bc6fd5c1"
parents:
- "P12"
generator:
  model: "gpt-4o-mini"
  meta-prompt: "P2"
  vary-run: "2025-07-11T12:00:00Z"
selection:
  policy: "top-k-5"
  contest-name: "web-cleanup-v4"
  select-run: "2025-07-11T14:00:00Z"
---

Find a more precise way to state this instruction:
Discard all HTML tags.

```
