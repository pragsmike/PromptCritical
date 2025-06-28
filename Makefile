.PHONY: test
run:
	clj -M:run help

test:
	clj -X:test


pack:
	(for i in README.md docs/DESIGN.md deps.edn Makefile ;\
	   do echo $$i; cat $$i; echo ---- ; done ;\
  echo PROMPTS; echo -----; \
  cat prompts/* ; \
	echo Source files; echo -----; \
	cat src/pcrit/*.clj ; cat test/pcrit/*.clj) >~/pcrit-pack.txt

