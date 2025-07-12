### What the “meta-prompt” symlinks are

*Under `links/` you will find short, human-readable pointers such as `seed`, `improve`, `refine`, `vary`, etc.*
Each of these is a **relative symbolic link** that points into the immutable prompt-database (`pdb/P42.prompt`, `pdb/P7.prompt`, …). They give every command—and you—stable names for important prompts without having to know the internal `P-id` that the database assigns.

---

### Who creates the links?

1. **`pcrit init`** merely drops a `bootstrap.edn` manifest that *describes* the links, but it does not touch `links/`.
2. **`pcrit bootstrap`** is the command that *materialises* them:

   * It ingests every entry in `bootstrap.edn` into the PDB, getting back prompt records.
   * For each `[link-name record]` pair it calls `expdir/link-prompt!`, which in turn calls the low-level `create-relative-symlink!` helper to make a relative link in `links/` that targets the real `.prompt` file in `pdb/` , .

   The bootstrap test suite shows the expected result—`seed`, `refine`, `vary`, … all appear after the command runs .
   (Nothing else in the current codebase creates or modifies these meta-prompt links.)

---

### Who uses the links?

* **`pcrit vary`**: before it can mutate the population, it needs a meta-prompt.
  It resolves `links/refine` (hard-coded today) via `pop/read-linked-prompt`, loads that prompt from the PDB, and feeds it into the LLM call .
* Other commands are *link-agnostic*:

  * `evaluate` and `select` operate on the generation’s `population/` directory (those links are created elsewhere).
  * You can still read any linked prompt manually with `pop/read-linked-prompt`, which several test helpers do for convenience.

Because the links are **relative**, you can move or rename the entire experiment directory (e.g. as the integration test does) and everything still resolves correctly .

---

### What decides which link points where?

The **keys in `bootstrap.edn` are the contract**.  Example from the default scaffold:

````edn
{:seed    "seeds/seed-object-prompt.txt"
 :improve "seeds/improve-meta-prompt.txt"}
``` :contentReference[oaicite:4]{index=4}

* The *key* (`:seed`, `:improve`, …) becomes the filename under `links/`.
* The *value* is the path of the raw text file to ingest.
* During bootstrap the ingestion process assigns each prompt an internal ID (`P1`, `P2`, …); the symlink is then created to that packed filename.

So if you want a new meta-prompt called `rephrase`, you just add:

```edn
:rephrase "seeds/rephrase-meta-prompt.txt"
````

to `bootstrap.edn`, put the file in `seeds/`, rerun `bootstrap`, and the system will automatically create `links/rephrase → ../pdb/Pn.prompt`.

---

### Why do we bother with this indirection?

* **Human ergonomics**: scripts and docs can say “use `links/improve`” instead of “use `P7.prompt`”.
* **Immutability & provenance**: the PDB file never moves or changes; the link is just a mutable pointer.
* **Portability**: relative links keep experiments reproducible even after `mv`-ing the directory to a new path.

In short, *bootstrap* owns the creation, *vary* (plus any helper utilities) read the links, and the **manifest keys** decide the mapping.
