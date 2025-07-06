This codebase is organized using [Polylith](https://polylith.gitbook.io/polylith) conventions.

To use the command-line `poly` tool, just use this alias.  The poly library is already in `deps.edn`.

  ```bash
  alias poly='clj -M:poly '
  ```

To run tests, do `poly test` .

To run the CLI,
  ```
  cd projects/pcrit-cli && clj -M:run <command>
  ```

To use Emacs CIDER, be sure to start the REPL using the top-level `deps.edn`.
There is a `.dir-locals` directory that tells Emacs CIDER to use the `:dev:test`
aliases when starting a REPL.
That will ensure that all the poly bricks are in the classpath, as will also be
whatever development-only namespaces are in `development/src`.


### References
  * [Emacs CIDER usage manual](https://docs.cider.mx/cider/basics/up_and_running.html)


### The Polylith Dependency Model Explained

The core philosophy of Polylith is that you develop and test all your components together *within a single workspace*. The tooling is designed around this concept.

1.  **Component `deps.edn`:** A component's `deps.edn` file should **only** declare its dependencies on **external, third-party libraries** (e.g., `cheshire/cheshire`, `clj-http/clj-http`). It should **NOT** declare dependencies on other components within the same workspace using `:local/root`.

2.  **Root `deps.edn`:** The `deps.edn` file at the root of the workspace is the "glue" for development and testing. Inside a development alias (like the `:dev` alias we have), you list **all** the components and bases. This makes them visible on the classpath when you run the `poly test` command or work in a development REPL. The Polylith tool uses this alias to understand the entire workspace.

3.  **Project `deps.edn`:** A project's `deps.edn` file is where you assemble a final, runnable application. This is the **only** place where you explicitly declare which `components` and `bases` are included in that specific deliverable, using the `:local/root` syntax.

**The key takeaway is:** *During development, components are implicitly available to each other via the workspace definition in the root `deps.edn`. For deployment, a project explicitly selects the components it needs.*

