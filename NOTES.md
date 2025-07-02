This codebase is organized using [Polylith](https://polylith.gitbook.io/polylith) conventions.


To use the command-line `poly` tool, just use this alias.  The poly library is already in deps.edn.

  ```bash
  alias poly='clj -M:poly '
  ```

To use Emacs CIDER, be sure to start the REPL using the top-level `deps.edn`.
There is a `.dir-locals` directory that tells Emacs CIDER to use the `:dev`
alias when starting a REPL.
That will ensure that all the poly bricks are in the classpath, as will also be
whatever development-only namespaces are in `development/src`.


References
  * [Emacs CIDER usage manual](https://docs.cider.mx/cider/basics/up_and_running.html)
