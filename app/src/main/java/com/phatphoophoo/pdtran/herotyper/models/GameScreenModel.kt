package com.phatphoophoo.pdtran.herotyper.models

import com.phatphoophoo.pdtran.herotyper.objects.BulletObject
import com.phatphoophoo.pdtran.herotyper.objects.EnemyObject
import com.phatphoophoo.pdtran.herotyper.objects.Player

data class GameScreenModel(val difficulty: GAME_DIFFICULTY = GAME_DIFFICULTY.EASY) {
    // Game properties
    var score : Int = 0
    var lives : Int = 3
    val timeLimit : Int = 30000 // MS
    var curTime : Int = 0 // MS
    var pause : Boolean = false

    var curGoalWord : String = ""
    var curTypedIndex : String = ""
    var curTypedState : TypedState = TypedState.DEFAULT

    // Objects
    var player : Player = Player(Pair(0f,0f))
    var bullets : List<BulletObject> = emptyList()
    var enemies : List<EnemyObject> = emptyList()
}

// Default = no color, starts like this
// Success = green
// Failed = red
enum class TypedState {
    FAILED, SUCCESS, DEFAULT
}

enum class GAME_DIFFICULTY {
    EASY, MEDIUM, HARD
}