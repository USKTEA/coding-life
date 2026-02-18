package tetris

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 터미널 raw 모드 키보드 입력 처리.
 *
 * Why raw 모드 + 별도 스레드?
 * → System.in.read()는 blocking call.
 * → 메인 스레드에서 호출하면 게임 루프 정지.
 * → 별도 daemon 스레드에서 읽고 큐에 적재.
 * → 상세: .claude/docs/PL-001-tetris/findings.md "입력 처리" 결정 참고
 *
 * 방향키 escape sequence:
 * → ↑: ESC [ A (27, 91, 65)
 * → ↓: ESC [ B (27, 91, 66)
 * → →: ESC [ C (27, 91, 67)
 * → ←: ESC [ D (27, 91, 68)
 */

enum class Action {
    LEFT, RIGHT, ROTATE, SOFT_DROP, HARD_DROP, QUIT, NONE
}

class Input {
    private val actionQueue = ConcurrentLinkedQueue<Action>()
    @Volatile private var running = true

    fun start() {
        val thread = Thread {
            while (running) {
                try {
                    val b = System.`in`.read()
                    if (b == -1) continue

                    val action = when (b) {
                        27 -> parseEscapeSequence()  // ESC
                        32 -> Action.HARD_DROP       // Space
                        113 -> Action.QUIT           // 'q'
                        81 -> Action.QUIT            // 'Q'
                        else -> Action.NONE
                    }

                    if (action != Action.NONE) {
                        actionQueue.add(action)
                    }
                } catch (e: Exception) {
                    // 입력 스트림 종료 시 무시
                }
            }
        }
        thread.isDaemon = true
        thread.start()
    }

    private fun parseEscapeSequence(): Action {
        val b2 = System.`in`.read()
        if (b2 != 91) return Action.NONE  // '[' 아니면 무시

        return when (System.`in`.read()) {
            65 -> Action.ROTATE      // ↑ A
            66 -> Action.SOFT_DROP   // ↓ B
            67 -> Action.RIGHT       // → C
            68 -> Action.LEFT        // ← D
            else -> Action.NONE
        }
    }

    /**
     * 큐에 쌓인 입력을 모두 꺼내고 마지막 것만 반환.
     *
     * Why 마지막만?
     * → 빠른 키 입력 시 프레임 사이에 여러 입력이 쌓임.
     * → 모두 처리하면 한 프레임에 여러 칸 이동 → UX 나쁨.
     * → 상세: .claude/docs/PL-001-tetris/tasks.md Errors Encountered #4 참고
     */
    fun poll(): Action {
        var last = Action.NONE
        while (true) {
            val action = actionQueue.poll() ?: break
            last = action
        }
        return last
    }

    fun stop() {
        running = false
    }
}
