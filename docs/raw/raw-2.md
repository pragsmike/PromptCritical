First step: generate a population of prompts to do text transformation, on a narrowly focused task to clean junk from scraped web pages (ads, teaser links, subscription buttons, etc.) this will be done by handwriting an ‘object’ prompt describing the cleanup task, including example before/after pair. This is the seed, the first generation, population of one. The first ‘breeding’ step at first is just mutation. That object prompt will be given to an llm with an instruction (a meta prompt? Subject prompt? Need crisper terms here) which tells the llm to improve the prompt. Population is now two. Another mutation is done by a meta prompt that instructs the llm to generate three variants.  Population is now five. At this point there have been no contests, no evaluations.  Next step is to run an experiment using failter (a tool that evaluates prompts by running them on, in this case, blog posts) and scores how well they did at the task (removing junk). The report it produces gives the fitness scores for each individual.  The worst one is eliminated, the best one is mutated again but remains in the population.  This is rather adhoc strategy, meant to exercise and prove the machinery at this stage.

This is a great starting strategy - focused, concrete, and designed to prove the machinery works. Let me help clarify the terminology and think through the implementation details.

## Terminology Clarification

I'd suggest these terms for clarity:

- **Object Prompt**: The actual working prompt that performs the text transformation task
- **Meta Prompt**: The instruction that tells an LLM how to improve/mutate an object prompt
- **Seed Prompt**: The initial handwritten object prompt (generation 0)
- **Fitness Evaluation**: The Failter experiment that scores how well object prompts clean web pages

## Implementation Flow

Here's how I see the v0.2 milestone working:

```
1. Create seed object prompt (P001)
   - Task: clean junk from scraped web pages
   - Include before/after examples
   - Store in PromptCritical format

2. First mutation cycle:
   - Meta prompt A: "Improve this prompt" → generates P002
   - Meta prompt B: "Generate 3 variants" → generates P003, P004, P005
   - Population: [P001, P002, P003, P004, P005]

3. Failter evaluation:
   - Package all 5 prompts for Failter
   - Run on blog post corpus
   - Ingest fitness scores back to prompt files

4. Simple selection + mutation:
   - Eliminate worst performer
   - Mutate best performer → new prompt
   - Population: [4 survivors + 1 new mutant]
```

## Key Design Questions

**Meta Prompt Design**: You'll need at least two meta prompts:
- **Incremental Improver**: "Here's a prompt for cleaning web pages. Analyze its weaknesses and generate an improved version."
- **Variant Generator**: "Generate 3 different approaches to this web cleaning task, maintaining the core objective but varying the strategy."

**Fitness Function**: What specific metrics will Failter use? Some options:
- Reduction in character count (proxy for junk removal)
- Preservation of semantic content (harder to measure)
- Removal of specific junk patterns (ads, buttons, etc.)
- Human annotation on a sample set

**Population Management**: Your "eliminate worst, mutate best" strategy is simple but might lead to premature convergence. Even for this proof-of-concept, you might want to occasionally mutate middle-performers to maintain diversity.

## Potential Object Prompt Structure

Here's a starting template for your seed prompt:

```
Task: Clean junk content from scraped web pages while preserving the main article text.

Remove:
- Advertisement blocks
- Social media sharing buttons  
- "Subscribe to newsletter" prompts
- "Related articles" teasers
- Navigation menus
- Comment sections
- Cookie consent banners

Preserve:
- Main article title and body text
- Author information
- Publication date
- Relevant images with captions

Example Input:
[messy web page content]

Example Output:
[cleaned version]

Instructions: Process the following web page content and return only the cleaned version.
```

What's your current thinking on the fitness evaluation? Are you planning to use automated metrics, or will you need some human judgment in the initial experiments?
