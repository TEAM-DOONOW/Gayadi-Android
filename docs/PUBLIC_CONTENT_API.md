# 공지·법률 문서 REST 연동

설정의 공지 목록·상세와 이용약관·개인정보처리방침은 `RestPublicContentDataSource`를 통해 Gayadi 서버에서 조회합니다. `AppContainer`가 기존 관광 API와 같은 `API_BASE_URL`을 전달합니다. 별도 인증 토큰이나 새로운 환경변수는 필요하지 않습니다.

| 화면 | API |
| --- | --- |
| 공지 목록 | `GET /api/v1/notices?limit=100&offset=0` |
| 공지 상세 | `GET /api/v1/notices/{noticeId}` |
| 이용약관 | `GET /api/v1/legal-documents/terms-of-service` |
| 개인정보처리방침 | `GET /api/v1/legal-documents/privacy-policy` |

공지 목록은 100건씩 offset을 증가시켜 마지막 페이지까지 읽습니다. 중간 페이지에서 실패하면 부분 목록을 성공으로 표시하지 않습니다. 서버의 `isPinned`, 게시 시각, 본문 순서와 nullable 필드를 그대로 DTO로 매핑합니다.

Retrofit + Moshi로 비동기 요청을 처리하며 연결 10초, 읽기 20초, 전체 호출 30초 제한을 적용합니다. HTTP 실패·빈 응답·잘못된 JSON은 기존 화면의 오류/재시도 흐름으로 전달하고, 서버 오류 본문이나 파서 진단을 화면에 노출하지 않습니다. Firestore로 자동 우회하지 않습니다.

## 검증

`PublicContentApiTest`는 MockWebServer로 실제 HTTP 요청 경로, 페이지네이션, 두 법률 문서 응답, nullable 필드, 404/429/503, 204, 잘못된 JSON과 잘못된 식별자를 검증합니다.

```sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```

배포 환경에서는 서버에 공개 상태의 공지와 두 법률 문서가 존재해야 합니다. 실제 배포 서버와 실기기에서의 화면 확인은 별도 검증 항목입니다.

관련 이슈: #115, 상위 통합 작업 #101. OAuth 로그인 #112는 별도 작업입니다.
