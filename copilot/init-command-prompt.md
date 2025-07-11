### **Implementation Plan: `pcrit init <experiment-dir>`**

> **Objective:** Create a new `pcrit init` command to scaffold a minimal, runnable experiment directory. This command will produce a directory structure that is immediately usable by the existing `pcrit bootstrap` command. This plan supersedes all previous discussions on this topic.

---

### 1. Public-Facing Behavior

| Behavior | Details |
| :--- | :--- |
| **Invocation** | `pcrit init <dir> [--force]` |
| **Happy Path** | Creates `<dir>` and populates it with a minimal, runnable experiment scaffold that is **fully compatible with `pcrit bootstrap`**. |
| **Safety** | Aborts if `<dir>` exists and is not empty, unless `--force` is specified. |
| **Idempotence** | Re-running on an existing directory is a no-op unless `--force`, which will overwrite the scaffold files. |
| **Exit Codes** | `0` for success; `1` for user error (e.g., non-empty dir); `2` for I/O errors. |

---

### 2. Correct Generated Directory Structure

The command will produce the following structure. This is the exact layout expected by `pcrit bootstrap`.

```
<experiment-dir>/
├── .gitignore
├── bootstrap.edn
├── evolution-parameters.edn
└── seeds/
    ├── improve-meta-prompt.txt
    └── seed-object-prompt.txt
```

*   **`.gitignore`**: Will only ignore transient runtime files, such as `*.lock`. **It will not ignore the `generations/` directory.**
*   **`bootstrap.edn`**: The manifest file that names the seed prompts.
*   **`evolution-parameters.edn`**: A default configuration file for the `evaluate` and `vary` commands.
*   **`seeds/`**: Contains the raw text files for the initial prompts.

---

### 3. Code Implementation

#### 3.1. New Command Namespace (`pcrit.command.init`)

This namespace will contain the core logic, which is strictly about file I/O. It will **not** parse CLI arguments.

```clojure
;; file: components/command/src/pcrit/command/init.clj

(ns pcrit.command.init
  (:require [clojure.java.io :as io]
            [pcrit.scaffold :as scaffold] ; New helper namespace
            [pcrit.log.interface :as log]))

(defn- pre-flight-checks! [target-dir force?]
  (cond
    (and (.exists target-dir) (seq (.list target-dir)) (not force?))
    (throw (ex-info (str "Directory " (.getCanonicalPath target-dir)
                         " exists and is not empty. Use --force to overwrite.")
                    {:type :validation-error}))
    :else
    (do
      (.mkdirs target-dir)
      true)))

(defn init!
  "The core implementation of the init command.
  Creates a scaffold experiment in the given directory."
  [ctx {:keys [force?]}]
  (try
    (let [target-dir (io/file (:exp-dir ctx))]
      (when (pre-flight-checks! target-dir force?)
        (scaffold/copy-scaffold-files! target-dir)
        (log/info "✓ Experiment skeleton created at" (.getCanonicalPath target-dir))
        {:exit-code 0}))
    (catch clojure.lang.ExceptionInfo e
      (log/error (.getMessage e))
      {:exit-code 1})
    (catch Exception e
      (log/error "An unexpected I/O error occurred:" (.getMessage e))
      {:exit-code 2})))
```

#### 3.2. New Scaffold Helper (`pcrit.scaffold`)

This new component will be responsible for copying template files from the classpath resources to the target directory.

```clojure
;; file: components/scaffold/src/pcrit/scaffold.clj (New Component)

(ns pcrit.scaffold
  (:require [clojure.java.io :as io]))

(def ^:private scaffold-files
  {"scaffold/.gitignore"              ".gitignore"
   "scaffold/bootstrap.edn"           "bootstrap.edn"
   "scaffold/evolution-parameters.edn" "evolution-parameters.edn"
   "scaffold/seeds/seed-object-prompt.txt" "seeds/seed-object-prompt.txt"
   "scaffold/seeds/improve-meta-prompt.txt" "seeds/improve-meta-prompt.txt"})

(defn- copy-resource! [resource-path target-file]
  (io/make-parents target-file)
  (with-open [in (io/input-stream (io/resource resource-path))
              out (io/output-stream target-file)]
    (io/copy in out)))

(defn copy-scaffold-files! [target-dir]
  (.mkdirs (io/file target-dir "seeds"))
  (doseq [[resource-path target-path] scaffold-files]
    (copy-resource! resource-path (io/file target-dir target-path))))
```

*Resource files will need to be created under `components/scaffold/resources/pcrit/scaffold/`.

#### 3.3. CLI Wiring (`pcrit.cli.main`)

We will update the existing CLI dispatcher to add the `init` command. It will handle argument parsing and call the command implementation.

```clojure
;; file: bases/cli/src/pcrit/cli/main.clj (Excerpt)

; ... inside the 'command-specs' map ...
"init" {:doc "Creates a new, minimal experiment skeleton directory."
        :handler do-init
        :options [["-f" "--force" "Overwrite existing scaffold files."]]}
; ...

; ... new handler function ...
(defn- do-init [{:keys [options arguments]}]
  (if (empty? arguments)
    (log/error "The 'init' command requires an <experiment-dir> argument.")
    (let [exp-dir (first arguments)
          ctx (exp/new-experiment-context exp-dir)
          {:keys [exit-code]} (cmd/init! ctx options)]
      (when (not (zero? exit-code))
        (System/exit exit-code)))))
```

---

### 4. Template File Contents

The following files will be placed in the `resources/` directory of the new `scaffold` component.

*   **`scaffold/.gitignore`**:
    ```
    # Ignore runtime lock files
    *.lock
    ```
*   **`scaffold/bootstrap.edn`**:
    ```clojure
    {:seed   "seeds/seed-object-prompt.txt"
     :improve "seeds/improve-meta-prompt.txt"}
    ```
*   **`scaffold/evolution-parameters.edn`**:
    ```clojure
    {;; Models to run prompts against during `evaluate`
     :evaluate {:models ["openai/gpt-4o-mini"]}

     ;; Model to use for creating new prompts during `vary`
     :vary {:model "openai/gpt-4o-mini"}}
    ```
*   **`scaffold/seeds/seed-object-prompt.txt`**:
    ```
    You are an expert text-processor. Your task is to clean up raw text scraped from the web.
    Remove all boilerplate, ads, navigation, and subscription offers.
    Preserve only the main article content.

    {{INPUT_TEXT}}
    ```*   **`scaffold/seeds/improve-meta-prompt.txt`**:
    ```
    Analyze the following prompt and generate an improved, more robust version.

    {{OBJECT_PROMPT}}
    ```

---

### 5. Tests & Documentation

*   **Unit Tests:** Create `pcrit.command.init_test.clj` to verify that `init!` creates the correct files, respects the `--force` flag, and handles non-empty directories correctly.
*   **Integration:** An end-to-end test will be added to ensure the sequence `pcrit init my-exp` followed by `pcrit bootstrap my-exp` succeeds.
*   **Documentation:** Update the main `README.md` with a "Quick Start" section demonstrating the new `init` command.

This corrected plan is architecturally consistent with the existing system and directly addresses the core requirement of creating a valid starting point for a PromptCritical experiment.
