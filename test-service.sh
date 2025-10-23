#!/bin/bash

# Test script for PDFBox Analysis Service

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080"

echo -e "${YELLOW}PDFBox Analysis Service - Test Script${NC}"
echo "========================================"
echo ""

# Test 1: Health Check
echo -e "${YELLOW}Test 1: Health Check${NC}"
response=$(curl -s -w "\n%{http_code}" ${BASE_URL}/api/pdf/health)
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" -eq 200 ]; then
    echo -e "${GREEN}✓ Health check passed${NC}"
    echo "Response: $body"
else
    echo -e "${RED}✗ Health check failed (HTTP $http_code)${NC}"
    exit 1
fi

echo ""

# Test 2: PDF Analysis
if [ -z "$1" ]; then
    echo -e "${YELLOW}Test 2: PDF Analysis${NC}"
    echo -e "${RED}No PDF file provided. Usage: ./test-service.sh <path-to-pdf>${NC}"
    echo "Skipping PDF analysis test"
else
    PDF_FILE="$1"

    if [ ! -f "$PDF_FILE" ]; then
        echo -e "${RED}Error: File '$PDF_FILE' not found${NC}"
        exit 1
    fi

    echo -e "${YELLOW}Test 2: Analyzing PDF: $PDF_FILE${NC}"

    response=$(curl -s -w "\n%{http_code}" -X POST \
        -F "file=@${PDF_FILE}" \
        ${BASE_URL}/api/pdf/analyze)

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✓ PDF analysis successful${NC}"
        echo ""
        echo "Analysis Results:"
        echo "$body" | python3 -m json.tool 2>/dev/null || echo "$body"
    else
        echo -e "${RED}✗ PDF analysis failed (HTTP $http_code)${NC}"
        echo "Response: $body"
        exit 1
    fi
fi

echo ""
echo -e "${GREEN}All tests completed!${NC}"
