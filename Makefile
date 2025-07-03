.PHONY: test
run:
	cd projects/pcrit-cli && clj -M:run

test:
	clj -M:poly test


pack:
	(for i in README.md docs/DESIGN.md docs/API.md docs/prompt-representation.md \
					copilot/onboard-*.md \
					deps.edn Makefile ;\
	   do echo $$i; cat $$i; echo ---- ; done ;\
  echo PROMPTS; echo -----; \
  cat prompts/* ; \
	echo Source files; echo -----; \
	cat src/pcrit/*.clj src/pcrit/*/*.clj test/pcrit/*.clj) >~/pcrit-pack.txt

