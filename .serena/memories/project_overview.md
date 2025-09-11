# WishRing 프로젝트 개요

## 프로젝트 목적
- 스마트 소망반지(WISH RING) Android 앱
- BLE 통신을 통한 하드웨어 제어
- 소망 카운트 추적 및 목표 달성 관리

## 기술 스택
- **언어**: Kotlin
- **UI**: Jetpack Compose
- **아키텍처**: MVVM + Clean Architecture
- **DI**: Hilt
- **데이터베이스**: Room
- **비동기**: Coroutines + Flow  
- **BLE**: Nordic BLE Library
- **빌드**: Android Gradle Plugin 8.2.2

## 주요 패키지 구조
```
com.wishring.app/
├── di/              # Hilt 의존성 주입 모듈
├── data/            # 데이터 계층 (Repository, Database, BLE)
├── domain/          # 도메인 계층 (Repository Interface, Model)  
├── presentation/    # 프레젠테이션 계층 (ViewModel, UI)
├── ble/             # BLE 통신 구현체
└── core/            # 공통 유틸리티
```

## 현재 상태
- 기본 앱 아키텍처 완성
- UI 화면들 구현 완료 (Home, WishInput, Detail, Settings)
- BLE Repository 인터페이스 정의됨
- Nordic BLE 기반 템플릿 구현 존재
- **MRD SDK 통합 필요** (현재 작업)