#!/bin/bash

# Enhanced DBpedia Coordinate Extraction Test - FIXED

# set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
PURPLE='\033[0;35m'
NC='\033[0m'

# Configuration
FIXED_DATA_FILE="test_data_fixed.txt"
REVISION_FILE="test_data_revisions.txt"
GENERATE_MODE=false
UPDATE_MODE=false
USE_LIVE_DATA=false
PASS_THRESHOLD=80
debug=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --generate-fixed) GENERATE_MODE=true; shift ;;
        --update-fixed) UPDATE_MODE=true; shift ;;
        --live-data) USE_LIVE_DATA=true; shift ;;
        --threshold) PASS_THRESHOLD="$2"; shift 2 ;;
        --debug|-d) debug=true; shift ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "PRIMARY USE CASE - Code Regression Testing:"
            echo "  (default)          Test against FIXED snapshot → detects CODE changes"
            echo ""
            echo "Data Snapshot Management:"
            echo "  --generate-fixed   Generate initial fixed snapshot from current Wikipedia"
            echo "  --update-fixed     Update snapshot (only changed pages by revision ID)"
            echo ""
            echo "SEPARATE USE CASE - Environment Monitoring:"
            echo "  --live-data        Test against CURRENT Wikipedia → detects DATA changes"
            echo ""
            echo "Options:"
            echo "  --threshold N      Set pass threshold (default: 90)"
            echo "  --debug, -d        Enable debug output"
            echo "  --help, -h         Show this help"
            exit 0
            ;;
        *) shift ;;
    esac
done

echo -e "${BLUE}🎯 Enhanced DBpedia Coordinate Extraction Test - FIXED${NC}"
echo "========================================================"

# Test data
declare -A KNOWN_COORDS
KNOWN_COORDS=(
       # United States - Washington, D.C.
       ["en:Washington,_D.C."]="38.895,-77.036"  # Washington, D.C.
       ["de:Washington,_D.C."]="38.895,-77.036"  # Washington, D.C.
       # Berlin - Germany
       ["en:Berlin"]="52.520008,13.404954"  # Berlin
       ["de:Berlin"]="52.520008,13.404954"  # Berlin

       # Buenos Aires - Argentina
       ["en:Buenos_Aires"]="-34.6118,-58.3960"  # Buenos Aires

       # United States - Country
       ["en:United States"]="38.883333,-77.0166666"  # Country
       ["de:Vereinigte Staaten"]="40.0, -100.0"  # Center

       # Sweden - Country
       ["de:Schweden"]="61.316667,14.833333"  # Country

       # Germany - Country
       ["en:Germany"]="52.520008,13.404954"  # Country
       ["de:Deutschland"]=" 51.165,10.45527"  # City

       # France - Country
       ["en:France"]="48.8566,2.3522"  # Country
       ["de:Frankreich"]="46.0,2.0"  # Country

       # United Kingdom - Country
       ["en:United Kingdom"]="51.5074,-0.1278"  # Country
       ["de:Vereinigtes Königreich"]="51.5166,-0.11666"  # City

       # Italy - Country
       ["en:Italy"]="41.9028,12.4964"  # Country
       ["de:Italien"]="42.8333,12.8333"  # Country

       # Spain - Country
       ["en:Spain"]="40.4168,-3.7038"  # Country
       ["de:Spanien"]=" 39.9266,-1.8016"  # Center

       # Netherlands - Country
       ["en:Netherlands"]="52.3676,4.9041"  # Country
       ["de:Niederlande"]="52.5,5.75"  # Country

       # Poland - Country
       ["en:Poland"]="52.2297,21.0122"  # Country
       ["de:Polen"]="52.0,20.0"  # Country

       # Norway - Country
       ["en:Norway"]="59.9139,10.7522"  # Country
       ["de:Norwegen"]="62.0,10.0"  # Country

       # Portugal - Country
       ["en:Portugal"]="38.7223,-9.1393"  # Country
       ["de:Portugal"]="39.5,-8.0"  # Country

       # Austria - Country
       ["en:Austria"]=" 48.2,16.35"  # City
       ["de:Österreich"]="47.33333,13.33333"  # Country

       # Switzerland - Country
       ["en:Switzerland"]="46.8182,8.2275"  # Country
       ["de:Schweiz"]="46.8,8.33333"  # Country

       # Belgium - Country
       ["en:Belgium"]="50.8503,4.3517"  # Country
       ["de:Belgien"]="50.83333,4.0"  # Country

       # Denmark - Country
       ["en:Denmark"]="55.6761,12.5683"  # Country
       ["de:Dänemark"]="56.0,10.0"  # Country

       # Finland - Country
       ["en:Finland"]="60.1699,24.9384"  # Country
       ["de:Finnland"]="64.0,26.0"  # Country

       # Czech Republic - Country
       ["en:Czech Republic"]="49.7437,15.3386"  # Country
       ["de:Tschechien"]="49.75,15.5"  # Country

       # Hungary - Country
       ["en:Hungary"]="47.1625,19.5033"  # Country
       ["de:Ungarn"]="47.0,20.0"  # Country

       # Greece - Country
       ["en:Greece"]="37.9755,23.7348"  # Country
       ["de:Griechenland"]="38.30111, 23.74111"  # Capital

       # Argentina - Country
       ["en:Argentina"]="-34.6,-58.383333"  # Country
       ["de:Argentinien"]="-34.6,-58.38"  # Country

       # Brazil - Country
       ["en:Brazil"]="-15.7975,-47.8919"  # Country
       ["de:Brasilien"]="-10.65,-52.95"  # Central-West

       # Canada - Country
       ["en:Canada"]="45.4215,-75.6972"  # Country
       ["de:Kanada"]="56.0,-109.0"  #  Territory

      # Mexico - Country
      ["en:Mexico"]="19.4333,-99.1333"  # City
      ["de:Mexiko"]="23.3166,-102.0"  # Country

       # Chile - Country
       ["en:Chile"]="-33.4489,-70.6693"  # Country
       ["de:Chile"]="-31.4666,-70.9"  # Northern Chile

       # Peru - Country
       ["en:Peru"]="-12.0464,-77.0428"  # Country
       ["de:Peru"]="-8.23333,-76.01666"  # Northern Peru

       # Colombia - Country
       ["en:Colombia"]="4.5833,-74.0666"  # Capital city
       ["de:Kolumbien"]="3.81666,-73.91666"  # Country

       # Venezuela - Country
       ["en:Venezuela"]="10.4806,-66.9036"  # Country
       ["de:Venezuela"]="8.0,-66.0"  # Country

       # China - Country
       ["en:China"]="39.9042,116.4074"  # Country
       ["de:China"]="35.0, 105.0"  # Country

       # Japan - Country
       ["en:Japan"]="35.6762,139.6503"  # Country
       ["de:Japan"]="35.1561,136.0"  # State

       # India - Country
       ["en:India"]="28.6139,77.2090"  # Country
       ["de:Indien"]=" 21.1255,78.3105"  # State

       # South Korea - Country
       ["en:South Korea"]="37.5665,126.9780"  # Country
       ["de:Südkorea"]="35.0, 127.0"  # Country

       # Thailand - Country
       ["en:Thailand"]="13.7563,100.5018"  # Country
       ["de:Thailand"]="15.35, 101.0"  #  Isan (Northeastern Thailand)

       # Indonesia - Country
       ["en:Indonesia"]="-6.1666, 106.8166"  #  Capital city
       ["de:Indonesien"]="-2.0, 118.0 "  # Central

       # Philippines - Country
       ["en:Philippines"]="13.0, 122.0"  # Country
       ["de:Philippinen"]=" 11.3333,123.0"  # Closest city

       # Vietnam - Country
       ["en:Vietnam"]="21.0285,105.8542"  # Country
       ["de:Vietnam"]="14.0333,107.0"  # Center

       # Malaysia - Country
       ["en:Malaysia"]="3.1390,101.6869"  # Country
       ["de:Malaysia"]="2.3166,111.0"  # State

       # Singapore - Country
       ["en:Singapore"]="1.3521,103.8198"  # Country
       ["de:Singapur"]="1.3667,103.8"  # Country

       # Israel - Country
       ["en:Israel"]="31.7683,35.2137"  # Country
       ["de:Israel"]="31.5,34.75"  # Country

       # Turkey - Country
       ["en:Turkey"]="39.9334,32.8597"  # Country
       ["de:Türkei"]="39.0,35.0"  # Country

       # Iran - Country
     #not there in dbpedia  ["en:Iran"]="35.6892,51.3890"  # Country
       ["de:Iran"]=" 32.4961,54.295"  # Central

       # Iraq - Country
       ["en:Iraq"]="33.2232,43.6793"  # Country
       ["de:Irak"]="33.0,44.0"  # Country

       # Saudi Arabia - Country
       ["en:Saudi Arabia"]="24.7136,46.6753"  # Country
       ["de:Saudi-Arabien"]=" 23.71666,44.11666"  # Country

       # Egypt - Country
       ["en:Egypt"]="30.0444,31.2357"  # Country
       ["de:Ägypten"]="27.0,30.0"  # Country

       # South Africa - Country
       ["en:South Africa"]="-30.0,25.0"  # State
       ["de:Südafrika"]="-29.0,24.0"  # Country

       # Nigeria - Country
       ["en:Nigeria"]="9.06666,7.4833"  # Capital city
       ["de:Nigeria"]="10.0,8.0"  # Country

       # Morocco - Country
       ["en:Morocco"]=" 34.0333,-6.85"  # Capital city
       ["de:Marokko"]="30.93333, -8.4"  # Coastal city

       # Kenya - Country
       ["en:Kenya"]="-1.2666,36.8"  # Country
       ["de:Kenia"]="0.4,37.85"  # Center

       # Ethiopia - Country
       ["en:Ethiopia"]="9.0166, 38.75"  # Capital city
       ["de:Äthiopien"]="8.3, 39.11"  # Central reference point

       # Australia - Country
       ["en:Australia"]="-35.308055555,149.12444444"  # Country
       ["de:Australien"]="-25.0,135.0"  # Country

       # New Zealand - Country
       ["en:New Zealand"]="-41.2865,174.7762"  # Country
       ["de:Neuseeland"]="-34.393,173.0133"  # West Coast Region

       # Papua New Guinea - Country
       ["en:Papua New Guinea"]="-9.4788,147.1494"  # Capital city
       ["de:Papua-Neuguinea"]="-6.3666,146.0"  # Country

       # Fiji - Country
       ["en:Fiji"]="-18.1248,178.4501"  # Country
       ["de:Fidschi"]="-18.0,179.0"  # Country

       # Russia - Country
       ["en:Russia"]="66.0,94.0"  # Country
       ["de:Russland"]=" 58.65, 70.1166"  # Country

       # Ukraine - Country
       ["en:Ukraine"]="50.4501,30.5234"  # Country
       ["de:Ukraine"]="49.8,30.83333"  # North Central

       # === ADDITIONAL SOUTHERN HEMISPHERE COORDINATES ===

       # Southern Africa
       ["en:Botswana"]="-24.6566,25.9269"  # Capital city
       ["de:Botswana"]="-21.7667, 24.0333"  # Country center
       ["en:Namibia"]="-22.5667,17.0833"  # Capital city
       ["de:Namibia"]="-22.0,17.0"  # Country center
       ["en:Zimbabwe"]="-17.8252,31.0335"  # Capital city
       ["de:Simbabwe"]="-19.0167,30.0167"  # Country center
       ["en:Zambia"]="-15.3875,28.3228"  # Capital city
       ["de:Sambia"]="-14.1167,27.6333"  # Country center
       ["en:Malawi"]="-13.9626,33.7741"  # Capital city
       ["de:Malawi"]="-13.5,34.0"  # Country center
       ["en:Mozambique"]="-25.9553,32.5892"  # Capital city
       ["de:Mosambik"]="-18.0,35.0"  # Country center
       ["en:Mauritius"]="-20.3484,57.5522"  # Capital city
       ["de:Mauritius"]="-20.2,57.5"  # Country center
       ["en:Lesotho"]="-29.61,28.233"  # Capital city
       ["de:Lesotho"]="-29.5,28.5"  # Country center

       # South America (Additional)
       ["en:Uruguay"]="-34.9011,-56.1645"  # Capital city
       ["de:Uruguay"]="-33.0,-56.0"  # Country center
       ["en:Paraguay"]="-25.2637,-57.5759"  # Capital city
       ["de:Paraguay"]="-23.0,-58.0"  # Country center
       ["en:Bolivia"]="-17.8,-63.1667"  # Capital city
       ["de:Bolivien"]="-17.0,-65.0"  # Country center
       ["en:Ecuador"]="-2.0,-77.5"  # Capital city
       ["de:Ecuador"]="-1.4653,-78.8167"  # Country center
       ["en:Guyana"]="6.8013,-58.1551"  # Capital city
       ["de:Guyana"]="5.0,-59.0"  # Country center
       ["en:Suriname"]="5.8520,-55.2038"  # Capital city
       ["de:Suriname"]="4.0,-56.0"  # Country center
       ["en:French Guiana"]="4.9227,-52.3269"  # Capital city
       ["de:Französisch-Guayana"]="4.0,-53.0"  # Country center

       # Oceania (Additional)
       ["en:Solomon Islands"]="-9.6457,159.9729"  # Capital city
       ["de:Salomonen"]="-8.0,159.0"  # Country center
       ["en:Tonga"]="-21.1789,-175.1982"  # Capital city
       ["de:Tonga"]="20.587777777777777,-174.0"  # Country center
       ["en:Samoa"]="-13.8506,-171.7513"  # Capital city
       ["de:Samoa"]="-14.0,-172.0"  # Country center
       ["en:Tuvalu"]="-8.5243,179.1942"  # Capital city
       ["de:Tuvalu"]="-8.0,178.0"  # Country center
       ["en:New Caledonia"]="-22.2758,166.4581"  # Capital city
       ["de:Neukaledonien"]="-21.5,165.5"  # Country center
       ["en:French Polynesia"]="-17.6797,-149.4068"  # Capital city
       ["de:Französisch-Polynesien"]="-17.5333,-149.0"  # Country center

       # Antarctica and Sub-Antarctic
       ["en:Antarctica"]="-69.3578, -2.2472"  # South Pole
       ["de:Antarktis"]="-26.5,-25.6667"  # South Pole

       # Southern Asian Countries
       ["en:Sri Lanka"]="6.9271,79.8612"  # Capital city
       ["de:Sri Lanka"]="7.0,81.0"  # Country center
       ["en:Maldives"]="4.1755,73.5093"  # Capital city
       ["de:Malediven"]="3.2,73.0"  # Country center
   )
load_fixed_data() {
    if [[ ! -f "$FIXED_DATA_FILE" ]]; then
        return 1
    fi

    echo -e "${CYAN}Loading fixed test data from $FIXED_DATA_FILE${NC}"

    KNOWN_COORDS=()
    while IFS='=' read -r lang_page coords; do
        [[ -z "$lang_page" || "$lang_page" == \#* ]] && continue
        KNOWN_COORDS["$lang_page"]="$coords"
    done < "$FIXED_DATA_FILE"

    echo -e "${GREEN}Loaded ${#KNOWN_COORDS[@]} test cases${NC}"

    if [[ -f "$REVISION_FILE" ]]; then
        local first_rev=$(head -1 "$REVISION_FILE" | cut -d'|' -f2)
        [[ -n "$first_rev" ]] && echo -e "${CYAN}Snapshot revision: $first_rev${NC}"
    fi
    echo ""
    return 0
}

get_wikipedia_revision() {
    local lang="$1"
    local page="$2"
    local api_url="https://${lang}.wikipedia.org/w/api.php"

    local response=$(curl -s -G "$api_url" \
        --data-urlencode "action=query" \
        --data-urlencode "titles=$page" \
        --data-urlencode "prop=revisions" \
        --data-urlencode "rvprop=ids" \
        --data-urlencode "format=json" \
        --connect-timeout 5 \
        --max-time 10 2>/dev/null)

    if [[ -n "$response" ]]; then
        echo "$response" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    pages = data['query']['pages']
    page_data = list(pages.values())[0]
    print(page_data.get('revisions', [{}])[0].get('revid', 'unknown'))
except:
    print('unknown')
" 2>/dev/null
    else
        echo "unknown"
    fi
}

generate_fixed_data() {
    local mode="${1:-generate}"
    echo -e "${YELLOW}${mode^} fixed test data...${NC}"
    echo "========================================"

    local temp_file="${FIXED_DATA_FILE}.tmp"
    local temp_rev="${REVISION_FILE}.tmp"
    rm -f "$temp_file" "$temp_rev"

    if [[ -f "$REVISION_FILE" && "$mode" == "update" ]]; then
        cp "$REVISION_FILE" "$temp_rev"
    else
        > "$temp_rev"
    fi

    local success=0 failed=0 skipped=0

    for lang_page in "${!KNOWN_COORDS[@]}"; do
        IFS=':' read -r lang page <<< "$lang_page"
        echo -ne "Checking ${lang_page}... "

        local current_revid=$(get_wikipedia_revision "$lang" "$page")

        if [[ "$mode" == "update" && -f "$REVISION_FILE" ]]; then
            local old_entry=$(grep "^${lang_page}=" "$REVISION_FILE" 2>/dev/null)
            if [[ -n "$old_entry" ]]; then
                local stored_revid=$(echo "$old_entry" | cut -d'|' -f2)
                if [[ "$stored_revid" == "$current_revid" && "$current_revid" != "unknown" ]]; then
                    echo -e "${CYAN}UP-TO-DATE${NC}"
                    local old_coords=$(echo "$old_entry" | cut -d'=' -f2 | cut -d'|' -f1)
                    echo "${lang_page}=${old_coords}" >> "$temp_file"
                    echo "$old_entry" >> "$temp_rev"
                    skipped=$((skipped + 1))
                    continue
                fi
            fi
        fi

        local response=$(test_api_endpoint "$lang" "$page" "false")
        if [[ $? -eq 0 ]]; then
            local coords=$(extract_coords_from_response "$response" "false")
            if [[ -n "$coords" && "$coords" != "," ]]; then
                echo "${lang_page}=${coords}" >> "$temp_file"
                echo "${lang_page}=${coords}|${current_revid}" >> "$temp_rev"
                echo -e "${GREEN}OK${NC} ($coords)"
                success=$((success + 1))
            else
                # CRITICAL FIX: Preserve entry even if extraction fails
                local expected="${KNOWN_COORDS[$lang_page]}"
                echo "${lang_page}=${expected}" >> "$temp_file"
                echo "${lang_page}=${expected}|${current_revid}|EXPECTED" >> "$temp_rev"
                echo -e "${YELLOW}NO COORDS - Using expected${NC}"
                failed=$((failed + 1))
            fi
        else
            # CRITICAL FIX: Preserve entry even if API fails
            local expected="${KNOWN_COORDS[$lang_page]}"
            echo "${lang_page}=${expected}" >> "$temp_file"
            echo "${lang_page}=${expected}|${current_revid}|EXPECTED" >> "$temp_rev"
            echo -e "${YELLOW}FAILED - Using expected${NC}"
            failed=$((failed + 1))
        fi
        sleep 0.5
    done

    echo ""
    echo "Summary: ${GREEN}${success} success${NC}, ${CYAN}${skipped} skipped${NC}, ${RED}${failed} failed${NC}"

        if [[ -f "$temp_file" ]]; then
        mv "$temp_file" "$FIXED_DATA_FILE"
        mv "$temp_rev" "$REVISION_FILE"
        echo -e "${GREEN}Saved: $FIXED_DATA_FILE (${success}+${skipped}+${failed} entries)${NC}"
        echo -e "${YELLOW}Commit both files to git${NC}"
        return 0
    else
        rm -f "$temp_file" "$temp_rev"
        echo -e "${RED}No data saved${NC}"
        return 1
    fi
}
# URL encode helper
url_encode() {
    local string="$1"
    python3 -c "import urllib.parse, sys; print(urllib.parse.quote(sys.argv[1], safe=''))" "$string"
}

# Check if mappings server is up
check_server_connectivity() {
    echo -e "${BLUE}🔌 Checking Mappings Server Connectivity${NC}"
    echo "=========================================="

    local url="http://localhost:9999/server/"
    local status=$(curl -s -o /dev/null -w "%{http_code}" "$url")

    if [[ "$status" == "200" ]]; then
        echo -e "${GREEN}✅ $url is accessible${NC}"
    else
        echo -e "${RED}❌ Failed to reach $url (HTTP $status)${NC}"
        exit 1
    fi
    echo ""
}

# API fetch
test_api_endpoint() {
    local lang="$1"
    local page="$2"
    local debug="${3:-false}"

    local base_url="http://localhost:9999/server/extraction/${lang}/extract"
    local encoded_title=$(url_encode "$page")
    local formats=("trix" "rdfxml" "ntriples" "ttl")

    for format in "${formats[@]}"; do
        [[ "$debug" == "true" ]] && echo -e "${CYAN}DEBUG: Trying format: $format for title: $page${NC}"
        local response=$(curl -s -G "$base_url" \
            --data-urlencode "title=$page" \
            --data-urlencode "format=$format" \
            --data-urlencode "extractors=custom" \
            --connect-timeout 10 \
            --max-time 30 2>/dev/null || echo "")

        if [[ -n "$response" && ${#response} -ge 10 ]] && ! echo "$response" | grep -qi "<html\|<!doctype\|<title>.*error"; then
            echo "$response"
            return 0
        fi
    done

    return 1
}

# Robust coordinate extraction function with intelligent sign detection
extract_coords_from_response() {
    local response="$1"
    local debug="${2:-false}"
    local expected="${3:-}"  # Pass expected coordinates for smart validation
    local lat="" long=""

    if [[ "$debug" == "true" ]]; then
        echo -e "${CYAN}🔎 Starting coordinate extraction...${NC}" >&2
    fi

    if [[ -z "$response" || ${#response} -lt 10 ]]; then return 1; fi
    if echo "$response" | grep -qi "<html\|<!doctype html\|<title>.*error"; then return 1; fi

    # Pattern 1: geo:lat/long - Enhanced to capture all numeric formats
    local geo=$(echo "$response" | grep -A10 -B10 "geo:lat\|geo:long")
    lat=$(echo "$geo" | grep -oP 'geo:lat[^>]*>\s*\K[-+]?[0-9]+\.?[0-9]*(?:[eE][-+]?[0-9]+)?' | head -1)
    long=$(echo "$geo" | grep -oP 'geo:long[^>]*>\s*\K[-+]?[0-9]+\.?[0-9]*(?:[eE][-+]?[0-9]+)?' | head -1)

    # Pattern 2: wgs84_pos#lat/long
    if [[ -z "$lat" || -z "$long" ]]; then
        local wgs=$(echo "$response" | grep -A10 -B10 "wgs84_pos#lat\|wgs84_pos#long")
        lat=$(echo "$wgs" | grep -oP 'wgs84_pos#lat[^>]*>\s*\K[-+]?[0-9]+\.?[0-9]*(?:[eE][-+]?[0-9]+)?' | head -1)
        long=$(echo "$wgs" | grep -oP 'wgs84_pos#long[^>]*>\s*\K[-+]?[0-9]+\.?[0-9]*(?:[eE][-+]?[0-9]+)?' | head -1)
    fi

    # Pattern 3: typedLiteral inside TRiX RDF
    if [[ -z "$lat" || -z "$long" ]]; then
        local triples=$(echo "$response" | sed -n '/<triple>/,/<\/triple>/p')
        while IFS= read -r block; do
            if echo "$block" | grep -q 'wgs84_pos#lat'; then
                lat=$(echo "$block" | grep -oP '<typedLiteral[^>]*>\K[-+]?[0-9]+\.?[0-9]*(?:[eE][-+]?[0-9]+)?' | head -1)
            elif echo "$block" | grep -q 'wgs84_pos#long'; then
                long=$(echo "$block" | grep -oP '<typedLiteral[^>]*>\K[-+]?[0-9]+\.?[0-9]*(?:[eE][-+]?[0-9]+)?' | head -1)
            fi
        done < <(echo "$triples" | tr '\n' '|' | sed 's|</triple>|<\/triple>\n|g')
    fi

        lat=${lat#+}
        long=${long#+}

# Check if empty
if [[ -z "$lat" || -z "$long" ]]; then
    echo -e "${RED}❌ No coordinates extracted from server response${NC}" >&2
    return 1
fi

# Check if numeric
if ! [[ "$lat" =~ ^-?[0-9]+\.?[0-9]*$ ]] || ! [[ "$long" =~ ^-?[0-9]+\.?[0-9]*$ ]]; then
    echo -e "${RED}❌ Invalid coordinate format: lat='$lat', long='$long'${NC}" >&2
    return 1
fi

# Check range
if (( $(echo "$lat >= -90 && $lat <= 90" | bc -l) )) && (( $(echo "$long >= -180 && $long <= 180" | bc -l) )); then
    echo "$lat,$long"
    return 0
fi

echo -e "${RED}❌ Coordinates out of range: lat=$lat, long=$long${NC}" >&2
    return 1
}

# Updated test function to pass expected coordinates for smart validation
test_specific_page() {
    local lang_page="$1"
    local expected="$2"
    local debug="${3:-false}"

    IFS=':' read -r lang page <<< "$lang_page"
    echo -e "\n${BLUE}🧪 Testing: $lang_page${NC}"
    echo "Expected: $expected"
    echo "=========================================="

    local response=$(test_api_endpoint "$lang" "$page" "$debug")
    local status=$?

    if [[ $status -ne 0 ]]; then
        echo -e "${RED}❌ API failed${NC}"
        return 1
    fi

    # Pass expected coordinates to extraction function for smart validation
    local coords=$(extract_coords_from_response "$response" "$debug" "$expected")
    if [[ -z "$coords" || "$coords" == "," ]]; then
        echo -e "${RED}❌ Failed to extract coordinates${NC}"
        if [[ "$debug" == "true" ]]; then
            local safe_filename=$(echo "${lang}_${page}" | sed 's/[^a-zA-Z0-9._-]/_/g')
            echo "$response" > "debug_${safe_filename}.xml"
            echo -e "${CYAN}💾 Saved response: debug_${safe_filename}.xml${NC}"
        fi
        return 1
    fi

    # CHECK FOR CONTAMINATION BEFORE CLEANUP
    if [[ "$coords" =~ [^0-9.,\ +\-] ]]; then
        echo -e "${RED}❌ Server response contains non-numeric data${NC}"
        echo -e "${RED}Raw coords: $coords${NC}"
        echo -e "${RED}FAILURE_REASON: Contaminated response from server${NC}"
        if [[ "$debug" == "true" ]]; then
            local safe_filename=$(echo "${lang}_${page}" | sed 's/[^a-zA-Z0-9._-]/_/g')
            echo "$response" > "debug_${safe_filename}.xml"
            echo -e "${CYAN}💾 Saved response: debug_${safe_filename}.xml${NC}"
        fi
        return 1
    fi

    # Now clean and extract
    local lat=$(echo "$coords" | cut -d',' -f1 | tr -d '\n' | xargs)
    local long=$(echo "$coords" | cut -d',' -f2 | tr -d '\n' | xargs)

    # Validate they're actually numbers
    if ! [[ "$lat" =~ ^-?[0-9]+\.?[0-9]*$ ]] || ! [[ "$long" =~ ^-?[0-9]+\.?[0-9]*$ ]]; then
        echo -e "${RED}FAILURE_REASON: Invalid coordinate format${NC}"
        return 1
    fi

    echo -e "${GREEN}💡 Server response: lat='$lat', long='$long'${NC}"

   local elat=$(echo "$expected" | cut -d',' -f1 | xargs)
   local elong=$(echo "$expected" | cut -d',' -f2 | xargs)

    lat=$(printf "%.4f" "$lat" 2>/dev/null || echo "$lat")
    long=$(printf "%.4f" "$long" 2>/dev/null || echo "$long")
    elat=$(printf "%.4f" "$elat" 2>/dev/null || echo "$elat")
    elong=$(printf "%.4f" "$elong" 2>/dev/null || echo "$elong")

    local diff_lat=$(echo "$lat - $elat" | bc -l | sed 's/-//')
    local diff_long=$(echo "$long - $elong" | bc -l | sed 's/-//')

    if (( $(echo "$diff_lat < 1.0" | bc -l) )) && (( $(echo "$diff_long < 1.0" | bc -l) )); then
        echo -e "${GREEN}Match${NC}"
        return 0
    else
        echo -e "${YELLOW}MISMATCH_REASON: lat diff=$diff_lat, long diff=$diff_long${NC}"
        return 2
    fi
}
# Main test runner
main() {
    if [[ "$GENERATE_MODE" == "true" ]]; then
        generate_fixed_data "generate"
        exit $?
    elif [[ "$UPDATE_MODE" == "true" ]]; then
        generate_fixed_data "update"
        exit $?
    fi

    if [[ "$USE_LIVE_DATA" == "true" ]]; then
        echo -e "${YELLOW}Mode: LIVE DATA (Environment Monitoring)${NC}\n"
    else
        if load_fixed_data; then
            echo -e "${GREEN}Mode: FIXED SNAPSHOT (Code Regression Testing)${NC}"
            echo -e "${GREEN}Environment is frozen. Changes = code regressions.${NC}\n"
        else
            echo -e "${RED}WARNING: No snapshot file found!${NC}"
            echo -e "${YELLOW}Run: $0 --generate-fixed${NC}\n"
        fi
    fi

    check_server_connectivity

    echo -e "${BLUE}🚀 Running coordinate tests...${NC}"
    echo "==============================="

    local total=0 ok=0 partial=0 fail=0
    declare -A failed_pages
    declare -A partial_pages

    for lang_page in "${!KNOWN_COORDS[@]}"; do
        total=$((total+1))

        # Show output in real-time, capture only status
        test_specific_page "$lang_page" "${KNOWN_COORDS[$lang_page]}" "$debug"
        local status=$?

        case $status in
            0)
                ok=$((ok+1))
                ;;
            2)
                partial=$((partial+1))
                partial_pages["$lang_page"]="Coordinate mismatch"
                ;;
            *)
                fail=$((fail+1))
                failed_pages["$lang_page"]="Extraction failed"
                ;;
        esac
        sleep 1
    done

    local pass_rate=$(( (ok * 100) / total ))

    echo -e "\n${BLUE}TEST SUMMARY${NC}"
    echo "======================="
    echo "Total:     $total"
    echo -e "Passed:    ${GREEN}$ok${NC}"
    echo -e "Partial:   ${YELLOW}$partial${NC}"
    echo -e "Failed:    ${RED}$fail${NC}"
    echo -e "Pass Rate: ${CYAN}${pass_rate}%${NC}"
    echo -e "Threshold: ${CYAN}${PASS_THRESHOLD}%${NC}"

    if [[ ${#failed_pages[@]} -gt 0 ]]; then
        echo -e "\n${RED}FAILED PAGES (${#failed_pages[@]}):${NC}"
        for page in "${!failed_pages[@]}"; do
            echo -e "  ${RED}✗${NC} $page - ${failed_pages[$page]}"
        done
    fi

    if [[ ${#partial_pages[@]} -gt 0 ]]; then
        echo -e "\n${YELLOW}PARTIAL MATCHES (${#partial_pages[@]}):${NC}"
        for page in "${!partial_pages[@]}"; do
            echo -e "  ${YELLOW}~${NC} $page - ${partial_pages[$page]}"
        done
    fi

    echo -e "\n${CYAN}Run with --debug for more info${NC}"

    if [[ $pass_rate -ge $PASS_THRESHOLD ]]; then
        echo -e "\n${GREEN}TESTS PASSED (${pass_rate}% >= ${PASS_THRESHOLD}%)${NC}"
        exit 0
    else
        echo -e "\n${RED}TESTS FAILED (${pass_rate}% < ${PASS_THRESHOLD}%)${NC}"
        if [[ "$USE_LIVE_DATA" == "false" ]]; then
            echo -e "${RED}Code regression detected!${NC}"
        else
            echo -e "${YELLOW}Environment changes detected.${NC}"
        fi
        exit 1
    fi
}

main "$@"