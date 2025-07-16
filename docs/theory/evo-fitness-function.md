**Fitness Function**: What specific metrics will Failter use? Some options:
- Reduction in character count (proxy for junk removal)
- Preservation of semantic content (harder to measure)
- Removal of specific junk patterns (ads, buttons, etc.)
- Human annotation on a sample set

**On the "Path-Dependent Fitness Landscapes":**

    This insight about landscapes reshaping themselves is crucial. It suggests
    we need evolutionary strategies that are robust to non-stationarity -
    perhaps something like:

- **Fitness landscape mapping**: Continuously sampling to detect when the terrain shifts
- **Adaptive mutation rates**: Increasing exploration when landscape changes are detected
- **Memory of past solutions**: Maintaining archives of historically successful prompts that might become relevant again

- **Object Prompt**: The actual working prompt that performs the text transformation task
- **Meta Prompt**: The instruction that tells an LLM how to improve/mutate an object prompt
- **Seed Prompt**: The initial handwritten object prompt (generation 0)
- **Fitness Evaluation**: The Failter experiment that scores how well object prompts clean web pages


