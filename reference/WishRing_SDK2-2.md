**4.8시간 설정**

// 해당 데이터 클래스 -> SetTimeBean

**매개 변수 인자 종류 인자 설명**

년

월 일 시간

분

초

값(0-6) 해당 일~토요일

9. **알람 메시지**

// 해당하는 데이터 클래스 -> MrdClocks

MrdReadEnum:SetClock//알람설정 MrdReadEnum: GetClock// 알람 받기

**매개 변수 인자 종류 인자 설명**

시간

분

스위치

10. **사용자 정보**

// 해당하는 데이터 클래스 -> MrdUserInfo

**매개 변수 인자 종류 인자 설명**

체중(단위:Kg)

키(단위: cm)

성별(1이면 남, 0이면 여)

연령

보행 길이 (단위: cm)

러닝 스텝 길이 (단위: cm)

11. **턴오버 헤드업 디스플레이**

**매개 변수 인자 종류 인자 설명**

턴테이블스위치

12. **단위계 정보**

//해당 데이터 클래스 -> UnitBean

**매개 변수 인자 종류 인자 설명**

미터법 0은 미터법, 미터법 1은 미터법

0은 섭씨이고 1은 화씨이다.

13. **시간 형식**

**매개 변수 인자 종류 인자 설명**

0은 24시간제, 1은 12시간제

14. **카메라 조작**

**매개 변수 인자 종류 인자 설명**

1: 포토타임 시작, 0: 포토타임 종료, 2: 포토타임 완료, 3: 포토타임 시작

true= 장치는 App false= 장치로


15. **심박수 정시 측정**

**매개 변수 종류 설명하다.**

측정스위치

측정간격

true 설정, false 가져오기

16. **화면 켜기 시간**

**매개 변수 종류 설명하다.**

true= 호출 get 함수 false=set 함수 호출

화면 켜기 시간

17. **날씨 알림**

**매개 변수 인자 종류 인자 설명**

날씨 유형(간편한 날씨는 부록 1, 복잡한 날씨는 부록 2 참조)

최고 온도 최저 온도 현재 온도

18. **장비 언어 정보**

**매개 변수 인자 설명**

00중국어 01영문 02일문 033번체 04프랑스어 05독일어 06이탈리아 07한국어 08포르투갈어 09러시아어0A스페인어0B네덜란드어0C히브리어0D아랍어OE말레이시아어0F~FF

보류

19. **푸시 메시지**

//해당 데이터 클래스 -> Appush

MrdReadEnum: ApppushName// 푸시 이름 성공 MrdReadEnum:AppushContent // 푸시 내용 성공

**매개 변수 종류 설명하다.**

내용

명칭

제 몇 조

20. **통화 제어**

MrdReadEnum. AnswerPhone // 팔찌 클릭으로 전화 받기 MrdReadEnum. 행업폰// 팔찌 클릭으로 전화 끊기

MrdReadEnurm. MutePhone // 팔찌는 전화 음소거를 클릭합니다.

21. **휴대폰 찾기 및 분실방지 알림톡**

// 해당하는 데이터 클래스 -> MrdFindLostDevice

**매개 변수 종류 설명하다.**

핸드폰 찾기 설정 (일부 팔찌는 돌아오지 않음)

기기가 주도적으로 휴대전화를 찾다

분실 방지 기능

스위치/시작종료

알람시간지연(isLostApp=true일 경우 유효)

분실방지 알림 상태 정보 조회 여부

22. **심박센서 적합 모드 상태**

MrdReadEnum. HrCorrectingOpen // 심박수 센서 어댑테이션 모드 켜기 MrdReadEnum. HrCorrectingclose // 심박수 센서 어댑티브 모드 OFF

23. **심박수 및 혈압 경보**

//해당 데이터 클래스 -> MrdHeartBloodAlert

**매개 변수 종류 설명하다.**

심박수 경보스위치

혈압경보스위치 심박수 경보치

혈압 경보치

24. **전역 방해 금지 모드 정보**

// 해당하는 데이터 클래스 -> MrdNotDisturb

**매개 변수 종류 설명하다.**

스위치

시작 시간 (00:00)

종료 시간 (00:00)

25. **운동 목표물 획득**

// 해당하는 데이터 클래스 -> GetSportTargetBean

**매개 변수 종류 설명하다.**

총 목표 단계 수

단일 목표 단계

목표 거리

26. **다중 운동 실시간 데이터**

// 해당하는 데이터 클래스 -> GetSportTargetBean

**매개 변수 종류 설명하다.**

걸음 수/ 횟수

스포츠 (0xe 조정기)

27. **여성생리건강정보**

MrdReadEnum. FEMALE\_DUE\_INFO\_CLEAN // 여성생리주기 정보 제거 성공

MrdReadEnum. FEEMALE\_DUE\_DATE // 출산 예정일 시간 설정 성공

MrdReadEnum. FEMAALE\_MENSTRUATION // 마지막 생리 시작시간 및 지속시간 설정 성공

// 해당 데이터 클래스 -> FemalePhysiologyInfoBean MrdReadEnum. FEEMALE\_DUE\_INFO//여성생리주기정보획득

**매개 변수 종류 설명하다.**

임신은 출산예정년도를 의미하고 임신하지 않은 것은 생리예정년도를 의미한다.

임신한 달은 출산 예정일을 뜻하며 임신하지 않은 달은 생리 기간을 의미한다.

임신한 경우 출산예정일 임신하지 않은 경우 생리예정일

임신 여부 월경기간

월경기간 지속일수

28. **음악적 통제**

MrdReadEnum. Start\_Or\_Pause // 미디어 재생을 시작하거나 중지합니다

MrdReadEnum. Music\_Previous/이전 곡 MrdReadEnum. Music\_Next//다음 곡

29. **설치 가능 다이얼 정보**

비고: 이 기능은 다이얼을 장착할 수 있는 장치만 지원합니다

MrdReadEnum. Chaance\_Dial\_Index // 다이얼 전환 성공 MrdReadEnum. Dial\_Index// 다이얼 번호 ID 가져오기

**매개 변수 종류 설명하다.**

현재 다이얼 번호 아이디

설치된 다이얼 번호 id

30. **물 마시기 알림**

// 해당 데이터 클래스 -> DrinkReminderBean

MrdReadEnum. Drik\_Reminder\_Info// 물 마시기 알림 설정 또는 받기

**매개 변수 인자 종류 인자 설명**

물 마시기 알람을 켤 지 여부

방해받지 않는 시간을 켭니까?

물 마시기 알림 시작 시간 (양식: 00:00) 물 마시기 알림 종료 시간 (양식: 00:00) 방해받지 않기 시작 시간 (포맷: 00:00) 방해받지 않기 종료 시간 (포맷: 00:00)

알림 시간 간격(싱글(비트: 분, 최대 4시간 지원)

31. **혈압 측정**

// 해당 데이터 클래스 -> BpCalibrationBean

MrdReadEnum. Bp\_Calibration // 혈압 측정 정보 설정 또는 획득

**매개 변수 인자 종류 인자 설명**

고압 정보 보정

저전압 정보를 조정하다.

저전압이 켜져 있는지 여부를 조정합니다.

32. **자명종 사건**

MrdReadEnum. 알람 이벤트\_Clock\_Clean// 지우기

// 해당하는 데이터 클래스 -> EventClockBean

MrdReadEnum. Event\_Clock\_Time // 알람 이벤트 세부 정보

**매개 변수 인자 종류 인자 설명**

알람 이벤트 ID

자명종 사건은 언제 발생했습니까? 자명종 사건은 어디에서 발생하였는가?

**4.33ZCare 값 푸시**

MrdReadEnum. ZCareSetting //true이면 푸시 성공, false이면 푸시 실패

**4.34PPG 데이터**

**매개 변수 인자 종류 인자 설명**

심박수

현재순번(0-255라운드순찰) 현재 시간 (long 시간 스탬프)

실제 길이가 5인 ppg 값 집합


35. **심박수 정보**

//해당 데이터 클래스 -> HeartModel

MrdReadEnum:HrLast//마지막 심박수 데이터 MrdReadEnurHrHrHistory // 심박수 이력 데이터 상세 정보 MrdReadErnum:HrNum // 심박수 기록 데이터 수

MrdReadEnum:HrTest // 심박수 측정과정

**매개 변수 종류 설명하다.**

시간 하늘 심박수

심박수 번호

심박수

데이터 업데이트 시간

36. **혈중 산소 정보**

//해당 데이터 클래스 -> BoModel MrdReadEnum: BoLast // 마지막 혈중 산소 수치

MrdReadEnurBoHistory//혈중 산소 이력 데이터 상세 정보

MrdReadEnum: BoNum // 혈중 산소 이력 데이터 수 MrdReadEnum:BoTest //혈중산소 측정과정

**매개 변수 종류 설명하다.**

시간

하늘

건수

번호를 매기다

혈중 산소

데이터 업데이트 시간

37. **혈압정보**

//해당 데이터 클래스 -> BpModel

MrdReadEnum : BpLast // 마지막 혈압 수치 MrdReadEnurBpHistory//혈압 이력 데이터 상세정보 MrdReadEnum : BpNum // 혈압이력 데이터 수

MrdReadEnum : BpTest // 혈압측정과정

**매개 변수 종류 설명하다.**

건수

번호를 매기다

고압

저압

심박수

데이터 업데이트 시간

38. **수면 정보**

//해당 데이터 클래스 -> SleepModel

MrdReadEnum : SleepHistory // 수면 이력 데이터 상세 정보

MrdReadEnum: SleepNum // 수면 이력 데이터 수 MrdReadEnum : SleepDay // 오늘의 수면 데이터

하늘

총건수

번호를 매기다

시작시간(yyy-MM-dd HH:MM)

종료시간(yyy-MM-dd HH:MM)

수면 데이터 유형 1 깊은 잠 2 얕은 잠 3 각성 4 전체

깊이 잠들다

얕은 잠

또렷하다

데이터 업데이트 시간

수면 데이터

수면 데이터


39. **체온정보**

// 해당 데이터 클래스 -> TempModel MrdReadEnum:TempLast//마지막 온도

MrdReadEnum:TempHistory //온도이력 데이터 상세정보 MrdReadEnum:TempNum //온도이력 데이터 개수 MrdReadEnum:TempTest // 온도 테스트 과정

**매개 변수 종류 설명하다.**

시간

하늘

건수

번호를 매기다

온도

40. **ECG 심박수 정보**

//해당 데이터 클래스 -> HeartModel

MrdReadEnurEcgHrLast//마지막 ECG 심박수 MrdReadEnum:EcgHrHistory//ECG 심박수 히스토리 데이터 상세정보 MrdReadEnum:EcgHrNum//ECG 심박수 히스토리 데이터 건수

MrdReadEnurEcgHrTest//ECG 심박수 측정과정

**매개 변수 종류 설명하다.**

시간 하늘

심박수

심박수 번호

심박수

데이터 업데이트 시간

MrdReadEnum: EcgHrLast//마지막 ECG 심박수

41. **걸음걸이, 단계별 걸음걸이, 운동 정보**

// 해당하는 데이터 클래스 -> SportBean

MrdReadEnum:Step\_history // 보보 기록 데이터 상세 정보 MrdReadEnum:Step\_history\_num // 보보 기록 데이터 수 MrdReadEnum:Step\_realTime // 현재 단계 데이터

MrdReadEnum:StepoSection\_history //단계별 보폭 기록 데이터 상세 정보 MrdReadEnum:StepSeection\_history\_num//단계별 단계 기록 데이터 건수

MrdReadEnum:Sport\_history //운동 이력 데이터 상세 정보 MrdReadEnum:Spoort\_history\_num //운동 이력 데이터 수 MrdReadEnum:Sport\_realTime // 현재 모션 데이터


**매개 변수 종류 설명하다.**

시간

타임스탬프

총 역사 건수

히스토리 패킷 번호

단계

마일(미터)

칼로리

운동시간(분)

0은 현재 1을 의미하며 분단계보 2는 운동을 의미한다.

0x0 걷기(달리기, 걷기) 0x1 자전거 타기; 0x2 수영; 0x3 줄넘기 0x4 팔굽혀펴기 0x5

등산 0x6 배드민턴 0x7 아이스하키 0x8 야구 0x9 복싱 0xa 경보, 0xb 체조, 0xc 축구, 0xd 농구, 0xe 조정기

**4.42Ecg 실시간 데이터**

**매개 변수 종류 설명하다.**

사용자 id, 0은 방문객, 1은 아빠, 2는 엄마

패킷 id는 0-15 사이에서 자가 증가합니다. ecg 데이터 집합

43. **센서 측정 데이터**

// 테스트 중지 (이 열거만 ss\_type, stopEnum, measureMode, error) // 심박수 측정 시작

// 심박수 1회 검사 // 심전도 측정 중지 // 심전도 측정 시작

// 혈압 측정 시작 //혈중산소 측정 시작

// 온도 테스트 시작 //혈당측정 시작

**매개 변수 종류 설명하다.**

true= 장치는 App false= 장치로

비트별 해석은 1로 설정하면 유효하고, BIT[6]=혈당, BIT[5]=심전, BIT[4]=체온, BIT[3]=미세순환, BIT[2]=혈중산소, BIT[1]=혈압, BIT[0]=심박수

측정방법, 측정오류 유효여부

측정 방법;TestModeContract. MrdMeasureMode. mode

측정 오류;TestModeContract. MrdTestErrorEnum. errorCode

**매개 변수 종류 설명하다.**

심박수 혈압

혈중 산소

미세순환

체온

심전도

혈당

압력

**매개 변수 종류 설명하다.**

수동측량 정시 측정 운동측정

**매개 변수 종류 설명하다.**

정상

장비 미착용

무효 심박수

/ ECG test stopped / ECG single test

/ blood pressure test

44. **앉기 알림 설정**

//해당 데이터 클래스 -> MrdSedentary

**매개 변수 종류 설명하다.**

방해받지 않는 스위치

방해받지 않기 시작 시간 (포맷: 00:00) 방해받지 않기 종료 시간 (포맷: 00:00)

구좌알림스위치

오래된 알림 시작 시간 (양식: 00:00)

오래 앉아있기 알림 종료 시간 (양식: 00:00) 오래 앉아 있기 알림 간격 (분)

45. **오래 앉아있기 알림 받기**

// 해당 데이터 클래스 -> SedentaryInfoBean

**매개 변수 종류 설명하다.**

오래 앉아있기 알림이 켜져 있는지 여부

오래 앉아 있기 알림 시작 시간 (시간) 오래 앉아 있기 알림 시작 시간 (분) 오래 앉아있기 알림 종료 시간( 시간) 오래 앉아 있기 알림 종료 시간 (분)

방해받지 않기 알림 시작 시간 (시간) 방해되지 않음 알림 시작 시간 (분) 방해받지 않기 알림 종료 시간 (시간) 방해받지 않기 알림 종료 시간 (분)

알람 간격 (분)

스텝 역치

46. **기기 UI 버전**

// 해당 데이터 클래스 -> UiversionBean

**매개 변수 종류 설명하다.**

UI 버전

이 장치가 UI 업그레이드를 지원하는지 여부

**4.47설비방송**

// 해당하는 데이터 클래스 -> AdvertisementBean

**매개 변수 종류 설명하다.**

true=문의, false=수정

방송 간격, 밀리초 단위 (50ms-5000ms)

출력 레벨 (0-6)

**파워**

48. **알고리즘 매개변수**

// 해당 데이터 클래스 -> AlgorithmParametersBean

**매개 변수 종류 설명하다.**

true=문의, false=수정

자유 낙하 지속 시간은 밀리초입니다. 이 uint16 변수의 기본값은 120(즉, 120밀리초)이어야 합니다.

가속도계 진폭은 이동평균을 벗어나야 활동을 확인할 수 있다. 이 uint16 변수의 기본값은 100이어야 합니다 (예: 0.1 Gs)

가속도계 진폭이 이 값을 초과해야 넘어짐을 식별할 수 있습니다. 이 uint16 변수의 기본값은 2900(즉, 2.9Gs)이어야 합니다.

시간 초과

최소값

49. **알고리즘 매개변수**

// 해당 데이터 클래스 -> AlgorithmParametersBean2

**매개 변수 종류 설명하다.**

50. **알고리즘 매개변수**

// 해당 데이터 클래스 -> AlgorithmParametersBean3


**매개 변수 종류 설명하다.**

51. **온도 정시 측정**

**매개 변수 종류 설명하다.**

측정스위치

측정간격

52. **장비의 GPS 운동 데이터**

// 해당 데이터 클래스 -> MrdGpsSportBean LongitudeType 경도 종류: 동경 0, 서경 1

위도 종류: 북위 0, 남위 1

**매개 변수 종류 설명하다.**

현재 시간대의 모든 GPS 정보에 기록된 데이터 건수

이번에 전송된 패킷 번호

수평 성분 정밀도

경도 위도

시간(MM-ddHH:mm:ss)

경도유형:동경,서경 위도 종류: 남위, 북위

**4.53GPS 운동정보 보고**

// 해당 데이터 클래스 -> MrdSportGpsReportBean

LongitudeType 경도 종류: 동경 0, 서경 1 LatitudeType 위도 종류: 북위 0, 남위 1

**매개 변수 종류 설명하다.**

해당 장치의 단계 수

수평 성분 정밀도

경도

위도

시간(MM-ddHH:mm:ss) 경도유형:동경,서경

위도 종류: 남위, 북위

54. **획득 파라미터 VITTALS\_OVERRIDE** //해당 데이터 클래스 -> VitalsoverrrideParam

**매개 변수 종류 설명하다.**

0:방송활력징후;1:방송알고리즘 디버깅


55. **스트레스 정보**

//해당 데이터 클래스 -> HrvHistoryBean

**매개 변수 종류 설명하다.**

데이터 갯수 (최근의 데이터를 가져올 때, 이 데이터는 의미가 없습니다) 데이터 번호 (최근의 데이터를 가져올 때, 이 데이터는 의미가 없습니다)

년

월 일 때

나누다.

초

압력치

56. **수면정보 보고**

// 해당하는 데이터 클래스 -> SleepReportBean

**매개 변수 종류 설명하다.**

수면상태 (0활동1 예비상태 본2 얕은잠상태3 깊은상태4 방치상태)

총심수면시간

총 수면 시간

**5.부록(Appendix)**

**5.1 날씨 유형**

**기본 날씨 유형 번호(Hex) 설명하다.**

맑다

음 흐리다

비 가랑비, 보통비, 큰비, 호우, 호우, 소나기, 뇌우 눈 진눈깨비, 소나기, 소설, 중설, 대설, 폭설

스모그 스모그, 미세먼지, 스모그, 스모그

모래 먼지 황사, 모래바람, 달리기, 토네이도
