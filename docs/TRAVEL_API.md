# 여행 REST API 연동 (#101)

여행 화면은 로그인 사용자의 서버 데이터를 읽고, 서버 저장 성공 뒤 로컬 캐시와 화면을 갱신한다. #119의 암호화 세션 저장소와 `GayadiApiClient`를 공유한다. PR #119는 main에 병합되었으며 이 변경은 그 위에 이어진다.

## 연결 범위

- 여행 목록/생성/수정/삭제 및 시작·종료 상태
- 초대 코드로 참여, 참여자 조회·제외, 가능 날짜 제출·조회·확정
- 일정 생성/수정/삭제, 방문 여부, 순서 변경
- 개인 지출/공동 경비 지출 CRUD, 공동 경비 충전·잔액, 서버 정산
- 서버 장소 ID 기반 장소 조회 및 즐겨찾기 조회/저장/삭제

공유 화면에 진입하면 다시 조회하며, 날짜 조율 화면은 열려 있는 동안 15초 간격으로 갱신한다. 여행·즐겨찾기는 페이지를 모두 조회한다. 사용자 변경 시 이전 계정의 캐시 메타데이터를 재사용하지 않는다. 오류는 대화상자로 표시하고, 날짜 확정이 실패하면 해당 화면에 머문다. 중복 일정 저장·공금 충전·즐겨찾기·날짜 제출 요청을 막는다.

## 검증

```sh
./gradlew :domain:test testDebugUnitTest :app:testDevDebugUnitTest lintDebug :app:lintDevDebug :app:assembleDevDebug :app:assembleDevDebugAndroidTest
adb shell am instrument -w -e liveApi true \
  -e class com.gayadi.android.api.DevTravelIntegrationTest \
  com.doonow.gayadi.test/androidx.test.runner.AndroidJUnitRunner
```

- 단위 테스트 191개, 실패 0개. Android Lint 및 dev APK 빌드 통과.
- `DevTravelIntegrationTest`는 명시적 `liveApi=true`와 지정된 dev 주소에서만 실행한다. 기존 로그인 세션이 없는 테스트 에뮬레이터가 필요하다.
- 임시 계정 두 개로 여행 생성/수정, 초대 참여, 공통 가능 날짜와 확정, 일정 CRUD·순서·방문 상태, 지출 수정과 정산, 공동 경비, 즐겨찾기 저장/삭제를 실제 서버에서 검증한다.
- 개인 지출 40,000원 + 공동 교통비 10,000원 = 총 50,000원, 공동 경비 100,000원 충전 후 잔액 90,000원을 검증했다.
- API 36 에뮬레이터의 일반 화면(1080×2400, 420dpi)과 작은 화면(720×1280, 360dpi)을 확인했다. 작은 화면은 목록을 스크롤해 하단 콘텐츠까지 확인한다.
- 앱에 테스트 세션을 주입한 뒤 실제 서버 데이터의 여행 목록·일정·참여자·소비 화면을 확인하고 캡처한다. Google OAuth 자체를 검증한 테스트는 아니다.
- 최종 테스트는 생성한 여행을 먼저 삭제하고 계정을 정리한다. 초기 정리 순서 오류가 발생한 점검에서는 dev 테스트 여행 ID 1·2와 소유 계정이 남았다. 해당 실행의 임시 인증정보를 보관하지 않아 추가 삭제는 수행하지 못했다. 실제 사용자 데이터는 수정하지 않았다.

## 스크린샷

| 화면 | 일반 | 작은 화면 |
| --- | --- | --- |
| 여행 목록 | [목록](screenshots/issue-101/travel-list.png) | [목록](screenshots/issue-101/travel-list-small.png) |
| 여행 일정 | [일정](screenshots/issue-101/travel-detail.png) | [일정](screenshots/issue-101/travel-detail-small.png) |
| 참여자 | [참여자](screenshots/issue-101/travel-participants.png) | [참여자](screenshots/issue-101/travel-participants-small.png) |
| 소비 | [소비](screenshots/issue-101/travel-ledger.png) | [소비](screenshots/issue-101/travel-ledger-small.png) |

## 별도 작업

실제 Google OAuth 설정과 영수증 파일 업로드 API는 별도 준비가 필요하다. 영수증 URI 필드는 파일 업로드가 아니다. AI 추천 경로 생성·지도 외부 SDK·배포·Discord 웹훅은 이번 여행 CRUD 연동의 검증 범위에 포함하지 않는다.

자동 일정과 경로 연동은 [PLANNING_API.md](PLANNING_API.md)를 참고하세요.
