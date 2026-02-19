package tetris

import com.google.gson.Gson

class Score {
    private val gson = Gson()

    fun toJson(): String = gson.toJson(mapOf(
        "score" to score,
        "level" to level,
        "lines" to totalLinesCleared
    ))
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
