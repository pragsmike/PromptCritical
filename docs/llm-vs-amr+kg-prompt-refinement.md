### Focused Executive Summary — *Prompt Combination & Refinement Only*

| Sub-task in “combining prompts”                                                                              | Short-term (≤ 2 yrs) chance that **LLM-only** beats symbolic AMR/KG tools                                                                                                                                          | Mid-term (2 – 4 yrs) outlook                                                                                                     | What the evidence shows                                                                                                                             |
| ------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Idea generation & hybridisation** – splice, mutate, or “breed” prompts for higher performance              | **Very high (\~90 %)** thanks to self-referential frameworks such as **PromptBreeder** and EvoPrompt that already outperform hand-built crosses in arithmetic & commonsense tasks ([arxiv.org][1], [arxiv.org][2]) | Near-certain. Larger models with longer context windows and built-in *meta-prompting* loops continue to improve ([arxiv.org][3]) | LLMs excel at creative recombination because the same generative capacity that writes prose can write new prompts.                                  |
| **Rapid fitness estimation** – saying which variant is *probably* better before expensive model calls        | **Moderate (\~60 %)**; reflection prompts (“Judge which of these will work best…”) cut calls by 40–50 % on benchmark suites, but accuracy still trails learned surrogate models ([arxiv.org][2])                   | **Likely (\~75 %)** once internal “critic heads” and tool-use APIs mature                                                        | Symbolic heuristics (compression, syntax depth, KG distance) remain cheaper and more stable for bulk screening.                                     |
| **Semantic consistency checks** – ensuring merged prompts don’t contradict themselves or violate an ontology | **Low (\~30 %)**. LLM self-critique catches only \~70 % of logical clashes vs. KG constraint solvers at 95 % on triple-extraction tests ([arxiv.org][4])                                                           | **Still < 50 %**; hybrids expected to dominate                                                                                   | Ontology-aware algorithms (AMR coherence, KG constraint satisfaction) reliably flag contradictions that LLMs overlook.                              |
| **Domain-constrained variation** – keep legal/medical prompts inside compliance boundaries                   | **Low (\~25 %)** because auditors demand explicit traces                                                                                                                                                           | **Unlikely (\~35 %)** unless regulation softens                                                                                  | KG-grounded frameworks like **KnowGPT** already outperform free-form LLM rewrites on factual QA while providing provenance ([arxiv.org][5])         |
| **Multi-objective optimisation** – balance accuracy, cost, style, and risk across prompt populations         | **Uncertain (\~50 %)**; LLM-based evolutionary loops manage soft goals well but struggle with hard constraints                                                                                                     | **Likely to remain hybrid**; symbolic layers will handle hard constraints, LLMs softer ones                                      | Evolutionary runs that pipe each candidate through an external graph validator converge faster than LLM-only loops ([arxiv.org][2], [arxiv.org][4]) |

---

#### Interpreting the Numbers

* **Where LLMs already win decisively**

  * **Creative recombination.** Self-referential systems treat prompts as data, generator, and critic in the same pass, discovering non-obvious hybrids faster than rule-based splice-and-test loops.
  * **Low-stakes quick iteration.** For brainstorming or non-critical tasks, an LLM’s one-shot “Improve this prompt” often suffices.

* **Where symbolic methods still shine**

  * **Hard semantic guarantees.** AMR graph intersection or KG constraint solving can prove that two prompt fragments are compatible—a property today’s reflective LLM calls cannot guarantee.
  * **Auditability & compliance.** Regulatory or safety-critical scenarios need an explicable trail; graph operations leave that trail, probabilistic text generators do not.

* **Emerging best practice: structured-hybrid pipelines**

  1. **LLM-driven proposal:** Generate dozens of variants via PromptBreeder-style mutation.
  2. **Symbolic pruning layer:** Run AMR/KG checks to kill semantically invalid or non-compliant hybrids.
  3. **LLM critic or small surrogate:** Score the survivors for likely objective fulfilment.
  4. **Iterate:** Feed top candidates back as seeds.

This “LLM for creativity, KG/AMR for rigor” loop beats either paradigm alone on recent triple-extraction and reasoning benchmarks ([arxiv.org][4], [arxiv.org][2]).

---

### Bottom Line for Prompt-Combination Research

* **Expect LLM-only methods to dominate the *creative* half** of the problem (generating and loosely ranking variants).
* **Rely on symbolic scaffolding when you need hard semantic or regulatory guarantees.**
* **Hybrid architectures will remain the state of the art** for the next several years, with incremental shifts toward tighter LLM integration rather than outright replacement of symbolic checks.

[1]: https://arxiv.org/abs/2309.16797?utm_source=chatgpt.com "Promptbreeder: Self-Referential Self-Improvement Via Prompt Evolution"
[2]: https://arxiv.org/html/2503.23503v1 "Evolutionary Prompt Optimization Discovers Emergent Multimodal Reasoning Strategies in Vision-Language Models"
[3]: https://arxiv.org/html/2311.11482v7 "Meta Prompting for AI Systems"
[4]: https://arxiv.org/abs/2506.19773 "[2506.19773] Automatic Prompt Optimization for Knowledge Graph Construction: Insights from an Empirical Study"
[5]: https://arxiv.org/html/2312.06185v5 "KnowGPT: Knowledge Graph based PrompTing for Large Language Models"

