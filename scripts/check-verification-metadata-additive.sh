#!/usr/bin/env bash
# Checks that changes to gradle/verification-metadata.xml are purely additive.
#
# Gradle's --write-verification-metadata is incremental: a legitimate dependency
# version bump only ADDS a new <component> block and never modifies or removes an
# existing <sha256>/<pgp> entry (stale entries are left for manual removal). A
# legitimate diff is therefore purely additive.
#
# The realistic tamper signal is a CHANGED <sha256 value="..."> line -- the only
# verification tag whose content can change without breaking the XML -- which is
# what a "same version, modified bytes" event (e.g. a poisoned cache/mirror)
# would produce. Removal of a <component>/<artifact>/<trusted-key> block is also
# abnormal for an incremental regeneration and is rejected as defense-in-depth.
#
# Usage:
#   check-verification-metadata-additive.sh                 # analyze `git diff` of the metadata file
#   check-verification-metadata-additive.sh <diff-file>     # analyze the diff contained in <diff-file>
#
# Exit codes:
#   0  diff is purely additive (or empty)
#   1  diff would alter/remove existing verification entries
set -euo pipefail

readonly METADATA_FILE="gradle/verification-metadata.xml"

if [ "$#" -ge 1 ] && [ -f "$1" ]; then
  diff_input=$(cat "$1")
else
  diff_input=$(git diff --unified=0 -- "$METADATA_FILE")
fi

# Removed/changed lines start with a single '-' (the '---' file header has three).
altered=$(printf '%s\n' "$diff_input" \
  | grep -E '^-[^-].*<(sha256|pgp|component|artifact|trusted-key)' || true)

if [ -n "$altered" ]; then
  echo "::error::Changes to ${METADATA_FILE} would alter/remove existing verification entries, which indicates a tampered or unexpected artifact rather than a new version. Aborting; nothing committed."
  echo "$altered"
  exit 1
fi

echo "verification metadata changes are purely additive."
exit 0
