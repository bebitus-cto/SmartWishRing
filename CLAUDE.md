# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 📚 Essential Documentation

**IMPORTANT**: When working with this codebase, refer to these documents in order:

1. **[`docs/Index.md`](docs/Index.md)** - Complete file index with descriptions for each file
2. **[`docs/Architecture.md`](docs/Architecture.md)** - Detailed architecture documentation including patterns, data flow, and design decisions

## Project Overview

**WISH RING** - Android app for a smart ring device that tracks and motivates users to achieve their wishes through button press counting.

## Architecture & Technology Stack

### Clean Architecture + MVVM Pattern
- **Presentation Layer**: Jetpack Compose UI + ViewModels with MVI pattern
  - Features: `home`, `settings`, `wishinput`, `wishdetail`, `splash`
  - Shared: `navigation`, `component`, `effect`, `event`, `viewmodel`
- **Domain Layer**: Repository interfaces, business models, use cases
- **Data Layer**: Repository implementations, Room database, DataStore preferences
- **BLE Layer**: MRD SDK integration, connection management, device validation
- **Core Layer**: Base classes, utilities, constants

### Core Technologies
- **UI**: Jetpack Compose with Material3 design system
- **DI**: Hilt/Dagger with modular structure (AppModule, BleModule, RepositoryModule)
- **Database**: Room with Flow-based reactive data access
- **Async**: Coroutines + StateFlow/SharedFlow for reactive programming
- **BLE**: Nordic BLE library + MRD SDK (AAR: `app/libs/sdk_mrd20240218_1.1.5.aar`)
- **Navigation**: Navigation Compose with type-safe routing
- **State Management**: MVI pattern with immutable ViewState, Event, Effect

## Project Structure

### Package Organization: `com.wishring.app`

```
app/src/main/java/com/wishring/app/
├── WishRingApplication.kt          # Hilt entry point, MRD SDK initialization
├── MainActivity.kt                 # Compose activity, navigation host
├── ui/theme/                       # Material3 theme system
├── di/                            # Hilt modules (App, Ble, Repository)
├── core/                          # Base classes, utilities, constants
├── ble/                           # 🔥 Main BLE implementation
├── data/                          # Data layer + BLE models
├── domain/                        # Domain interfaces & models
└── presentation/                  # All UI features & components
```

### BLE Architecture (Critical)

**🔥 IMPORTANT**: BLE implementation has two locations:

1. **Active Implementation**: `/ble/` (Used by DI system)
   - `BleRepositoryImpl.kt` - Main BLE repository
   - `MrdProtocolAdapter.kt` - **ACTIVE** MRD SDK adapter  
   - `BleConnectionManager.kt` - Connection handling
   - `BleAutoConnectService.kt` - Background auto-connect
   - `WishRingDeviceValidator.kt` - Device validation
   - `BleConstants.kt` - BLE UUIDs

2. **Data Models**: `/data/ble/model/` 
   - `BleConnectionState.kt` - Connection state enum
   - `BleConstants.kt` - Additional constants
   - ⚠️ `/data/ble/MrdProtocolAdapter.kt` - **UNUSED DUPLICATE**

## Development Guidelines

### File Navigation
- **Start with**: `docs/Index.md` for file locations and purposes
- **Architecture**: `docs/Architecture.md` for patterns and data flow
- **BLE Work**: Focus on `/ble/` directory (main implementation)
- **UI Features**: `/presentation/{feature}/` for complete feature modules

### Key Patterns

#### MVI State Management
```kotlin
// Every feature follows this pattern:
{Feature}ViewState    → Immutable UI state
{Feature}Event        → User actions/intentions  
{Feature}Effect       → One-time side effects
{Feature}ViewModel    → State management logic
{Feature}Screen       → Compose UI entry point
```

#### Repository Pattern
- **Interfaces**: `domain/repository/{Name}Repository.kt`
- **Implementations**: `data/repository/{Name}RepositoryImpl.kt`
- **DI Binding**: `di/RepositoryModule.kt`

#### Feature Organization
Each presentation feature has consistent structure:
```
presentation/{feature}/
├── {Feature}Screen.kt          # Main Compose screen
├── {Feature}ViewModel.kt       # Business logic
├── {Feature}ViewState.kt       # UI state model
├── {Feature}Event.kt           # User events
├── {Feature}Effect.kt          # Side effects
└── component/                  # Feature-specific components
```

## 🔴 CRITICAL: Test Synchronization Rules

**⚠️ MANDATORY: All code changes MUST include corresponding test updates**

### Absolute Requirements

1. **Test-Code Synchronization**: Every production code change requires matching test updates
2. **Coverage Maintenance**: Minimum 85% coverage, critical paths (BLE, persistence) need 95%+
3. **Test-First Bug Fixes**: Write failing test first, then fix code

### Test Structure (Comprehensive)

```
app/src/test/java/com/wishring/app/
├── core/                   # Core utility tests
├── ble/                    # BLE implementation tests  
├── data/repository/        # Repository implementation tests
├── domain/                 # Domain model & use case tests
├── presentation/viewmodel/ # ViewModel unit tests
├── property/               # Property-based testing
├── integration/            # End-to-end integration tests
├── concurrency/            # Threading & race condition tests
└── performance/            # Performance & large dataset tests
```

### File Mapping Rules

| Production File | Required Test | Test Type |
|----------------|---------------|-----------|
| `*ViewModel.kt` | `*ViewModelTest.kt` | Unit + Integration |
| `*RepositoryImpl.kt` | `*RepositoryImplTest.kt` | Unit + Integration |
| `*Screen.kt` | `*ScreenTest.kt` | Compose UI Test |
| BLE components | Mock + Integration | BLE simulation |
| Domain models | Property-based | Property testing |

### Pre-commit Enforcement

```bash
# .git/hooks/pre-commit (MUST be installed)
./scripts/check-test-sync.sh || exit 1
./gradlew test || exit 1
```

### Commit Message Convention

```
feat(home): Add wish count animation

- Added animation to WishCountCard component  
- Updated WishCountCardTest with animation verification
- Added UI test for animation timing and performance

Test: WishCountCardTest, WishCountAnimationTest
Coverage: 92% (+2%)
```

## Development Workflow

### 1. Understanding Existing Code
```bash
# Check file purpose and location
cat docs/Index.md | grep -i "filename"

# Understand feature architecture  
ls app/src/main/java/com/wishring/app/presentation/{feature}/

# Check existing tests
ls app/src/test/java/com/wishring/app/**/*Test.kt
```

### 2. Making Changes

**For BLE Work:**
```kotlin
// Work in main BLE implementation
app/src/main/java/com/wishring/app/ble/

// Test with:
app/src/test/java/com/wishring/app/ble/
```

**For UI Features:**
```kotlin
// Complete feature in presentation layer
app/src/main/java/com/wishring/app/presentation/{feature}/

// Test with:
app/src/test/java/com/wishring/app/presentation/viewmodel/{Feature}ViewModelTest.kt
```

### 3. Testing Requirements

**Before any commit:**
```bash
# Run all tests  
./gradlew test

# Check coverage
./gradlew jacocoTestReport

# Ensure BLE integration works
./gradlew connectedAndroidTest
```

## Important Notes

- **MRD SDK**: Located at `app/libs/sdk_mrd20240218_1.1.5.aar`
- **Android SDK**: 26-34 (minimum 26 for BLE stability)
- **Architecture**: Framework-independent business logic in Domain layer
- **State Management**: Immutable ViewState with StateFlow
- **BLE Focus**: Main implementation in `/ble/`, avoid `/data/ble/MrdProtocolAdapter.kt` (duplicate)
- **Testing**: Comprehensive test coverage including property-based and concurrency testing

## Quick Reference Commands

```bash
# Build and test
./gradlew clean build test

# Run specific test suite
./gradlew test --tests "*BleRepositoryImplTest*"

# Check lint and compile
./gradlew lintDebug compileDebugKotlin

# Install debug APK  
./gradlew installDebug
```

---

**Last Updated**: 2025-01-13  
**For Questions**: Check `docs/Index.md` for file locations, `docs/Architecture.md` for patterns