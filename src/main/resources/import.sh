#!/usr/bin/env bash
#
# import_done.sh - Bash equivalent of FileDB.importDone(Path from)
#
# Reads an input file where every line is a JSON object with (at least)
# a "hash" field, e.g.:
#   {"hash":"ABCDEF0123456789...","name":"...","seeders":1,...}
#
# For every valid JSON line (lines not starting with '{' are skipped,
# just like the Java implementation) it:
#   1. Extracts the "hash" field.
#   2. Computes the shard prefix = first 3 chars of hash, uppercased.
#   3. Appends "<hash>#<json>" to:
#        $HOME/dht-meta/filedb/<PREFIX>.txt
#
# Requires: jq (for reliable JSON field extraction).
#
# Usage:
#   ./import_done.sh /path/to/done.txt
#
set -euo pipefail

SHARD_PREFIX_LEN=3
BASE_DIR="${HOME}/dht-meta/filedb"

usage() {
    echo "Usage: $0 <input-file>" >&2
    exit 1
}

if [[ $# -ne 1 ]]; then
    usage
fi

INPUT_FILE="$1"

if [[ ! -f "${INPUT_FILE}" ]]; then
    echo "Input file not found: ${INPUT_FILE}" >&2
    exit 1
fi

mkdir -p "${BASE_DIR}"

line_num=0
imported=0
skipped=0

#!/usr/bin/env bash

awk -v len="$SHARD_PREFIX_LEN" -v dir="$BASE_DIR" '
{
    prefix = toupper(substr($0, 1, len))
    print >> (dir "/" prefix ".txt")
}
' "$INPUT_FILE"

