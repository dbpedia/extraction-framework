#!/bin/bash
set -e
FAILED=0

API="http://localhost:9999/server/extraction/en"
TITLE="Albert_Einstein"

echo "Testing Multi-Extractor API"
echo "=============================="

test_case() {
  local desc="$1"
  shift
  echo -n "$desc... "
  if eval "$@" >/dev/null 2>&1; then
    echo "✓ PASSED"
  else
    echo "✗ FAILED"
    FAILED=1
  fi
}

# Test 1
test_case "Test 1: Mappings-only + N-Triples" \
  'curl -sf "$API/extract?title=$TITLE&format=n-triples&extractors=mappings" | grep -q "^<"'

# Test 2
test_case "Test 2: All-enabled (custom) + Turtle" \
  'curl -sf "$API/extract?title=$TITLE&format=turtle&extractors=custom" | grep -qE "(@prefix|^<)"'

# Test 3
test_case "Test 3: PageId + Label + N-Triples" \
  'curl -sf "$API/extract?title=$TITLE&format=n-triples&extractors=PageIdExtractor,LabelExtractor" | grep -q "^<"'

# Test 4
test_case "Test 4: PageId + Label + Turtle" \
  'curl -sf "$API/extract?title=$TITLE&format=turtle&extractors=PageIdExtractor,LabelExtractor" | grep -qE "(@prefix|^<)"'

# Test 5 (invalid extractor)
echo -n "Test 5: Invalid extractor handling... "
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API/extract?title=$TITLE&format=n-triples&extractors=NoSuchExtractor")
if [[ "$HTTP_CODE" =~ ^400|500$ ]]; then
  echo "✓ PASSED ($HTTP_CODE)"
else
  echo "✗ FAILED (got $HTTP_CODE)"
  FAILED=1
fi

# Test 6 (partial + warning header)
echo -n "Test 6: Partial results with warnings... "
RESPONSE=$(curl -sD - "$API/extract?title=$TITLE&format=n-triples&extractors=PageIdExtractor,InvalidExtractor")
if echo "$RESPONSE" | grep -qi "X-Extraction-Warnings" && echo "$RESPONSE" | grep -q "^<"; then
  echo "✓ PASSED"
else
  echo "✗ FAILED"
  FAILED=1
fi

echo "=============================="

if [ $FAILED -eq 0 ]; then
  echo "✓ All tests passed"
  exit 0
else
  echo "✗ Some tests failed"
  exit 1
fi
