# Firebase 여행 성향 설문

## 데이터 위치

- 프로젝트: `gayadi`
- 리전: `asia-northeast3` (서울)
- 설문: `surveys/travel-personality-v1`
- 문항: `surveys/travel-personality-v1/questions/{q01..q09}`
- 결과 유형: `surveys/travel-personality-v1/results/{PNA..SCR}`

원본과 동일한 데이터는 `firebase-data/travel-personality-v1.json`에서 확인할 수 있습니다.

## 결과 계산

각 문항에서 선택한 `code`를 차원별로 집계합니다.

1. `preparation`: `P`와 `S` 중 많은 코드
2. `place`: `N`과 `C` 중 많은 코드
3. `energy`: `A`와 `R` 중 많은 코드
4. 위 순서대로 코드를 합쳐 결과 문서 ID를 조회

각 차원에 문항이 3개라 동점은 발생하지 않습니다. 예를 들어 `S`, `C`, `A`가 우세하면 `results/SCA`를 조회합니다.

## 보안 정책

설문과 하위 문항·결과는 앱에서 로그인 없이 읽을 수 있습니다. 클라이언트 쓰기는 차단되어 있으며, 내용 변경은 Firebase Console 권한이 있는 팀원이 수행해야 합니다. 그 밖의 모든 Firestore 경로는 기본 차단합니다.

규칙 배포:

```bash
npx firebase-tools deploy --only firestore:rules --project gayadi --account teamsda01@gmail.com
```

서비스 계정 키와 Firebase Admin 인증 파일은 저장소에 올리지 않습니다.
