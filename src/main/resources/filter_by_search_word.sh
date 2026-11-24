#!/usr/bin/env bash
# Usage: ./search_lines.sh <directory> <search_word>

DIR="$1"
SEARCH_WORD="$2"
OUTPUT_FILE="$3"

if [[ -z "$DIR" || -z "$SEARCH_WORD" ]]; then
    echo "Usage: $0 <directory> <search_word>"
    exit 1
fi

find "$DIR" -maxdepth 1 -type f -name '*.txt' -print0 |
while IFS= read -r -d '' file; do
    awk -v word="$SEARCH_WORD" '
        index($0, word) > 0 {
            print
        }
    ' "$file" >> "$OUTPUT_FILE"
done

echo "Matching lines appended to $OUTPUT_FILE"
```