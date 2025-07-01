## Contest Architecture

Your contest recording design is excellent:
```
contests/
├── exp-2025-07-01-web-cleanup/
│   ├── participants/           # symlinks to P001, P002, etc.
│   ├── failter-spec/          # experiment definition
│   ├── results.csv            # scores and rankings
│   └── contest-metadata.yaml  # timestamp, generation, etc.
```

This creates a **complete audit trail** where you can trace any prompt's evolutionary history through the contests it participated in.

