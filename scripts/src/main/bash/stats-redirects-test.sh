#!/bin/bash

# Tests that stats and redirects endpoints return 200 for all @mappings languages

BASE_URL="http://localhost:9999/server"
FAILED=0
FAILED_TESTS=()

echo "=========================================="
echo "Fetching languages with @mappings..."

MAPPINGS_PAGE=$(curl -s "${BASE_URL}/mappings/")
[ -z "$MAPPINGS_PAGE" ] && { echo "❌ ERROR: Could not fetch mappings page"; exit 1; }

LANGUAGES=($(echo "$MAPPINGS_PAGE" | grep -oP 'href="\K[^/"]+(?=/">)' | sort -u))
[ ${#LANGUAGES[@]} -eq 0 ] && { echo "❌ ERROR: No languages found"; exit 1; }

echo "Found ${#LANGUAGES[@]} languages: ${LANGUAGES[@]}"
echo ""

# Test endpoint function
test_endpoint() {
    local lang="$1"
    local type="$2"
    local url="$3"

    http_code=$(curl -s -o /dev/null -w "%{http_code}" "$url")

    # Wikidata stats endpoint expected to return 501 or 500 
    if [[ "$lang" == "wikidata" && "$type" == "Stats    " ]]; then
        if [[ "$http_code" == "501" || "$http_code" == "500" ]]; then
            echo "✅ $lang $type: HTTP $http_code (expected - no template stats for Wikidata)"
            return 0  # Return success, don't increment FAILED
        fi
    fi

    if [ "$http_code" = "200" ]; then
        echo "✅ $lang $type: HTTP $http_code"
    else
        echo "❌ $lang $type: HTTP $http_code"
        FAILED=$((FAILED + 1))
        FAILED_TESTS+=("$lang $type (HTTP $http_code)")
    fi
}

# Test both endpoints for each language
echo "Testing Statistics and Redirects..."
echo "=========================================="
for lang in "${LANGUAGES[@]}"; do
    test_endpoint "$lang" "Stats    " "${BASE_URL}/statistics/${lang}/"
    test_endpoint "$lang" "Redirects" "${BASE_URL}/mappings/${lang}/redirects/"
done

# Summary
echo ""
echo "=========================================="
echo "SUMMARY: $((${#LANGUAGES[@]} * 2)) tests, $((${#LANGUAGES[@]} * 2 - FAILED)) passed, $FAILED failed"

if [ $FAILED -gt 0 ]; then
    echo ""
    echo "❌ Failed:"
    printf '  %s\n' "${FAILED_TESTS[@]}"
    exit 1
fi

echo "✅ All tests passed!"
exit 0