.PHONY: test
run:
	cd projects/pcrit-cli && clj -M:run

test:
	clj -M:poly test


pack:
	(for i in README.md USAGE.md docs/DESIGN.md docs/API.md \
					docs/RISKS.md \
					docs/evo-process.md \
					docs/evo-population-bootstrapping.md \
					docs/prompt-representation.md \
					copilot/onboard-*.md \
					Makefile `find . -name deps.edn` ;\
	   do echo $$i; cat $$i; echo ---- ; done ;\
  echo PROMPTS; echo -----; \
  cat prompts/* ; \
	echo Source files; echo -----; \
  (find components -name '*.clj' | xargs cat) ;\
  (find bases -name '*.clj' | xargs cat) \
  ) >~/pcrit-pack.txt

