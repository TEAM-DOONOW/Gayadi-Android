# Commit Convention

## Structure

`<type>/#<issue-number>: <subject>`

An issue number is required. Do not invent one when it cannot be extracted from the branch; ask the user.

## Types

- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 수정
- `style`: 코드 포맷팅 등 동작 변경이 없는 수정
- `refactor`: 코드 리팩토링
- `test`: 테스트 코드 추가 또는 수정
- `chore`: 설정, 유지보수 또는 패키지 관리
- `add`: 에셋 추가
- `ci`: CI 설정 수정
- `build`: 빌드 관련 파일 수정
- `perf`: 성능 개선
- `design`: 디자인 작업

## Subject Rules

- Keep the subject within 50 characters.
- Do not end with a period or special character.
- For Korean, end with an action noun such as `구현`, `수정`, `추가`, `개선`, or `제거`.
- For English, start with an uppercase imperative verb such as `Add`, not `Added`.

## Examples

```text
feat/#10: Empty/Error 상태 화면 컴포넌트 구현
fix/#25: 로그인 토큰 갱신 오류 수정
docs/#35: README 설치 방법 수정
```
