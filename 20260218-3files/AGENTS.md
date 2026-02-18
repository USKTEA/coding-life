# Agents Documentation

> 이 파일은 Claude Code가 프로젝트를 이해하고 작성한 아키텍처 문서입니다.
> 코드베이스 변경 시 자동으로 업데이트됩니다.

## Project Overview

콘솔 기반 테트리스 게임. 외부 의존성 없이 순수 Kotlin + ANSI escape code로 구현.

- **Language**: Kotlin 1.9
- **Dependencies**: 없음 (순수 Kotlin 표준 라이브러리만 사용)
- **Build**: kotlinc 직접 컴파일 또는 Gradle
- **Platform**: macOS / Linux (ANSI 터미널 필요)

## Architecture

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│  Input   │────▶│   Game   │────▶│ Renderer │
│(키보드)   │     │ (루프)    │     │(콘솔출력) │
└──────────┘     └──────────┘     └──────────┘
                      │
              ┌───────┼───────┐
              ▼       ▼       ▼
         ┌────────┐ ┌─────┐ ┌───────┐
         │ Board  │ │Piece│ │ Score │
         │(10x20) │ │(SRS)│ │(레벨)  │
         └────────┘ └─────┘ └───────┘
```

## Module Structure

```
src/
└── tetris/
    ├── Main.kt        # 진입점, 터미널 설정, 게임 시작
    ├── Game.kt        # 게임 루프, 상태 관리, 타이머
    ├── Board.kt       # 10x20 그리드, 충돌 감지, 라인 클리어
    ├── Piece.kt       # 7종 테트로미노 정의, SRS 회전, 월킥
    ├── Input.kt       # 터미널 raw 모드, 키 입력 처리
    ├── Renderer.kt    # ANSI escape code 렌더링, 더블 버퍼링
    └── Score.kt       # 점수 계산, 레벨 시스템
```

## Key Patterns

### Game Loop
- 메인 스레드: 게임 루프 (자동 낙하 + 상태 업데이트 + 렌더링)
- 별도 스레드: 키보드 입력 대기 (Input.kt)
- `Thread.sleep` 기반 프레임 제어 (ScheduledExecutor 대비 단순함)

### Rotation: SRS (Super Rotation System)
- 표준 테트리스 회전 규칙 적용
- 월킥(Wall Kick): 벽 근처 회전 시 자동 위치 보정
- 회전 불가 시 원래 상태 유지

### Rendering
- ANSI escape code로 색상 및 커서 제어
- 더블 버퍼링: 이전 프레임과 비교하여 변경된 셀만 업데이트
- 프레임 전체를 StringBuilder에 모아서 한 번에 출력 (깜빡임 방지)

### Collision Detection
- 이동/회전 전 Board에 충돌 검사
- out-of-bounds + 기존 블록 겹침 동시 체크

## Controls

| Key | Action |
|-----|--------|
| ← → | 좌우 이동 |
| ↑ | 회전 (시계 방향) |
| ↓ | 소프트 드롭 (빠른 낙하) |
| Space | 하드 드롭 (즉시 착지) |
| q | 게임 종료 |

## Scoring

| Action | Points |
|--------|--------|
| 1줄 클리어 | 100 × 레벨 |
| 2줄 클리어 | 300 × 레벨 |
| 3줄 클리어 | 500 × 레벨 |
| 4줄 클리어 (테트리스) | 800 × 레벨 |
| 소프트 드롭 | 1 × 줄 수 |
| 하드 드롭 | 2 × 줄 수 |

## Related Documents

- [CLAUDE.md](CLAUDE.md): 3-File Pattern 워크플로우 정의
- [.claude/docs/PL-001-tetris/](.claude/docs/PL-001-tetris/): 구현 작업 기록
