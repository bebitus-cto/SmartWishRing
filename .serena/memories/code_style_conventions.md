# 코딩 스타일 및 규칙

## Kotlin 스타일
- **네이밍**: camelCase (함수/변수), PascalCase (클래스), UPPER_SNAKE_CASE (상수)
- **파일명**: PascalCase (클래스명과 동일)
- **패키지명**: 소문자, 점으로 구분

## 아키텍처 패턴
- **Clean Architecture**: data - domain - presentation 계층 분리
- **MVVM**: ViewModel + StateFlow/Flow 사용
- **UDA**: Unidirectional Data Architecture (Event -> State -> Effect)

## 의존성 주입 (Hilt)
- `@Singleton` 어노테이션으로 싱글톤 관리
- Repository는 인터페이스와 구현체 분리
- `@ApplicationContext` 사용하여 Context 주입

## Flow/Coroutines 사용법
- UI 상태: `StateFlow` 사용
- 이벤트: `SharedFlow` 사용  
- suspend 함수에서 IO 작업 처리
- `@IoDispatcher` 등 적절한 Dispatcher 사용

## 파일 구조 규칙
```
presentation/
├── viewmodel/       # ViewModel 클래스들
├── ui/             # Composable UI 함수들  
├── state/          # UI State 데이터 클래스
├── event/          # UI Event 데이터 클래스
└── effect/         # Side Effect 데이터 클래스
```

## BLE 통신 규칙
- 모든 BLE 작업은 Repository 패턴으로 추상화
- Suspend 함수 사용하여 비동기 처리
- Flow 사용하여 상태 변화 관찰
- `@SuppressLint("MissingPermission")` 필요시에만 사용