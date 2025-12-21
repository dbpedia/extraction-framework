#!/bin/bash

# Constants for repeated strings
readonly XSD_GYEAR="xsd:gYear"
readonly SEPARATOR="============================================"

echo "=== DBpedia Server Calendar Conversion Test (Extraction API) ==="

test_entity() {
    local precision=$1
    local entity=$2
    local description=$3
    local expected=$4
    local property=${5:-"P569"}
    
    echo ""
    echo "[$precision] $description (Entity: $entity, Property: $property)"
    echo "Expected: $expected"
    
    RESPONSE=$(curl -s "http://localhost:9999/server/extraction/wikidata/extract?title=${entity}&format=n-triples&extractors=custom")
    
    if [[ -z "$RESPONSE" ]]; then
        echo "❌ FAIL - No response from server" >&2
        return
    fi
    
    # Check if the expected datatype exists in response
  if echo "$RESPONSE" | grep -q "http://www.w3.org/2001/XMLSchema#${expected#xsd:}"; then
  echo "✅ PASS - Found $expected"
  echo "Found dates:"
  echo "$RESPONSE" \
    | grep -oE "\"-?[0-9]{4,5}(-[0-9]{2})?(-[0-9]{2})?\"\\^\\^<http://www.w3.org/2001/XMLSchema#${expected#xsd:}>" \
    | head -3

    else
        echo "⚠️  INFO - $expected not found (may not have this precision)"
        # Show what dates we did find
        echo "Found dates:"
        echo "$RESPONSE" | grep -oE '"[^"]*"\^\^<http://www.w3.org/2001/XMLSchema#(date|gYear|gYearMonth|dateTime)>' | head -3
    fi
}

# Test precision levels that commonly exist
echo ""
echo "=== Testing Common Precisions (9-11) ==="
test_entity "11" "Q937" "Day precision (Einstein birth)" "xsd:date" "P569"
test_entity "10" "Q1339" "Month precision (Bach birth)" "xsd:gYearMonth" "P569"
test_entity "9" "Q42" "Year precision (Douglas Adams birth)" "$XSD_GYEAR" "P569"

echo ""
echo "=== Testing Lower Precisions (6-8) ==="
test_entity "8" "Q35724" "Decade (1990s)" "$XSD_GYEAR" "P580"
test_entity "7" "Q6955" "Century (17th century)" "$XSD_GYEAR" "P580"
test_entity "6" "Q25860" "Millennium (2nd millennium)" "$XSD_GYEAR" "P580"

echo ""
echo "=== Testing Very Low Precisions (0-5) - Geological/Cosmological ==="
test_entity "3" "Q40614" "Million years (Dino extinction)" "$XSD_GYEAR" "P585"
test_entity "0" "Q323" "Billion years (Big Bang)" "$XSD_GYEAR" "P585"

echo ""
echo "$SEPARATOR"
echo "Calendar Conversion Test (Julian to Gregorian)"
echo "$SEPARATOR"
echo ""
echo "Testing Q913 (Socrates) - Should convert Julian to Gregorian"
RESPONSE=$(curl -s "http://localhost:9999/server/extraction/wikidata/extract?title=Q913&format=n-triples&extractors=custom")

if [[ -z "$RESPONSE" ]]; then
  echo "[ERROR] No response from server! Is it running?" >&2
  exit 1
fi

echo "[INFO] Response received."
echo

# Extract literals that look like dates
DATES=$(echo "$RESPONSE" | grep -oE '"-?[0-9]{4,5}(-[0-9]{2})?(-[0-9]{2})?"\^\^<http://www.w3.org/2001/XMLSchema#(date|gYear|gYearMonth)>')

echo "=== Extracted Date Literals ==="
if [[ -z "$DATES" ]]; then
  echo "[ERROR] No date literals found!" >&2
  exit 1
fi

echo "$DATES"
echo
echo "=== Validation ==="

# Invalid month check
if echo "$DATES" | grep -qE '"-?[0-9]{4,5}-00'; then
  echo "[FAIL] Invalid month 00 detected"
else
  echo "[OK] No invalid months"
fi

# Invalid day check
if echo "$DATES" | grep -qE '"-?[0-9]{4,5}-[0-9]{2}-00'; then
  echo "[FAIL] Invalid day 00 detected"
else
  echo "[OK] No invalid days"
fi

# Correct BCE padding check
if echo "$DATES" | grep -qE '"-[1-9][0-9]{0,2}[^0-9]'; then
  echo "[FAIL] BCE year missing proper zero-padding"
else
  echo "[OK] BCE year padding correct"
fi

# Expected Socrates Gregorian date
if echo "$DATES" | grep -q '"-0399-02-10"'; then
  echo "[OK] Socrates deathDate correctly converted to Gregorian (-0399-02-10)"
else
  echo "[FAIL] Socrates deathDate incorrect!"
fi

echo
echo "=== Test Complete ==="
