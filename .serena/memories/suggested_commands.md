# 권장 개발 명령어

## 빌드 및 실행
```bash
# 프로젝트 빌드
./gradlew build

# 디버그 APK 빌드
./gradlew assembleDebug  

# APK 설치
./gradlew installDebug

# 앱 실행 (디바이스 연결 필요)
adb shell am start -n com.wishring.app/.presentation.MainActivity
```

## 코드 품질 관리
```bash
# Lint 검사
./gradlew lint

# 단위 테스트 실행
./gradlew test

# 통합 테스트 실행  
./gradlew connectedAndroidTest

# 의존성 업데이트 확인
./gradlew dependencyUpdates
```

## 데이터베이스
```bash
# Room 스키마 검증
./gradlew :app:kspDebugKotlin

# 데이터베이스 파일 확인 (디바이스)
adb shell run-as com.wishring.app ls /data/data/com.wishring.app/databases/
```

## 유틸리티 명령어 (macOS)
```bash
# 연결된 안드로이드 디바이스 확인
adb devices

# 로그 확인
adb logcat | grep "WishRing"

# 앱 제거
adb uninstall com.wishring.app

# 파일 검색
find . -name "*.kt" | grep -i "ble"

# 디렉토리 구조 확인  
tree -I 'build|.gradle|.idea'
```

## Git 관리
```bash
# 상태 확인
git status

# 커밋
git add . && git commit -m "feat: MRD SDK integration"

# 브랜치 관리
git checkout -b feature/mrd-sdk-integration
```