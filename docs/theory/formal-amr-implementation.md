There are no native Clojure or Java libraries for doing AMR analysis, only Python ones.
Therefore, our best bet is to invoke the python tool in a subprocess and parse
the Penman result into a Clojure data structure.

```clojure
(defn parse-to-amr [prompt-text]
  (-> (shell/sh "python" "amr_parser.py" prompt-text)
      :out
      (instaparse/parse amr-grammar)))
```

Using Python subprocesses for AMR parsing and Instaparse for consuming the
Penman format gives us the best of both worlds - leveraging the existing Python
ecosystem while keeping your core logic in Clojure.

This keeps the AMR complexity contained while giving you structured data to work with in Clojure.
