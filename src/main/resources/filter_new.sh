#!/usr/bin/env bash
#
# filter_new.sh - Filters out lines that already exist in the sharded
# files under $BASE_DIR (as produced by import.sh).
#
# For every line in the input file:
#   1. Computes the shard prefix = first 3 chars of the line, uppercased.
#   2. Checks if the line already exists (exact match) in:
#        $BASE_DIR/<PREFIX>.txt
#   3. If it exists, the line is skipped.
#   4. If it does NOT exist, the line is written to the output file.
#
# Usage:
#   ./filter_new.sh <input-file> <output-file>
#
set -euo pipefail

SHARD_PREFIX_LEN=3
BASE_DIR="${HOME}/dht-meta/filedb"

usage() {
    echo "Usage: $0 <input-file> <output-file>" >&2
    exit 1
}

if [[ $# -ne 2 ]]; then
    usage
fi

INPUT_FILE="$1"
OUTPUT_FILE="$2"

if [[ ! -f "${INPUT_FILE}" ]]; then
    echo "Input file not found: ${INPUT_FILE}" >&2
    exit 1
fi

mkdir -p "${BASE_DIR}"

# Truncate/create output file
: > "${OUTPUT_FILE}"

awk -v len="$SHARD_PREFIX_LEN" -v dir="$BASE_DIR" -v out="$OUTPUT_FILE" '
{
    prefix = toupper(substr($0, 1, len))
    shard = dir "/" prefix ".txt"

    # Search shard file for an exact line match
    found = 0
    while ((getline shard_line < shard) > 0) {
        if (shard_line == $0) {
            found = 1
            break
        }
    }
    close(shard)

    if (!found) {
        print $0 >> out
    }
}
' "$INPUT_FILE"
