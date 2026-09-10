# Android ↔ Server API 연동 규격

최종 검증: 2026-09-10, Android API 35 에뮬레이터 + Gayadi Server `dev`

## 구현 규칙

- Base URL은 `BuildConfig.API_BASE_URL` 한 곳에서만 가져온다.
- 화면은 URL이나 JSON을 직접 다루지 않고 Domain UseCase/Repository를 호출한다.
- 로그인 API는 `HttpAuthApiDataSource`, 인증이 필요한 공통 요청은 `GayadiApiClient`를 사용한다.
- 공지·법률 문서는 기존 `RestPublicContentDataSource`의 Retrofit + Moshi 규격을 유지한다.
- Bearer 토큰은 Data 계층에서 넣고, HTTP 401일 때만 Refresh Token으로 한 번 갱신한다.
- 서버 숫자 ID는 Data 계층 경계에서 검증하고 Domain에는 문자열로 전달한다.
- 에뮬레이터에서 로컬 서버는 `http://10.0.2.2:{port}`로 접근한다.

## 앱 사용 API

| 영역 | 요청 |
| --- | --- |
| 인증 | `POST /api/v1/auth/google-tokens`, `POST /api/v1/auth/token-refreshes`, `DELETE /api/v1/auth/sessions/current` |
| 개발 계정 | `POST /api/v1/auth/registrations` |
| 내 프로필 | `GET/PATCH/DELETE /api/v1/users/current` |
| 설문 | `GET /api/v1/surveys/travel-personality-v1`, `GET .../results/{code}`, `POST .../submissions` |
| 고객지원 | `POST /api/v1/inquiries` |
| 공지·약관 | `GET /api/v1/notices`, `GET /api/v1/notices/{id}`, `GET /api/v1/legal-documents/{id}` |
| 장소 | `GET /api/v1/places`, `GET /api/v1/users/current/favorite-places` |
| 여행 | `GET/POST /api/v1/trips`, `GET/PATCH/DELETE /api/v1/trips/{tripId}`, `PATCH .../status` |
| 참여·날짜 | `GET/PUT/DELETE .../participants`, `POST /api/v1/trip-memberships`, `GET/PUT .../date-coordination` |
| 일정 | `GET/POST .../schedules`, `PATCH/DELETE .../schedules/{scheduleId}`, `PATCH .../schedule-orders` |
| 경비 | `GET/POST .../expenses`, `PATCH/DELETE .../expenses/{expenseId}`, `GET .../expense-settlement` |
| 공동 경비 | `GET .../shared-fund`, `POST .../shared-fund/contributions` |
| 친구 | `GET/POST /api/v1/friendships`, `PATCH/DELETE /api/v1/friendships/{id}`, `GET /api/v1/users?query=...` |
| 사용자 초대 | `GET/POST .../invitations`, `PATCH .../invitations/{id}` |
| 자동 일정·홈 | `GET/POST .../plans`, `GET .../dashboard` |
| 경로 | `POST .../route-recommendations`, `GET/PUT/DELETE .../route-selections` |
| 여행별 성향 | `POST .../survey-responses`, `GET .../personality-profile` |
| 현장 상황 | `POST .../event-observations`, `GET/PATCH .../change-proposals` |
| Agent | `POST /api/v1/recommendations/places`, `POST .../situation-responses` |
| 날씨·혼잡 | `GET /api/v1/weather/*`, `GET /api/v1/congestion/forecast` |
| 관광정보 | `GET /api/v1/tour/areas`, `/locations`, `/keywords`, `/festivals`, `/stays` |

`...`는 `/api/v1/trips/{tripId}`를 뜻한다. 상세 요청·응답은 서버 Swagger와
[TRAVEL_API.md](TRAVEL_API.md), [SOCIAL_API.md](SOCIAL_API.md),
[PUBLIC_CONTENT_API.md](PUBLIC_CONTENT_API.md)를 기준으로 한다.

## 응답과 오류

- 요청과 응답은 UTF-8 JSON이다.
- 보호 API는 `Authorization: Bearer {accessToken}`을 사용한다.
- 오류 분기는 HTTP 상태와 서버의 안정적인 `code`를 기준으로 한다. 사용자 입력값이나 서버 원문은 로그에 남기지 않는다.
- `401`은 자동 갱신을 한 번만 시도하고, 다시 실패하면 로그인 만료로 처리한다.

## 에뮬레이터 검증

아래 네 계측 테스트가 실제 HTTP 요청을 검증한다. 기존 핵심 기능 테스트는 주요 화면 렌더링도 확인하고,
`DevAdvancedApiIntegrationTest`는 아직 화면에 연결하지 않은 API 계약을 검증한다.

```sh
adb shell pm clear com.doonow.gayadi
adb shell pm grant com.doonow.gayadi android.permission.POST_NOTIFICATIONS
adb shell am instrument -w -e liveApi true \
  -e class com.gayadi.android.api.DevTravelIntegrationTest \
  com.doonow.gayadi.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w -e liveApi true \
  -e class com.gayadi.android.api.DevSurveyIntegrationTest \
  com.doonow.gayadi.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w -e liveApi true \
  -e class com.gayadi.android.api.DevFriendshipIntegrationTest#friendshipRoundTrip \
  com.doonow.gayadi.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w -e liveApi true \
  -e class com.gayadi.android.api.DevAdvancedApiIntegrationTest \
  com.doonow.gayadi.test/androidx.test.runner.AndroidJUnitRunner
```

2026-09-10 검증에서는 여행·참여자·날짜·일정·경비·공금·장소·즐겨찾기,
가입·토큰 갱신·로그아웃·프로필·설문·문의·공지·약관, 친구·사용자 지정 초대와
자동 일정·대시보드·경로·여행별 성향·현장 상황·Agent·날씨·혼잡·TourAPI가 모두 통과했다.
Agent·날씨·TourAPI는 `scripts/mock-external-api.mjs`의 계약 스텁으로 서버 서비스까지 검증했으며,
실제 외부 사업자와 Google OAuth는 유효한 개발 키가 있는 환경에서 별도 확인해야 한다.
