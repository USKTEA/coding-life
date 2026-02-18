# Progress Log

## Session 2026-02-15

### Phase 1: Requirements & Discovery
- **Status:** complete
- **Started:** 10:00
- Actions taken:
  - 게임 규칙 정의: 표준 테트리스 가이드라인 기반
  - 기술 제약 확인: 순수 Kotlin, 외부 라이브러리 금지
  - 터미널 입력 방식 조사: `stty` + `System.in` raw read
  - ANSI escape code 동작 확인: macOS Terminal, iTerm2에서 정상
  - 7종 테트로미노 형태 및 색상 정의
- Files created/modified:
  - `.claude/docs/PL-001-tetris/tasks.md` (계획 수립)
  - `.claude/docs/PL-001-tetris/findings.md` (조사 결과 기록)

### Phase 2: Planning & Structure
- **Status:** complete
- **Started:** 11:00
- Actions taken:
  - 모듈 구조 설계: 7개 파일로 분리 (Main, Game, Board, Piece, Input, Renderer, Score)
  - 회전 시스템 결정: SRS (단순 회전 대비 UX 우수, 구현 비용 낮음)
  - 렌더링 결정: ANSI escape code (외부 의존성 금지 조건)
  - 깜빡임 방지: 더블 버퍼링 방식 채택
  - 게임 루프: Thread.sleep (Coroutine은 kotlinx 필요하므로 제외)
  - **과도함 검토**: 네트워크 대전 모드 제거 (YAGNI), 사운드 제거 (터미널 한계)
- Files created/modified:
  - `.claude/docs/PL-001-tetris/tasks.md` (Phase 2 업데이트)
  - `.claude/docs/PL-001-tetris/findings.md` (기술 결정 기록)

---

## Session 2026-02-16

### Phase 3: Implementation
- **Status:** complete
- **Started:** 09:00
- Actions taken:
  - `Piece.kt` 구현: 7종 테트로미노 정의, SRS 회전 매트릭스, 월킥 오프셋 테이블
  - `Board.kt` 구현: 10x20 그리드, 충돌 감지, 라인 클리어, 행 이동
  - `Score.kt` 구현: 점수 계산 (1줄=100, 2줄=300, 3줄=500, 4줄=800) × 레벨
  - `Input.kt` 구현: stty raw 모드, 방향키 escape sequence 파싱, 입력 큐
  - `Renderer.kt` 구현: ANSI 컬러 렌더링, 더블 버퍼링, 고스트 피스
  - `Game.kt` 구현: 게임 루프, 자동 낙하, 상태 관리 (PLAYING/GAME_OVER)
  - `Main.kt` 구현: 진입점, 터미널 설정/복원, shutdown hook
- Files created/modified:
  - `src/tetris/Piece.kt` (신규)
  - `src/tetris/Board.kt` (신규)
  - `src/tetris/Score.kt` (신규)
  - `src/tetris/Input.kt` (신규)
  - `src/tetris/Renderer.kt` (신규)
  - `src/tetris/Game.kt` (신규)
  - `src/tetris/Main.kt` (신규)

### Error Log (Session 2026-02-16)
| Timestamp | Error | Attempt | Resolution |
|-----------|-------|---------|------------|
| 09:45 | 회전 시 ArrayIndexOutOfBounds | 월킥 오프셋 테이블 구현 | 5개 위치 순서대로 시도, 모두 실패 시 회전 취소 |
| 11:30 | 라인 클리어 후 블록 안 내려옴 | 위→아래 순서 행 이동 | 아래쪽 행부터 처리하여 정상 동작 |
| 14:00 | Ctrl+C 시 터미널 복원 안 됨 | Runtime.addShutdownHook | 모든 종료 경로에서 stty sane 보장 |
| 15:20 | 렌더링 깜빡임 | 더블 버퍼링 도입 | 변경 셀만 업데이트, StringBuilder로 일괄 출력 |
| 16:00 | 하드 드롭 후 다음 피스 지연 | 같은 프레임 내 처리 | drop→lock→clear→spawn 순서를 한 프레임에 처리 |

---

## Session 2026-02-17

### Phase 4: Multi-perspective Review
- **Status:** complete
- **Started:** 09:00
- Actions taken:
  - 11개 관점 순차 검토 수행
  - Board 배열 접근 시 범위 검증 추가 (보안 검토)
  - 디버그용 printBoard() 제거 (불필요한 코드 검토)
  - WIDTH=10, HEIGHT=20 상수 추출 (코드 품질 검토)
  - 연쇄 검토 2회차에서 새 문제 없음 확인, 종료
- Files modified:
  - `src/tetris/Board.kt` (범위 검증 + 상수 추출)
  - `src/tetris/Game.kt` (디버그 코드 제거)

## Review Log
| 검토 단계 | 관점 | 결과 | 발견된 문제 |
|-----------|------|------|-------------|
| 1 | 목적 부합 | PASS | 7종 피스, 회전, 클리어, 점수 모두 정상 |
| 2 | 버그/보안 | ISSUE | Board 배열 범위 검증 누락 → require() 추가 |
| 3 | 개선 부작용 | PASS | 월킥 추가가 기존 회전에 영향 없음 |
| 4 | 함수/파일 크기 | PASS | Game.kt 120줄, 최대 파일도 적절 |
| 5 | 코드 통합/재사용 | PASS | Piece 회전 데이터 상수로 통합 완료 |
| 6 | 사이드 이펙트 | ISSUE | 터미널 raw 모드 복원 누락 가능성 → shutdown hook |
| 7 | 전체 변경 사항 | PASS | 전체 diff 정상 |
| 8 | 불필요한 코드 | ISSUE | 디버그용 printBoard() 잔존 → 제거 |
| 9 | 코드 품질 | ISSUE | 매직 넘버 산재 → WIDTH, HEIGHT 상수 추출 |
| 10 | 사용자 흐름 | PASS | 시작 → 플레이 → 게임오버 → 점수 표시 정상 |
| 11-1 | 연쇄 검토 1회차 | PASS | require() 추가로 인한 새 문제 없음 |
| 11-2 | 연쇄 검토 2회차 | PASS | 상수 추출로 인한 새 문제 없음, 종료 |

---

## Session 2026-02-17 (오후)

### Phase 5: Final Gate & Delivery
- **Status:** complete
- **Started:** 14:00
- Actions taken:
  - 배포 준비도 평가: 통과
  - 전체 플레이 테스트 수행: 시작→레벨3까지 플레이→게임오버 정상
  - progress.md 최종 기록
  - 커밋: `feat: implement console tetris in pure Kotlin`

## Test Results
| Test | Input | Expected | Actual | Status |
|------|-------|----------|--------|--------|
| 좌우 이동 | ← → 키 | 피스 좌우 1칸 이동 | 정상 이동 | PASS |
| 벽 충돌 | 왼쪽 벽에서 ← | 이동 안 됨 | 이동 안 됨 | PASS |
| SRS 회전 | ↑ 키 | 시계 방향 90도 회전 | 정상 회전 | PASS |
| 월킥 | 벽 근처에서 ↑ | 자동 위치 보정 후 회전 | 정상 월킥 | PASS |
| 소프트 드롭 | ↓ 키 | 1칸 빠른 낙하 | 정상 낙하 | PASS |
| 하드 드롭 | Space | 즉시 착지 | 즉시 착지 | PASS |
| 1줄 클리어 | 한 줄 완성 | 100 × 레벨 점수 | 정상 계산 | PASS |
| 4줄 클리어 | 4줄 동시 완성 | 800 × 레벨 점수 | 정상 계산 | PASS |
| 레벨 업 | 10줄 클리어 | 레벨 2, 속도 증가 | 정상 전환 | PASS |
| 게임 오버 | 블록 쌓여서 꼭대기 도달 | GAME_OVER 표시 | 정상 표시 | PASS |
| 고스트 피스 | 플레이 중 | 착지 위치에 반투명 표시 | 정상 표시 | PASS |
| 종료 | q 키 | 게임 종료, 터미널 복원 | 정상 종료 | PASS |

## 5-Question Reboot Check
| Question | Answer |
|----------|--------|
| 1. 현재 어느 단계인가? | Phase 5 - 완료 |
| 2. 다음에 할 일은? | PR 리뷰 대기 |
| 3. 목표는? | 순수 Kotlin 콘솔 테트리스 구현 |
| 4. 지금까지 배운 것? | See findings.md (SRS, ANSI, 더블 버퍼링) |
| 5. 완료한 작업은? | 전체 구현 + 11단계 검토 + 배포 준비 완료 |
