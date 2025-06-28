## Prompt: Design a System for Evolving Prompts

You are tasked with designing a system that evolves a population of prompts to improve their performance on a specific, narrowly defined task. The system should start with an initial (seed) population of prompts and, through multiple generations, produce new prompts that perform better as measured by an external evaluation harness. This evaluation harness is a black box: you provide it with a prompt, and it returns a performance score.

For this task, treat the underlying evolutionary algorithm (e.g., selection, replacement) as a black box. Instead, focus on enumerating all the operations required to support the evolutionary process, especially those that require the use of a large language model (LLM) like yourself.

### Instructions

1. **System Overview:**
   Briefly describe the high-level process by which the system evolves prompts from generation to generation.

2. **Operation Enumeration:**
   List all distinct operations required in the system. For each operation, specify whether it requires LLM involvement or can be handled by standard software logic.

3. **LLM-Dependent Operations:**
   For each operation that requires LLM computation, provide:
   - **Step Name:** A concise name for the operation.
   - **Prompt Template Outline:** A template (in natural language) that could be used to instruct an LLM to perform this operation.
   - **Inputs to the Template:** List the specific inputs or parameters that the template would require.

4. **Comprehensiveness:**
   Ensure your outline covers the entire evolutionary process, including (but not limited to):
   - Initialization of the seed prompt population
   - Generation of prompt variations (e.g., mutation, crossover)
   - Validity and constraint checking for prompts
   - Any other necessary operations to maintain or improve the population
   - Termination criteria

5. **Clarity and Structure:**
   Present your answer in a clear, structured format with appropriate headings and bullet points for readability.

**Note:**
Do not detail the implementation of the evolutionary algorithm or the evaluation harness. Focus only on the operations and how LLMs would be used within this system.

**Your response should enable a technical team to understand the full scope of LLM-driven operations necessary for evolving prompts in this context.**
