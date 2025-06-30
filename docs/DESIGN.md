# PromptCritical System Design (Current State)

**Version:** 1.1
**Date:** 2025-06-29
**Author:** Engineering Assistant

## 1. Overview

PromptCritical is a framework designed to facilitate the evolutionary optimization of Large Language Model (LLM) prompts. The central goal is to create a closed-loop system where prompts can be generated, tested, and mutated in a controlled and reproducible manner.

The integrity of the experimental process is paramount. This requires a robust and reliable system for storing and managing the core artifacts: the prompts themselves. This document outlines the design of the foundational component of PromptCritical: the file-based prompt database.

## 2. Core Component: The Prompt Database (`pcrit.pdb.*`)

The prompt database is implemented as a self-contained library, decomposed into several namespaces under `pcrit.pdb.*` to ensure a clean separation of concerns. This modular design isolates high-level business logic from low-level file I/O, concurrency management, and ID generation, making the system easier to maintain, reason about, and extend.

*   **`pcrit.pdb.core`**: The public-facing API. This is the main entry point for all prompt operations (`create-prompt`, `read-prompt`, `update-metadata`). It coordinates the other, lower-level namespaces to execute its tasks.
*   **`pcrit.pdb.io`**: Responsible for all direct file interactions, including parsing prompt files, serializing prompt records to strings, and the critical atomic file writing logic.
*   **`pcrit.pdb.id`**: Dedicated solely to the atomic generation of new prompt IDs (`P1`, `P2`, etc.), including managing the on-disk counter file.
*   **`pcrit.pdb.lock`**: Contains the robust, self-healing file-locking protocol used to manage concurrent access to prompt files and the ID counter.

### 2.1. Public API (`pcrit.pdb.core`)

The library exposes a minimal but complete API for all prompt operations:

*   **`(create-prompt db-dir prompt-text & {:keys [metadata]})`**
    *   **Purpose:** Atomically creates a new, unique prompt in the database.
    *   **Design Rationale:** This function orchestrates the creation of a complete prompt record and uses the system's atomic writing protocol to commit it to disk, preventing partial writes in the event of a crash.

*   **`(read-prompt db-dir id)`**
    *   **Purpose:** Loads a prompt from the database.
    *   **Design Rationale:** Performs an integrity check on every read by comparing the stored `sha1-hash` with a freshly computed hash of the body. The comparison is case-insensitive to gracefully handle hashes that may have been hand-edited.

*   **`(update-metadata db-dir id f)`**
    *   **Purpose:** Atomically updates the metadata of an existing prompt.
    *   **Design Rationale:** Enforces the immutability of the prompt body by only allowing metadata to be changed. It takes a function `f` to prevent race conditions. The entire process is protected by the locking protocol and uses the atomic writing protocol for the final commit.

## 3. Data Model: The Prompt Record

A prompt is represented consistently across the system.

### 3.1. On-Disk Representation

*   **Format:** A single UTF-8 encoded text file with a `.prompt` extension, named by its ID (e.g., `P123.prompt`).
*   **Structure:**
    1.  **YAML Front Matter:** A block of human-readable, block-style YAML containing all prompt metadata.
    2.  **Prompt Body:** The raw prompt text, beginning after the closing `---` delimiter.
*   **Parsing:** The parser is designed to be robust, using a regular expression (`(?s)^---\n(.*?)\n?---\n(.*)$`) that correctly handles cases where the YAML block is not terminated by a final newline before the `---`.

### 3.2. In-Memory Representation

A prompt is a Clojure map with keywordized keys, ensuring type consistency.

```clojure
{:header {:id "P123", :created-at "...", :sha1-hash "..."}
 :body   "Canonical prompt text...\n"}
```

*   **Design Rationale (Keywordizing Keys):** The `clj-yaml` library's default `:keywords true` option is used during parsing to ensure all keys in the `:header` are keywords (e.g., `:id`). This prevents subtle bugs that could arise from mixed `String`/`Keyword` types.

### 3.3. The Canonical Text Representation

To guarantee data integrity and consistent hashing, all prompt text (both the body *and* the YAML front matter string) is converted to a **canonical form** before being hashed or written to disk.

*   **Definition:** The canonical form is UTF-8, normalized to Unicode NFC. All line endings are converted to a single Line Feed (LF), and the text is guaranteed to end with exactly one LF.
*   **Rationale:** This ensures that semantically identical prompts always have the same byte representation, which is critical for reliable hashing and corruption detection. This logic is centralized in `pcrit.util/canonicalize-text`.

## 4. Concurrency and Data Integrity

The system is designed to be robust against data corruption and race conditions.

### 4.1. The Atomic Write Protocol

*   **Challenge:** A simple `spit` or `write` operation is not atomic. A system crash during a write can leave a file in a corrupt, zero-byte, or partially-written state.
*   **Solution (Centralized in `pcrit.pdb.io/atomic-write-file!`)**: All durable writes in the system use a single, robust atomic-write helper. This protocol is used for creating new prompts, updating metadata, and updating the ID counter.
    1.  **Write to Temp:** The entire new content is written to a temporary file (e.g., `.P123.prompt.new`) in the same directory.
    2.  **Atomic Replace:** `java.nio.file.Files/move` with the `ATOMIC_MOVE` option is used to instantly replace the original file with the new one. This is an atomic operation on POSIX-compliant filesystems.
    3.  **Fallback with `fsync`:** If `ATOMIC_MOVE` is not supported (e.g., on some network filesystems), the system logs a warning and falls back to a non-atomic move, immediately followed by an `fsync` call to request that the operating system flush the file's contents to the physical storage, ensuring data durability.

### 4.2. The Lock Protocol for Concurrency

*   **Challenge:** When multiple processes attempt to update the same prompt, they can interfere with each other, leading to lost updates.
*   **Solution (The `.lock` Protocol)**: The `pcrit.pdb.lock/execute-with-lock` function implements a robust locking protocol. Before any write operation, a process must acquire a lock by atomically creating a file like `P123.prompt.lock`.
    *   **Stale Lock Recovery:** The lock protocol is self-healing. If a lock file is older than a configurable threshold, it is considered stale (left behind by a crashed process) and is automatically removed, allowing the system to proceed. This same logic applies to the ID counter's lock file, preventing stalled batch jobs.
    *   **Configurable Timeouts:** The retry timings and stale-lock thresholds are not hard-coded. They are read from `pcrit.config/config` to allow for tuning in different environments (e.g., systems with high-latency network storage).

## 5. Supporting Modules

*   **`pcrit.config`**: A centralized map for application configuration. This decouples behavior (like LLM endpoints or locking timeouts) from the code itself.
*   **`pcrit.util`**: A collection of pure, stateless utility functions for text canonicalization and hashing.
*   **`pcrit.log`**: A facade for the logging library, standardizing log formatting.
*   **`pcrit.llm-interface`**: The module responsible for communicating with LLMs.
