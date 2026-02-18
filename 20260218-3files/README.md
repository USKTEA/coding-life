# 3-File Pattern: Claude Code + CodeRabbit 워크플로우

> 프로젝트에 3가지 파일을 추가하면, **코드 고고학이 쉬워지고** **AI 코드 리뷰가 똑똑해집니다.**

---

## TL;DR

| 파일 | 역할 | 누가 읽나 |
|------|------|-----------|
| **CLAUDE.md** | 프로젝트 컨벤션·규칙 | Claude Code, CodeRabbit, 팀원 |
| **AGENTS.md** | AI가 이해한 아키텍처 문서 | Claude Code, CodeRabbit, 팀원 |
| **.claude/docs/{branch}/** | 기능별 계획·발견·진행 기록 | Claude Code, 미래의 나, 미래의 팀원 |

→ 팀 전원이 **동일한 컨텍스트**로 작업하고, 6개월 뒤에도 **"왜 이렇게 만들었지?"** 에 답할 수 있습니다.

---

## 우리가 겪는 문제

### 문제 1: 코드 고고학의 한계

```
# 6개월 뒤, 새 팀원이 코드를 보며...

"이 NotificationService에서 왜 Redis 대신 DB 큐를 썼지?"
"PR #342에 이유가 있을 텐데... PR 설명이 '알림 기능 추가'뿐이네"
"커밋 메시지도 'feat: add notification api'가 전부..."
"Slack 뒤져봐야 하나? 그때 담당자가 누구지?"
```

**현실**: PR 설명과 커밋 메시지만으로는 **기술적 의사결정의 맥락**을 복원할 수 없습니다.

### 문제 2: AI 리뷰가 맥락을 모른다

```yaml
# CodeRabbit이 프로젝트 컨텍스트 없이 리뷰하면...

❌ "이 메서드는 너무 깁니다. 분리를 고려하세요."     # 우리 기준에서는 적절한 길이
❌ "에러 핸들링을 추가하세요."                       # 글로벌 핸들러가 이미 있음
❌ "테스트를 추가하세요."                            # 이 레이어는 통합 테스트로 커버하는 정책
```

**현실**: 프로젝트 맥락 없는 AI 리뷰는 **generic하고 noise가 많습니다.**

---

## 해결: 3-File Pattern

### 파일 구조

```
your-project/
├── CLAUDE.md                         ← 프로젝트 규칙 (팀이 작성)
├── AGENTS.md                         ← 아키텍처 문서 (AI가 자동 생성)
└── .claude/
    └── docs/
        ├── PL-001-tetris/            ← 기능 A 작업 기록
        │   ├── tasks.md
        │   ├── findings.md
        │   └── progress.md
        └── PL-002-multiplayer/       ← 기능 B 작업 기록
            ├── tasks.md
            ├── findings.md
            └── progress.md
```

### 각 파일의 역할

| 파일 | 작성 주체 | 내용 | 수명 |
|------|-----------|------|------|
| **CLAUDE.md** | 사람 (+ AI 보조) | 프로젝트 규칙, 컨벤션, 아키텍처 원칙 | 프로젝트 전체 |
| **AGENTS.md** | AI (Claude Code) | 코드베이스 구조, 모듈 관계, 기술 스택 | 프로젝트 전체 (자동 업데이트) |
| **tasks.md** | AI (작업 중) | 작업 계획, 단계, 에러 로그 | 기능/브랜치 단위 |
| **findings.md** | AI (작업 중) | 기술적 발견, **의사결정 근거** | 기능/브랜치 단위 |
| **progress.md** | AI (작업 중) | 세션별 작업 내역, 테스트 결과 | 기능/브랜치 단위 |

---

## 장점 1: 코드 고고학이 쉬워진다

### Before: 3-File 없이

```
Q: "테트리스 회전에서 왜 단순 90도 회전 대신 SRS를 썼지?"

탐색 경로:
  git log --oneline src/tetris/  → "feat: implement console tetris" (정보 없음)
  PR #15 설명                    → "테트리스 구현" (정보 없음)
  Slack 검색                     → 6개월 전 대화 유실
  담당자에게 물어보기              → 퇴사함 😇

결론: 알 수 없음. 건드리기 무서우니 그냥 두자...
```

### After: 3-File 적용

```
Q: "테트리스 회전에서 왜 단순 90도 회전 대신 SRS를 썼지?"

탐색 경로:
  .claude/docs/PL-001-tetris/findings.md 열기

  → ## Technical Decisions
    | Decision    | Options Considered         | Chosen  | Rationale                              |
    |-------------|---------------------------|---------|----------------------------------------|
    | 회전 시스템  | ① 단순 90도 ② SRS (표준)   | ② SRS  | 단순 회전은 벽 근처에서 회전 불가 → UX 나쁨. |
    |             |                           |         | SRS + 월킥이 표준. 오프셋 테이블만 추가.    |

결론: 명확한 근거 확인 ✅ 대안도 검토한 이력 확인 ✅
```

### 에러 히스토리도 남는다

```
Q: "렌더링에서 왜 더블 버퍼링을 쓰지? 그냥 매번 다시 그리면 안 되나?"

.claude/docs/PL-001-tetris/findings.md 열기

→ ## Issues Encountered
  ### 4. 렌더링 깜빡임
  문제: 매 프레임마다 화면 지우고 다시 그리면 깜빡임 발생
  원인: 화면 지우기와 다시 그리기 사이에 빈 화면이 노출됨
  해결: 더블 버퍼링 도입. 변경 셀만 ANSI 커서로 업데이트. StringBuilder로 일괄 flush
  결과: 깜빡임 완전 제거

결론: 실제 문제를 겪고 해결한 이력이 있음 ✅ 되돌리면 안 됨 ✅
```

**핵심**: `findings.md`가 **의사결정의 이유**를, `tasks.md`가 **시행착오의 이력**을 영구 보존합니다.

---

## 장점 2: CodeRabbit이 더 똑똑해진다

### CodeRabbit이 읽는 파일

CodeRabbit은 별도 설정 없이 프로젝트의 `CLAUDE.md`와 `AGENTS.md`를 **자동으로** 인식하고 참조합니다.

### Before: 컨텍스트 없는 리뷰

```
CodeRabbit:
  ❌ "Game.kt가 120줄입니다. 분리를 고려하세요."
  ❌ "Thread.sleep 대신 coroutine을 사용하세요."
  ❌ "매직 넘버 16을 상수로 추출하세요."
```

→ 프로젝트 맥락을 모르는 **generic한 지적** (noise)

### After: 컨텍스트 있는 리뷰

```
CodeRabbit (CLAUDE.md + AGENTS.md 참조):
  ✅ "AGENTS.md에 Thread.sleep 기반 루프를 선택한 이유가 명시되어 있습니다.
      Coroutine은 kotlinx 의존성이 필요하여 '외부 라이브러리 금지' 조건에 맞지 않습니다."
  ✅ "SRS 월킥 오프셋 테이블이 표준과 일치하는지 확인이 필요합니다.
      (AGENTS.md의 Rotation: SRS 섹션 참고)"
  ✅ "Renderer의 더블 버퍼링 패턴이 잘 구현되어 있습니다.
      prevBuffer 초기값 -1이 실제 color 값과 충돌하지 않는지 확인해주세요."
```

→ **프로젝트 컨텍스트를 이해한** 정확한 리뷰, 실질적인 피드백

---

## 팀 전체에 미치는 효과

### 현재: 컨텍스트 공유

```
팀원 A: Claude Code로 게임 로직 구현 → CLAUDE.md 컨텍스트 공유
팀원 B: Claude Code로 렌더링 개선   → 동일한 CLAUDE.md 기반 작업
팀원 C: Claude Code로 버그 수정     → AGENTS.md에서 구조 즉시 파악

→ 모든 팀원의 AI가 동일한 프로젝트 이해를 가짐
→ 일관된 코드 스타일, 일관된 아키텍처 결정
```

### 미래: 코드 고고학

```
6개월 후 신규 팀원 D:
  1. CLAUDE.md 읽기 → 프로젝트 규칙 이해 (5분)
  2. AGENTS.md 읽기 → 코드베이스 구조 파악 (10분)
  3. .claude/docs/ 탐색 → 각 기능의 의사결정 이력 확인

→ 온보딩 시간 대폭 단축
→ "왜?"에 대한 답이 항상 존재
```

### 지속: CodeRabbit 리뷰 품질

```
모든 PR에서:
  - CLAUDE.md 컨벤션 기반 리뷰 → false positive 감소
  - AGENTS.md 아키텍처 기반 리뷰 → 구조적 문제 조기 발견
  - 프로젝트 특화 제안 → 실질적 개선점만 제시

→ 리뷰 noise 감소, 의미 있는 피드백만 수신
```

---

## Quick Start: 우리 프로젝트에 적용하기

### Step 1: CLAUDE.md 작성 (10분)

프로젝트 루트에 `CLAUDE.md`를 생성합니다:

```markdown
# Project: our-awesome-service

## 기술 스택
- Kotlin 1.9 / JDK 21
- 외부 라이브러리: (사용하는 것만 기재)

## 코드 컨벤션
- 파일당 150줄 이내 권장
- public 함수에 KDoc 작성
- 매직 넘버는 companion object 상수로 추출

## 테스트 정책
- 핵심 로직: 단위 테스트 필수
- UI/IO: 수동 테스트

## 브랜치 전략
- feature/{jira-ticket}-{설명}
- 커밋: conventional commits (feat:, fix:, refactor:)
```

### Step 2: AGENTS.md 생성

Claude Code에게 요청합니다:

```
"현재 프로젝트의 구조와 아키텍처를 AGENTS.md에 문서화해줘"
```

### Step 3: 작업 시 3-File 자동 생성

Claude Code로 작업할 때:

```
"PL-001 테트리스 게임 구현해줘. .claude/docs/에 작업 기록 남겨줘"
```

---

## FAQ

### Q: CLAUDE.md는 누가 관리하나요?
테크 리드가 초기 작성, 팀원 누구나 PR로 업데이트합니다. 컨벤션 변경 시 함께 업데이트합니다.

### Q: AGENTS.md는 수동으로 작성하나요?
Claude Code가 자동 생성·업데이트합니다. 필요시 수동 보강 가능합니다.

### Q: .claude/docs/는 git에 커밋하나요?
**네, 반드시 커밋합니다.** 이것이 코드 고고학의 핵심입니다. PR에 포함시키면 리뷰어도 의사결정 맥락을 볼 수 있습니다.

### Q: 모든 작업에 3-File을 만들어야 하나요?
아닙니다. **3단계 이상의 복잡한 작업**에만 적용합니다. 단순 버그 수정이나 1-2줄 변경에는 불필요합니다.

### Q: CodeRabbit 없이도 효과가 있나요?
네. CLAUDE.md와 .claude/docs/만으로도 컨텍스트 공유·코드 고고학 효과가 있습니다. CodeRabbit은 추가 보너스입니다.

### Q: 기존 프로젝트에도 적용 가능한가요?
네. CLAUDE.md를 먼저 추가하고, 새 기능 개발부터 3-File을 적용하면 됩니다. 기존 코드 소급 문서화는 불필요합니다.

---

## 이 프로젝트의 예시 파일

| 파일 | 설명 |
|------|------|
| [CLAUDE.md](CLAUDE.md) | 3-File Pattern 워크플로우 정의 |
| [AGENTS.md](AGENTS.md) | AI가 자동 생성한 아키텍처 문서 예시 |
| [.claude/docs/PL-001-tetris/](.claude/docs/PL-001-tetris/) | 완료된 기능의 3-File 예시 (tasks, findings, progress) |
| [src/tetris/](src/tetris/) | 순수 Kotlin 콘솔 테트리스 소스 코드 |