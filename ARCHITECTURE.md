# Gayadi Android Architecture

이 프로젝트는 단일 앱 모듈 안에서 기능별 패키지와 클린 아키텍처 계층을 사용합니다.

## Dependency rule

```text
presentation ──> domain <── data
       │                     │
       └──── composition root┘
```

- `domain`: Android 프레임워크에 의존하지 않는 모델, Repository 계약, UseCase
- `data`: Domain Repository를 구현하고 실제 또는 Mock 데이터 출처를 관리
- `feature/*/presentation`: ViewModel, UiState, Compose 화면
- `di`: 앱 시작 시 Data 구현체와 Domain UseCase를 조립하는 composition root
- `navigation`: 화면 전환과 ViewModel 생명주기 관리

Domain 계층은 Data 및 Presentation 계층을 참조하지 않습니다. Presentation 계층은 Data Repository 구현체에 의존하지 않고, Domain Model 및 UseCase 같은 Domain 추상화에만 의존합니다.

## Package structure

```text
com.gayadi.android
├── data
│   ├── datasource
│   ├── mapper
│   ├── model
│   └── repository
├── domain
│   ├── model
│   ├── repository
│   └── usecase
├── feature
│   ├── basicinfo
│   │   └── presentation
│   ├── survey
│   │   └── presentation
│   └── surveyresult
│       └── presentation
├── di
├── navigation
└── ui
    ├── components
    ├── screens
    └── theme
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

## Migration status

- 기본 정보 입력: Domain, Data, Presentation 분리 완료
- 여행 성향 설문·결과: Firestore DataSource, Domain UseCase, Presentation 분리 완료
- 나머지 화면: 현재 정적 UI 또는 Mock 화면으로, 비즈니스 로직 및 데이터 연동 도입 시 동일한 구조로 이전

멀티 모듈은 기능 수와 빌드 시간이 증가할 때 도입합니다. 현재는 단일 모듈로 계층 경계를 유지해 초기 복잡도를 제한합니다.
