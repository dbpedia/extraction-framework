#!/bin/bash

# Test script for Wikidata extractors
# Tests each extractor individually against Obama entity (Q76)
# Usage: ./test-wikidata-extractors.sh [server_url] [revision_id]

set -e

# Configuration
SERVER_URL="${1:-http://localhost:9999}"
ENTITY_ID="Q76"  # Barack Obama
REVISION_ID="${2:-2424202969}"  # Barack Obama revision from Nov 1, 2025

# Constants
readonly SEPARATOR="============================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# List of Wikidata extractors to test
EXTRACTORS=(
    "WikidataSameAsExtractor"
    "WikidataRawExtractor"
    "WikidataLabelExtractor"
    "WikidataDescriptionExtractor"
    "WikidataAliasExtractor"
    "WikidataR2RExtractor"
    "WikidataReferenceExtractor"
    "WikidataNameSpaceSameAsExtractor"
    "WikidataLLExtractor"
)

# Counter for results
PASSED=0
FAILED=0
TOTAL=${#EXTRACTORS[@]}

echo "$SEPARATOR"
echo "Testing Wikidata Extractors"
echo "$SEPARATOR"
echo "Server URL: $SERVER_URL"
echo "Entity ID: $ENTITY_ID"
echo "Revision ID: $REVISION_ID"
echo "Total extractors to test: $TOTAL"
echo "$SEPARATOR"
echo ""

# Function to test a single extractor
test_extractor() {
    local extractor=$1
    
    echo -n "Testing $extractor... "
    
    # Construct the URL for extraction
    local url="${SERVER_URL}/server/extraction/wikidata/extract?title=${ENTITY_ID}&revid=${REVISION_ID}&extractors=.${extractor}"
    
    # Make the request and capture headers + body + status
    local temp_headers=$(mktemp)
    local temp_body=$(mktemp)
    
    http_code=$(curl -s -w "%{http_code}" -D "$temp_headers" -o "$temp_body" "$url" 2>&1)
    curl_exit=$?
    
    if [[ $curl_exit -ne 0 ]]; then
        echo -e "${RED}FAILED${NC} (curl error: exit code $curl_exit)" >&2
        rm -f "$temp_headers" "$temp_body"
        ((FAILED++))
        return
    fi
    
    # Check for X-DIEF-Error header (primary error indicator)
    local error_header=$(grep -i "^X-DIEF-Error:" "$temp_headers" | cut -d: -f2- | tr -d '\r' | xargs || echo "")
    
    # Read body for additional validation
    local body=$(cat "$temp_body")
    
    # Clean up temp files
    rm -f "$temp_headers" "$temp_body"
    
    # Check results using header flag first, then HTTP status
    if [[ -n "$error_header" ]]; then
        echo -e "${RED}FAILED${NC} (X-DIEF-Error: $error_header)"
        ((FAILED++))
    elif [[ "$http_code" -eq 200 ]]; then
        # Additional body validation as fallback
        if echo "$body" | grep -qi "\"error\"\s*:\|\"exception\"\s*:\|\"failed\"\s*:"; then
            echo -e "${YELLOW}WARNING${NC} (HTTP 200 but body contains error fields)"
            ((FAILED++))
        else
            echo -e "${GREEN}PASSED${NC} (HTTP 200)"
            ((PASSED++))
        fi
    else
        echo -e "${RED}FAILED${NC} (HTTP $http_code)"
        ((FAILED++))
    fi
}

# Test each extractor
for extractor in "${EXTRACTORS[@]}"; do
    test_extractor "$extractor" || true
done

echo ""
echo "$SEPARATOR"
echo "Test Summary"
echo "$SEPARATOR"
echo -e "Total:  $TOTAL"
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo "$SEPARATOR"

# Exit with error code if any tests failed
if [[ $FAILED -gt 0 ]]; then
    echo -e "${RED}Some tests failed!${NC}"
    exit 1
else
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
fi