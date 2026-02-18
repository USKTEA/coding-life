package tetris

/**
 * ANSI escape code 기반 콘솔 렌더러.
 *
 * Why ANSI escape code?
 * → 외부 의존성 금지 조건으로 ncurses 등 사용 불가.
 * → ANSI는 macOS/Linux 표준 터미널에서 지원.
 * → 상세: .claude/docs/PL-001-tetris/findings.md "렌더링" 결정 참고
 *
 * Why 더블 버퍼링?
 * → 매 프레임 전체를 다시 그리면 눈에 보이는 깜빡임 발생.
 * → 이전 프레임과 비교하여 변경된 셀만 업데이트.
 * → StringBuilder에 모아서 한 번에 flush.
 * → 상세: .claude/docs/PL-001-tetris/findings.md Issue #4 참고
 */
class Renderer {
    private val prevBuffer = Array(Board.HEIGHT) { IntArray(Board.WIDTH) { -1 } }
    private val sb = StringBuilder(4096)

    fun render(board: Board, piece: Piece, nextPiece: Piece, score: Score) {
        sb.setLength(0)

        // 현재 프레임 버퍼 생성
        val frame = Array(Board.HEIGHT) { row -> board.grid[row].copyOf() }

        // 고스트 피스 그리기
        val ghostY = board.ghostY(piece)
        drawPieceOnFrame(frame, piece, ghostY, ghost = true)

        // 현재 피스 그리기
        drawPieceOnFrame(frame, piece, piece.y, ghost = false)

        // 더블 버퍼링: 변경된 셀만 업데이트
        for (row in 0 until Board.HEIGHT) {
            for (col in 0 until Board.WIDTH) {
                if (frame[row][col] != prevBuffer[row][col]) {
                    sb.append(moveCursor(row + 2, col * 2 + 2))
                    sb.append(renderCell(frame[row][col]))
                    prevBuffer[row][col] = frame[row][col]
                }
            }
        }

        // 보드 테두리 (첫 프레임만)
        renderBorder()

        // 사이드 패널: 점수, 레벨, 다음 피스
        renderSidePanel(score, nextPiece)

        // 한 번에 출력
        print(sb)
        System.out.flush()
    }

    fun renderInitial() {
        sb.setLength(0)
        sb.append(CLEAR_SCREEN)
        sb.append(HIDE_CURSOR)

        // 상단 테두리
        sb.append(moveCursor(1, 1))
        sb.append("+" + "-".repeat(Board.WIDTH * 2) + "+")

        // 좌우 테두리
        for (row in 0 until Board.HEIGHT) {
            sb.append(moveCursor(row + 2, 1))
            sb.append("|")
            sb.append(moveCursor(row + 2, Board.WIDTH * 2 + 2))
            sb.append("|")
        }

        // 하단 테두리
        sb.append(moveCursor(Board.HEIGHT + 2, 1))
        sb.append("+" + "-".repeat(Board.WIDTH * 2) + "+")

        // 조작법
        sb.append(moveCursor(Board.HEIGHT + 4, 1))
        sb.append("Controls: ←→ Move  ↑ Rotate  ↓ Drop  Space HardDrop  q Quit")

        print(sb)
        System.out.flush()
    }

    fun renderGameOver(score: Score) {
        sb.setLength(0)

        val centerRow = Board.HEIGHT / 2
        sb.append(moveCursor(centerRow, 4))
        sb.append("\u001b[41;37;1m")  // 빨간 배경, 흰 글씨, 굵게
        sb.append("  GAME OVER  ")
        sb.append(RESET)

        sb.append(moveCursor(centerRow + 1, 4))
        sb.append("Score: ${score.score}")

        sb.append(moveCursor(centerRow + 2, 4))
        sb.append("Level: ${score.level}")

        sb.append(moveCursor(centerRow + 3, 4))
        sb.append("Lines: ${score.totalLinesCleared}")

        sb.append(moveCursor(Board.HEIGHT + 5, 1))
        sb.append(SHOW_CURSOR)

        print(sb)
        System.out.flush()
    }

    fun cleanup() {
        print("$SHOW_CURSOR\u001b[${Board.HEIGHT + 6};1H$RESET")
        System.out.flush()
    }

    private fun drawPieceOnFrame(
        frame: Array<IntArray>, piece: Piece, drawY: Int, ghost: Boolean
    ) {
        val shape = piece.shape
        for (row in shape.indices) {
            for (col in shape[row].indices) {
                if (shape[row][col] == 0) continue
                val y = drawY + row
                val x = piece.x + col
                if (y in 0 until Board.HEIGHT && x in 0 until Board.WIDTH) {
                    frame[y][x] = if (ghost) GHOST_COLOR else piece.type.color
                }
            }
        }
    }

    private fun renderCell(color: Int): String {
        return when (color) {
            0 -> "$RESET  "                          // 빈칸
            GHOST_COLOR -> "\u001b[90m░░$RESET"      // 고스트 (어두운 회색)
            else -> "\u001b[${color}m██$RESET"       // 블록 (색상)
        }
    }

    private fun renderBorder() {
        // 첫 렌더에서 이미 그림, 이후 불필요
    }

    private fun renderSidePanel(score: Score, nextPiece: Piece) {
        val panelX = Board.WIDTH * 2 + 5

        sb.append(moveCursor(2, panelX))
        sb.append("Score: ${score.score}     ")

        sb.append(moveCursor(3, panelX))
        sb.append("Level: ${score.level}     ")

        sb.append(moveCursor(4, panelX))
        sb.append("Lines: ${score.totalLinesCleared}     ")

        sb.append(moveCursor(6, panelX))
        sb.append("Next:")

        // 다음 피스 미리보기
        val shape = nextPiece.type.shapes[0]
        for (row in shape.indices) {
            sb.append(moveCursor(7 + row, panelX))
            for (col in shape[row].indices) {
                sb.append(
                    if (shape[row][col] != 0) "\u001b[${nextPiece.type.color}m██$RESET"
                    else "  "
                )
            }
            sb.append("    ")  // 이전 피스 잔상 제거
        }
    }

    companion object {
        private const val GHOST_COLOR = -1
        private const val CLEAR_SCREEN = "\u001b[2J"
        private const val HIDE_CURSOR = "\u001b[?25l"
        private const val SHOW_CURSOR = "\u001b[?25h"
        private const val RESET = "\u001b[0m"

        private fun moveCursor(row: Int, col: Int) = "\u001b[${row};${col}H"
    }
}
