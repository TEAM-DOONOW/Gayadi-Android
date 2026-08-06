# Gayadi Android Architecture

이 프로젝트는 기능 및 계층별 Gradle 멀티모듈과 클린 아키텍처를 사용합니다.

## Dependency rule

```text
app ──> di ──> data ──> domain
 │       └─────────────> domain
 ├──> feature/* ───────> domain
 │          └──────────> core:designsystem
 └──> core:designsystem

feature:home/trip/mypage ──> core:ui ──> core:designsystem
```

- `domain`: Android 프레임워크에 의존하지 않는 모델, Repository 계약, UseCase
- `data`: Domain Repository를 구현하고 실제 또는 Mock 데이터 출처를 관리
- `feature/*`: 기능별 ViewModel, UiState, Compose 화면
- `core:designsystem`: 공통 색상, 타이포그래피, 테마 및 폰트
- `core:ui`: 여러 기능이 공유하는 UI 컴포넌트
- `di`: 앱 시작 시 Data 구현체와 Domain UseCase를 조립하는 composition root
- `app`: 애플리케이션 진입점, 화면 전환 및 ViewModel 생명주기 관리

Domain 계층은 Data 및 Presentation 계층을 참조하지 않습니다. Presentation 계층은 Data Repository 구현체에 의존하지 않고, Domain Model 및 UseCase 같은 Domain 추상화에만 의존합니다.

## Module structure

```text
GayadiAndroid
├── app
├── domain
├── data
├── di
├── core
│   ├── designsystem
│   └── ui
└── feature
    ├── auth
    ├── basicinfo
    ├── survey
    ├── surveyresult
    ├── home
    ├── trip
    └── mypage
```

## State management

각 기능의 ViewModel은 단일 `StateFlow<UiState>`를 외부에 노출하고 `UiEvent`를 통해 사용자 입력을 처리합니다. Compose Route는 생명주기를 인식해 상태를 수집하고, 상태 없는 Screen 컴포저블에 이벤트와 상태를 전달합니다.

DataSource는 원본 DTO 또는 Entity를 반환하며 Repository 구현체가 Mapper를 이용해 Domain Model로 변환합니다. 여행 설문은 `FirestoreSurveyDataSource`가 비동기 콜백으로 데이터를 제공하고, Presentation은 Firebase SDK를 직접 참조하지 않습니다.

## Tests

- Mapper 및 Repository 변환 테스트
- UseCase 단위 테스트
- 기본 정보 입력 제한과 제출 상태 테스트
- 설문 시작, 선택, 질문 전환, 완료 상태 테스트
- 8개 유형 점수 계산 및 결과 화면 상태 테스트

## Dependency boundaries

- `domain`은 Android 및 다른 프로젝트 모듈에 의존하지 않습니다.
- `data`는 `domain`의 Repository 계약을 구현합니다.
- 기능 모듈은 필요한 경우에만 `domain`과 공통 UI 모듈에 의존합니다.
- 기능 모듈끼리는 직접 의존하지 않으며 화면 연결은 `app`의 navigation에서 담당합니다.
- `app`은 구현을 직접 생성하지 않고 `di`의 composition root를 사용합니다.
