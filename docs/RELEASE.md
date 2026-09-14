# Android 릴리즈 가이드

## 버전 기준

- 버전은 `app/build.gradle.kts`의 `versionName`과 `versionCode`에서 관리한다.
- 새 릴리즈는 Play Console에 마지막으로 등록된 버전을 기준으로 두 값을 각각 한 단계 올린다.
- 원격 브랜치의 값이 실제 마지막 릴리즈보다 낮을 수 있으므로 원격 값만 보고 버전을 정하지 않는다.
- pull, stash, reset 전에 로컬 릴리즈 설정과 마지막 산출물의 `output-metadata.json`을 확인한다.
- 2026-09-12 릴리즈 준비 버전은 `0.0.11`, 버전 코드는 `11`이다.

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

- `config/prod.properties`: API URL과 앱 SDK 키
- `keystore.properties`: release 키스토어 경로와 서명 정보
- 실제 키스토어 파일

필수 프로덕션 속성 이름은 `config/prod.properties.example`을 기준으로 한다. 실제 값, 비밀번호, 키스토어는 커밋하거나 빌드 로그에 출력하지 않는다. 기존 키 파일을 삭제하거나 새 키로 교체하면 Play Console에서 기존 앱 업데이트가 불가능할 수 있다.

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

패키지 이름은 `com.doonow.gayadi`이다. 앱은 디버그/릴리즈 Android 클라이언트로 서명되고, Google 토큰 요청의 `serverClientId`는 웹 클라이언트 ID를 쓴다.

| 빌드 | Android 클라이언트 ID | SHA-1 |
| --- | --- | --- |
| debug | `6035741280-g9agek5bfnkprhp9ubqklb2ustbjd8ld.apps.googleusercontent.com` | `B4:5B:35:CD:37:FB:F7:E2:6E:D0:B8:3D:2E:D6:F5:B5:85:F7:52:94` |
| release | `6035741280-jedtnq850vigud4osf3ce6223i4abbe4.apps.googleusercontent.com` | `08:CB:66:B5:60:AB:2E:5F:9A:49:B6:F2:99:FB:41:DD:34:1B:35:AD` |

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

1. 마지막 배포 버전에서 `versionName`과 `versionCode`를 각각 올린다.
2. `compileSdk`, `targetSdk`, AGP, CI SDK의 API 수준을 함께 확인한다.
3. 프로덕션 설정에 누락값이나 example placeholder가 없는지 확인한다.
4. `assembleProdRelease`와 `bundleProdRelease`가 성공해야 한다.
5. APK는 `apksigner verify`, AAB는 `jarsigner -verify`로 서명을 확인한다.
6. Play Console 업로드 전에 `output-metadata.json`의 버전명과 버전 코드를 다시 확인한다.

## API 수준 오류가 다시 나타날 때

Play Console에서 “API 수준 36 이상을 타겟팅해야 합니다”가 나오면 업로드한 AAB가 `targetSdk = 35`로 빌드된 것이다. 이전 산출물을 잘못 선택하지 않았는지 먼저 확인하고, API 36 설정으로 새 AAB를 빌드한다. 업로드가 수락되지 않은 버전 코드는 새로 올릴 필요가 없다.
