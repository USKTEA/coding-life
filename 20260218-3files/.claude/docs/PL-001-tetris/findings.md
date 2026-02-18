# Findings & Decisions

## Requirements
- [x] 7종 표준 테트로미노 (I, O, T, S, Z, L, J)
- [x] 좌우 이동, 회전, 소프트 드롭, 하드 드롭
- [x] 라인 클리어 및 점수 계산
- [x] 레벨 시스템 (10줄 클리어마다 레벨 업, 속도 증가)
- [x] 다음 피스 미리보기
- [x] 고스트 피스 (착지 위치 표시)
- [x] 게임 오버 조건 (새 피스 spawn 불가 시)
- [x] 외부 의존성 없음 (순수 Kotlin)

## Research Findings

### 터미널 입력 처리
- Java/Kotlin 표준 라이브러리에 raw 키 입력 API 없음
- `stty -icanon min 1 -echo` 명령으로 터미널을 raw 모드로 전환 가능
- `Runtime.getRuntime().exec()` 로 stty 호출
- 방향키는 3바이트 escape sequence: `ESC [ A` (↑), `ESC [ B` (↓), `ESC [ C` (→), `ESC [ D` (←)
- **주의**: 게임 종료 시 반드시 `stty sane`으로 터미널 복원 필요

### ANSI Escape Code
- 커서 이동: `\u001b[{row};{col}H`
- 색상: `\u001b[{color}m` (31=빨강, 32=초록, 33=노랑, 34=파랑, 35=자홍, 36=청록, 37=흰색)
- 배경색: `\u001b[4{n}m`
- 화면 지우기: `\u001b[2J`
- 커서 숨기기: `\u001b[?25l`, 보이기: `\u001b[?25h`
- **한계**: Windows cmd는 ANSI 미지원 (Windows Terminal은 지원)

### 테트리스 회전 시스템 (SRS)
- Super Rotation System: 공식 테트리스 가이드라인의 표준 회전 방식
- 4가지 회전 상태: 0 → R → 2 → L → 0
- 월킥: 기본 회전 위치가 충돌하면 최대 4개의 대체 위치를 순서대로 시도
- I 피스는 별도 월킥 테이블 사용 (다른 피스와 다름)
- 참고: https://tetris.wiki/Super_Rotation_System

### 게임 속도
- 레벨 1: 1000ms 간격 (1초에 1칸 낙하)
- 레벨별 공식: `max(100, 1000 - (level - 1) * 80)` ms
- 레벨 12 이상: 100ms 고정 (최대 속도)

## Technical Decisions

| Decision | Options Considered | Chosen | Rationale |
|----------|-------------------|--------|-----------|
| 회전 시스템 | ① 단순 90도 회전 ② SRS (표준) | **② SRS** | 단순 회전은 벽 근처에서 회전 불가 → UX 나쁨. SRS + 월킥이 표준이고, 오프셋 테이블만 추가하면 되어 구현 비용 낮음 |
| 렌더링 | ① println 매 프레임 ② ANSI escape code | **② ANSI** | println은 스크롤 발생하여 게임 불가. ANSI로 커서 이동 + 제자리 덮어쓰기 필수 |
| 깜빡임 방지 | ① 전체 다시 그리기 ② 더블 버퍼링 (변경 셀만) | **② 더블 버퍼링** | 매 프레임 전체 출력 시 눈에 보이는 깜빡임 발생. 이전 프레임과 diff하여 변경된 셀만 업데이트 |
| 게임 루프 | ① Thread.sleep ② ScheduledExecutor ③ Coroutine | **① Thread.sleep** | 이 규모에서 ScheduledExecutor는 과도. Coroutine은 kotlinx 의존성 필요 (외부 라이브러리 금지). Thread.sleep으로 충분 |
| 입력 처리 | ① 메인 스레드 polling ② 별도 스레드 blocking read | **② 별도 스레드** | System.in.read()는 blocking. 메인 스레드에서 호출하면 게임 루프 정지. 별도 daemon 스레드에서 읽고 큐에 적재 |
| 피스 데이터 | ① 2D Boolean 배열 ② 좌표 리스트 ③ 비트마스크 | **① 2D 배열** | 회전 시 행렬 변환이 직관적. 좌표 리스트는 회전 계산 복잡. 비트마스크는 가독성 나쁨 |
| 고스트 피스 | ① 구현 ② 미구현 | **① 구현** | 하드 드롭 착지 위치를 보여줘야 UX 좋음. 현재 피스를 바닥까지 시뮬레이션하면 되어 구현 비용 낮음 |
| 네트워크 대전 | ① 구현 ② 미구현 | **② 미구현** | 과도함 검토에서 YAGNI 판정. 소켓 통신, 동기화 등 복잡도 대비 데모 목적에 불필요 |

## Issues Encountered

### 1. 회전 시 ArrayIndexOutOfBounds
**문제**: 피스가 오른쪽 벽에 붙어있을 때 시계 방향 회전하면 배열 범위 초과
**원인**: 회전된 좌표가 Board 범위(0..9) 밖으로 나감
**해결**: 월킥 오프셋 테이블 구현. 기본 위치 충돌 시 최대 4개 대체 위치 순서대로 시도. 모두 실패하면 회전 취소
**결과**: 벽 근처, 바닥 근처 모든 위치에서 자연스러운 회전 가능

### 2. 라인 클리어 후 블록이 내려오지 않음
**문제**: 꽉 찬 줄을 제거했지만, 위의 블록들이 그대로 떠 있음
**원인**: 행 제거만 하고 위 행을 아래로 이동하는 로직 누락
**해결**: 클리어된 행 위의 모든 행을 아래로 한 줄씩 복사. 아래쪽 행부터 처리하여 덮어쓰기 방지
**결과**: 동시 4줄 클리어(테트리스)까지 정상 동작 확인

### 3. 게임 종료 시 터미널 복원 안 됨
**문제**: Ctrl+C로 종료하면 터미널이 raw 모드로 남아서 입력이 깨짐
**원인**: 정상 종료 경로에서만 `stty sane` 호출. 시그널 종료 시 미호출
**해결**: `Runtime.addShutdownHook`으로 종료 훅 등록. 정상 종료 + Ctrl+C + 예외 모두에서 터미널 복원 보장
**결과**: 어떤 방식으로 종료해도 터미널 정상 복원

### 4. 렌더링 깜빡임
**문제**: 매 프레임마다 화면을 지우고 다시 그리면 눈에 보이는 깜빡임 발생
**원인**: `\u001b[2J` (화면 지우기) + 전체 다시 그리기 사이에 빈 화면이 노출됨
**해결**: 더블 버퍼링 도입. 이전 프레임 상태를 저장하고, 변경된 셀만 ANSI 커서 이동으로 업데이트. 전체 출력을 StringBuilder에 모아 한 번에 flush
**결과**: 깜빡임 완전 제거

### 5. 하드 드롭 후 다음 피스 지연
**문제**: 스페이스바 하드 드롭 후 다음 피스가 한 프레임 늦게 등장
**원인**: 하드 드롭 → lock → 다음 프레임에서 spawn 순서로 되어 있어 1프레임 공백
**해결**: 하드 드롭 → lock → 라인 클리어 → spawn을 같은 프레임 내에서 처리
**결과**: 하드 드롭 후 즉시 다음 피스 등장

## Review Findings

| 검토 관점 | 발견 사항 | 조치 |
|-----------|-----------|------|
| 목적 부합 | 7종 피스, 회전, 클리어, 점수 모두 정상 | 없음 |
| 버그/보안 | Board 배열 접근 시 범위 검증 일부 누락 | require() 추가 |
| 개선 부작용 | 월킥 추가로 기존 회전 영향 없음 확인 | 없음 |
| 사이드 이펙트 | 터미널 raw 모드 복원 누락 가능성 | shutdown hook 추가 |
| 불필요한 코드 | 디버그용 printBoard() 잔존 | 제거 |
| 코드 품질 | 매직 넘버(10, 20) 산재 | WIDTH, HEIGHT 상수 추출 |

## Resources
- Tetris Guideline: https://tetris.wiki/Tetris_Guideline
- SRS 회전: https://tetris.wiki/Super_Rotation_System
- ANSI Escape Codes: https://en.wikipedia.org/wiki/ANSI_escape_code
- Kotlin Thread: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.concurrent/thread.html
