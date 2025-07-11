#!/usr/bin/env bash
# file: test-cli-integration.sh
#
# Exit on first error, on unset vars, and propagate errors through pipes
set -euo pipefail

###############################################################################
# Configuration
###############################################################################
CLI_BIN="${CLI_BIN:-pcrit}"      # Command put on $PATH by your installer
ROOT_DIR="$(mktemp -d)"          # Throw-away workspace
EXP_NAME="experiment"
EXP_DIR="$ROOT_DIR/$EXP_NAME"

###############################################################################
# Helper functions
###############################################################################
log()  { printf "\n\033[1m%s\033[0m\n" "$*"; }
die()  { echo "❌  $*" >&2; exit 1; }

assert_file() {
  [[ -e "$1" ]] || die "Expected file “$1” is missing"
}

assert_relative_symlink() {
  [[ -L "$1" ]]               || die "“$1” is not a symlink"
  [[ $(readlink "$1") != /* ]] || die "Symlink “$1” points via absolute path"
}

###############################################################################
# 1 Bootstrap
###############################################################################
log "▶︎ bootstrap → $EXP_DIR"
"$CLI_BIN" bootstrap "$EXP_DIR" >"$ROOT_DIR/bootstrap.out" 2>&1

# Minimal smoke check – customise if your bootstrap generates more files
for f in config.edn prompts/original.txt README.md; do
  assert_file "$EXP_DIR/$f"
done

###############################################################################
# 2 Evaluate (1st run)
###############################################################################
log "▶︎ evaluate (1st run)"
"$CLI_BIN" evaluate "$EXP_DIR" >"$ROOT_DIR/evaluate1.out" 2>&1

for f in runs/latest/eval/report.csv runs/latest/eval/metadata.json; do
  assert_file "$EXP_DIR/$f"
done

###############################################################################
# 3 Vary (1st run)
###############################################################################
log "▶︎ vary (1st run)"
"$CLI_BIN" vary "$EXP_DIR" >"$ROOT_DIR/vary1.out" 2>&1
assert_file "$EXP_DIR/runs/latest/vary/candidates.csv"

###############################################################################
# 4 Select (1st run)
###############################################################################
log "▶︎ select (1st run)"
"$CLI_BIN" select "$EXP_DIR" >"$ROOT_DIR/select1.out" 2>&1
assert_file "$EXP_DIR/runs/latest/select/winners.csv"

# Ensure every symlink under runs/ is *relative* (no absolute paths)
log "▶︎ verifying symlink relativity"
while IFS= read -r link; do
  assert_relative_symlink "$link"
done < <(find "$EXP_DIR/runs" -type l)

###############################################################################
# 5 Rename experiment directory and repeat
###############################################################################
NEW_EXP_DIR="${EXP_DIR}_moved"
log "▶︎ mv $EXP_DIR → $NEW_EXP_DIR"
mv "$EXP_DIR" "$NEW_EXP_DIR"

log "▶︎ evaluate / vary / select (2nd run after move)"
for cmd in evaluate vary select; do
  "$CLI_BIN" "$cmd" "$NEW_EXP_DIR" \
      >"$ROOT_DIR/${cmd}2.out" 2>&1
done

# Re-check the *same* expected artefacts
for f in runs/latest/eval/report.csv  \
         runs/latest/vary/candidates.csv \
         runs/latest/select/winners.csv; do
  assert_file "$NEW_EXP_DIR/$f"
done

###############################################################################
log "✅  All integration checks passed"
exit 0
