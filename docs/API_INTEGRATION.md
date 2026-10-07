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
| 홈 순위 | `GET /api/v1/rankings?type=ATTRACTION\|FESTIVAL\|REGION\|RESTAURANT&region=...&limit=10` |
| 여행 | `GET/POST /api/v1/trips`, `GET/PATCH/DELETE /api/v1/trips/{tripId}`, `PATCH .../status` |
| 참여·날짜 | `GET/PUT/DELETE .../participants`, `POST /api/v1/trip-memberships`, `GET/PUT .../date-coordination` |
| 일정 | `GET/POST .../schedules`, `PATCH/DELETE .../schedules/{scheduleId}`, `PATCH .../schedule-orders` |
| 경비 | `GET/POST .../expenses`, `PATCH/DELETE .../expenses/{expenseId}`, `GET .../expense-settlement` |
| 공동 경비 | `GET .../shared-fund`, `POST .../shared-fund/contributions` |
| 친구 | `GET/POST /api/v1/friendships`, `PATCH/DELETE /api/v1/friendships/{id}`, `GET /api/v1/users?query=...` |
| 사용자 초대 | `GET/POST .../invitations`, `PATCH .../invitations/{id}` |
| 자동 일정·홈 | `GET/POST .../plans`, `GET .../dashboard` |
| 경로 | `POST .../route-recommendations`, `GET/PUT/DELETE .../route-selections` |
| 여행루트 | `POST .../itinerary-recommendations`, `PUT .../itinerary-selections/{date}` |
| 여행별 성향 | `POST .../survey-responses`, `GET .../personality-profile` |
| 현장 상황 | `POST .../event-observations`, `GET/PATCH .../change-proposals` |
| Agent | `POST /api/v1/recommendations/places`, `POST .../situation-responses` |
| 날씨·혼잡 | `GET /api/v1/weather/*`, `GET /api/v1/congestion/forecast`, `GET /api/v1/congestion/forecast/hourly`, `GET /api/v1/congestion/places/{placeId}` |
| 관광정보 | `GET /api/v1/tour/areas`, `/locations`, `/keywords`, `/festivals`, `/stays` |

`...`는 `/api/v1/trips/{tripId}`를 뜻한다. 상세 요청·응답은 서버 Swagger와
[TRAVEL_API.md](TRAVEL_API.md), [SOCIAL_API.md](SOCIAL_API.md),
[PUBLIC_CONTENT_API.md](PUBLIC_CONTENT_API.md)를 기준으로 한다.

혼잡 일별·시간대·날씨 API는 각각 독립된 기존 계약이다. 서버와 앱을 함께 배포하는 별도 변경 없이
`/congestion/forecast` 응답에 시간대나 날씨를 합치지 않는다.

## 응답과 오류

### 장소찾기 이동시간순과 일정 지도

- `GET /api/v1/places`를 기존 Bearer 인증으로 호출한다. 기본 최신순은 `sort=RECENT`, 이동시간순은 `sort=TRAVEL_TIME`이다. 이 검색은 추천 생성·선택 API나 성향 Agent를 호출하지 않는다.
- 도보(`WALK`)·자전거(`BICYCLE`)의 이동시간은 직선거리 기반 추정치이므로 공급자/fallback 값과 무관하게 항상 추정으로 표시한다.
- 이동수단은 `PUBLIC_TRANSIT`, `CAR`, `WALK`, `BICYCLE`이다. 장소찾기에서 선택하면 이동시간순으로 조회하고, 앱 내 다음 장소찾기에서도 선택을 이어 쓴다.
- 끝에 추가할 때는 같은 날짜의 마지막 MAIN 방문지를 기준으로 한다. 중간 삽입 문맥이 전달되면 앞·뒤 방문지를 각각 `originLatitude/Longitude`, `nextLatitude/Longitude`로 보낸다.
- 저장된 장소 좌표가 없으면 선택한 `placeId`의 상세 정보를 조회한다. 이전 좌표를 정할 수 없으면 다음 좌표도 생략하고 여행 지역만 보낸다. 숙소 좌표를 별도로 추정하거나 임의의 검색 결과를 기준점으로 쓰지 않는다.
- 후보는 `items` 순서를 유지하며 클라이언트가 거리로 재정렬하지 않는다. `travelTime.durationMinutes`, `additionalDurationMinutes`를 표시하고 음수 추가 시간도 유지한다. `fallback=true` 또는 `LOCAL_ESTIMATE`는 `추정`으로 표시한다.
- 실제 `ranking.sort=RECENT` 응답은 기준 좌표 유무에 맞는 안내를 표시한다. 기준 좌표를 보냈는데 `ranking`이 없으면 서버 정렬 정보 누락 오류로 처리한다. `limited` 값은 보존하지만 목록과 필터에는 후보 제한 안내 문구를 표시하지 않는다. 이동시간순에서는 커서를 보내거나 더보기를 노출하지 않는다. 최신순에서만 `nextCursor/hasNext`로 페이지를 추가한다.
- 서버 대기 예산 30초를 고려해 이 검색의 클라이언트 읽기 제한은 35초, 전체 호출 제한은 40초로 둔다. 오류 시 이전 이동시간순 목록을 정상 결과처럼 남기지 않고 재시도를 제공한다.
- 선택한 장소 ID·좌표를 로컬 일정에 보존한다. 서버에는 기존 일정 API 계약대로 장소 ID를 저장하며, 중간 삽입은 생성 후 일정 순서 API로 반영한다. 순서 저장만 실패하면 생성된 일정은 유지하고 오류를 안내한다.
- 지도는 선택한 장소 좌표 또는 해당 ID의 상세 좌표를 사용한다. 제목으로 카카오 `keywordSearch`를 호출하거나 저장 실패 시 이름이 같은 다른 장소로 대체하지 않는다.
- 전체 경로 API 어댑터의 `transportMode` 지원은 호환성을 위해 유지하되 홈의 별도 추천 버튼은 제거했다.

### 공통 규칙

- 요청과 응답은 UTF-8 JSON이다.
- 보호 API는 `Authorization: Bearer {accessToken}`을 사용한다.
- 오류 분기는 HTTP 상태와 서버의 안정적인 `code`를 기준으로 한다. 사용자 입력값이나 서버 원문은 로그에 남기지 않는다.
- `401`은 자동 갱신을 한 번만 시도하고, 다시 실패하면 로그인 만료로 처리한다.

### 홈 카테고리 순위

- 나의여행 홈 칩별로 `GET /api/v1/rankings`를 Bearer 인증으로 호출한다. 인기 관광지는 `type=ATTRACTION&region=서울`, 축제·인기 지역·찜 많은 맛집은 지역 없이 전국 기준(`FESTIVAL`, `REGION`, `RESTAURANT`)이다.
- 관광지·인기 지역은 한국관광 데이터랩, 축제는 TourAPI 행사 정보(진행 중·임박순), 맛집은 가야디 사용자 찜 수 기준이다. 리뷰 기반 순위는 없다.
- 응답의 `providerDataAvailable=false`는 제공기관 순위 대신 대체 목록(또는 빈 목록)이라는 뜻이다. 앱은 목록을 그대로 보여주고 짧게 안내한다.
- 첫 요청은 서버가 여러 공공 API를 조합할 수 있어 읽기 35초·전체 40초 제한을 쓴다. 칩별 결과는 화면 수명 동안 캐시하고 오류에는 `다시 시도`를 제공한다.

### 장소찾기 인라인 추천과 여행지 연계

- AI 추천(`POST /api/v1/recommendations/places`)은 장소 목록 안의 카드로 표시한다. 추천 응답에 이미지가 없으므로 각 `placeId`의 `GET /api/v1/places/{placeId}`로 이미지·주소·좌표를 채우고, 실패한 항목은 기본 카드로 보여준다.
- 추천 기준점은 여행지 연계 설정을 따른다. 연계를 켜면 마지막으로 추가한 장소(없으면 같은 날 마지막 방문지)를, 바꿀 일정을 고르면 그 일정의 앞뒤 방문지 중간 지점(한쪽만 있으면 그 방문지)을 `latitude/longitude`로 보낸다. 연계를 끄면 목록 첫 후보 좌표로 지역 전체를 추천한다.
- 연계 모드에서 바꿀 일정을 고르면 이동시간순 검색은 그 일정을 제외한 앞뒤 방문지를 `origin`/`next` 좌표로 보낸다.
- `이 장소로 변경`은 `PATCH .../schedules/{scheduleId}`로 `placeId`와 `title`만 바꾸고 시간·메모는 유지한다. 앞뒤 방문지와의 거리가 기존 거리의 1.5배(최소 5km)를 넘으면 연계 모드에서만 확인을 받는다.

### 엄격한 여행루트 데모

- `POST /api/v1/trips/{tripId}/itinerary-recommendations`는 `date`, `startTime`, `endTime`, `transportMode`, `variation`으로 하루 전체 루트를 추천한다. 서버는 여행 지역의 공개 장소 중 좌표가 있는 후보를 사용한다.
- 응답 `stops`는 서버가 확정한 순서로 주며, 각 항목에 `arrivalTime`, `departureTime`, `stayMinutes`, `travelMinutesFromPrevious`, `distanceMetersFromPrevious`가 있다. 현재 데모는 직선거리와 이동수단별 평균 속도로 계산하므로 `estimated=true`이다.
- `variation`을 증가시켜 재요청하면 기존 장소 하나만 바꾸지 않고 루트 전체를 다시 구성한다.
- `PUT /api/v1/trips/{tripId}/itinerary-selections/{date}`는 보고 있는 `placeId` 순서를 `expectedPlaceIds`로 함께 보낸다. 서버가 동일한 요청을 다시 계산했을 때 순서가 달라졌으면 `ROUTE_CALCULATION_CHANGED`로 적용을 멈춘다. 같으면 해당 날짜의 `MAIN` 일정을 하나의 트랜잭션으로 교체한다. 삭제된 일정에 연결된 비용은 기존 DB 정책대로 일정 연결만 해제된다.

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
