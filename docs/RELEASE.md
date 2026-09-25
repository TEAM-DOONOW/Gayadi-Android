# Android 릴리즈 가이드

## 버전 기준

- 사용자에게 표시하는 `versionName`은 `app/build.gradle.kts`에서 관리하고 출시할 변경에 맞춰 올린다.
- 로컬 `versionCode` 기본값은 같은 파일에서 관리한다. CI는 `VERSION_CODE` Gradle 속성으로 덮어쓰며 `PLAY_VERSION_CODE_BASE + github.run_number`를 사용한다.
- 로컬에서 별도로 업로드한다면 Play Console의 모든 트랙에서 이미 사용한 코드보다 높은 값을 지정한다. CI 자동 배포와 수동 업로드의 버전 코드 범위를 겹치게 사용하지 않는다.
- 원격 브랜치의 값이 실제 마지막 릴리즈보다 낮을 수 있으므로 원격 값만 보고 버전을 정하지 않는다.
- pull, stash, reset 전에 로컬 릴리즈 설정과 마지막 산출물의 `output-metadata.json`을 확인한다.

## GitHub Actions에서 테스트 및 프로덕션 자동 배포

### 실행 흐름

1. `stg` 또는 `main` 대상 PR은 `.github/workflows/android.yml`에서 브랜치명, Wrapper, 배포 스크립트 테스트를 검사한다. 기능 브랜치는 `<type>/#<issue-number>`를 사용하며, `stg → main` 출시 PR과 `main → stg` 동기화 PR도 허용한다.
2. 단위 테스트(app devDebug, Android 라이브러리 debug, domain)와 Lint/dev APK 빌드를 병렬로 실행한다. 필수 상태 검사는 `ci-result`로 설정한다.
3. `stg` PR을 머지하면 push 이벤트로 `.github/workflows/play-release.yml`이 실행된다. 같은 커밋의 CI 통과 후 서명한 `prodRelease` AAB와 R8 매핑 파일을 내부 테스트 트랙에 업로드한다.
4. 내부 테스트 후 `stg → main` PR을 생성하고 머지한다. `main` push도 CI 통과 후 해당 main 커밋을 새 versionCode로 빌드해 프로덕션 트랙에 업로드한다. 테스트한 변경 사항을 유지하도록 출시 PR을 확인한다.
5. PR 생성·수정 시에는 검사만 실행하고, 운영 설정과 서명 키는 사용하지 않는다. 실제 배포는 머지 후 push 또는 명시적 수동 실행에서만 발생한다. 직접 push도 배포를 실행하므로 두 브랜치에 PR 필수 및 `ci-result` 상태 검사 규칙을 설정한다.

| 대상 브랜치 | PR | 머지 후 push / 수동 실행 | GitHub Environment |
| --- | --- | --- | --- |
| `stg` | 테스트·Lint·빌드 | Play `internal` 배포 | `play-internal` |
| `main` | 테스트·Lint·빌드 | Play `production` 출시 제출 | `play-production` |

수동 실행은 Actions의 **Play release → Run workflow → stg 또는 main**을 사용한다. 다른 브랜치에서는 배포 job이 실행되지 않는다. 두 트랙 모두 `status: completed`이며 프로덕션은 전체 사용자 대상 출시 요청이다. 실제 공개 시점은 Google 심사와 관리형 게시 설정에 따른다. 앱 초안 상태 또는 게시 요건이 남아 있으면 Play Console에서 먼저 해결해야 한다. [Play 트랙과 출시 상태](https://developers.google.com/android-publisher/tracks)

두 트랙은 동일 패키지의 운영 설정(`PROD_PROPERTIES`)과 업로드 서명 키를 사용한다. `stg`는 테스트 배포 브랜치이며 별도의 개발 서버용 앱 flavor를 의미하지 않는다. `main` 배포는 내부 테스트 AAB의 승격이 아니라 main 커밋의 새 빌드이다.

### 최초 설정

1. Play Console에서 패키지 `com.doonow.gayadi`의 앱 생성, 최초 수동 AAB 업로드와 Play App Signing 설정을 완료한다.
2. Google Cloud에서 Google Play Android Developer API를 활성화하고 서비스 계정을 생성한다.
3. Play Console의 사용자 및 권한에 서비스 계정 이메일을 추가하고 이 앱의 **앱 정보 보기(읽기 전용)**, **앱을 테스트 트랙으로 출시**, **프로덕션으로 출시, 기기 제외, Play 앱 서명 사용** 권한을 부여한다. 서비스 계정을 분리한다면 내부 테스트 계정에는 조회·테스트 출시, 프로덕션 계정에는 조회·프로덕션 출시 권한을 부여한다.
4. GitHub Settings → Environments에서 `play-internal`은 허용 브랜치를 `stg`로, `play-production`은 `main`으로 제한한다. 기존 `play-internal`에 등록한 `main` 규칙은 `stg`로 변경한다.
5. 아래 Secrets를 **각 Environment**에 등록한다. 같은 앱의 업로드 서명 키와 운영 설정을 사용한다. 값을 소스, 이슈, 채팅 또는 로그에 붙여 넣지 않는다.
6. `PLAY_VERSION_CODE_BASE`는 **Settings → Secrets and variables → Actions → Variables**의 저장소 변수로 등록한다. 기존 Environment 변수는 제거하고 저장소 변수 하나로 관리한다. 환경마다 다른 기준값을 사용하지 않도록 버전 할당은 Environment가 없는 job에서 실행한다.

현재 인증 액션은 사전 버전 조회용 OAuth 액세스 토큰도 발급한다. Google Cloud에서 IAM Service Account Credentials API를 활성화하고, 서비스 계정 리소스의 권한에 해당 서비스 계정 자신을 주 구성원으로 추가해 `Service Account Token Creator` (`roles/iam.serviceAccountTokenCreator`) 역할을 부여한다. Play Console의 앱 배포 권한과 별도 설정이다. [인증 액션 안내](https://github.com/google-github-actions/auth#inputs-service-account-key-json)

| 구분 | 이름 | 내용 |
| --- | --- | --- |
| Secret | `ANDROID_UPLOAD_KEYSTORE_BASE64` | Play에 등록된 업로드 키스토어의 Base64 표현. Base64는 암호화가 아니다 |
| Secret | `ANDROID_KEY_ALIAS` | 업로드 키 별칭 |
| Secret | `ANDROID_KEY_PASSWORD` | 업로드 키 비밀번호 |
| Secret | `ANDROID_STORE_PASSWORD` | 키스토어 비밀번호 |
| Secret | `PLAY_SERVICE_ACCOUNT_JSON` | 서비스 계정 JSON 인증 정보 |
| Secret | `PROD_PROPERTIES` | `config/prod.properties.example`과 같은 구조의 운영 설정 전체. `DEBUG_LOGGING=false`, 실제 운영 HTTPS 도메인 및 SDK 설정 필요 |
| Secret | `DISCORD_WEBHOOK_URL` | 배포 결과를 받을 Discord 텍스트 채널의 웹훅 URL. 각 Environment에 등록 |
| Repository Variable | `PLAY_VERSION_CODE_BASE` | 기존에 사용한 최대 versionCode 이상인 정수. 두 트랙이 공유하며 최초 설정 후 유지 |

현재 저장소에 추적 중인 `app/google-services.json`을 사용한다. 해당 Firebase 설정의 Android 앱과 운영 Google Web OAuth 클라이언트가 배포 앱에 맞는지 콘솔에서 확인한다. 운영 로그인은 로컬 업로드 키뿐 아니라 Play App Signing 인증서 지문 등록이 필요하다.

### Discord 배포 알림

1. Discord의 알림용 텍스트 채널 설정 → 연동(Integrations) → 웹후크(Webhooks)에서 웹훅을 만들고 URL을 복사한다. 웹훅 관리 권한이 필요하다.
2. GitHub `play-internal`, `play-production` 환경에 각각 `DISCORD_WEBHOOK_URL` Secret을 등록한다. 같은 채널을 쓰려면 같은 URL, 채널을 나누려면 각 채널의 URL을 등록한다. 봇 토큰은 필요하지 않다. [Discord 웹훅 안내](https://docs.discord.com/developers/resources/webhook)
3. 배포 워크플로우가 끝나면 별도 `notify` job이 결과를 전송한다. `stg`는 **STG**, `main`은 **PROD**로 표시하고 브랜치·트랙·versionCode·커밋·실행자와 해당 실행 시도의 로그 링크를 포함한다.

CI 검사나 버전 설정 실패로 실제 업로드가 생략되어도 실패 단계를 알린다. 프로덕션 성공은 **출시 제출 완료**로 표시하며 스토어 공개 완료를 의미하지 않는다. 이미 업로드한 실행을 재실행하면 **기존 배포 확인**으로 구분한다. PR 검사에는 알림을 보내지 않는다.

웹훅 미등록은 Actions 경고와 함께 알림을 건너뛰고, 전송 실패는 배포 성공/실패 결과를 바꾸지 않는다. URL과 Discord 응답 본문은 로그에 출력하지 않으며 알림은 전체 멘션을 발생시키지 않는다. 취소된 실행도 알림 job이 실행될 수 있으면 취소 상태를 전송하지만, 대기열 취소·강제 종료·러너 장애 또는 Environment 승인이 거부된 경우 전송을 보장하지 않는다. 로컬 검증에서는 네트워크 호출을 모킹하며 실제 Discord 메시지를 전송하지 않는다.

### 버전과 재실행 규칙

- 예를 들어 기준값이 `100`, 배포 워크플로우 실행 번호가 `7`이면 업로드 versionCode는 `107`이다. 이 예시값 대신 실제 Play의 기존 최대 코드를 확인해 기준값을 정한다.
- `stg`와 `main`은 하나의 배포 워크플로우 실행 번호를 공유한다. 예를 들어 stg 실행 7은 `107`, 다음 main 실행 8은 `108`이다. 브랜치별로 워크플로우 파일을 분리하지 않는다.
- 같은 실행의 **Re-run jobs**는 동일 코드를 사용한다. `run_attempt`로 코드를 바꾸지 않는다.
- 해당 브랜치의 대상 트랙에 동일 코드·커밋 SHA의 완료된 릴리즈가 있으면 빌드와 업로드를 건너뛴다. 다른 트랙의 릴리즈를 성공으로 오인하거나 자동 승격하지 않는다.
- 같은 코드가 다른 릴리즈에 사용됐거나 초안/일부 업로드 상태이면 실패시키고 Play Console에서 확인한다. 성공으로 처리하거나 자동 덮어쓰기하지 않는다.
- 더 높은 코드가 어느 트랙에든 이미 등록됐다면 오래된 실행은 중단한다. 원하는 브랜치에서 새 배포 실행을 시작하거나 기준값 충돌을 해결한다.
- 업로드 시점에 Play가 versionCode 재사용을 최종 검사한다. 사전 조회로 모든 과거 업로드 이력을 확인할 수 있다고 가정하지 않는다.
- 워크플로우 이름/파일을 바꿔 실행 번호가 새로 시작되거나 별도 수동 업로드를 도입하면 기준값을 다시 검토한다.
- `versionName`은 실행 번호와 별개다. 정식 출시 전에 소스의 사용자 표시 버전을 갱신한다.

### 배포 파일과 로그

- 실행마다 한국어 변경 사항 `distribution/whatsnew/whatsnew-ko-KR`을 갱신한다.
- 단위 테스트 및 Lint 보고서는 실패해도 7일간 보관한다. 예시 설정의 dev APK는 빌드 검증용이며 3일간 보관한다.
- prod AAB와 `mapping.txt`는 30일간 보관한다. 난독화된 버전의 장기 장애 분석에 필요하면 별도 보관 정책을 적용한다.
- 운영 빌드는 Gradle 캐시 저장/복원을 끄고 `--no-build-cache --no-configuration-cache`로 실행한다. 운영 설정 및 서명 파일은 artifact에 포함하지 않는다.
- 배포가 끝나면 임시 서명/설정 파일을 삭제한다. 배포는 일회성 GitHub-hosted runner에서만 지원한다.
- 두 브랜치는 동일 앱의 Play edit 충돌을 막기 위해 배포 워크플로우를 직렬 실행하며 업로드 중인 실행은 새 push로 취소하지 않는다. GitHub concurrency 특성상 대기 중인 이전 실행은 다른 브랜치의 최신 실행으로도 교체될 수 있으므로 모든 커밋마다 배포되는 것을 보장하지 않는다. main 출시가 대기 중 교체됐다면 main에서 새 수동 실행을 시작한다.
- UI 계측 테스트 자동 실행은 이 워크플로우의 필수 검사에 포함하지 않는다. 별도 에뮬레이터 job 도입 전에는 주요 화면을 기기에서 확인한다.

### 로컬 검증

배포 버전/중복 업로드 판단은 실제 Play API 호출이나 키 접근 없이 검증한다.

```sh
python3 -m unittest discover -s scripts/ci -p 'test_*.py' -v
```

`prepare_play_signing.py`는 GitHub-hosted runner에서만 실행한다. 로컬 키스토어·운영 설정을 복사하거나 덮어쓰는 용도로 사용하지 않는다.

### 참고

- [Google Play Developer API 설정](https://developers.google.com/android-publisher/getting_started)
- [업로드 액션](https://github.com/r0adkll/upload-google-play)
- [앱 버전 관리](https://developer.android.com/studio/publish/versioning)
- [참고한 GitHub Actions 업로드 흐름](https://charko.tistory.com/22)
- [Android CI/CD 및 협업 도구 연동 참고 글](https://velog.io/@hearit/CICD-%EC%99%9C-%ED%95%B4%EC%95%BC-%ED%95%A0%EA%B9%8C-ft.-GitHub-Actions%EB%A1%9C-%EC%99%84%EC%84%B1%ED%95%98%EB%8A%94-Android-%EC%9E%90%EB%8F%99-%EB%B0%B0%ED%8F%AC)

## Google Play 타겟 API

2026-08-31부터 일반 Android 신규 앱과 업데이트는 Android 16(API 36) 이상을 타겟팅해야 한다.

- `compileSdk = 36`
- `targetSdk = 36`
- Android Gradle Plugin은 API 36의 최소 지원 버전인 `8.9.1` 이상
- CI도 `platforms;android-36`과 `build-tools;36.0.0`을 설치

`compileSdk`만 올리고 `targetSdk`를 그대로 두면 Play Console 업로드 오류가 계속 발생한다.

- [Google Play 타겟 API 정책](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Android 16 SDK 설정](https://developer.android.com/about/versions/16/setup-sdk)

## 로컬 프로덕션 설정

`prodRelease` 빌드는 Git에서 제외된 다음 파일을 사용한다.

- `config/prod.properties`: 운영 API URL, 앱 SDK 키, Credential Manager용 Google 웹 클라이언트 ID
- `keystore.properties`: release 키스토어 경로와 서명 정보
- 실제 키스토어 파일

필수 프로덕션 속성 이름은 `.env.example`과 `config/prod.properties.example`을 기준으로 한다. 일회성 빌드 값은 `-PAPI_BASE_URL=...` 형식으로 넘길 수 있고 운영 API URL은 HTTPS 도메인이어야 한다. 실제 값, 비밀번호, 키스토어는 커밋하거나 빌드 로그에 출력하지 않는다. 기존 키 파일을 삭제하거나 새 키로 교체하면 Play Console에서 기존 앱 업데이트가 불가능할 수 있다.

## 릴리즈 서명 인증서 지문

로컬 `prodRelease` 업로드 키(CN=Gayadi, O=doonow) 지문이다. Kakao, Firebase, Maps 등 콘솔에 앱 서명 키를 등록할 때 사용한다.

- SHA-1: `08:CB:66:B5:60:AB:2E:5F:9A:49:B6:F2:99:FB:41:DD:34:1B:35:AD`
- SHA-256: `B8:59:19:E2:12:64:BC:43:71:CE:81:F8:2E:83:3D:BE:17:F3:EC:73:14:45:E0:4A:C0:6B:E2:AB:26:17:CE:F3`

Play App Signing을 쓰면 Play Console의 앱 서명 키 지문이 위 값과 다를 수 있다. 스토어 배포 앱용 콘솔에는 Play Console > 앱 무결성 > 앱 서명에 나온 지문도 함께 등록한다.

서명된 APK에서 다시 확인하려면:

```text
apksigner verify --print-certs app/build/outputs/apk/prod/release/app-prod-release.apk
```

## Google OAuth 클라이언트

앱과 서버의 OAuth 설정값은 Web OAuth 클라이언트 ID인 `GOOGLE_WEB_CLIENT_ID` 하나만 사용한다. Android OAuth 클라이언트 ID는 앱 빌드 설정이 아니다. Google Cloud Console에서 패키지 `com.doonow.gayadi`와 배포 경로별 SHA-1을 연결하는 등록 항목으로만 관리한다.

| 배포 경로 | Google Cloud 등록 | SHA-1 |
| --- | --- | --- |
| debug | `6035741280-g9agek5bfnkprhp9ubqklb2ustbjd8ld.apps.googleusercontent.com` | `B4:5B:35:CD:37:FB:F7:E2:6E:D0:B8:3D:2E:D6:F5:B5:85:F7:52:94` |
| release (local/upload signing) | `6035741280-jedtnq850vigud4osf3ce6223i4abbe4.apps.googleusercontent.com` | `08:CB:66:B5:60:AB:2E:5F:9A:49:B6:F2:99:FB:41:DD:34:1B:35:AD` |
| Play App Signing (A2:1D) | `6035741280-8eidnon8bfv74u40jvvgbkrj8hov7ds6.apps.googleusercontent.com` | `A2:1D:9A:41:88:52:31:22:56:A0:FC:CE:DC:E5:F2:9C:73:B5:2F:BD` |
| Play App Signing (70:52) | `6035741280-cv8v741od57p7pkqh45er3pg2qs86664.apps.googleusercontent.com` | `70:52:86:10:71:BF:A6:33:A3:93:26:D5:E5:B8:9B:FA:27:DC:24:F8` |

Play 키 교체가 적용된 앱은 기기 조건에 따라 기존 키 또는 교체 키로 서명될 수 있으므로 두 등록을 모두 유지한다. 어느 Android 클라이언트 ID도 `GOOGLE_WEB_CLIENT_ID`나 Credential Manager의 `serverClientId`에 넣지 않는다.

## 빌드

Windows Android Studio JDK 환경에서 다음 작업을 실행한다.

```text
gradlew.bat assembleProdRelease bundleProdRelease
```

산출물:

- APK: `app/build/outputs/apk/prod/release/app-prod-release.apk`
- AAB: `app/build/outputs/bundle/prodRelease/app-prod-release.aab`
- 버전 확인: `app/build/outputs/apk/prod/release/output-metadata.json`

## 검증 체크리스트

1. 출시 `versionName`을 갱신하고, CI 코드 또는 수동 `versionCode`가 기존 사용 코드와 충돌하지 않는지 확인한다.
2. `compileSdk`, `targetSdk`, AGP, CI SDK의 API 수준을 함께 확인한다.
3. 프로덕션 설정에 누락값이나 example placeholder가 없는지 확인한다.
4. `assembleProdRelease`와 `bundleProdRelease`가 성공해야 한다.
5. APK는 `apksigner verify`, AAB는 `jarsigner -verify`로 서명을 확인한다.
6. Play Console 업로드 전에 `output-metadata.json`의 버전명과 버전 코드를 다시 확인한다.

## API 수준 오류가 다시 나타날 때

Play Console에서 “API 수준 36 이상을 타겟팅해야 합니다”가 나오면 업로드한 AAB가 `targetSdk = 35`로 빌드된 것이다. 이전 산출물을 잘못 선택하지 않았는지 먼저 확인하고, API 36 설정으로 새 AAB를 빌드한다. 업로드가 수락되지 않은 버전 코드는 새로 올릴 필요가 없다.
