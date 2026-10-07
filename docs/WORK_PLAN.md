# 작업계획표 (홈 순위 · 인라인 추천 · 여행지 연계)

> 최종 갱신: 2026-10-02
> 대상 저장소: `Gayadi-Android`, `Gayadi-Server` 모두 `feat/travel-route-demo` 브랜치(기존 순위·연계 미커밋 작업 포함)
> 상태: **홈 순위·자유여행 연계 + 여행루트 데모 구현 완료, 커밋/푸시/PR/배포 전**

새 에이전트는 이 문서부터 읽는다. 끝난 항목은 체크하고, 새로 알게 된 사실은 해당 절에 추가한다. 확정된 규칙은 이 문서가 아니라 `design.md`, `docs/API_INTEGRATION.md`에 반영한다.

## 0. 작업 규칙 (사용자 지시)

- **커밋·푸시·PR은 사용자가 명시적으로 요청할 때만 한다.** "마저 작업해" 같은 포괄적인 지시는 승인으로 보지 않는다.
- 비밀 파일(`.env*`, `*.jks`, `keystore.properties`, `config/*.properties`)은 읽거나 출력하지 않는다(`AGENTS.md`). 서명 파일 묶음이 필요하면 사용자에게 먼저 확인한다.
- UI를 바꾸기 전에 `design.md`를 읽고, 새 디자인 결정은 같은 변경에서 `design.md`에 반영한다.
- 빌드·테스트는 Windows Gradle로 실행한다: `cmd.exe /c "gradlew.bat ..."`. WSL의 `java`는 Windows `java.exe` 래퍼라서, WSL에서 띄운 서버에는 WSL `curl`로 접속되지 않는다. `curl.exe`를 쓴다.

## 1. 사용자 요구사항 (원문 요약)

1. 홈의 "인기 관광 TOP 10" 외에 카테고리별 TOP 10: 서울 인기 관광, 축제, 인기 지역. 식당은 리뷰 대신 **찜 수**로 순위를 정한다.
2. 관광지 인기도는 **한국관광 데이터랩**을 기준으로 한다(사용자 선택).
3. 추천 여행지를 하나씩 골라 이어 가는 흐름: 첫 장소를 고르면 가까운 두 번째를 추천하고, 세 번째는 두 번째를 기준으로 추천한다. 마음에 안 드는 장소를 바꾸면 다시 추천한다.
4. AI 추천은 별도 페이지가 아니라 **기존 장소 목록 안에 이미지와 내용이 있는 같은 카드**로 넣는다. 버튼은 **`일정에 추가`**와 **`이 장소로 변경`** 두 개다.
5. "이 장소로 변경"은 **방금 고른(직전에 추가한) 장소를 교체**한다(사용자 선택). 임의 위치(첫 번째 등)도 교체할 수 있어야 한다.
6. **여행지 연계 / 비연계 토글**을 둔다.
   - 연계: 앞뒤 일정과 이어지는 곳을 추천하고, 떨어진 장소로 바꾸면 "기존 여행지와 연계되지 않아요. 그래도 고르시겠어요?"라고 확인한다.
   - 비연계: 동선과 무관하게 자유롭게 고르고, 경고하지 않는다.

## 2. 완료된 구현

### 여행루트 데모 (`feat/travel-route-demo`)

- 기존 장소 추가 흐름은 `하나씩 고르기`로 이름을 명확히 했다. 먼저 고른 장소를 기준으로 다음 후보를 이동시간순 추천하며, 다른 순번을 독립적으로 바꿀 수 있다.
- 여행 홈의 날짜별 계획에 `여행루트` 진입을 추가했다. 시간대·이동수단을 고르면 서버가 장소 순서, 구간별 이동시간, 장소별 체류시간을 함께 추천한다.
- 여행루트의 개별 장소는 잠금 상태다. `새 루트 받기`로 전체를 다시 구성하고, `이 루트로 일정 바꾸기`로 해당 날짜의 MAIN 일정 전체를 서버 트랜잭션으로 교체한다.
- 여행 계획의 각 날짜에 `하나씩 고르기`와 `모두 추천받기`를 항상 함께 표시한다. 별도 스위치를 없애 진입 방법을 바로 알 수 있게 했고, 헤더 도움말에서 두 방식의 차이를 설명한다. 두 액션은 기존 색상 토큰을 사용해 중립/주요 액션으로 구분하고, 시각 높이 38dp·실제 터치 영역 48dp의 10dp 둥근 버튼으로 정리했다. 흐린 중립 버튼에는 `Border` 1dp 선을 더하고 주요 버튼은 채움만 유지한다.
- 재추천 조건 변경 뒤 요청이 실패하면 이전 루트를 적용할 수 없도록 오래된 미리보기를 제거한다. 일정 적용만 실패한 경우에는 검토 중인 루트를 유지하고 오류를 함께 보여 준다.
- 여행루트 상단에 대상 날짜를 표시하고, 장소 이미지는 다른 장소 화면과 같은 공통 `PlacePhoto` 실패 대체 이미지를 사용한다.
- 여행 시간은 가운데 정렬된 시작·종료 버튼에서 분 단위로 직접 고른다. 3~12시간 범위를 벗어나면 안내하고, 유효한 변경은 진행 중인 이전 요청을 취소한 뒤 최신 시간으로 루트를 자동 재계산한다.
- 앱 번들은 새로 만들지 않았다.

### 서버 (`Gayadi-Server`, 브랜치 `feat/rankings`, `dev`에서 분기)

- `GET /api/v1/rankings?type=ATTRACTION|FESTIVAL|REGION|RESTAURANT&region=&limit=1..20` (JWT 필요)
  - `ranking/RankingController`, `RankingService`, `DataLabClient`, `RankingRepository`, `RankingType`, `dto/response/*`
  - `ATTRACTION`: 데이터랩 `LocgoHubTarService1/areaBasedList1`의 `hubRank`를 쓴다. 기준월은 두 달 전부터 네 달 전까지 거슬러 조회한다. 음식·숙박 분류는 제외한다. 이미지는 TourAPI `searchKeyword2`로 이름을 매칭해 보강한다. 지역이 없으면 서울이다. 자료가 없으면 TourAPI `areaBasedList2`로 대체하고 `providerDataAvailable=false`로 응답한다.
  - `FESTIVAL`: TourAPI `searchFestival2`를 쓴다. 진행 중인 행사 → 가까운 시작일 → 이미지 있음 순으로 정렬하고, 끝난 행사는 제외한다. 조회에 실패하면 빈 목록과 `false`로 응답한다(오류로 응답하지 않는다).
  - `REGION`: 데이터랩 `DataLabService/locgoRegnVisitrDDList`에서 외지인(2)·외국인(3) 방문자 수를 모아 일평균으로 계산한다. 조회 구간은 7일이다. 자료가 없으면 한 달씩 최대 3번까지 앞으로 옮겨 조회한다.
  - `RESTAURANT`: `user_favorite_places` 찜 수로 정렬한다. 공개(`PUBLIC`)·활성(`ACTIVE`) 장소만 포함한다. 지역은 지역명이나 주소 토큰으로 필터한다.
  - 캐시: 관광지·지역 12시간, 축제 1시간, 맛집 5분이다. 대체 결과는 10분만 보관한다.
- 설정: `application.yml`의 `datalab.api.*`. 키는 `DATALAB_API_KEY`를 쓰고, 없으면 `TOUR_API_KEY`를 쓴다. `.env.example`과 `README.md`도 갱신했다.

### 앱 (`Gayadi-Android`, `main` 작업 트리)

- **홈 순위**
  - `domain/model/RankingModels.kt`, `domain/repository/RankingGateway.kt`
  - `data/remote/ranking/ServerRankingGateway.kt`. 읽기 35초, 전체 40초 클라이언트를 `di/AppContainer.kt`에 등록했다.
  - `feature/trip/.../HomeRankingViewModel.kt`: 칩 4개(인기 관광지=서울, 축제·행사, 인기 지역, 찜 많은 맛집). 칩별로 캐시한다.
  - `MyTripScreen.kt`: 하드코딩된 추천을 제거했다. 가로 순위 카드에 로딩·오류(다시 시도)·빈 상태·대체 안내를 넣었다. 이미지가 없으면 장소 아이콘을 보여준다.
- **장소 찾기 인라인 추천**
  - `PlaceRecommendationViewModel.kt`: 추천마다 `GET /api/v1/places/{id}`로 이미지·좌표를 보강한다. `headline`과 `recommendedPlaces`를 제공한다.
  - `PlaceSearchScreen.kt`: 추천 박스를 제거하고, 목록 맨 위에 `PlaceCard`(추천 배지, 추천 이유)를 넣었다. `일정에 추가`와 `이 장소로 변경` 버튼, 변경 확인·연계 경고 대화상자가 있다.
- **여행지 연계**
  - `PlaceLinkage.kt`: 같은 날 방문 목록, 앞뒤 이웃, 연계 기준점, 연계 판정을 담당한다. 판정 기준은 기존 거리의 1.5배(최소 5km)이다.
  - `PlaceSearchScreen.kt`의 `LinkModeControls`: 스위치와 `바꿀 일정` 칩.
  - `app/navigation/PlaceSearchPreferences.kt`: 연계 여부를 기기에 저장한다(기본값 켜짐).
  - `TripNavGraph.kt`(PLACE_SEARCH)
    - 기준점을 계산한다. 연계 모드에서 교체 대상을 고르면 앞뒤 일정의 중간점을 쓰고, 한쪽만 있으면 그 일정을 쓴다. 대상이 없으면 마지막으로 추가한 장소, 그다음 마지막 방문지 순서다. 비연계면 지역 첫 후보를 쓴다.
    - 기준이 바뀌면 자동으로 다시 추천한다.
    - 교체는 `tripViewModel.upsertSchedule`(PATCH placeId/title, 시간·메모 유지)로 한다.
    - 연계 모드에서 교체 대상을 고르면, 대상 일정을 제외한 앞뒤를 기준으로 이동시간순 검색을 한다.
- **기타**
  - 버전: 원격 `main` 기준 `versionName 0.0.27`, `versionCode 27`(CI에서는 override 가능, `app/build.gradle.kts`).
  - 문서: `design.md`(인라인 추천, 연계 토글, 홈 순위 카드), `docs/API_INTEGRATION.md`.
  - 테스트 APK 인터넷 권한: `feature/trip/src/androidTest/AndroidManifest.xml`. 이미지 URL이 있는 UI 테스트가 `SecurityException`으로 실패하던 기존 문제를 해결했다.

## 3. 검증 결과 (2026-10-02)

| 범위 | 결과 |
|---|---|
| 서버 전체 `gradlew test --dependency-verification lenient` | 원격 `dev` 반영 후 242개 통과(여행루트 계산 및 선택적 Tour API 보강 실패 회귀 테스트 포함). 원격 의존성 갱신 artifacts가 검증 메타데이터에 아직 없어 strict 모드는 실행 전 차단됨 |
| 서버 순위 테스트 | `RankingServiceTest` 7, `RankingHttpIntegrationTests` 4(401·400·200·OpenAPI), `RankingRestaurantIntegrationTests` 1 |
| 서버 전 구간(로컬 jar + 가짜 KTO API) | 관광지·축제·인기 지역(전국/서울) 200, 토큰 없으면 401, 캐시 적중 확인. 맛집은 로컬 시드에 식당이 없어 통합 테스트로만 검증 |
| 앱 전체 유닛 테스트 `gradlew test` | 324개 통과(여행루트 API 계약·시간 변경 자동 재계산·상태 오류 테스트 포함) |
| 여행루트 계측 테스트 | `Phone_API_35`의 1080×2400/420dpi와 720×1280/360dpi에서 시간 선택기·가운데 정렬 시간 버튼·잠금 장소·체류시간·구간 이동시간·전체 재추천/적용 UI와 적용 실패 시 미리보기 유지 2개 통과 |
| 앱 `feature/home` UI 테스트(에뮬레이터 Phone_API_35) | 4개 모두 통과(두 계획 방식 상시 노출·도움말 포함) |
| 앱 `feature/trip` UI 테스트(에뮬레이터 Phone_API_35) | 38개 모두 통과 |
| 로컬 H2 실제 연동 | `하나씩 고르기`에서 첫 장소 추가 후 해당 장소 기준 다음 후보 재추천, `모두 추천받기`에서 3곳의 체류·이동시간 포함 하루 루트 생성 확인 |
| 화면 캡처 확인 | 홈 순위(정상·오류), 장소 찾기(연계 카드, 추천 카드) 레이아웃 확인. 이 과정에서 잘못된 대체 이미지, 이모지 겹침, 빈 줄, 카드 경계를 고쳤다 |
| 개발 서버 기존 API | 여행 상세·목록·일정·dashboard, 날씨 3종, 혼잡도(일별·시간대별), 관광지·장소 조회 모두 정상. `/api/v1/rankings`는 **404(미배포)** |
| 운영 서버 | 공개 `tour/areas` 200, 인증 API는 401로 경로만 확인(운영 계정으로는 검증하지 않음) |

**검증하지 못한 것**
- 데이터랩 실제 응답: 키 승인 전이라 확인하지 못했다. 특히 `LocgoHubTarService1`이 `signguCd` 없이 광역 단위 조회를 지원하는지 모른다. 지원하지 않으면 시군구별로 최대 30번 호출하는 대체 경로를 탄다.
- 실제 기기에서 Google 로그인 후 전 구간 흐름.
- 2026-10-02 저장소 밖에서 로컬 H2 서버를 실행하고 에뮬레이터 dev 앱을 재시드해, 여행 생성부터 설문·루트 추천·일정 전체 적용·재진입까지 실제 API 전 구간을 확인했다. 로컬 후보가 충분한데 선택적 Tour API가 설정되지 않았을 때 503이 나던 문제도 수정해 재검증했다.
- 장소 추천 Agent가 꺼진 로컬 서버에서도 저장된 공개 장소를 거리순으로 반환하도록 대체 경로를 추가해, 첫 장소 선택 뒤 다음 후보가 다시 추천되는 흐름을 확인했다.

## 4. 남은 작업 (우선순위 순)

1. [ ] **사용자 승인 후** 커밋과 PR을 만든다. 템플릿: `.github/pull_request_template.md`, 제목 `<type>/#<issue>: <subject>`.
   - 서버: `feat/rankings` → `dev`
   - 앱: 새 브랜치 → `main`(최근 PR은 `feat/#142` → `main`)
2. [ ] 서버 `feat/rankings`를 배포한다. 배포 전에 앱 코드 29 번들을 출시하면 홈 순위에 오류 상태가 보인다.
3. [ ] 공공데이터포털에서 `지역별 중심 관광지 정보`(LocgoHubTarService1)와 `지역별 방문자수`(DataLabService)를 활용신청한다. 승인 후 실제 응답 필드명을 확인한다(`hubRank`, `hubTatsNm`, `signguCode`, `touDivCd`, `touNum`).
4. [ ] Play Console에 업로드한다.
   - 기능 없는 1.0.0: `앱출시/Gayadi-v1.0.0-code28-release.aab`
   - 최종 기능 포함: `앱출시/Gayadi-v1.0.0-code29-final-release.aab`(2026-09-26 빌드, 업로드 키 SHA-1 `08:CB:…:35:AD`, `jar verified`)
   - 이전 파일 `code29-release.aab`, `code29-linkmode-release.aab`는 중간 산출물이다. 올리지 않는다.
5. [ ] (선택) 홈 순위 카드를 누르면 상세나 장소 찾기로 이동하게 한다. 지금은 클릭 동작이 없다.
6. [ ] (선택) 연계 모드에서 교체 후 뒤쪽 일정과의 연계를 다시 점검해 안내한다.
7. [ ] (선택) `.kotlin/` 빌드 캐시 폴더를 `.gitignore`에 추가한다(빌드 중에 생긴 추적되지 않은 폴더).

## 5. 산출물과 로컬 상태

- 앱 번들 보관 폴더: `C:\Users\castle\Desktop\project\gayadi\앱출시\`
- 서명·설정 묶음(팀원 전달용, AES-256 암호화): `C:\Users\castle\Desktop\project\gayadi\gayadi-signing.zip`
  - 포함: `keystore.properties`, `gayadi-release.jks`, `config/prod.properties`, `config/dev.properties`
  - 암호는 사용자가 알고 있다. 이 문서에는 적지 않는다.
- git stash(앱 저장소)
  - `stash@{0}` "crlf-only changes before pull": `fix/#132`의 줄바꿈만 다른 변경이다. 필요 없으면 사용자 확인 후 삭제한다.
- 개발용 검증 계정: 개발 서버 점검에 쓴 일회용 계정과 여행은 삭제했다.
