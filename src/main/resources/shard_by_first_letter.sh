#!/usr/bin/env bash
#
# shard_by_first_letter.sh - Reads all lines from a text file, takes the
# first letter of each line (uppercased) and appends the line to a file
# named after that letter under $BASE_DIR.
#
# Example:
#   line "apple"   -> $BASE_DIR/A.txt
#   line "Banana"  -> $BASE_DIR/B.txt
#
# Usage:
#   ./shard_by_first_letter.sh <input-file>
#
set -euo pipefail

BASE_DIR="${HOME}/dht-meta/unresolved"

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

awk -v dir="$BASE_DIR" '
{
    if (length($0) == 0) next
    letter = toupper(substr($0, 1, 1))
    outfile = dir "/" letter ".txt"
    print $0 >> outfile
    close(outfile)
}
' "$INPUT_FILE"
