.PHONY: test
run:
	cd projects/pcrit-cli && clj -M:run

test:
	clj -M:poly test


# Create a pack file to upload to AI assistant
pack:
	(for i in README.md USAGE.md PolylithNotes.md bin/pcrit \
					docs/OVERVIEW.md \
					docs/DESIGN.md \
					docs/API.md \
					docs/RISKS.md \
					docs/evo-process.md \
					docs/evo-population-bootstrapping.md \
					docs/evo-command-sequence.md \
					docs/evo-cost-calculation.md \
					docs/prompt-representation.md \
					copilot/onboard-*.md \
					Makefile `find . -name deps.edn` ;\
	   do echo $$i; cat $$i; echo ---- ; done ;\
  echo Resource files; echo -----; \
  (for i in `find . -path '*/resources/*' -type f`; do echo $$i; cat $$i; echo ----; done) ;\
	echo Source files; echo -----; \
  (find components -name '*.clj' | xargs cat) ;\
  (find bases -name '*.clj' | xargs cat) \
  ) >~/pcrit-pack.txt
