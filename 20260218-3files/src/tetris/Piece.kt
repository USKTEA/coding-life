package tetris

/**
 * 7종 표준 테트로미노 정의 및 SRS(Super Rotation System) 회전.
 *
 * Why SRS?
 * → 단순 90도 회전은 벽 근처에서 회전 불가 → UX 나쁨.
 * → SRS + 월킥이 표준이고, 오프셋 테이블만 추가하면 되어 구현 비용 낮음.
 * → 상세: .claude/docs/PL-001-tetris/findings.md "회전 시스템" 결정 참고
 */

enum class PieceType(val color: Int, val shapes: Array<Array<IntArray>>) {
    // 각 피스는 4개의 회전 상태를 가짐 (0, R, 2, L)
    // color: ANSI 색상 코드
    I(36, arrayOf(
        arrayOf(intArrayOf(0,0,0,0), intArrayOf(1,1,1,1), intArrayOf(0,0,0,0), intArrayOf(0,0,0,0)),
        arrayOf(intArrayOf(0,0,1,0), intArrayOf(0,0,1,0), intArrayOf(0,0,1,0), intArrayOf(0,0,1,0)),
        arrayOf(intArrayOf(0,0,0,0), intArrayOf(0,0,0,0), intArrayOf(1,1,1,1), intArrayOf(0,0,0,0)),
        arrayOf(intArrayOf(0,1,0,0), intArrayOf(0,1,0,0), intArrayOf(0,1,0,0), intArrayOf(0,1,0,0)),
    )),
    O(33, arrayOf(
        arrayOf(intArrayOf(1,1), intArrayOf(1,1)),
        arrayOf(intArrayOf(1,1), intArrayOf(1,1)),
        arrayOf(intArrayOf(1,1), intArrayOf(1,1)),
        arrayOf(intArrayOf(1,1), intArrayOf(1,1)),
    )),
    T(35, arrayOf(
        arrayOf(intArrayOf(0,1,0), intArrayOf(1,1,1), intArrayOf(0,0,0)),
        arrayOf(intArrayOf(0,1,0), intArrayOf(0,1,1), intArrayOf(0,1,0)),
        arrayOf(intArrayOf(0,0,0), intArrayOf(1,1,1), intArrayOf(0,1,0)),
        arrayOf(intArrayOf(0,1,0), intArrayOf(1,1,0), intArrayOf(0,1,0)),
    )),
    S(32, arrayOf(
        arrayOf(intArrayOf(0,1,1), intArrayOf(1,1,0), intArrayOf(0,0,0)),
        arrayOf(intArrayOf(0,1,0), intArrayOf(0,1,1), intArrayOf(0,0,1)),
        arrayOf(intArrayOf(0,0,0), intArrayOf(0,1,1), intArrayOf(1,1,0)),
        arrayOf(intArrayOf(1,0,0), intArrayOf(1,1,0), intArrayOf(0,1,0)),
    )),
    Z(31, arrayOf(
        arrayOf(intArrayOf(1,1,0), intArrayOf(0,1,1), intArrayOf(0,0,0)),
        arrayOf(intArrayOf(0,0,1), intArrayOf(0,1,1), intArrayOf(0,1,0)),
        arrayOf(intArrayOf(0,0,0), intArrayOf(1,1,0), intArrayOf(0,1,1)),
        arrayOf(intArrayOf(0,1,0), intArrayOf(1,1,0), intArrayOf(1,0,0)),
    )),
    L(37, arrayOf(
        arrayOf(intArrayOf(0,0,1), intArrayOf(1,1,1), intArrayOf(0,0,0)),
        arrayOf(intArrayOf(0,1,0), intArrayOf(0,1,0), intArrayOf(0,1,1)),
        arrayOf(intArrayOf(0,0,0), intArrayOf(1,1,1), intArrayOf(1,0,0)),
        arrayOf(intArrayOf(1,1,0), intArrayOf(0,1,0), intArrayOf(0,1,0)),
    )),
    J(34, arrayOf(
        arrayOf(intArrayOf(1,0,0), intArrayOf(1,1,1), intArrayOf(0,0,0)),
        arrayOf(intArrayOf(0,1,1), intArrayOf(0,1,0), intArrayOf(0,1,0)),
        arrayOf(intArrayOf(0,0,0), intArrayOf(1,1,1), intArrayOf(0,0,1)),
        arrayOf(intArrayOf(0,1,0), intArrayOf(0,1,0), intArrayOf(1,1,0)),
    ));
}

data class Piece(
    val type: PieceType,
    var x: Int,
    var y: Int,
    var rotation: Int = 0,
) {
    val shape: Array<IntArray>
        get() = type.shapes[rotation]

    val size: Int
        get() = shape.size

    fun rotatedShape(newRotation: Int): Array<IntArray> =
        type.shapes[newRotation.mod(4)]

    fun copy(): Piece = Piece(type, x, y, rotation)

    companion object {
        fun random(): Piece {
            val type = PieceType.entries.random()
            val startX = (Board.WIDTH - type.shapes[0][0].size) / 2
            return Piece(type, startX, 0)
        }

        /**
         * 월킥 오프셋 테이블 (SRS 표준).
         *
         * 기본 회전 위치가 충돌하면 이 오프셋들을 순서대로 시도.
         * I 피스는 별도 테이블 사용.
         *
         * Why 월킥?
         * → 월킥 없이는 벽 근처에서 회전 불가 → 유저 불만 예상.
         * → 상세: .claude/docs/PL-001-tetris/findings.md "월킥 구현" 참고
         */
        val WALL_KICK_OFFSETS: Map<String, Array<Pair<Int, Int>>> = mapOf(
            // (from rotation) → (to rotation): offsets to try
            "0>1" to arrayOf(Pair(0,0), Pair(-1,0), Pair(-1,-1), Pair(0,2), Pair(-1,2)),
            "1>2" to arrayOf(Pair(0,0), Pair(1,0), Pair(1,1), Pair(0,-2), Pair(1,-2)),
            "2>3" to arrayOf(Pair(0,0), Pair(1,0), Pair(1,-1), Pair(0,2), Pair(1,2)),
            "3>0" to arrayOf(Pair(0,0), Pair(-1,0), Pair(-1,1), Pair(0,-2), Pair(-1,-2)),
        )

        val I_WALL_KICK_OFFSETS: Map<String, Array<Pair<Int, Int>>> = mapOf(
            "0>1" to arrayOf(Pair(0,0), Pair(-2,0), Pair(1,0), Pair(-2,1), Pair(1,-2)),
            "1>2" to arrayOf(Pair(0,0), Pair(-1,0), Pair(2,0), Pair(-1,-2), Pair(2,1)),
            "2>3" to arrayOf(Pair(0,0), Pair(2,0), Pair(-1,0), Pair(2,-1), Pair(-1,2)),
            "3>0" to arrayOf(Pair(0,0), Pair(1,0), Pair(-2,0), Pair(1,2), Pair(-2,-1)),
        )
    }
}
