# What You're Assumed to Know

This project targets researchers and programmers who already work with large language models (LLMs).  The sections below spell out the background we assume you have, and point to concise external references when you need a refresher.

---

## Table of Contents

| Section                                                          | What you should already grasp                                                                       |
| ---------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| **[LLM Essentials](#llm-essentials)**                            | The vocabulary of tokens & context windows, basic prompt/response cycle, calling an LLM API         |
| **[Prompt Engineering Basics](#prompt-engineering-basics)**      | System / user / assistant roles, few‑shot vs zero‑shot, chain‑of‑thought & self‑reflection patterns |
| **[Unix Shell & Git](#unix-shell--git)**                         | Navigating directories, creating symlinks, committing & branching in Git                            |
| **[Clojure & EDN Syntax](#clojure--edn-syntax)**                 | How to read parentheses‑heavy Clojure code and write EDN config maps                                |
| **[Polylith Workspace & Tooling](#polylith-workspace--tooling)** | The workspace layout, the `poly` CLI and how components are wired together                          |
| **[PromptCritical File Layout](#promptcritical-file-layout)**    | Where prompts live, why we use symlinks, naming conventions for `seed`, `improve`, `refine`, etc.   |
| **[Cost Tracking & API Usage](#cost-tracking--api-usage)**       | Interpreting token‑cost tables and tracing where calls originate                                    |

---

## LLM Essentials

We assume you already understand:

* **Tokenization & context windows** – how model input is chunked and the relationship between model size and max tokens.
* **Temperature / top‑p / log‑probabilities** – the knobs that trade off determinism and diversity.
* **Embedding vs generation endpoints** – why you would call one or the other.

> *Quick ref*: [OpenAI API quickstart](https://platform.openai.com/docs/quickstart) · [Anthropic Claude docs](https://docs.anthropic.com/claude/getting-started)

---

## Prompt Engineering Basics

You should be comfortable with:

* **Prompt roles** – `system`, `user`, `assistant` (and how they influence model behavior).
* **Few‑shot / zero‑shot prompting** – supplying exemplars to steer style or format.
* **Chain‑of‑thought & self‑critique** – eliciting visible reasoning or asking the model to verify its own work.

A compact overview is *"Prompt Engineering Guide – Quick Patterns"* [https://www.promptingguide.ai](https://www.promptingguide.ai).

---

## Unix Shell & Git

PromptCritical leans on simple shell commands and Git for workflow automation.  We expect you can:

* Navigate directories (`cd`, `ls`), create symbolic links (`ln -s`).
* Understand file permissions and shebangs.
* Clone, branch, commit, and rebase with Git.

If rusty, skim *"Pro Git"* Chapter 2 ([https://git-scm.com/book/en/v2](https://git-scm.com/book/en/v2)) and the *Bash Cheat Sheet* ([https://devhints.io/bash](https://devhints.io/bash)).  Symlink fundamentals: [https://linuxize.com/post/how-to-create-symbolic-links-in-linux/](https://linuxize.com/post/how-to-create-symbolic-links-in-linux/).

---

## Clojure & EDN Syntax

Most glue code and configuration is written in **Clojure** and **EDN**.  You should recognize:

* Prefix notation and immutable data structures.
* Namespaces, `require` / `import` patterns.
* EDN literals – vectors `[1 2]`, maps `{:k v}` and keywords `:foo/bar`.

Helpful primers: [Clojure for the Brave & True – Ch. 1‑3](https://www.braveclojure.com/do-things/) · [EDN spec](https://github.com/edn-format/edn).

---

## Polylith Workspace & Tooling

PromptCritical follows [Polylith](https://polylith.gitbook.io/polylith) conventions.  At a glance:

1. **Single workspace, many projects** – all components live side‑by‑side and are tested together.
2. **Components declare only external deps** in their own `deps.edn`; the *workspace* alias glues everything for dev/test.
3. **Projects assemble deliverables** by selecting the components they need.

### Quick start

```bash
# Use the poly CLI (already in deps.edn):
alias poly='clj -M:poly '

# Run the full test suite
poly test

# Execute the CLI application
cd projects/pcrit-cli && clj -M:run <command>
```

*Using Emacs CIDER?*  Open a REPL from the repo root; `.dir-locals` will auto‑inject the `:dev:test` aliases so every brick is on the classpath.  See the [CIDER manual](https://docs.cider.mx/cider/basics/up_and_running.html).

---

## PromptCritical File Layout

```
docs/                  ← Markdown documentation
links/                 ← Symlinks pointing at canonical prompt files
prompts/seed/…         ← Initial prompt samples
prompts/improve/…      ← Unary mutations (“improve”)
prompts/refine/…       ← Fine‑tune or bug‑fix iterations
```

*Symlinks matter.*  Command‑line tools hard‑code the `links/` paths, so moving or renaming them will break examples.  See `docs/prompt-symlinks.md` for the complete mapping.

---

## Cost Tracking & API Usage

PromptCritical tracks usage cost at two layers:

1. **Runtime accounting** inside the `pcrit-llm` library (authoritative token prices).
2. **Post‑run auditing** – CSV logs aggregated by the `evo-cost-calculation.md` notebook.

Make sure you understand how your API key’s pricing model works (e.g., input vs output tokens) before running large batches.

> Reference: [OpenAI pricing](https://openai.com/pricing) · [Anthropic pricing](https://docs.anthropic.com/claude/reference/pricing)

---

*If you are comfortable with everything above, you are ready to dive into the rest of the PromptCritical documentation.*
