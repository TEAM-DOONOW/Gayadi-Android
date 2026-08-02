# Firebase 여행 성향 설문

## 데이터 위치

- 프로젝트: `gayadi`
- 리전: `asia-northeast3` (서울)
- 설문: `surveys/travel-personality-v1`
- 문항: `surveys/travel-personality-v1/questions/{q01..q09}`
- 결과 유형: `surveys/travel-personality-v1/results/{PNA..SCR}`

원본과 동일한 데이터는 `firebase-data/travel-personality-v1.json`에서 확인할 수 있습니다.

Android 앱은 `FirestoreSurveyDataSource`에서 데이터를 읽고 `DefaultSurveyRepository`와 Domain UseCase를 거쳐 설문·결과 ViewModel에 전달합니다. Presentation 계층은 Firebase SDK에 직접 의존하지 않습니다.

## 결과 계산

각 문항에서 선택한 `code`를 차원별로 집계합니다.

1. `preparation`: `P`와 `S` 중 많은 코드
2. `place`: `N`과 `C` 중 많은 코드
3. `energy`: `A`와 `R` 중 많은 코드
4. 위 순서대로 코드를 합쳐 결과 문서 ID를 조회

각 차원에 문항이 3개라 동점은 발생하지 않습니다. 예를 들어 `S`, `C`, `A`가 우세하면 `results/SCA`를 조회합니다.

## 보안 정책

설문과 하위 문항·결과는 앱에서 로그인 없이 읽을 수 있습니다. 클라이언트 쓰기는 차단되어 있으며, 내용 변경은 Firebase Console 권한이 있는 팀원이 수행해야 합니다. 그 밖의 모든 Firestore 경로는 기본 차단합니다.

최초 적재 및 규칙 배포 전, Firebase CLI와 Google ADC에 Firebase 프로젝트 권한이 있는 계정으로 로그인합니다.

```bash
npx firebase-tools login:add
npx firebase-tools login:use <account-email>
gcloud auth application-default login
```

그다음 아래 스크립트를 실행합니다. seed 작업은 같은 문서 ID에 upsert하므로 재실행해도 중복 문서가 생기지 않습니다. 실제 기본 데이터베이스가 `asia-northeast3`인지 먼저 확인하고, 다른 경우 데이터를 쓰지 않고 실패합니다.

```bash
scripts/deploy_firestore_survey.sh
```

데이터를 쓰지 않고 JSON 구조와 적재 문서 수만 확인하려면 다음 명령을 사용합니다.

```bash
python3 scripts/seed_firestore_survey.py --dry-run
```

서비스 계정 키와 Firebase Admin 인증 파일은 저장소에 올리지 않습니다.
