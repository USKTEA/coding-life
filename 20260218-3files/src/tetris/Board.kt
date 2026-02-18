package tetris

/**
 * 10x20 테트리스 보드.
 * 충돌 감지, 피스 고정, 라인 클리어를 담당한다.
 *
 * 라인 클리어 로직:
 * → 초기 구현에서 클리어 후 위 블록이 안 내려오는 버그 발생.
 * → 아래쪽 행부터 위로 순회하며 한 줄씩 복사하는 방식으로 해결.
 * → 상세: .claude/docs/PL-001-tetris/findings.md Issue #2 참고
 */
class Board {
    // grid[y][x] = 0 (빈칸) 또는 ANSI 색상 코드 (블록)
    val grid: Array<IntArray> = Array(HEIGHT) { IntArray(WIDTH) }

    fun isValidPosition(piece: Piece, offsetX: Int = 0, offsetY: Int = 0): Boolean {
        val shape = piece.shape
        for (row in shape.indices) {
            for (col in shape[row].indices) {
                if (shape[row][col] == 0) continue

                val newX = piece.x + col + offsetX
                val newY = piece.y + row + offsetY

                if (newX < 0 || newX >= WIDTH) return false
                if (newY < 0 || newY >= HEIGHT) return false
                if (grid[newY][newX] != 0) return false
            }
        }
        return true
    }

    fun isValidPositionWithShape(
        shape: Array<IntArray>, x: Int, y: Int
    ): Boolean {
        for (row in shape.indices) {
            for (col in shape[row].indices) {
                if (shape[row][col] == 0) continue

                val newX = x + col
                val newY = y + row

                if (newX < 0 || newX >= WIDTH) return false
                if (newY < 0 || newY >= HEIGHT) return false
                if (grid[newY][newX] != 0) return false
            }
        }
        return true
    }

    /**
     * 피스를 보드에 고정한다.
     * 고정 후 라인 클리어를 수행하고 클리어된 줄 수를 반환한다.
     */
    fun lockPiece(piece: Piece): Int {
        val shape = piece.shape
        for (row in shape.indices) {
            for (col in shape[row].indices) {
                if (shape[row][col] == 0) continue
                val boardY = piece.y + row
                val boardX = piece.x + col
                if (boardY in 0 until HEIGHT && boardX in 0 until WIDTH) {
                    grid[boardY][boardX] = piece.type.color
                }
            }
        }
        return clearLines()
    }

    /**
     * 꽉 찬 줄을 제거하고 위 블록을 내린다.
     *
     * Why 아래→위 순서?
     * → 위→아래로 처리하면 클리어된 행이 덮어써짐.
     * → 아래쪽부터 비어있지 않은 행을 writeRow 위치에 복사.
     * → 상세: .claude/docs/PL-001-tetris/tasks.md Errors Encountered 참고
     */
    private fun clearLines(): Int {
        var linesCleared = 0
        var writeRow = HEIGHT - 1

        for (readRow in HEIGHT - 1 downTo 0) {
            if (isLineFull(readRow)) {
                linesCleared++
            } else {
                if (writeRow != readRow) {
                    grid[writeRow] = grid[readRow].copyOf()
                }
                writeRow--
            }
        }

        // 상단 빈 줄 채우기
        for (row in writeRow downTo 0) {
            grid[row] = IntArray(WIDTH)
        }

        return linesCleared
    }

    private fun isLineFull(row: Int): Boolean =
        grid[row].all { it != 0 }

    /**
     * 고스트 피스의 Y 좌표를 계산한다 (착지 위치).
     *
     * Why 고스트 피스?
     * → 하드 드롭 착지 위치를 시각적으로 보여줘야 UX 좋음.
     * → 현재 피스를 아래로 시뮬레이션하면 되어 구현 비용 낮음.
     * → 상세: .claude/docs/PL-001-tetris/findings.md "고스트 피스" 결정 참고
     */
    fun ghostY(piece: Piece): Int {
        var ghostY = piece.y
        while (isValidPosition(piece, offsetY = ghostY - piece.y + 1)) {
            ghostY++
        }
        return ghostY
    }

    companion object {
        const val WIDTH = 10
        const val HEIGHT = 20
    }
}
