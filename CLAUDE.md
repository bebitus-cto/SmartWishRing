# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 📚 Essential Documentation

**IMPORTANT**: When working with this codebase, refer to these documents in order:

1. **[`docs/Index.md`](docs/Index.md)** - Complete file index with descriptions for each file
2. **[`docs/Architecture.md`](docs/Architecture.md)** - Detailed architecture documentation including patterns, data flow, and design decisions

## Project Overview

**WISH RING** - Android app for a smart ring device that tracks and motivates users to achieve their wishes through button press counting.

## Quick Reference

### Clean Architecture + MVVM Pattern
- **Presentation Layer**: Jetpack Compose UI + ViewModels (MVI pattern for state management)
- **Domain Layer**: Use cases, repository interfaces, business models
- **Data Layer**: Repository implementations, local database (Room), DataStore preferences, BLE communication

### Core Technologies
- **UI**: Jetpack Compose with Material3
- **DI**: Hilt/Dagger
- **Database**: Room with Flow support
- **Async**: Coroutines + StateFlow/SharedFlow
- **BLE**: Nordic BLE library + MRD SDK (AAR in libs/)
- **Navigation**: Navigation Compose

## Development Guidelines

### File Navigation
Always check `docs/Index.md` first to quickly locate specific files and understand their purposes.

### Architecture Understanding
Refer to `docs/Architecture.md` for:
- Data flow patterns (UDF/MVI)
- Layer interactions and dependencies
- State management strategies
- BLE communication architecture
- Error handling patterns

### Key Patterns

#### State Management (MVI)
```kotlin
ViewState → immutable UI state
Event → user actions
Effect → one-time side effects
```

#### Repository Pattern
All data operations go through repository interfaces in the domain layer, with implementations in the data layer.

#### BLE Communication
- Interface: `domain/repository/BleRepository.kt`
- Implementation: `ble/BleRepositoryImpl.kt`
- Protocol Adapter: `ble/MrdProtocolAdapter.kt`

### Testing Strategy
- Unit tests for ViewModels and Use Cases
- Integration tests for Repositories
- Instrumented tests for Database and BLE

## 🔴 CRITICAL: Test Synchronization Rules

**⚠️ MANDATORY: All code changes MUST include corresponding test updates**

### Absolute Requirements (MUST)

1. **Test-Code Synchronization**
   ```
   When modifying: HomeViewModel.kt
   MUST also update: HomeViewModelTest.kt
   ```
   - Every production code change MUST have matching test changes
   - New features MUST be committed WITH their tests
   - Deleted code MUST have corresponding test removals

2. **Test-First for Bug Fixes**
   ```kotlin
   // Step 1: Write failing test that reproduces the bug
   @Test
   fun `should handle edge case properly`() {
       // This test MUST fail before fix
   }
   
   // Step 2: Fix the actual code
   // Step 3: Verify test now passes
   ```

3. **Coverage Requirements**
   - New code MUST maintain or increase coverage (minimum 85%)
   - Modified code MUST NOT decrease existing coverage
   - Critical paths (BLE, Data persistence) MUST have 95%+ coverage

### File Mapping Rules

| If you modify... | You MUST update... | Test type required |
|------------------|-------------------|-------------------|
| `*ViewModel.kt` | `*ViewModelTest.kt` | Unit + Integration |
| `*Repository.kt` | `*RepositoryTest.kt` | Unit + Integration |
| `*Screen.kt` | `*ScreenTest.kt` | Compose UI Test |
| `*UseCase.kt` | `*UseCaseTest.kt` | Unit Test |
| Domain models | Property-based tests | Property + Unit |
| BLE components | Mock + Integration | Mock framework |

### Automated Enforcement

```bash
# Pre-commit hook (MUST be installed)
#!/bin/bash
# .git/hooks/pre-commit

# Check if source files changed
CHANGED_SRC=$(git diff --cached --name-only | grep -E '\.kt$' | grep -v Test)

# For each changed source file, verify test exists
for file in $CHANGED_SRC; do
    TEST_FILE="${file//.kt/Test.kt}"
    if ! git diff --cached --name-only | grep -q "$TEST_FILE"; then
        echo "❌ ERROR: Changed $file but no corresponding test update"
        exit 1
    fi
done

# Run tests before commit
./gradlew test || exit 1
```

### Commit Message Convention

```
feat: Add wish count animation

- Added animation to WishCountCard component
- Updated WishCountCardTest with animation verification
- Added UI test for animation timing

Test: WishCountCardTest, WishCountAnimationTest
Coverage: 92% (+2%)
```

### Test Update Examples

#### Example 1: Adding a new method
```kotlin
// BEFORE: HomeViewModel.kt
class HomeViewModel {
   fun incrementCount() { /* ... */ }
}

// AFTER: HomeViewModel.kt
class HomeViewModel {
   fun incrementCount() { /* ... */ }

   fun resetCount() {  // NEW METHOD
      _uiState.update { it.copy(count = 0) }
   }
}

// MUST ADD: HomeViewModelTest.kt
@Test
fun `resetCount should set count to zero`() {
   // Given
   viewModel.incrementCount()
   assertEquals(1, viewModel.uiState.value.count)

   // When
   viewModel.resetCount()

   // Then
   assertEquals(0, viewModel.uiState.value.count)
}
```

#### Example 2: Modifying existing logic
```kotlin
// If you change the increment logic
fun incrementCount() {
   // OLD: count++
   // NEW: count += 2
}

// MUST UPDATE the test expectation
@Test
fun `incrementCount should increase by 2`() {  // Updated test name
   viewModel.incrementCount()
   assertEquals(2, viewModel.uiState.value.count)  // Updated assertion
}
```

### CI/CD Integration

```yaml
# .github/workflows/test-sync.yml
name: Test Synchronization Check

on: [pull_request]

jobs:
   check-test-sync:
      runs-on: ubuntu-latest
      steps:
         - name: Check test coverage
           run: |
              ./gradlew jacocoTestReport
              if [ $(cat build/reports/jacoco/coverage.txt) -lt 85 ]; then
                echo "❌ Coverage below 85%"
                exit 1
              fi

         - name: Verify test updates
           run: |
              # Check that modified source files have corresponding test updates
              ./scripts/check-test-sync.sh
```

### Exceptions (Require Explicit Justification)

Only the following are exempt from test requirements:
1. Pure UI layout changes (must be noted in PR)
2. Configuration files (unless they affect behavior)
3. Generated code (must be marked with @Generated)

**⚠️ WARNING**: Commits without proper test synchronization will be:
1. Automatically rejected by pre-commit hooks
2. Blocked in PR reviews
3. Reverted if accidentally merged

## Important Notes

- The app uses Android SDK 26-34 (minimum SDK 26 for BLE stability)
- MRD SDK is included as AAR file in `app/libs/` directory
- All business logic is in the Domain layer (framework-independent)
- UI state is managed through immutable data classes and StateFlow