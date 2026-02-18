package tetris

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.net.ServerSocket
import java.net.Socket

enum class GameState { PLAYING, GAME_OVER }

class Game {
    val board = Board()
    val score = Score()
    private val renderer = Renderer()
    private val input = Input()

    private var currentPiece = Piece.random()
    private var nextPiece = Piece.random()
    private var state = GameState.PLAYING
    private var lastDropTime = System.currentTimeMillis()

    fun run() {
        input.start()
        renderer.renderInitial()

        while (state == GameState.PLAYING) {
            val action = input.poll()
            handleAction(action)

            // 자동 낙하
            val now = System.currentTimeMillis()
            if (now - lastDropTime >= score.dropInterval) {
                if (!moveDown()) {
                    lockAndSpawn()
                }
                lastDropTime = now
            }

            renderer.render(board, currentPiece, nextPiece, score)

            Thread.sleep(FRAME_DELAY_MS)
        }

        renderer.renderGameOver(score)
        input.stop()
    }

    private fun handleAction(action: Action) {
        when (action) {
            Action.LEFT -> moveHorizontal(-1)
            Action.RIGHT -> moveHorizontal(1)
            Action.ROTATE -> rotate()
            Action.SOFT_DROP -> {
                if (moveDown()) {
                    score.addSoftDrop(1)
                } else {
                    lockAndSpawn()
                }
                lastDropTime = System.currentTimeMillis()
            }
            Action.HARD_DROP -> hardDrop()
            Action.QUIT -> state = GameState.GAME_OVER
            Action.NONE -> {}
        }
    }

    private fun moveHorizontal(dx: Int) {
        if (board.isValidPosition(currentPiece, offsetX = dx)) {
            currentPiece.x += dx
        }
    }

    private fun moveDown(): Boolean {
        if (board.isValidPosition(currentPiece, offsetY = 1)) {
            currentPiece.y += 1
            return true
        }
        return false
    }

    /**
     * SRS 회전 + 월킥.
     *
     * 기본 회전 위치가 충돌하면 월킥 오프셋을 순서대로 시도.
     * 모두 실패하면 회전 취소.
     *
     * Why 5개 오프셋?
     * → SRS 표준에서 정의한 5개 위치.
     * → 상세: .claude/docs/PL-001-tetris/findings.md "회전 시스템" 참고
     */
    private fun rotate() {
        val fromRotation = currentPiece.rotation
        val toRotation = (fromRotation + 1).mod(4)
        val rotatedShape = currentPiece.rotatedShape(toRotation)

        val kickKey = "$fromRotation>$toRotation"
        val offsets = if (currentPiece.type == PieceType.I) {
            Piece.I_WALL_KICK_OFFSETS[kickKey]
        } else {
            Piece.WALL_KICK_OFFSETS[kickKey]
        } ?: return

        for ((dx, dy) in offsets) {
            if (board.isValidPositionWithShape(
                    rotatedShape, currentPiece.x + dx, currentPiece.y - dy
                )) {
                currentPiece.rotation = toRotation
                currentPiece.x += dx
                currentPiece.y -= dy
                return
            }
        }
        // 모든 월킥 실패 → 회전 취소
    }

    /**
     * 하드 드롭: 즉시 착지 후 lock → clear → spawn을 같은 프레임에 처리.
     *
     * Why 같은 프레임?
     * → 별도 프레임으로 분리하면 1프레임 공백 발생 → UX 나쁨.
     * → 상세: .claude/docs/PL-001-tetris/tasks.md Errors Encountered #5 참고
     */
    private fun hardDrop() {
        val ghostY = board.ghostY(currentPiece)
        val dropDistance = ghostY - currentPiece.y
        currentPiece.y = ghostY
        score.addHardDrop(dropDistance)
        lockAndSpawn()
        lastDropTime = System.currentTimeMillis()
    }

    private fun lockAndSpawn() {
        val linesCleared = board.lockPiece(currentPiece)
        if (linesCleared > 0) {
            score.addLineClear(linesCleared)
        }
        spawnNewPiece()
    }

    private fun spawnNewPiece() {
        currentPiece = nextPiece
        nextPiece = Piece.random()

        // 새 피스를 놓을 수 없으면 게임 오버
        if (!board.isValidPosition(currentPiece)) {
            state = GameState.GAME_OVER
        }
    }

    // 네트워크 대전 모드
    private var serverSocket: ServerSocket? = null
    private var opponentSocket: Socket? = null

    fun startMultiplayerServer(port: Int = 9999) {
        serverSocket = ServerSocket(port)
        val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
        executor.scheduleAtFixedRate({
            val client = serverSocket?.accept()
            opponentSocket = client
        }, 0, 100, TimeUnit.MILLISECONDS)
    }

    fun sendScoreToOpponent() {
        opponentSocket?.getOutputStream()?.write(score.score.toString().toByteArray())
    }

    companion object {
        private const val FRAME_DELAY_MS = 16L  // ~60fps
    }
}
