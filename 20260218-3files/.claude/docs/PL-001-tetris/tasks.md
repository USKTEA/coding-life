# Project: [PL-001] 콘솔 테트리스

## Goal
외부 의존성 없이 순수 Kotlin으로 콘솔 테트리스를 구현한다.
ANSI escape code로 컬러 렌더링하고, 표준 테트리스 규칙(SRS 회전, 월킥, 레벨 시스템)을 따른다.

## Current Phase
Phase 5: Final Gate & Delivery

## Phases

### Phase 1: Requirements & Discovery
- [x] 게임 규칙 정의: 표준 테트리스 (Tetris Guideline)
- [x] 기술 제약 확인: 순수 Kotlin, 외부 라이브러리 금지
- [x] 플랫폼 확인: macOS/Linux 터미널 (ANSI 지원 필수)
- [x] 입력 방식 조사: 터미널 raw 모드로 실시간 키 입력
- **Status:** complete

### Phase 2: Planning & Structure
- [x] 모듈 구조 설계: Main, Game, Board, Piece, Input, Renderer, Score
- [x] 회전 시스템 결정: SRS (Super Rotation System)
- [x] 렌더링 방식 결정: ANSI escape code + 더블 버퍼링
- [x] 게임 루프 방식 결정: Thread.sleep 기반
- [x] 과도함 검토: 네트워크 대전 모드 제거, 사운드 제거 (YAGNI)
- **Status:** complete

### Phase 3: Implementation
- [x] Board: 10x20 그리드, 충돌 감지, 라인 클리어
- [x] Piece: 7종 테트로미노, SRS 회전, 월킥
- [x] Game: 게임 루프, 상태 관리, 자동 낙하
- [x] Input: 터미널 raw 모드, 키보드 입력 스레드
- [x] Renderer: ANSI 컬러 렌더링, 더블 버퍼링
- [x] Score: 점수 계산, 레벨 시스템, 속도 증가
- [x] Main: 진입점, 터미널 설정/복원
- **Status:** complete

### Phase 4: Multi-perspective Review
- [x] 목적 부합: 7종 피스, 회전, 라인 클리어, 점수 모두 동작
- [x] 버그/보안: 배열 인덱스 범위 검증 강화
- [x] 개선 부작용: 월킥 추가 후 기존 회전 정상 동작 확인
- [x] 함수/파일 크기: Game.kt 120줄, 적절
- [x] 코드 통합/재사용: Piece의 회전 데이터를 상수로 통합
- [x] 사이드 이펙트: 터미널 raw 모드 복원 누락 가능성 → shutdown hook 추가
- [x] 전체 변경 사항 통합 검토
- [x] 불필요한 코드: 디버그용 printBoard() 제거
- [x] 코드 품질: Kotlin 컨벤션 준수
- [x] 사용자 흐름: 시작 → 플레이 → 게임오버 → 점수 표시 정상
- [x] 연쇄 검토: 2회차에서 새 문제 없음, 종료
- **Status:** complete

### Phase 5: Final Gate & Delivery
- [x] 전체 테스트 확인
- [x] progress.md 기록 완료
- [x] 커밋 및 PR 작성
- **Status:** complete

## Key Questions
1. ~~회전 시스템을 어떤 걸 쓸지?~~ → SRS (findings.md 참고)
2. ~~콘솔 렌더링을 어떻게 할지?~~ → ANSI escape code + 더블 버퍼링
3. ~~게임 루프를 어떻게 구현할지?~~ → Thread.sleep + 별도 입력 스레드
4. ~~Windows 지원 필요한지?~~ → 불필요 (macOS/Linux만)

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| SRS 회전 | 단순 회전은 벽 근처에서 UX 나쁨. SRS + 월킥이 표준이고 구현 비용 낮음 |
| ANSI 렌더링 | 외부 의존성 금지 조건. ncurses 등 불가. ANSI는 표준 터미널에서 지원 |
| 더블 버퍼링 | 매 프레임 전체 다시 그리면 깜빡임 심함. 변경 셀만 업데이트로 해결 |
| Thread.sleep 루프 | ScheduledExecutor는 이 규모에서 과도. sleep으로 충분히 정확 |
| 월킥 구현 | 월킥 없이 벽 근처 회전 불가 → 유저 불만 예상. 표준 오프셋 테이블로 해결 |
| 네트워크 대전 제거 | 과도함 검토에서 YAGNI 판정. 싱글 플레이로 충분 |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
| 회전 시 ArrayIndexOutOfBounds | 월킥 오프셋 적용 전 범위 체크 추가 | 5개 월킥 위치를 순서대로 시도, 모두 실패 시 회전 취소 |
| 라인 클리어 후 위 블록 안 내려옴 | 클리어 후 위→아래 순서로 행 이동 | 클리어된 행 위의 모든 행을 한 줄씩 아래로 복사 |
| 터미널 raw 모드 복원 안 됨 | Runtime.addShutdownHook으로 복원 | 정상 종료 + Ctrl+C 모두에서 `stty sane` 실행 보장 |
| 빠른 키 입력 시 프레임 스킵 | 입력 큐에 최신 1개만 유지 | ConcurrentLinkedQueue 사용, 프레임마다 큐 drain 후 마지막 입력만 처리 |
| 하드 드롭 후 다음 피스 즉시 등장 안 함 | lock → spawn 순서 수정 | 하드 드롭 → 즉시 lock → 라인 클리어 → 새 피스 spawn 순서로 변경 |

## Notes
- Phase 상태를 업데이트하세요: pending -> in_progress -> complete
- 중요한 결정 전에 이 계획을 다시 읽으세요 (attention manipulation)
- 모든 오류를 기록하세요 - 같은 실수를 반복하지 않습니다
