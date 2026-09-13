#!/usr/bin/env bash
#
# sort_dedup.sh - Sorts all lines in a text file alphabetically (ascending)
# and removes consecutive duplicate lines.
#
# Usage:
#   ./sort_dedup.sh <input-file> <output-file>
#
set -euo pipefail

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

LC_ALL=C sort "${INPUT_FILE}" | uniq > "${OUTPUT_FILE}"
