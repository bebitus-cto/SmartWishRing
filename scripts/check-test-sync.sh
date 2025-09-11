#!/bin/bash

# Test Synchronization Checker
# This script verifies that all changed source files have corresponding test updates

set -e

echo "🔍 Checking test synchronization..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get list of changed Kotlin source files (excluding tests)
CHANGED_SRC=$(git diff --cached --name-only --diff-filter=AM | grep -E '\.kt$' | grep -v Test || true)

if [ -z "$CHANGED_SRC" ]; then
    echo -e "${GREEN}✅ No source files changed${NC}"
    exit 0
fi

# Track missing tests
MISSING_TESTS=""
FOUND_TESTS=""

echo "📋 Changed source files:"
for file in $CHANGED_SRC; do
    echo "  - $file"
    
    # Determine expected test file name
    if [[ $file == *"ViewModel.kt" ]]; then
        TEST_FILE="${file//ViewModel.kt/ViewModelTest.kt}"
    elif [[ $file == *"Repository.kt" ]]; then
        TEST_FILE="${file//Repository.kt/RepositoryTest.kt}"
    elif [[ $file == *"UseCase.kt" ]]; then
        TEST_FILE="${file//UseCase.kt/UseCaseTest.kt}"
    elif [[ $file == *"Screen.kt" ]]; then
        TEST_FILE="${file//Screen.kt/ScreenTest.kt}"
    else
        # Generic test file naming
        TEST_FILE="${file//.kt/Test.kt}"
    fi
    
    # Check if test file is also modified
    if git diff --cached --name-only | grep -q "$TEST_FILE"; then
        FOUND_TESTS="$FOUND_TESTS\n  ✅ $file → $TEST_FILE"
    else
        MISSING_TESTS="$MISSING_TESTS\n  ❌ $file → $TEST_FILE (MISSING)"
    fi
done

echo ""
echo "📊 Test synchronization report:"

if [ -n "$FOUND_TESTS" ]; then
    echo -e "${GREEN}Found test updates:${NC}"
    echo -e "$FOUND_TESTS"
fi

if [ -n "$MISSING_TESTS" ]; then
    echo -e "${RED}Missing test updates:${NC}"
    echo -e "$MISSING_TESTS"
    echo ""
    echo -e "${RED}❌ ERROR: Some source files don't have corresponding test updates!${NC}"
    echo -e "${YELLOW}Please update the test files before committing.${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✅ All source changes have corresponding test updates!${NC}"

# Optional: Run tests to ensure they pass
echo ""
echo "🧪 Running tests..."
if ./gradlew test --quiet; then
    echo -e "${GREEN}✅ All tests passed!${NC}"
else
    echo -e "${RED}❌ Some tests failed! Please fix them before committing.${NC}"
    exit 1
fi

exit 0