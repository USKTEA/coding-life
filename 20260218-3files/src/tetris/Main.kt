package tetris

/**
 * 콘솔 테트리스 진입점.
 *
 * 터미널 raw 모드 설정/복원:
 * → stty -icanon min 1 -echo: 즉시 키 입력 + 에코 끄기
 * → stty sane: 종료 시 터미널 복원
 *
 * Why shutdown hook?
 * → Ctrl+C 종료 시에도 반드시 터미널을 복원해야 함.
 * → 상세: .claude/docs/PL-001-tetris/findings.md Issue #3 참고
 */
fun main() {
    // 터미널 raw 모드 설정
    setRawMode()

    // 종료 시 터미널 복원 보장 (정상 종료 + Ctrl+C + 예외)
    Runtime.getRuntime().addShutdownHook(Thread {
        restoreTerminal()
    })

    try {
        val game = Game()
        game.run()
    } finally {
        restoreTerminal()
    }
}

private fun setRawMode() {
    Runtime.getRuntime().exec(arrayOf("/bin/sh", "-c", "stty -icanon min 1 -echo < /dev/tty"))
        .waitFor()
}

private fun restoreTerminal() {
    Runtime.getRuntime().exec(arrayOf("/bin/sh", "-c", "stty sane < /dev/tty"))
        .waitFor()
}
