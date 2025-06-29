# PromptCritical System Design (Current State)

**Version:** 1.0
**Date:** 2025-06-29
**Author:** Engineering Assistant

## 1. Overview

PromptCritical is a framework designed to facilitate the evolutionary optimization of Large Language Model (LLM) prompts. The central goal is to create a closed-loop system where prompts can be generated, tested, and mutated in a controlled and reproducible manner.

The integrity of the experimental process is paramount. This requires a robust and reliable system for storing and managing the core artifacts: the prompts themselves. This document outlines the design of the foundational component of PromptCritical: the file-based prompt database.

## 2. Core Component: The Prompt Database (`pcrit.pdb`)

The prompt database is implemented as a self-contained library within the `pcrit.pdb` namespace. It provides a simple, high-level API that encapsulates all the complexities of file I/O, data serialization, hashing, and concurrency management. This design decision ensures that the rest of the application can interact with prompts without needing to know the low-level storage details, making the overall system easier to maintain and extend.

### 2.1. Public API

The library exposes a minimal but complete API for all prompt operations:

*   **`(create-prompt db-dir prompt-text & {:keys [metadata]})`**
    *   **Purpose:** Creates a new, unique prompt in the database.
    *   **Design Rationale:** This is the sole entry point for new prompts. It immediately canonicalizes the input text, calculates its hash, assigns a new unique ID, and writes the complete, consistent record to disk. It returns the canonical prompt record, ensuring the caller has the same view of the data as what is on disk.

*   **`(read-prompt db-dir id)`**
    *   **Purpose:** Loads a prompt from the database.
    *   **Design Rationale:** This function reads the on-disk representation and performs an integrity check by comparing the stored `sha1-hash` with a freshly computed hash of the body. This guards against silent data corruption.

*   **`(update-metadata db-dir id f)`**
    *   **Purpose:** Atomically updates the metadata of an existing prompt.
    *   **Design Rationale:** Immutability of the prompt body is a core principle. This function enforces it by only allowing metadata to be changed. It takes a function `f` (which receives the old metadata and returns the new) to prevent race conditions where two processes might read the same metadata and one's update would overwrite the other's. The entire update process is managed via a lock file protocol to ensure atomicity.

## 3. Data Model: The Prompt Record

A prompt is represented consistently across the system, both on-disk and in-memory.

### 3.1. On-Disk Representation

*   **Format:** A single UTF-8 encoded text file with a `.prompt` extension.
*   **Structure:**
    1.  **YAML Front Matter:** A block of YAML, enclosed between `---` delimiters, containing all prompt metadata (e.g., `id`, `created-at`, `sha1-hash`).
    2.  **Prompt Body:** The raw prompt text, which begins immediately after the closing `---` delimiter.
*   **Naming:** Files are named by their ID (e.g., `P123.prompt`).

### 3.2. In-Memory Representation

A prompt is represented as a simple Clojure map, cleanly separating the metadata from the body:

```clojure
{:header {:id "P123", ...}
 :body   "Canonical prompt text...\n"}
```

### 3.3. The Canonical Text Representation

This is a critical design decision for ensuring data integrity. To guarantee that a given prompt text always produces the same SHA1 hash, all prompt bodies are converted to a **canonical form** before being hashed or written to disk.

*   **Definition:** The canonical form is UTF-8, normalized to Unicode NFC. All line endings (CR, LF, CRLF) are converted to a single Line Feed (LF), and the text is guaranteed to end with exactly one LF.
*   **Rationale:** Without canonicalization, two prompts that are semantically identical could have different byte representations (e.g., `\n` vs. `\r\n` line endings), leading to different hashes and undermining the system's ability to detect corruption or identify duplicates. All API functions operate on and return this canonical form. The logic is centralized in `pcrit.util/canonicalize-text` for system-wide consistency.

## 4. Concurrency and Data Integrity

The system is designed with the expectation that multiple processes may read or write from the database directory concurrently.

### 4.1. Immutability and Corruption Detection

*   **Principle:** The prompt body is immutable. Once created, its text never changes.
*   **Mechanism:** The `sha1-hash` in the metadata acts as a checksum. It is computed from the canonical body text. The `read-prompt` function validates this hash on every read, logging a warning if a mismatch is found. This ensures that any accidental or unauthorized modification to the prompt body is detected.

### 4.2. Atomic Metadata Updates

*   **Challenge:** Modifying a prompt file (even just its metadata) is not an atomic operation on most filesystems. If a process crashes mid-write, the file could be left in a corrupted, unreadable state.
*   **Solution (The `.lock` Protocol):** The `update-metadata` function implements a robust locking protocol to ensure updates are atomic and safe.
    1.  **Acquire Lock:** Atomically create a `P123.prompt.lock` file. If it already exists, the update fails, preventing simultaneous writes.
    2.  **Write to Temp:** Write the *entire new content* (updated header, same body) to a temporary file, `P123.prompt.new`, in the same directory.
    3.  **Atomic Replace:** Use `java.nio.file.Files/move` with the `ATOMIC_MOVE` option to instantly replace the original file with the new one. This operation is atomic on POSIX-compliant filesystems. A reader will either see the complete old file or the complete new file, never a partial state.
    4.  **Release Lock:** Delete the `.lock` file.

## 5. Command-Line Interface (`pcrit.core`)

The main entry point (`pcrit.core`) serves as a simple command dispatcher. It currently implements a `create` command that demonstrates the use of the `pcrit.pdb` library by reading prompt text from standard input and creating a new prompt record. This separates the core database logic from the user-facing application logic.

## 6. Supporting Modules

*   **`pcrit.util`:** A collection of pure, stateless utility functions, primarily for text canonicalization and hashing. Centralizing these prevents logic duplication and ensures consistency.
*   **`pcrit.log`:** A facade for the `taoensso/telemere` logging library. It standardizes log formatting and provides a single point of control for logging configuration.
*   **`pcrit.config`:** A centralized map for application configuration. This decouples behavior (like LLM endpoints or timeouts) from the code itself.
*   **`pcrit.llm-interface`:** The module responsible for communicating with LLMs. Currently, it performs a pre-flight check for the `LITELLM_API_KEY`, enforcing a key dependency. TBD
