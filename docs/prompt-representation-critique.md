### Critique of the *Prompt-file format* specification

---

#### 1  Ambiguities & Inconsistencies

| Area                              | Issue                                                                                                                                                                                                                                                                                                                                            |     |                                             |
| --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --- | ------------------------------------------- |
| **Front-matter delimiters**       | You write “enclosed between lines of dashes” but do not pin down the exact delimiter. The example uses `---` on its own line *twice*; some Markdown processors demand a closing `---` while others accept `...`. The spec should name the delimiter unambiguously and forbid its appearance inside the prompt body (or explain the escape rule). |     |                                             |
| **Immutability vs. mutability**   | The text is said to be *immutable*, yet later you allow new metadata lines to be appended to the same file. That changes file contents—some version-control workflows treat this as a mutation. Clarify whether “immutable” means *byte-for-byte immutability of the body only* or *no new commits whatsoever*.                                  |     |                                             |
| **Hash field naming**             | The prose says “hash of the body text (see below)” but the example key is `md5-hash`. Is MD5 required, strongly discouraged, or merely illustrative? The field name and algorithm should be fixed in the spec or made parameterised.                                                                                                             |     |                                             |
| **Ancestor vs. parent**           | You list both “id’s of ancestor prompts” and “id of the prompt that generated it.” Those overlap (the direct parent *is* an ancestor). Decide whether you need both keys or one structured list with lineage depth.                                                                                                                              |     |                                             |
| **Date / time**                   | Example uses just a date (`2022-08-17`); earlier text promises “creation timestamp” (implying time-of-day and maybe timezone). Fix the expected format (e.g., ISO-8601 `2022-08-17T14:37:22Z`).                                                                                                                                                  |     |                                             |
| **“Generator” semantics**         | The metadata sometimes calls this field `algorithm or model that generated it`, sometimes `generator: human`. Is `generator` a free-text label, an enum (human                                                                                                                                                                                   | LLM | script), or an object with version numbers? |
| **YAML typing**                   | Are numbers such as `id: 323` guaranteed to be integers? YAML parsers treat bare numbers as *numeric*; if you intend string IDs (`"P0323"`), quoting rules must be declared.                                                                                                                                                                     |     |                                             |
| **Prompt hash computation**       | Stated start point: “first nonblank line after front matter”. End point is not fixed: is the trailing newline included? How are CRLF vs LF line endings canonicalised?                                                                                                                                                                           |     |                                             |
| **Where to put analysis metrics** | You allow entropy etc. to be “added as metadata,” yet also say the header “only describes properties of the prompt itself.” Some metrics depend on statistical models, not just the prompt string. Decide whether those belong in the same file or a companion record.                                                                           |     |                                             |

---

#### 2  Missing or Unstated Assumptions

* **Character encoding and normalisation** – The hash is meaningless unless encoding (UTF-8?) and Unicode-NFC/NFKC normalisation are fixed.
* **File extension / MIME type** – Should these files end with `.prompt`, `.yaml`, `.md`? Tools and editors depend on it.
* **Concurrency / locking** – If metadata can be appended later, how do you avoid two processes writing to the same file simultaneously?
* **Reserved vs. user-defined keys** – Which keys are mandatory, optional, or forbidden? How are unknown keys handled?
* **Maximum prompt length** – Not relevant to the format itself, but some production pipelines choke on multi-megabyte bodies.
* **Escaping triple dashes in body** – If the prompt itself contains a line with exactly `---`, the front-matter parser might stop early.
* **Lineage depth control** – Without pruning, the `ancestors` list could grow unbounded; do you store full lineage or a digest?
* **Validation schema** – A JSON-Schema or Kwalify spec would make automated linting straightforward.
* **Compression / binary artefacts** – If prompts can contain binary blobs (e.g., BASE64 images), does the format still apply?
* **Deletion / tombstones** – How do you mark a prompt as withdrawn or unsafe without deleting the file (and breaking hashes in descendants)?

---

#### 3  Questions to Clarify the Spec

1. **Delimiter rules**
   *Exactly which sequence* marks the end of the YAML header? Must it be `---` alone on a line, or may `...` be used?

2. **Hash algorithm & field name**
   Do you standardise on MD5 (weak) or allow SHA-256/SHA-512? Should the key be `hash`, `sha256`, or something else?

3. **Canonicalisation for hashing**
   What newline convention, Unicode normalisation, and trailing-newline policy apply before hashing?

4. **Mandatory metadata keys**
   Which keys *must* appear in every prompt file? `id`, `created_at`, `hash`, …? Is there a schema we can validate against?

5. **ID uniqueness & scope**
   Are `id` values unique across a repository, across time, or within one experiment? Might you switch to UUID/ULID?

6. **Lineage representation**
   Should `ancestors` be a top-level YAML list (`ancestors: [101, 102]`) or flattened string? How deep a chain do you expect?

7. **Time-zone handling**
   Will you require `Z`-suffix UTC or allow local time with offset (`2025-06-28T09:14:00-04:00`)?

8. **Body delimiter conflicts**
   If my prompt **must** contain `---` on a line by itself (e.g., Markdown snippet), how do I escape or encode it?

9. **Policy for post-creation edits**
   Are *any* edits to the body allowed under exceptional circumstances (e.g., redaction)? If so, how do you invalidate descendants?

10. **Storage hierarchy & filenames**
    Is there a recommended directory tree (by `id`, date, lineage) or naming convention (`000323.prompt`)?

11. **Versioning of the spec itself**
    How will you indicate future changes? Perhaps add `spec_version` to the header.

Answering these questions and tightening the loose points above will make the format precise enough for automated tooling and long-term reproducibility.
