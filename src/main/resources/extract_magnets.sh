#!/usr/bin/env bash
#
# extract_magnets.sh - Iterates all .html files in a given directory, parses
# each <tr class=tlz> or <tr class=tlr> ... </tr> block (these are the
# alternating-color row styles used for torrent listing rows) and extracts:
#   1. magnet hash (between "btih:" and ";dn")
#   2. dn value    (between "dn=" and ";tr")
#   3. size value  (content of the <td>...</td> cell holding the size, e.g. "6.18 GB")
#
# Output: one CSV-ish line per row, in the form:
#   hash,dn,size
# appended to the given output file.
#
# Usage:
#   ./extract_magnets.sh <input-directory> <output-file>
#
set -euo pipefail

log() {
    printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" >&2
}

usage() {
    echo "Usage: $0 <input-directory> <output-file>" >&2
    exit 1
}

if [[ $# -ne 2 ]]; then
    usage
fi

INPUT_DIR="$1"
OUTPUT_FILE="$2"

log "Starting extraction. input-directory='${INPUT_DIR}' output-file='${OUTPUT_FILE}'"

if [[ ! -d "${INPUT_DIR}" ]]; then
    log "ERROR: Input directory not found: ${INPUT_DIR}"
    exit 1
fi

: > "${OUTPUT_FILE}"
log "Output file initialized (truncated): ${OUTPUT_FILE}"

shopt -s nullglob
html_files=("${INPUT_DIR}"/*.html)
shopt -u nullglob

if [[ ${#html_files[@]} -eq 0 ]]; then
    log "No .html files found in: ${INPUT_DIR}"
    exit 0
fi

log "Found ${#html_files[@]} .html file(s) to process."

total_matches=0
file_count=0

for f in "${html_files[@]}"; do
    file_count=$((file_count + 1))
    log "Processing file (${file_count}/${#html_files[@]}): ${f}"

    lines_before=$(wc -l < "${OUTPUT_FILE}" 2>/dev/null || echo 0)

    awk '
        BEGIN {
            in_row = 0
            row = ""
        }
        {
            line = $0
        }
        # Detect start of a tlz/tlr row (allow optional quotes/spaces around class value)
        /<tr[[:space:]]+class=["'\''"]?tl[a-z]["'\''"]?/ {
            in_row = 1
            row = line
            if (line ~ /<\/tr>/) {
                process_row(row)
                in_row = 0
                row = ""
            }
            next
        }
        {
            if (in_row) {
                row = row "\n" line
                if (line ~ /<\/tr>/) {
                    process_row(row)
                    in_row = 0
                    row = ""
                }
            }
        }

        function process_row(r,    hash, dn, size, tmp, n, parts, i, cell) {
            hash = ""
            dn = ""
            size = ""

            # Extract hash: between "btih:" and the next "&" (e.g. "&amp;dn=...")
            if (match(r, /btih:[^;]+/)) {
                hash = substr(r, RSTART + 5, RLENGTH - 5)
            }

            # Extract dn: between "dn=" and the next "&" (e.g. "&amp;tr=...")
            if (match(r, /dn=[^;]*/)) {
                dn = substr(r, RSTART + 3, RLENGTH - 3)
                # URL-decode common escapes
                gsub(/%28/, "(", dn)
                gsub(/%29/, ")", dn)
                gsub(/%2B/, "+", dn)
                gsub(/\+/, " ", dn)
            }

            # Extract size: find a <td>X</td> cell whose content looks like a size
            # (number followed by KB/MB/GB/TB, case-insensitive)
            n = split(r, parts, "<td")
            for (i = 1; i <= n; i++) {
                cell = parts[i]
                if (match(cell, />[^<]*[0-9](\.[0-9]+)?[[:space:]]*(KB|MB|GB|TB)[^<]*</)) {
                    size = substr(cell, RSTART + 1, RLENGTH - 2)
                    gsub(/^[[:space:]]+|[[:space:]]+$/, "", size)
                    break
                }
            }

            if (hash != "" && dn != "") {
                print hash "," dn "," size
            }
        }
    ' "$f" >> "${OUTPUT_FILE}"

    lines_after=$(wc -l < "${OUTPUT_FILE}" 2>/dev/null || echo 0)
    file_matches=$((lines_after - lines_before))
    total_matches=$((total_matches + file_matches))
    log "Finished file: ${f} -> ${file_matches} matching row(s) extracted."
done

log "Extraction complete. Files processed: ${file_count}. Total matches: ${total_matches}. Output: ${OUTPUT_FILE}"
echo "Done. Results written to: ${OUTPUT_FILE}"
