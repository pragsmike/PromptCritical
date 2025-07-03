Are you planning to use existing AMR parsers and CDS implementations, or build
custom tools for the prompt evolution domain?


**One architectural question**: Are you planning to cache the AMR parses? Since
prompts are immutable with SHA-1 hashes, you could build a persistent cache
mapping `prompt-hash → amr-graph` to avoid re-parsing during evolution cycles.
