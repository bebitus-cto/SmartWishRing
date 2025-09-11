#!/bin/bash

# Install Git Hooks for WISH RING Project
# This script sets up pre-commit hooks for test synchronization

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
HOOKS_DIR="$PROJECT_ROOT/.git/hooks"

echo "🔧 Installing Git hooks for WISH RING project..."

# Create pre-commit hook
cat > "$HOOKS_DIR/pre-commit" << 'EOF'
#!/bin/bash

# WISH RING Pre-commit Hook
# Ensures test synchronization and code quality

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🚀 Running pre-commit checks...${NC}"

# 1. Check test synchronization
echo -e "${YELLOW}1️⃣ Checking test synchronization...${NC}"
if ./scripts/check-test-sync.sh; then
    echo -e "${GREEN}✅ Test synchronization check passed${NC}"
else
    echo -e "${RED}❌ Test synchronization check failed${NC}"
    echo "Please ensure all code changes have corresponding test updates."
    exit 1
fi

# 2. Check code formatting (ktlint)
echo -e "${YELLOW}2️⃣ Checking code formatting...${NC}"
if ./gradlew ktlintCheck --quiet; then
    echo -e "${GREEN}✅ Code formatting check passed${NC}"
else
    echo -e "${RED}❌ Code formatting issues found${NC}"
    echo "Run './gradlew ktlintFormat' to fix formatting issues"
    exit 1
fi

# 3. Check for TODO comments in staged files
echo -e "${YELLOW}3️⃣ Checking for TODO comments...${NC}"
TODOS=$(git diff --cached --name-only --diff-filter=AM | xargs grep -l "TODO" 2>/dev/null || true)
if [ -n "$TODOS" ]; then
    echo -e "${YELLOW}⚠️ Warning: TODO comments found in:${NC}"
    echo "$TODOS"
    echo "Consider addressing TODOs or creating issues for them."
fi

# 4. Check commit message format (if available)
if [ -f "$1" ]; then
    echo -e "${YELLOW}4️⃣ Checking commit message format...${NC}"
    COMMIT_MSG=$(cat "$1")
    if [[ ! "$COMMIT_MSG" =~ ^(feat|fix|docs|style|refactor|test|chore|perf):.+ ]]; then
        echo -e "${YELLOW}⚠️ Warning: Commit message doesn't follow conventional format${NC}"
        echo "Consider using: feat|fix|docs|style|refactor|test|chore|perf: <message>"
    fi
fi

# 5. Check for large files
echo -e "${YELLOW}5️⃣ Checking for large files...${NC}"
LARGE_FILES=$(git diff --cached --name-only --diff-filter=AM | xargs -I {} find {} -size +1M 2>/dev/null || true)
if [ -n "$LARGE_FILES" ]; then
    echo -e "${YELLOW}⚠️ Warning: Large files detected (>1MB):${NC}"
    echo "$LARGE_FILES"
    echo "Consider using Git LFS for large files."
fi

echo -e "${GREEN}✅ All pre-commit checks completed!${NC}"
EOF

# Make hooks executable
chmod +x "$HOOKS_DIR/pre-commit"
chmod +x "$SCRIPT_DIR/check-test-sync.sh"

echo "✅ Git hooks installed successfully!"
echo ""
echo "📋 Installed hooks:"
echo "  - pre-commit: Enforces test synchronization"
echo ""
echo "🔧 To bypass hooks (emergency only):"
echo "  git commit --no-verify"
echo ""
echo "⚠️  Remember: Test synchronization is MANDATORY!"