package tetris

/**
 * 점수 계산 및 레벨 시스템.
 *
 * 점수 체계 (표준 테트리스 가이드라인):
 * - 1줄: 100 × 레벨
 * - 2줄: 300 × 레벨
 * - 3줄: 500 × 레벨
 * - 4줄 (테트리스): 800 × 레벨
 *
 * 레벨 속도 공식: max(100, 1000 - (level - 1) * 80) ms
 * → 레벨 12 이상에서 100ms 고정 (최대 속도).
 * → 상세: .claude/docs/PL-001-tetris/findings.md "게임 속도" 참고
 */
class Score {
    var score: Int = 0
        private set

    var level: Int = 1
        private set

    var totalLinesCleared: Int = 0
        private set

    private val lineScores = intArrayOf(0, 100, 300, 500, 800)

    fun addLineClear(lines: Int) {
        if (lines in 1..4) {
            score += lineScores[lines] * level
            totalLinesCleared += lines

            // 10줄마다 레벨 업
            val newLevel = totalLinesCleared / 10 + 1
            if (newLevel > level) {
                level = newLevel
            }
        }
    }

    fun addSoftDrop(rows: Int) {
        score += rows
    }

    fun addHardDrop(rows: Int) {
        score += rows * 2
    }

    /** 현재 레벨의 자동 낙하 간격 (ms) */
    val dropInterval: Long
        get() = maxOf(100L, 1000L - (level - 1) * 80L)
}
