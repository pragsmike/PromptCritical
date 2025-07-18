## Prompt-Optimization Resources

### Curated Meta-Resource Lists

* **[Awesome-LLM-Prompt-Optimization](https://github.com/jxzhangjhu/Awesome-LLM-Prompt-Optimization)** — *Jingxuan Zhang*, 2023 – present
  Continuously updated GitHub catalog of papers, codebases, and tutorials focused on prompt design, tuning, and evolutionary optimization for large language models. A convenient entry point for surveying state-of-the-art techniques.

* **[PromptPapers: Must-Read Papers on Prompt-Based Tuning](https://github.com/thunlp/PromptPapers)** — *THUNLP Group*, 2025 – present
  Curated reading list that organizes influential research on prompt engineering, adaptation, and evaluation across tasks and model families, with tags, summaries, and citation links for quick exploration.

### Evolutionary & Genetic Prompt-Optimization

* **[Connecting Large Language Models with Evolutionary Algorithms Yields Powerful Prompt Optimizers](https://arxiv.org/abs/2309.08532)** — *Qingyan Guo et al.*, 2024
  Introduces **EvoPrompt**, in which an LLM performs mutation and crossover while an evolutionary algorithm selects high-scoring prompts, yielding up to 25 % gains on BIG-Bench Hard and 31 other benchmarks.

  * **[EvoPrompt — Evolutionary Algorithms Meets Prompt Engineering: A Powerful Duo](https://ai.gopubby.com/evoprompt-evolutionary-algorithms-meets-prompt-engineering-a-powerful-duo-c30c427e88cc)** — *Austin Starks*, 2024
    Practitioner-friendly blog post that walks through EvoPrompt’s operators and highlights early use-cases.

  * **[Microsoft Research’s EvoPrompt – Evolutionary Algorithms Meets Prompt Engineering](https://www.reddit.com/r/MachineLearning/comments/1aji7np/d_microsoft_researchs_evoprompt_evolutionary/)** — Reddit thread, 2025
    Community discussion sharing implementation tips and caveats.

  * **[EvoPrompt (Official implementation)](https://github.com/beeevita/EvoPrompt)** — GitHub repo, 2024
    Reference code, datasets, and scripts to reproduce EvoPrompt results.

* **[GAAPO: Genetic Algorithmic Applied to Prompt Optimization](https://arxiv.org/abs/2504.07157)** — *Xavier Sècheresse et al.*, 2025
  A hybrid GA that blends OPRO-style search with error-driven “textual gradients,” trimming prompt length by 40–60 % while raising accuracy on ETHOS, MMLU-Pro, and GPQA.

  * **[\[Literature Review\] GAAPO](https://www.themoonlight.io/review/gaapo-genetic-algorithmic-applied-to-prompt-optimization)** — *Moonlight AI*, 2025
    Concise critique situating GAAPO within the prompt-optimization landscape.

* **[Promptbreeder: Self-Referential Self-Improvement via Prompt Evolution](https://arxiv.org/abs/2309.16797)** — *Chrisantha Fernando et al.*, 2023
  Evolves prompts that themselves write mutations, leading to continual, model-agnostic performance gains.

* **[PromptOptimization: Genetic Algorithms for Prompt Optimization](https://community.openai.com/t/promptoptimization-genetic-algorithms-for-prompt-optimization/457839)** — OpenAI forum post, 2023
  Open-source GA framework that refines chatbot system prompts against unit tests.

* **[Promptimizer — A Genetic-Algorithm-Based Prompt Optimization Framework](https://www.reddit.com/r/MachineLearning/comments/1edgtft/d_i_created_promptimizer_a_genetic_algorithm/)** — Reddit post, 2025
  Describes a lightweight GA toolkit and early benchmark wins.

* **[A Novel Approach to LLM Prompt Injection Using Genetic Algorithms](https://www.brightsec.com/blog/llm-prompt-injection/)** — *Bar Hofesh*, 2023
  Shows how evolutionary search can craft adversarial prompts that bypass safety filters, underscoring dual-use risks.

* **[Exploring the Prompt Space of Large Language Models through Evolutionary Sampling](https://dl.acm.org/doi/10.1145/3638529.3654049)** — *Martina Saletta & Claudio Ferretti*, 2024
  Uses structured grammatical evolution to systematically chart high-performing regions of the prompt landscape.

* **[Diverse Prompts: Illuminating the Prompt Space of Large Language Models with MAP-Elites](https://arxiv.org/abs/2504.14367)** — *Gabriel M. Santos et al.*, 2025
  Applies MAP-Elites to uncover multiple local optima, yielding a portfolio of diverse, effective prompts.

* **[An Evolutionary Large Language Model for Hallucination Mitigation](https://arxiv.org/abs/2412.02790)** — *Abdennour Boulesnane & Abdelhakim Souilah*, 2024
  Demonstrates that evolving prompts can significantly reduce hallucinations in generation tasks.


### Evolutionary & Swarm-Intelligence Prompt Optimizers

* **[PROPEL: Prompt Optimization with Expert Priors for Small and Medium-sized LLMs](https://aclanthology.org/2025.knowledgenlp-1.25/)** — *Kawin Mayilvaghanan, Varun Nathan & Ayush Kumar*, 2025
  PROPEL orchestrates three LLM roles—**Responder**, **Judge**, and **Optimizer**—and injects prompt-design principles as “expert priors” during an iterative search. The framework lifts response quality by up to **24 %** on query-based summarization and **16 %** on entity extraction for 1-8 B-parameter models, comfortably outperforming baseline optimizers while remaining model-agnostic.

* **[SwarmPrompt: Swarm Intelligence-Driven Prompt Optimization Using Large Language Models](https://www.scitepress.org/Papers/2025/130903/130903.pdf)** — *Thilak S. Shriyan, Janavi Srinivasan, Suhail Ahmed, Richa Sharma & Arti Arya*, 2025
  This ICAART 2025 paper adapts **Particle Swarm Optimization** and **Grey Wolf Optimization** to discrete prompt search, treating each candidate as a “swarm agent” refined via LLM-guided updates. SwarmPrompt surpasses human-crafted prompts by **≈ 4 %** on classification and **≈ 2 %** on simplification/summarization while halving the iterations needed for convergence.

---

### Gradient-Free & Reinforcement-Based Prompt Search

* **[Black-Box Prompt Optimization (OPRO): Aligning LLMs without Model Training](https://arxiv.org/pdf/2309.03409.pdf)** — *Xiao Liu et al.*, 2023
  Frames prompt search as a derivative-free optimization problem and uses a bandit approach to close the gap with fine-tuned models.

* **[TextGrad: Automatic “Differentiation” via Text](https://arxiv.org/pdf/2406.07496.pdf)** — *Mert Yuksekgonul et al.*, 2024
  Approximates gradients in discrete prompt space via textual perturbations, enabling back-prop-like learning for prompts.

* **[StablePrompt: Automatic Prompt Tuning Using Reinforcement Learning](https://arxiv.org/abs/2410.07652)** — *Minchan Kwon et al.*, 2024
  Formulates prompt tuning as an RL problem, balancing exploration and robustness.

---

### Prompt-Engineering Frameworks & Tooling

* **[DSPy: Compiling Declarative Language Model Calls into Self-Improving Pipelines](https://arxiv.org/pdf/2310.03714.pdf)** — *Omar Khattab et al.*, 2024
  A DSL that turns high-level specs into prompt trees, then automatically tunes them through beam search and self-refinement.

* **[Automatic Prompt Engineer (APE)](https://arxiv.org/pdf/2211.01910.pdf)** — *Ruiyang Zhou et al.*, 2022
  Iteratively assembles and scores candidate prompts to match few-shot performance—no gradients required.

  * **[Automatic Prompt Engineer (Technique Page)](https://www.promptingguide.ai/techniques/automatic_prompt_engineer)** — Prompt Engineering Guide, 2025
    Step-by-step tutorial and code snippets for practitioners.

* **[AutoPrompt: Eliciting Knowledge from Language Models with Automatically Generated Prompts](https://aclanthology.org/2020.emnlp-main.346/)** — *Taylor Shin et al.*, 2020
  Pioneering gradient-free token search that “triggers” factual recall in LMs.

* **[Self-Refine: Iterative Refinement with Self-Feedback](https://arxiv.org/abs/2303.17651)** — *Aman Madaan et al.*, 2023
  Lets the model critique and rewrite its own output, steadily polishing answers.

* **[Reflexion (Technique Page)](https://www.promptingguide.ai/techniques/reflexion)** — Prompt Engineering Guide, 2025
  Explains how to prompt models to reflect on mistakes before finalising answers.

* **[Prompt Methods on RoBERTa-large (“Revisiting Automated Prompting”)](https://arxiv.org/pdf/2304.03609.pdf)** — *Yulin Zhou et al.*, 2023
  Benchmarks automated prompt-mining on encoder-only models, revealing performance ceilings.

* **[The Unreasonable Effectiveness of Eccentric Automatic Prompts](https://arxiv.org/abs/2402.10949)** — *Rick Battle & Teja Gollapudi*, 2024
  Finds that “eccentric” automatically generated prompts excel on math word problems for models like Mistral-7B.

---

### Theoretical & Meta Perspectives

* **[On Meta-Prompting](https://arxiv.org/abs/2312.06562)** — *Adrian de Wynter et al.*, 2023
  Uses category theory to model in-context learning as meta-program execution.

* **[Prompts Are Programs Too! Understanding How Developers Build Software Containing Prompts](https://arxiv.org/abs/2409.12447)** — *Jenny T. Liang et al.*, 2024
  Interview study with 20 developers showing how “prompt programming” diverges from traditional coding practices.

* **[The Complete LLM Evaluation Playbook](https://www.confident-ai.com/blog/the-ultimate-llm-evaluation-playbook)** — *Jeffrey Ip*, 2025
  Hands-on guide for designing, running and interpreting LLM evaluation pipelines.

* **Automating Tools for Prompt Engineering** — *Sandrine Ceurstemont*, 2025, *Communications of the ACM* 68(5)
  Surveys emerging software that streamlines prompt design and tuning for everyday developers.

---

### Reasoning & Zero-Shot Prompting

* **[Chain-of-Thought Prompting Elicits Reasoning in Large Language Models](https://arxiv.org/abs/2201.11903)** — *Jason Wei et al.*, 2023
  Shows that adding step-by-step reasoning traces drastically boosts arithmetic and commonsense accuracy.

* **[Large Language Models Are Zero-Shot Reasoners](https://arxiv.org/pdf/2201.11903.pdf)** — *Takeshi Kojima et al.*, 2022
  Demonstrates that the simple cue “Let’s think step by step” unlocks latent reasoning abilities without examples.

---

### Knowledge Graphs & LLM Integration

* **[Knowledge Graphs and Their Reciprocal Relationship with Large Language Models](https://doi.org/10.3390/make7020038)** — *Ramandeep S. Dehal et al.*, 2025
  Reviews how KGs can enhance LLMs and vice versa, outlining open research challenges.

* **[Large Language Models for Intelligent RDF Knowledge Graph Construction](https://doi.org/10.3389/frai.2025.1546179)** — *Apostolos Mavridis et al.*, 2025
  Presents an LLM-assisted pipeline that maps clinical terms to RDF triples with high precision.

* **[LLM-Based Multi-Hop Question Answering with Knowledge Graph Integration](https://aclanthology.org/2024.findings-emnlp.844/)** — *Ruirui Chen et al.*, 2024
  Combines KG retrieval with chain-of-thought prompting to improve multi-hop QA under knowledge drift.

---

### Foundations of Meaning & Semantics

* **[Mathematical Foundations for a Compositional Distributional Model of Meaning](https://arxiv.org/pdf/1003.4394.pdf)** — *Bob Coecke et al.*, 2010
  Introduces DisCoCat, marrying category theory with distributional semantics.

* **[Information Theory–Based Compositional Distributional Semantics](https://doi.org/10.1162/coli_a_00454)** — *Enrique Amigó et al.*, 2022
  Extends compositional semantics with information-theoretic measures.

* **[At the Interface of Algebra and Statistics](https://arxiv.org/pdf/2004.05631.pdf)** — *Tai-Danae Bradley*, 2020 (PhD thesis)
  Explores categorical probability and its applications to machine-learning generative models.


